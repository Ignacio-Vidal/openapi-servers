# Gradle build cache is not relocatable for Quarkus modules

## Summary

`QuarkusApplicationModelTask` serialises the resolved application model to
`build/quarkus/application-model/quarkus-app-model*.dat`, and that file contains absolute filesystem
paths. Downstream tasks consume it as an `@InputFile`, so their build-cache keys change whenever the
project is built from a different directory — even with byte-identical sources.

This makes the Gradle build cache effectively non-relocatable for every Quarkus module: cache entries
produced on one checkout path can never be reused from another. It defeats the main purpose of a
shared/remote build cache, where different agents necessarily use different working directories.

`@PathSensitive(PathSensitivity.RELATIVE)` on the consumer does not help — it normalises the input
file's *location*, not its *contents*.

**Status:** reproduced, root-caused, and fixed **three different ways**. Upstream issue filed and one
approach submitted upstream; all three are open as PRs on the fork for comparison. See
[Current state](#current-state).

---

## The mechanism

**Producer** — `io.quarkus.gradle.tasks.QuarkusApplicationModelTask`:

```java
@DisableCachingByDefault(because = "Not cacheable")
public abstract class QuarkusApplicationModelTask extends DefaultTask {
    @OutputFile
    public abstract RegularFileProperty getApplicationModel();
    ...
    ToolingUtils.serializeAppModel(model, getApplicationModel().get().getAsFile().toPath());
```

The task is explicitly **not cacheable** (`@DisableCachingByDefault`). Gradle reports per module:

```
Non-cacheable because Caching has not been enabled for the task [NOT_CACHEABLE]
```

So it re-runs every build and rewrites the `.dat` file. Confirmed: `quarkusGenerateAppModel` was
never `UP-TO-DATE` or `FROM-CACHE` in any observed build.

**Consumer** — `io.quarkus.gradle.tasks.QuarkusGenerateCode` (and `QuarkusBuildTask`):

```java
@InputFile
@PathSensitive(PathSensitivity.RELATIVE)
public abstract RegularFileProperty getApplicationModel();
```

Gradle fingerprints an `@InputFile` by content hash. Because the serialized model contains absolute
paths, the content hash — and therefore the consumer's cache key — is a function of the checkout
directory and of `GRADLE_USER_HOME`.

### Notes on the shape of the problem

- The declaring class for two of the three consumers is `QuarkusBuildTask` (the shared base class of
  `QuarkusBuildDependencies` / `QuarkusBuildCacheableAppParts` / `QuarkusBuild`), so the `@InputFile`
  appears in **two source files**, not three.
- The `.dat` is **plain JSON**, not an opaque binary blob. `ApplicationModelSerializer` defaults to
  JSON; Java Object Serialization is only used when
  `-Dquarkus.bootstrap.application-model.serialization.format=jos` is set.
- The cascade into `compileJava` / `jar` / `test` **does not occur**. `compileJava` had an identical
  cache key across checkouts and hit `FROM-CACHE`, because its inputs are the *generated sources*
  (which are relocatable), not the model file. The damage is contained to the tasks that declare
  `applicationModel` directly.

### Measured path content of the `.dat`

342 absolute path occurrences in a single module's model file:

| Root | Occurrences | Effect |
|---|---|---|
| Checkout directory | 22 | breaks relocation between working directories |
| `GRADLE_USER_HOME` (dependency cache) | 320 | breaks sharing between differently-configured agents |

The checkout-dependent strings live in a small, well-defined set of fields:

```
.app-artifact.module.artifact-sources[].sources[].dir / .dest-dir
.app-artifact.module.artifact-sources[].resources[].dir / .dest-dir
.app-artifact.module.build-dir
.app-artifact.module.module-dir
.app-artifact.module.build-files[]
.app-artifact.resolved-paths[]
.dependencies[].resolved-paths[]
```

---

## Validation

Reproduced locally with two byte-identical checkouts (`diff -r` clean) at different paths, sharing
one local build cache directory. No CI pipeline was needed — separate directories on one machine
model a CI agent's workspace slots exactly.

**Before any fix:**

| Task | Slot A key | Slot B key | |
|---|---|---|---|
| `quarkusGenerateCode` | `570e1324f0b6…` | `e291f541b031…` | DIFFER |
| `quarkusAppPartsBuild` | `ae260f5add20…` | `2809c2cc46ea…` | DIFFER |
| `compileJava` | `7a3af6aaf20e…` | `7a3af6aaf20e…` | same (FROM-CACHE) |

Slot B: `16 actionable tasks: 9 executed, 7 from cache`.

Isolating the cause — comparing every input fingerprint of `quarkusGenerateCode` between the two
slots, three inputs differed: `applicationModel`, `mainClassesDirs`, `projectDescriptor`; 59 were
identical.

Proof that the checkout path alone is responsible: the two `.dat` files are the same size (102905
bytes) with different hashes, but become **byte-identical** once the checkout-path token is
substituted:

```
raw:                19711c81bea3baaa34209df9d4597ea4   c55eb82604ae6d6469ddb770ae0b0aad
path-normalised:    9f5838f46f072bc3f81fe1ec6eeb1cb6   9f5838f46f072bc3f81fe1ec6eeb1cb6
```

Control: rebuilding slot A from clean at its *own* path reproduced its original key exactly
(`570e1324f0b6…`) and hit `FROM-CACHE`. Same path → hit; different path → miss.

---

## Three approaches

The upstream issue lists three possible fixes, from least to most intrusive. All three have now been
implemented and verified, each on its own branch off `main` (`742a8f91b11`) so they can be compared
side by side and either merged alone.

| | (1) Consumer | (2) Producer | (3) Serializer |
|---|---|---|---|
| Fix lives in | the two consuming tasks | `QuarkusApplicationModelTask` | `ApplicationModelSerializer` |
| Scope | Gradle only | Gradle only | Gradle + Maven + CLI + bootstrap |
| Wire format | untouched | untouched | **changed** |
| Cache-relevant fields | enumerated by hand | whole model, minus paths | whole model, minus paths |
| Who benefits | those two tasks | those two tasks | every reader of a `.dat` |
| Unlocks a cacheable producer | no | closer | yes |
| Fork PR | [#1](https://github.com/Ignacio-Vidal/quarkus/pull/1) | [#2](https://github.com/Ignacio-Vidal/quarkus/pull/2) | [#3](https://github.com/Ignacio-Vidal/quarkus/pull/3) |

### (1) Consumer — declare the model's content, not the file

Mark `getApplicationModel()` `@Internal` on both consumers and expose the semantically relevant
parts as normalised inputs:

```java
@Internal
public abstract RegularFileProperty getApplicationModel();

@Classpath
public Provider<List<File>> getApplicationModelDependencies() {
    return ApplicationModelCacheKey.resolvedDependencyFiles(getApplicationModel());
}

@Input
public Provider<Map<String, String>> getApplicationModelProperties() {
    return ApplicationModelCacheKey.relocatableProperties(getApplicationModel());
}
```

`@Classpath` hashes the dependency jars by content, contributing their identity without their
locations. The `@Input` map carries coordinates, dependency flags, workspace module ids, platform
properties, extension capabilities and the artifact-key sets — all as location-independent strings.

**Two implementation traps worth recording:**

1. **Inputs must be lazy.** Computing them eagerly fails the build outright:
   `NoSuchFileException: …/quarkus-app-model-build.dat`, because Gradle queries inputs at
   configuration time, before the producer has run. Returning `Provider`s derived from the file
   property fixes it and preserves the task dependency.
2. **`toString()` on model types is a correctness hazard.** `ExtensionCapabilities` has no
   `toString()` override, so hashing it hashed *identity hash codes* — which change on every JVM run
   (observed: `CapabilityContract@11b141e1` vs `@1c7c0cc9`). That would have made the key unstable
   even within one directory. The contract is now spelled out field by field. `WorkspaceModuleId` has
   the same gap at the interface level.

### (2) Producer — write a relocatable companion file

`QuarkusApplicationModelTask` writes, next to the `.dat`, a `*-relocatable.json` in which absolute
paths are replaced by a token plus the path relative to the corresponding root:

```
/home/ci/slot-3/app/build/classes   ->  ${quarkus.build.dir}/classes
/home/ci/slot-3/contracts/…/x.jar   ->  ${quarkus.root.dir}/contracts/…/x.jar
/home/ci/.gradle/caches/foo.jar     ->  ${quarkus.gradle.user.home}/caches/foo.jar
```

Consumers declare *that* file as their `@InputFile` (`@PathSensitive(NONE)` — only contents matter)
and keep the model itself `@Internal`. Object members are sorted so the rendering does not depend on
map iteration order.

**Why a companion file rather than normalising the `.dat` in place:** the model is handed to the
Quarkus bootstrap as-is. `BeforeTestAction` passes its path to the test JVM via
`SERIALIZED_TEST_APP_MODEL`, where `BootstrapAppModelFactory` deserialises it *outside* the Gradle
plugin. Tokens there would break `test`. Writing a companion keeps the wire format untouched, so
Maven and the CLI — which share `ApplicationModelSerializer` — are unaffected.

### (3) Serializer — make the model itself relocatable

Relocation moved into `ApplicationModelSerializer`, so the `.dat` is checkout-independent for every
tool that reads it. On read, tokens resolve against the *reading* environment — the property neither
other approach can offer, since both leave the file itself checkout-dependent.

Two design points that turned out to be forced:

- **Only root *names* are recorded**, never their locations. The first iteration recorded name→location
  pairs; every model path relocated correctly, keys still differed, and the only absolute strings left
  in the file were the recorded roots themselves.
- **Readers must derive roots unaided.** `BootstrapAppModelFactory` calls bare `deserialize(Path)` in a
  forked JVM. So project/build directories are recovered from the model file's own location, environment
  roots from `maven.repo.local` / `GRADLE_USER_HOME`, and callers who know better pass their own — a
  multi-module Gradle build contributes the root of the build, and `DevMojo` contributes the local
  repository Maven actually resolved from `settings.xml` (and now passes it to the dev JVM so both
  sides agree).

Backward compatibility: a model with no roots entry (older version, or
`quarkus.bootstrap.application-model.relocation.disabled=true`) is read back unchanged, and
`ApplicationModelBuilder.fromMap` ignores unknown keys.

---

## Results

Same two-checkout test, against this project (multi-module, `:quarkus-app` + `:contracts`), with the
locally built `999-SNAPSHOT` plugin:

| Approach | `quarkusGenerateCode` | `quarkusAppPartsBuild` | Slot B totals |
|---|---|---|---|
| before | keys differ | keys differ | 9 executed, 7 from cache |
| (1) consumer | MATCH, FROM-CACHE | MATCH, FROM-CACHE | 7 executed, 9 from cache |
| (2) producer | `a7cf4fd8…` both slots | `42e71012…` both slots | 7 executed, 9 from cache |
| (3) serializer | `779fa551…` both slots | `56604115…` both slots | 7 executed, 9 from cache |

Correctness checks performed for each:

- Generated sources byte-identical between the two checkouts (`diff -r` clean).
- Full `./gradlew build` succeeds from the relocated checkout.
- **Cache soundness** — adding a real dependency (`io.quarkus:quarkus-cache`) still changes the key
  (e.g. `a7cf4fd8…` → `a3fed55b…`) and forces re-execution; removing it restores the original key and
  a clean rebuild in the other slot reproduces it exactly. No real input was dropped.

### The multi-module gap

The first version of approach (2) had three roots (build dir, project dir, Gradle home) and **still
produced differing keys here**. Diffing the two relocatable renderings token by token showed exactly
one difference out of 2790: `:contracts`'s jar. A sibling module's jar is not under `:quarkus-app`'s
project directory — only the *root* directory contains both. Hence a fourth root.

The Quarkus plugin's own `CachingTest` fixture cannot catch this, because there the Quarkus
application **is** the root project, so a sibling's jar already falls under the project dir. A first
attempt at a multi-module test passed even with the fix deleted — it proved nothing. The test now
moves the app into an `app` subproject with a sibling `library`, and fails without the fix.

This project found a real gap that the upstream test suite could not.

### Version skew — the cost of approach (3)

Changing the wire format means mixed versions break:

```
java.lang.IllegalArgumentException: ${quarkus.build.dir}/classes/java/main does not exist
    at io.quarkus.paths.PathTree.ofDirectoryOrArchive
```

Hit here for real: this project pins Quarkus 3.38.3, so the `999-SNAPSHOT` plugin wrote relocation
tokens while the test JVM resolved `quarkus-bootstrap-core:3.38.3` from the BOM and read them
literally. Deserialising the same model with the matching bootstrap resolves every path correctly
(0 unresolved), so the mechanism is sound and the failure is purely version skew.

Consequences: the build plugin and `quarkus-bootstrap-core` must ship together. Two mitigations were
identified but deliberately left to the maintainers, as they concern the supported version floor:
make the reader tolerate unresolved tokens rather than fail, or gate token *writing* behind a
compatibility flag.

This is why the issue ranks (3) last, and why (1) and (2) avoid the risk entirely by not touching the
format.

### Not addressed by any approach

`projectDescriptor` and `mainClassesDirs` on the producer task remain checkout-dependent. That costs
nothing today, since `QuarkusApplicationModelTask` is `@DisableCachingByDefault` anyway, but it would
need attention before the producer itself could be made `@CacheableTask` — the additional prize that
approach (3) otherwise unlocks.

---

## Reproducing locally

Two copies of a real project at different paths sharing one build cache model a CI agent's workspace
slots exactly:

1. Copy the project to `slotA` and `slotB` (excluding `build/`, `.gradle/`).
2. Append to `settings.gradle.kts` in both:
   ```kotlin
   buildCache { local { directory = "/path/to/shared-cache"; isEnabled = true } }
   ```
3. Build slot A, then slot B, with
   `--build-cache --no-configuration-cache -Dorg.gradle.caching.debug=true`.
4. Compare the `Build cache key for task ':quarkus-app:quarkusGenerateCode'` lines.

Two gotchas when testing a local Quarkus build:

- Swap **only the plugin** to `999-SNAPSHOT` and keep the project's own BOM version. Using the
  `999-SNAPSHOT` BOM triggers dependency version conflicts and needs a full local Quarkus build.
- If `independent-projects/bootstrap` changed, `./mvnw install` it **before** building the Gradle
  plugin, or Gradle silently compiles against the stale jar in `~/.m2`.

---

## Current state

The code lives in a separate Quarkus checkout at `~/development/quarkus`, not in this repository.

| | |
|---|---|
| Upstream issue | [quarkusio/quarkus#56214](https://github.com/quarkusio/quarkus/issues/56214) |
| Upstream PR | [quarkusio/quarkus#56215](https://github.com/quarkusio/quarkus/pull/56215) — approach (1) only |
| Fork | `git@github.com:Ignacio-Vidal/quarkus.git` |
| Fork PRs | [#1](https://github.com/Ignacio-Vidal/quarkus/pull/1) (consumer), [#2](https://github.com/Ignacio-Vidal/quarkus/pull/2) (producer), [#3](https://github.com/Ignacio-Vidal/quarkus/pull/3) (serializer) |
| Branches | `gradle-relocatable-app-model-cache-key`, `-provider`, `-serializer` |

Fork PRs target the fork's own `main`, purely to get CI signal and to compare the approaches. They are
not submissions to `quarkusio/quarkus`. Only approach (1) has been submitted upstream.

### Verification status

- `CachingTest` relocation cases (single- and multi-module) pass on all three branches, and fail when
  the fix is reverted.
- `devtools/gradle` test suites pass in full on all three branches.
- Approach (3) additionally passes `independent-projects/bootstrap` (52 core tests plus 5 new
  relocation tests) and `devtools/maven`.
- Formatter and import-order checks (`formatter:validate`, `impsort:check`) pass on every touched
  module.
- Known gap: with approach (3), a full `./gradlew build` including `test` fails against an older
  pinned Quarkus, for the version-skew reason above.

### Note on upstream submission

`quarkusio/quarkus` ships an `AI_POLICY.md`, linked from `CONTRIBUTING.md` and from both the issue and
PR templates. It forbids using bots or agents to open PRs or file issues without human authorship and
responsibility, and requires the submitter to be able to defend every aspect of the contribution under
maintainer review. It also explicitly rejects generated tests that do not validate real behaviour.
Submitting any of these upstream is therefore a human step.
