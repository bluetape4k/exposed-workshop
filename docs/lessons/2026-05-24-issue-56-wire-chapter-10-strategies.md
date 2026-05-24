# Issue 56 Wire Chapter 10 Strategies

## Context

Issue #56 asked to wire chapter 10 Spring Boot strategy examples into documentation and verification.

## Decision

Add the tenant onboarding module to chapter 10 English/Korean README files with module links, test/build tasks, and a CI/nightly coverage decision.

## Outcome

Updated `10-multi-tenant/README.md` and `README.ko.md` for `08-tenant-onboarding-spring-web`.

## Verification

Passed: `git diff --check`.

## Future Guidance

Docs PRs for strategy wiring should list dependent feature PRs and record whether new modules require nightly container coverage.
