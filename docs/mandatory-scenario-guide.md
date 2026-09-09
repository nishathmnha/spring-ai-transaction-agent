# Mandatory Scenario Guide

Use `DEMO_AUTH_TOKEN=demo-token` and start the backend with `USE_SCRIPTED_AGENT=true` for deterministic local demonstration.

Valid transfer:

```bash
curl -s -X POST http://localhost:8080/api/agent/chat \
  -H "Authorization: Bearer demo-token" -H "Content-Type: application/json" \
  -d '{"conversationId":"CONV-001","message":"Transfer LKR 10 from my salary account to Varuni."}'

curl -s -X POST http://localhost:8080/api/agent/conversations/CONV-001/confirm \
  -H "Authorization: Bearer demo-token"
```

Other prompts:

- `Transfer money to Varuni.`
- `Transfer LKR 10 from my unknown account to Varuni.`
- `Transfer LKR 10 from my salary account to Unknown Person.`
- `Transfer LKR 10 from my dormant account to Varuni.`
- `Transfer LKR 10 from my salary account to Frozen Beneficiary.`
- `Transfer LKR 2,000 from my salary account to Varuni.`
- `Transfer LKR 100 from my salary account to Varuni.`
- `What is the balance of my salary account?` then `Transfer LKR 10 from that account to Varuni.`
- `What does available balance mean?`

Inspect the last selected tool sequence:

```bash
curl -s http://localhost:8080/api/agent/conversations/CONV-001/trace
```
