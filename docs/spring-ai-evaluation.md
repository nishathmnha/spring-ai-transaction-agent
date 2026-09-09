# Spring AI Evaluation Scorecard

| Evaluation area | Weight | Spring AI score | Evidence |
|---|---:|---:|---|
| Agentic Reasoning and Tool Orchestration | 20% | 16/20 | `ChatClient` registers tools and allows model-directed calls; deterministic tests use scripted runtime. |
| Tool Definition and Tool Calling | 10% | 9/10 | Four annotated `@Tool` methods with descriptions and typed arguments. |
| Conversation and Context Management | 8% | 6/8 | In-memory short context and pending state; production should use persistent chat memory. |
| Safety and Transaction Controls | 12% | 11/12 | Service-level validation, confirmation, idempotency, locks, and transaction boundary. |
| Dynamic Agents and Tools | 8% | 5/8 | Static four-tool MVP; Spring AI can support dynamic tool providers later. |
| MCP and Interoperability | 7% | 5/7 | MCP documented but not implemented for MVP. |
| HITL and Approval | 7% | 7/7 | Server confirmation endpoint gates execution; model text cannot confirm. |
| Observability and Explainability | 7% | 6/7 | Micrometer timers, structured logs, sanitized trace endpoint and UI. |
| Evaluation and Testing | 6% | 5/6 | Mandatory scenarios and JSON dataset included; live OpenAI eval remains manual. |
| Error Handling and Resilience | 5% | 4/5 | Global error handler and failure tests; timeout-specific model retry policy remains future work. |
| Developer Experience | 5% | 4/5 | Maven project, compose, README, scripts via curl; supplied source files were absent. |
| Deployment and Runtime | 3% | 2/3 | Docker Compose for PostgreSQL; app image not built in this MVP. |
| Governance and Enterprise Readiness | 2% | 1/2 | No real credentials; production audit/security controls still required. |

Strengths:

- Spring AI tool registration is concise and maps naturally to Spring services.
- Banking invariants are enforced outside the model.
- Tool trace makes selected orchestration inspectable.

Limitations:

- One demo user and in-memory pending state.
- No real identity provider, policy engine, audit ledger, OTP, or fraud controls.
- Standard tests use a scripted runtime to avoid nondeterministic model calls.

Framework-specific workarounds:

- Response JSON is shaped by the application after the model turn because confirmation state is server-controlled.
- Tool tracing is wrapped at the tool methods instead of relying only on provider telemetry.

Production concerns:

- Add Spring Security, customer-scoped beneficiary relationship tables, persistent chat memory, audit trails, maker-checker controls, AML/fraud screening, limits by segment, and regulated data-retention policies.

MCP and interoperability:

- The four tools could later be exposed as MCP tools by adding a Spring AI MCP server starter and parallel `@McpTool` methods or adapters.
- Spring AI can consume MCP tools through MCP client starters and explicit `ToolCallbackProvider` registration with `ChatClient`.
- Security must be added before exposing any MCP endpoint; Spring AI docs note HTTP MCP transports are authentication-agnostic by default.
- Portability improves if tool contracts remain DTO-first and framework-neutral.
