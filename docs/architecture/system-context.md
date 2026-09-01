# System Context

```mermaid
flowchart TD
    Customer --> Backend[Workshop Management Backend]
    Admin[Administrative User] --> Backend
    Backend --> PostgreSQL[(PostgreSQL)]
```

No external integrations are implemented in Phase 1.
