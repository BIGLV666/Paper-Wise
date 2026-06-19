# Repository Guidelines

## Project Structure & Module Organization

```
PaperWise/
鈹溾攢鈹€ pom.xml                     # Maven build config
鈹溾攢鈹€ src/main/java/org/example/paperwise/
鈹?  鈹溾攢鈹€ PaperWiseApplication.java
鈹?  鈹溾攢鈹€ Advice/                 # AOP aspects (rate limiting, slow-call logging)
鈹?  鈹溾攢鈹€ Config/                 # Security, Redis, CORS, MyBatis-Plus, LangChain4j
鈹?  鈹溾攢鈹€ Controller/             # REST controllers (11 total)
鈹?  鈹溾攢鈹€ Dto/                    # Request / response DTOs
鈹?  鈹溾攢鈹€ entry/                  # JPA / MyBatis entity classes (13 entities)
鈹?  鈹溾攢鈹€ enums/                  # CardType, CardDifficulty, CardMastery
鈹?  鈹溾攢鈹€ Interface/              # Custom annotations (@RateLimit, @LookCount)
鈹?  鈹溾攢鈹€ Mapper/                 # MyBatis Mapper interfaces
鈹?  鈹溾攢鈹€ Service/                # Business logic layer
鈹?  鈹溾攢鈹€ Task/                   # Scheduled tasks (Redis sync, leaderboard)
鈹?  鈹斺攢鈹€ Until/                  # Utility classes (JWT, email, password hashing)
鈹溾攢鈹€ src/main/resources/
鈹?  鈹溾攢鈹€ application.yml         # App config (DB, Redis, JWT, AI providers)
鈹?  鈹溾攢鈹€ Mapper/*.xml            # MyBatis XML mappings
鈹?  鈹溾攢鈹€ Sql/User.sql            # Database DDL
鈹?  鈹斺攢鈹€ logback-spring.xml      # Logging config
鈹斺攢鈹€ src/test/java/              # Unit tests
```

Source code lives under `org.example.paperwise`. Each functional module (cards, favorites, AI chat, review, etc.) spans a Controller鈥揝ervice鈥揗apper鈥搑esource XML quartet. Place entity classes in `entry/`, DTOs in `Dto/`, and utilities in `Until/`.

---

## Build, Test, and Development Commands

| Command | Purpose |
|---------|---------|
| `mvn clean package -DskipTests` | Build the project JAR, skipping tests |
| `mvn spring-boot:run` | Run the application locally (port 8080) |
| `java -jar target/PaperWise-0.0.1-SNAPSHOT.jar` | Run the packaged JAR |
| `mvn test` | Run all unit tests |

The application requires MySQL (port 3306), Redis (port 6379), and optionally Ollama (port 11434) for AI chat features. API docs are served at `/doc.html` when running.

---

## Coding Style & Naming Conventions

- **Language & runtime**: Java 17 with Spring Boot 3.5.x.
- **Packages**: Lowercase, singular 鈥?`entry`, `mapper`, `service`, `until`.
- **Classes**: PascalCase. Controllers end with `Controller`, services with `Service`, mappers with `Mapper`.
- **Methods**: camelCase. Use consistent prefixes like `get*`, `add*`, `update*`, `delete*` for CRUD operations in controllers.
- **SQL XML**: MyBatis XML files live in `resources/Mapper/`, one per entity, named after their Mapper interface.
- **Lombok**: Actively used. Prefer `@Data`, `@Builder`, `@AllArgsConstructor` on entities and DTOs.
- **No auto-formatter** is configured. Maintain consistent 4-space indentation and keep lines readable.
- **Logging**: Use `logback-spring.xml` 鈥?avoid System.out in production code (the MyBatis `StdOutImpl` is development-only).

---

## Testing Guidelines

- **Framework**: Spring Boot Test with JUnit 5 (ships via `spring-boot-starter-test`).
- **Coverage**: The project currently has minimal test coverage. Add unit tests for new services and integration tests for controllers.
- **Test location**: `src/test/java/org/example/paperwise/`. Mirror the main source package structure.
- **Naming**: `{ClassUnderTest}Test.java` (e.g., `CardServiceTest.java`).
- **To run**: `mvn test` executes all tests in the `test/` source root.

---

## Commit & Pull Request Guidelines

- **Commit messages**: Use short, descriptive English or Chinese messages that summarize the change. Current history uses version tags (`1.0`, `0.1.1`) or brief descriptions (`0.1.1-鏈畬鎴愮増鏈琡). Prefer imperative mood: "Add rate-limit annotation", "Fix SM-2 interval calculation".
- **Scope**: Keep commits atomic 鈥?one logical change per commit.
- **Pull requests**: Include a summary of changes, any new dependencies or configuration keys, and screenshots for UI-related changes. Link related issues where applicable.
- The project is private / learning-focused. No conventional-commit or signed-off requirement is enforced.

---

## Security & Configuration Tips

- `application.yml` contains secrets (DB password, JWT secret, email credentials, DashScope API key). Never commit real credentials to version control 鈥?use environment variables or an external config server in production.
- Spring Security is configured to permit all routes; the actual authentication is handled by `LoginInterceptor`, which parses the `Authorization: Bearer <JWT>` header and injects `userid` via `@RequestAttribute`.
- Rate limiting is applied via the custom `@RateLimit` annotation backed by a Redis sliding-window. Use it on any public endpoint that could receive frequent submissions.

---

## Adding a New Feature

1. Add the entity class in `entry/` and update MySQL DDL in `Sql/User.sql`.
2. Create the Mapper interface and corresponding XML in `resources/Mapper/`.
3. Implement business logic in `Service/`.
4. Add the REST endpoint in `Controller/`.
5. Add request/response DTOs in `Dto/` as needed.
6. Write unit tests in `src/test/java/.../`.
7. Expose the new endpoint in the API docs (Knife4j / SpringDoc annotations auto-discover controllers).
