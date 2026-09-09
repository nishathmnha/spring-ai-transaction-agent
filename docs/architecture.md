# Architecture

```mermaid
flowchart LR
    UI[React chat UI] --> AgentController[AgentController]
    Postman[Postman / external tests] --> SavingsController[SavingsController]
    AgentController --> BankingAgentService[BankingAgentService]
    BankingAgentService --> SpringAI[Spring AI ChatClient + OpenAI]
    SpringAI --> Tools[TransactionTools]
    Tools --> Services[Account, Beneficiary, Validation, Transfer services]
    SavingsController --> Services
    Services --> Repos[JPA repositories]
    Repos --> DB[(PostgreSQL)]
    BankingAgentService --> State[Conversation memory + pending transfer state]
    Tools --> Trace[Micrometer + sanitized tool trace]
```

The agent gets the four Spring AI tools and decides which to call. Controllers do not run a fixed transfer workflow. Sensitive operations are guarded again inside the tool-backed services.
