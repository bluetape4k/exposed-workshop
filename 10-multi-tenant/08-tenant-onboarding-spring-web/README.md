# 08 Tenant Onboarding Spring Web

English | [한국어](./README.ko.md)

This module demonstrates tenant onboarding and provisioning for schema-per-tenant applications. The service persists tenant metadata, creates the tenant schema, provisions a marker table, rejects duplicate tenants, and drops partially created schema resources when provisioning fails.

## Architecture Diagram

![Tenant onboarding architecture](../../docs/images/readme-diagrams/10-multi-tenant-08-tenant-onboarding-spring-web-architecture-01.png)

## Workflow

1. Validate and normalize `tenantId`.
2. Check the tenant catalog for duplicates.
3. Create the tenant schema and marker table.
4. Persist the tenant catalog record only after provisioning succeeds.
5. Drop the tenant schema when provisioning fails before catalog persistence.

## Verification

```bash
./gradlew :08-tenant-onboarding-spring-web:test
```

Use this pattern when tenant creation must be observable, auditable, and recoverable after partial provisioning failures.
