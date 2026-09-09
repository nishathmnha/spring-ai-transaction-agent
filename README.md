# Spring AI Transaction Agent Evaluation MVP

This repository implements the Spring AI candidate for the Agentic AI SDK and Framework Evaluation Exercise. It is a conversational saved-beneficiary P2P transfer demo using Java 21, Spring Boot, Maven, Spring AI, OpenAI, PostgreSQL, Flyway, JPA, Micrometer, JUnit, Mockito, Docker Compose, and a minimal React/Vite UI.

This application is an evaluation MVP using synthetic data. It must not be connected to real customer accounts or production transaction systems without appropriate security, authentication, authorization, auditing, regulatory review, privacy controls, and operational safeguards.

## Requirement Source

The prompt required inspection of `Agentic_AI_Framework_Evaluation_Exercise.docx` and `DF Savings - P2P Fund Transfer Flow.postman_collection.json`. These files were not present in the reachable workspace or immediate parent directory during implementation, so this MVP uses the detailed requirements and endpoint payloads supplied in the task message as the authoritative available specification.

Extracted requirements implemented:

- Build only the Spring AI candidate.
- Preserve the four banking API contracts from the supplied examples.
- Expose exactly four Spring AI tools: source account inquiry, saved beneficiary inquiry, amount validation, fund transfer execution.
- Let the Spring AI model select tools dynamically in normal runtime.
- Keep sensitive banking controls deterministic in Spring services.
- Enforce mandatory server-side confirmation before transfer execution.
- Use `uuid` as the transfer idempotency key.
- Use one Spring Boot backend and a minimal React chat frontend.

## Architecture

```text
Controller
-> Agent / Spring AI ChatClient
-> TransactionTools
-> Business services
-> JPA repositories
-> PostgreSQL
```

The same service layer is reused by the REST controllers and Spring AI tools. The tools do not call the REST endpoints over HTTP.

See [docs/architecture.md](docs/architecture.md) for the diagram.

## Database Model

The MVP uses only:

- `customer`
- `savings_account`
- `fund_transfer`

`customer.saved_beneficiary` represents whether a beneficiary is saved. In production this should be replaced by an owner-to-beneficiary relationship table with authorization, limits, and audit history.

Seed data includes:

- Demo user `CUST001`
- Source account `001020020001`, alias `salary account`, balance `LKR 1000.00`
- Saved beneficiary `Varuni`, account `001020020974`, balance `LKR 500.00`
- Dormant, frozen, closed, restricted, unknown, and unsaved-beneficiary scenarios

## Tool Contracts

- `sourceAccountBalanceInquiry`: read-only source account lookup.
- `savedBeneficiaryInquiry`: read-only saved DFP beneficiary lookup.
- `validateTransferAmount`: validates positive amount, LKR 50.00 limit, statuses, saved beneficiary, currency, and balance. Creates pending transfer state only when valid.
- `executeFundTransfer`: executes only if pending transfer exists, is confirmed server-side, is unexpired, exactly matches confirmed details, and passes critical validation again.

## Why The Workflow Is Agent-Directed

Normal runtime registers the four tools with Spring AI `ChatClient` in `SpringAiAgentRuntime`. The application does not contain a `performTransferWorkflow()` method or controller-level fixed chain. The model receives the system instruction, tool descriptions, conversation context, and prior tool results, then decides which tools to call.

The test profile uses `USE_SCRIPTED_AGENT=true` to make tests deterministic and avoid real OpenAI API calls.

## Deterministic Controls

Java services enforce:

- Demo bearer-token authentication.
- Source account ownership.
- Saved beneficiary restriction.
- Active customer/account statuses.
- Positive amount and LKR 50.00 transaction limit.
- Sufficient balance.
- Currency match.
- Server-side confirmation state and TTL.
- Idempotency by unique `fund_transfer.uuid`.
- Atomic balance updates and transaction insert under database transaction and account locks.

## Configuration

Copy `.env.example` and set values:

```text
OPENAI_API_KEY=
OPENAI_MODEL=gpt-4.1-mini
OPENAI_TEMPERATURE=0
DEMO_AUTH_TOKEN=demo-token
POSTGRES_DB=banking_agent
POSTGRES_USER=banking_agent
POSTGRES_PASSWORD=banking_agent
PENDING_TRANSFER_TTL_SECONDS=300
USE_SCRIPTED_AGENT=false
```

Use the same `OPENAI_MODEL` and `OPENAI_TEMPERATURE` values across Spring AI, LangChain4j, Mastra, and Claude Agent SDK comparisons.

## Run PostgreSQL

```bash
docker compose up -d postgres
```

## Run Backend

Maven is the primary build:

