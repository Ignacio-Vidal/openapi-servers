---
name: quarkus
description: Provides informaiton about Quarkus and how to access the MCP server locally
---

When we talk about the local application in the context of Quarkus, the application is executed in quarkus-dev mode which exposes an MCP server in the url http://localhost:8080/q/dev-mcp.
This server provides information about the local application such as the application name, version, and the list of available endpoints. It also allows you to interact with the application by sending requests to the endpoints and receiving responses.


1. **Start with by checking the MCP server is running locally**: Do an http request to local mcp server to validate it's working
2. **Getting the list of tools avaialable*: Make a request to the endpoint http://localhost:8080/q/dev-mcp/tools to get the list of tools available in the local application. This will return a JSON response with the list of tools and their details.
