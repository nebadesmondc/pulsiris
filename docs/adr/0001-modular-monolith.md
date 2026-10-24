# 1. Modular monolith with hexagonal modules

Date: 2026-10-23
Status: Accepted

## Context

Pulsiris must run anywhere from a laptop to a cloud VM and stay easy for a small team to operate. Its parts (target registry, health checks, incidents, notifications) change for different reasons but share one deployment.

## Decision

- One Spring Boot 4.1 application on Java 21, split into Spring Modulith modules: `registry`, `checks`, `incidents`, `notifications`.
- Each module uses ports and adapters. The domain is plain Java.
- Each module owns its own PostgreSQL schema. There are no cross-module joins or foreign keys; modules refer to each other's data by UUID.
- Modules query each other through published module APIs and react to each other through Spring Modulith events, persisted by the Event Publication Registry.
- Module boundaries are verified in tests with `ApplicationModules.verify()` and ArchUnit.

## Consequences

- There is one artifact to build, deploy and monitor.
- A module can be extracted into its own service later without untangling shared tables.
- Reactions between modules are eventually consistent, so event listeners must be idempotent.
- Keeping the domain separate from persistence adds mapping code. MapStruct keeps it small.
