# Agent And Tool Design

The Spring AI candidate exposes exactly four business tools:

- `sourceAccountBalanceInquiry`: read-only account lookup for the authenticated demo user.
- `savedBeneficiaryInquiry`: read-only saved beneficiary lookup for DFP transfers.
- `validateTransferAmount`: validates amount, statuses, saved-beneficiary rules, balance, currency, and the LKR 50.00 limit; creates pending server state only when valid.
- `executeFundTransfer`: executes only when the server pending transfer is confirmed, unexpired, and exactly matched.

Agent orchestration is delegated to Spring AI `ChatClient` in normal runtime. The model receives the system instruction, tool metadata, short conversation context, and previous server state. The standard tests use `USE_SCRIPTED_AGENT=true` to avoid OpenAI calls; this runtime exists only for deterministic evaluation and does not replace the Spring AI runtime.

Deterministic controls remain in Java:

- Demo bearer-token authentication.
- Source account ownership.
- Saved-beneficiary enforcement.
- Account and customer status checks.
- Positive amount, balance, currency, and LKR 50.00 limit checks.
- Server-side confirmation state and TTL.
- Idempotency by `fund_transfer.uuid`.
- Atomic debit/credit/insert in one database transaction with pessimistic account locks.

Changed amount, source, destination, or beneficiary causes a new validation result and pending UUID, so any previous confirmation no longer matches the execution tool arguments.
