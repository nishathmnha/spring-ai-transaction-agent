# Licensing And Commercial Assessment

Verified facts:

- Spring AI is open source and released under the Apache 2.0 license, according to the Spring AI project contribution documentation: https://github.com/spring-projects/spring-ai/blob/main/CONTRIBUTING.md
- Spring AI supports tool calling through `@Tool` methods and `ChatClient`, and the application remains responsible for executing tools safely: https://docs.spring.io/spring-ai/reference/api/tools.html
- Spring AI documents MCP client/server support, including consuming MCP tools and exposing Spring tools through MCP server starters: https://docs.spring.io/spring-ai/reference/api/tools.html and https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html
- OpenAI API usage has model/API costs. OpenAI published GPT-4.1 series token prices in its GPT-4.1 API announcement, including `gpt-4.1-mini` pricing per 1M tokens: https://openai.com/index/gpt-4-1/
- OpenAI states that API and business data is not used for model training by default unless the organization opts in: https://openai.com/business-data/ and https://platform.openai.com/docs/models/default-usage-policies-by-endpoint

Assessment:

- Framework cost: Spring AI itself has no framework license fee.
- License type: Apache 2.0 is generally permissive for commercial use, subject to preserving notices and license terms.
- Model/provider cost: real OpenAI integration is usage-based and depends on model, input/output tokens, retries, and tool-call loop length.
- Paid enterprise features: OpenAI enterprise privacy, retention, residency, SSO, EKM, RBAC, analytics, and support features may require paid plans or sales approval.
- Self-hosting: the Spring Boot application and PostgreSQL are self-hostable. The configured OpenAI model is not self-hosted.
- Model-provider dependency: the MVP depends on OpenAI-compatible Spring AI model configuration. Spring AI can target other providers with dependency/configuration changes.
- Commercial-use restrictions: no Spring AI-specific commercial restriction was identified beyond Apache 2.0 obligations. OpenAI API use remains subject to OpenAI terms, usage policies, and account eligibility.
- Enterprise support: Spring ecosystem support may be available commercially through Spring/Broadcom channels; OpenAI support level depends on plan.
- Vendor lock-in: business services and DTO tool contracts are provider-neutral, but prompt behavior, tool-call quality, token costs, and model configuration are provider-sensitive.

Assumptions:

- This project uses synthetic data only.
- Legal review is still required before any banking or regulated deployment.
