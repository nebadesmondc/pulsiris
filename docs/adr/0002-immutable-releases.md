# 2. Immutable release images with scripted rollback

Date: 2026-11-04
Status: Accepted

## Context

Pulsiris runs as one backend container next to Postgres, on a laptop, a VPS or a cloud VM. The teams running it have no dedicated operations staff, so a release must be easy to trust and a bad one quick to undo, on any of those machines.

## Decision

- CI builds one image per master commit, starts it with Postgres and publishes it to GHCR as `sha-<commit>`. Machines never build.
- A release is a `vX.Y.Z` tag. The Release workflow adds the version to the image of that commit instead of rebuilding it, and a version never moves to another image.
- `docker/deploy.sh` deploys a version: it pulls the image, records the version in `docker/.env`, waits for readiness and starts the previous version again when the new one is not ready.
- Rolling back means deploying the previous version, so migrations keep the schema usable by the previous release (expand, then contract).

## Consequences

- What runs is exactly the image CI tested, and any version can be deployed again later.
- A rollback takes as long as a deploy, under a minute.
- With one container, every deploy has about 30 s of downtime.
- Removing or renaming a column takes three releases. A migration that removes something the previous release uses can't be undone by a rollback; it needs a fix forward or a database restore.
- `/actuator/info` reports `sha-<commit>`, not the version, because the version is added after the build.
- Deploys are started by hand on each machine.
