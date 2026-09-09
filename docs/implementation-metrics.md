# Implementation Metrics

Measured on 2026-09-09 in `D:\R&D\spring-ai-transaction-agent\spring-ai-transaction-agent`.

| Metric | Value |
|---|---:|
| Person-hours | Not measured; single Codex implementation session |
| Files created | 85 files excluding `.git`, build output, `node_modules`, `dist`, `.gradle`, and `.idea` |
| Lines of application code | 2,323 across backend main sources, frontend sources, migrations, and app config |
| Lines of test code | 292 |
| Backend test command | Gradle wrapper jar with `C:\Program Files\Java\jdk-21\bin\java.exe` |
| Backend test time | 31 seconds |
| Startup time | 8.061 seconds with `demo` profile |
| Memory footprint | Not measured |
| Container image size | Not built |
| Average model latency | Unavailable; standard tests do not call OpenAI |
| Average tool latency | Available per response trace and Micrometer at runtime |
| End-to-end valid-transfer latency | Demonstrated locally; not averaged |
| Test pass rate | 13/13 backend tests passed |
| Java version | 21 required; local PATH Java is 8, JDK 21 is installed |
| Spring Boot version | 3.5.7 |
| Spring AI version | 1.0.3 |
| PostgreSQL image | postgres:16-alpine |
| React/Vite | React 19.1.1, Vite 7.3.6 |

Unavailable values are intentionally not invented. Re-run after deployment to record startup time, memory, image size, and live model latency.
