package com.example;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import io.quarkus.jackson.ObjectMapperCustomizer;
import jakarta.inject.Singleton;

@Singleton
public class JacksonCustomizer implements ObjectMapperCustomizer {

    @Override
    public void customize(ObjectMapper objectMapper) {
        objectMapper.setAnnotationIntrospector(new JacksonAnnotationIntrospector() {
            @Override
            public String findTypeName(AnnotatedClass ac) {
                Class<?> superclass = ac.getRawType().getSuperclass();
                if (superclass != null && superclass.isAnnotationPresent(JsonSubTypes.class)) {
                    // Don't use @JsonTypeName on child classes - rely on parent's @JsonSubTypes mapping
                    return null;
                }
                return super.findTypeName(ac);
            }

            @Override
            public TypeResolverBuilder<?> findTypeResolver(com.fasterxml.jackson.databind.cfg.MapperConfig<?> config,
                                                            AnnotatedClass ac,
                                                            com.fasterxml.jackson.databind.JavaType baseType) {
                Class<?> superclass = ac.getRawType().getSuperclass();
                if (superclass != null && superclass.isAnnotationPresent(JsonSubTypes.class)) {
                    // Don't use @JsonTypeInfo on child classes - only use parent's type resolver
                    return null;
                }
                return super.findTypeResolver(config, ac, baseType);
            }
        });
    }
}