```bash
mvn spring-boot:run
```

This environment did not have `mvn` on PATH. The included Gradle wrapper can be used as a local fallback, but the path contains `&`, so invoke the wrapper jar directly:

```powershell
& 'C:\Program Files\Java\jdk-21\bin\java.exe' "-Dorg.gradle.appname=gradlew" -jar "D:\R&D\spring-ai-transaction-agent\spring-ai-transaction-agent\gradle\wrapper\gradle-wrapper.jar" bootRun
```

For local deterministic demonstration without OpenAI:

```bash
USE_SCRIPTED_AGENT=true mvn spring-boot:run
```

## Run Frontend

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`.

## Run Tests

Standard tests do not call OpenAI.

```bash
mvn test
```

Local fallback used during implementation:

```powershell
& 'C:\Program Files\Java\jdk-21\bin\java.exe' "-Dorg.gradle.appname=gradlew" -jar "D:\R&D\spring-ai-transaction-agent\spring-ai-transaction-agent\gradle\wrapper\gradle-wrapper.jar" test
```

## API Contracts

All require:

```text
Authorization: Bearer <DEMO_AUTH_TOKEN>
```

Balance:

```http
GET /api/savings/savings/balance
```

Saved beneficiaries:

```http
GET /api/savings/beneficiary/get/DFP
```

Amount validation:

```http
POST /api/savings/transfers/validate/amount
Content-Type: application/json

{
  "amount": "100.00",
  "account_number": "001020020974",
  "receiver_bank_code": "6995",
  "receiver_bank_name": "Dialog Finance PLC"
}
```

Fund transfer:

```http
POST /api/savings/transfer
Content-Type: application/json

{
  "account_number": "001020020974",
  "amount": 10,
  "initiator_mobile_no": "",
  "narration": "",
  "receiver_bank": "6995",
  "receiver_branch": null,
  "receiver_name": "Varuni",
  "uuid": "ec461156-9356-4b83-be52-dddfcb8ebf42",
  "version": "V2"
}
```

Agent chat:

```http
POST /api/agent/chat
Content-Type: application/json

{
  "conversationId": "CONV-001",
  "message": "Transfer LKR 10 from my salary account to Varuni."
}
```

Confirm:

```http
POST /api/agent/conversations/{conversationId}/confirm
```

Reject:

```http
POST /api/agent/conversations/{conversationId}/reject
```

Trace:

```http
GET /api/agent/conversations/{conversationId}/trace
```

## Demo Scenarios

See [docs/mandatory-scenario-guide.md](docs/mandatory-scenario-guide.md) and [docs/evaluation-dataset.json](docs/evaluation-dataset.json).

Key checks:

- Valid transfer requests confirmation first, then debits source to `LKR 990.00` and credits Varuni to `LKR 510.00`.
- Missing amount/source asks for clarification.
- Unknown source or destination stops before execution.
- Dormant, frozen, closed, and restricted accounts stop before execution.
- LKR 100.00 exceeds the LKR 50.00 limit and does not invoke transfer.
- User rejection clears pending state and does not change balances.
- Changed amount revalidates and asks for confirmation again.
- Duplicate UUID returns the original transaction result without a second debit.
- Non-transfer questions avoid transaction tools.

## Observability

The app records:

- Conversation ID and agent request ID in MDC.
- Tool name, sanitized input summary, status, duration, and error code.
- Micrometer timers for tool and agent turn duration.
- Transfer UUID and final result in API responses.

It does not log authorization tokens, OpenAI keys, full prompts, hidden reasoning, or full account numbers by default.

## Postman

Import the original supplied Postman collection if available and set:

```text
baseUrl=http://localhost:8080
token=<DEMO_AUTH_TOKEN>
```

The implemented endpoint paths and request fields match the examples provided in the prompt.

## Known Limitations

- The two supplied authoritative files were unavailable during implementation.
- One synthetic demo customer is used.
- Beneficiary ownership is simplified to `customer.saved_beneficiary`.
- Pending transfer state and chat memory are in memory.
- No Spring Security, OTP, AML/fraud checks, maker-checker flow, ledger subsystem, or production audit store.
- Standard tests use a deterministic scripted runtime; live OpenAI behavior should be manually evaluated with the integration profile.
- The REST `/api/savings/transfer` endpoint is preserved for contract testing and uses demo assumptions; the agent path still requires confirmation.

## Production Hardening

Before production use, add a real identity provider, Spring Security authorization, customer-beneficiary relationship tables, encrypted audit logs, durable chat memory, transaction ledger integration, fraud/AML screening, rate limits, model-output validation, provider failover, secrets management, data retention controls, incident monitoring, and regulatory approval.
