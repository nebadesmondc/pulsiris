# Deploy and roll back

`docker/deploy.sh` deploys a backend image from GHCR: a release such as `0.1.0`, a master build such as `sha-d3dfae1`, or `local`, the image built from source.

```bash
docker/deploy.sh 0.2.0   # deploy
docker/deploy.sh 0.1.0   # roll back: deploy the previous version
```

The script pulls the version, records it in `docker/.env` as `PULSIRIS_VERSION`, recreates the backend and waits for readiness. If the backend is not ready within 180 s (`DEPLOY_TIMEOUT`) or runs another image than the one pulled, it prints the backend's last logs and starts the previous version again. The backend is unavailable for about 30 s while its container is replaced.

| Exit code | Meaning |
|---|---|
| 0 | Deployed |
| 1 | Not ready; the previous version runs again |
| 2 | The previous version did not start either |
| 3 | The version could not be pulled; nothing changed |
| 64 | Not a version |

## Releasing

Every commit on master is published as `sha-<commit>`. To release one, tag it once its CI run is green:

```bash
git tag -a v0.2.0 -m "Pulsiris 0.2.0"
git push origin v0.2.0
```

The Release workflow adds `0.2.0` to the image that commit already published and creates a GitHub release. A released version never moves to another image.

## Migrations

Rolling back replaces the code, not the database. Flyway warns about migrations the older version does not know and carries on, so the older version starts as long as the schema still has everything it uses. A release therefore never removes or renames what the previous release uses:

1. Expand: add the new column or table, and write to both.
2. Switch reads to the new one in the next release.
3. Contract: drop the old one a release later.

After a migration that removed something, the previous version fails schema validation (`missing column`) and the script returns to the new version. Fix forward with a new release, or restore a database backup.

## Exercises

| # | Run | Expect |
|---|---|---|
| 1 | `docker/deploy.sh 9.9.9` | Nothing changes, exit 3 |
| 2 | `DEPLOY_TIMEOUT=5 docker/deploy.sh 0.1.0` | Rolls back to the previous version, exit 1 |
| 3 | `docker/deploy.sh 0.1.0`, then `docker/deploy.sh local` | Two deploys; `/actuator/info` shows `sha-d3dfae1`, then `dev` |
