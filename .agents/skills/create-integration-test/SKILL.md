---
name: create-integration-test
description: Generates a Kotlin integration test for a REST controller/endpoint in this hexagonal architecture project (Testcontainers, Spring MockMvc, JWT security). Use this skill whenever the user wants to create, add, or scaffold integration tests for a controller or endpoint — even if they just say "test this endpoint" or "add IT for this resource".
argument-hint: <ControllerName> [--context=<name>]
---

# Create Integration Test

## Arguments
$ARGUMENTS

Expected format:
```
<ControllerName> [--context=<contextName>]
```
- `<ControllerName>` — PascalCase controller or model name, e.g. `MedicatieController` or `Medicatie`
- `--context=<name>` — package subfolder

**If `<ControllerName>` is missing, ask the user.**

---

## Execution Steps

1. **Parse arguments.**
2. **Locate and read the controller** to understand the endpoints, HTTP methods, request/response bodies, and URL paths.
3. **Identify the repository** for the model. Autowire it in the test so you can insert test data directly — this is often simpler and more readable than creating data through POST requests.
4. **Generate the IT file.** Cover the happy path for each endpoint, error/edge cases (404, validation), cross-user isolation, and 401 for each HTTP method.
5. **Write** the test file at `src/it/kotlin/{BASE_PATH}/{context}/{Model}IT.kt`.

---

## Conventions

### Test base class
All integration tests extend `AbstractIT()` — it bootstraps the Spring context with Testcontainers. Never configure Testcontainers manually.

### Dependency injection
Inject dependencies (MockMvc, repositories) via the constructor as `val` parameters annotated with `@param:Autowired`. Never use `@Autowired lateinit var`.

### HTTP testing with Spring MockMvc
Use Spring's `MockMvc` with the Kotlin DSL (`mockMvc.get`, `mockMvc.post`, etc.) to call endpoints. Annotate the test class with `@AutoConfigureMockMvc`.

### Test data setup
Inject the repository via the constructor and use it to insert test data directly. This avoids coupling test setup to the POST endpoint and makes tests for GET, PUT, and DELETE independent.

### Security
- Authenticated requests: `.with(jwt().jwt { it.subject("00000000-0000-0000-0000-000000000001") })`
- Cross-user requests: `.with(jwt().jwt { it.subject("00000000-0000-0000-0000-000000000002") })`
- Unauthenticated requests: `.with(anonymous())`
- Include one 401 test per HTTP method.

### Assertion library
Use **AssertJ** for assertions outside MockMvc expectations. Inside MockMvc `andExpect` blocks, use the built-in DSL (`status { isOk() }`, `jsonPath`).

---

## Reference

Read `references/endpoint-template.md` for a complete REST endpoint integration test example with repository-based test data setup.
