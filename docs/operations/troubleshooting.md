# Troubleshooting

Commands run from `docker/`.

## First look

| Question | Command |
|---|---|
| What is running, healthy or restarting? | `docker compose ps` |
| What went wrong? | `docker compose logs backend --since 10m` |
| How often did it restart, and how did it last exit? | `docker inspect pulsiris-backend-1 --format '{{.RestartCount}} {{.State.ExitCode}}'` |
| Was it killed? | `docker events --since 10m --filter container=pulsiris-backend-1 --filter event=die --filter event=oom` |
| Memory and CPU | `docker stats --no-stream` |
| Is the app ready for traffic? | `curl localhost:8080/actuator/health/readiness` |
| Which configuration did Compose resolve? | `docker compose config` |

| Exit code | Meaning |
|---|---|
| 0 | Normal exit |
| 1 | Application error, usually at startup; the cause is in the logs |
| 137 | Killed with SIGKILL: out of memory, or still running after `stop_grace_period` |
| 143 | Stopped with SIGTERM, a normal `docker stop` |

Docker restarts a container only when it exits. An unhealthy container keeps running.

## Symptoms

### Backend unhealthy, readiness reports `db: DOWN`

Postgres is stopped or unreachable. Liveness stays `UP`, so the backend is not restarted. Scheduled checks log `Connection is not available, request timed out after 5000ms`; each database call gives up after 5 s.

Check `docker compose ps postgres` and `docker compose logs postgres`. Once Postgres is back, the backend reconnects and turns healthy on its own.

### Backend restarts in a loop with `password authentication failed`

`docker compose ps` can show `Up 5 seconds (health: starting)`; the restart count and the short uptime give it away.

`docker/secrets/db_password` no longer matches the password the database was created with. Postgres reads `POSTGRES_PASSWORD_FILE` only when its volume is first initialised, so editing the file later does not change the database password. A file written by PowerShell with `echo` or `>` starts with a byte-order mark and ends with a line break, so it never matches; use the `Set-Content` command from the README.

Put the original password back and recreate the backend:

```bash
docker compose up -d --force-recreate backend
```

To change the password, run `\password pulsiris` in `docker compose exec postgres psql -U pulsiris`, then update the file and recreate the backend.

### `Bind for 127.0.0.1:8080 failed: port is already allocated`

Another container or process holds the port.

```bash
docker ps --filter publish=8080
netstat -ano | findstr :8080        # Windows
ss -ltnp 'sport = :8080'            # Linux
```

Stop it, or set another `APP_PORT` in `docker/.env`.

### Exit code 137 and an `oom` event

The backend used more than `BACKEND_MEMORY`. Swap is disabled for the container, so it is killed instead of slowing down. `OOMKilled` in `docker inspect` resets when the container restarts; `docker events` keeps the record.

The heap is 60% of the limit, and the JVM needs about 200 MiB beyond the heap. Keep `BACKEND_MEMORY` at 512M or more.

### `OutOfMemoryError: Java heap space`

The heap is too small for the data the app holds. The JVM exits on the error and Docker restarts it. Raise `BACKEND_MEMORY`.

### Data is gone

`docker compose down -v` deletes the `postgres-data` volume. The next start creates an empty database with the password currently in the secret file. `docker volume ls` shows whether the volume exists.

## Exercises

Predict the outcome, break it, find the cause with the commands above, then restore.

| # | Break | Restore |
|---|---|---|
| 1 | `docker compose stop postgres`, wait 15 s | `docker compose start postgres` |
| 2 | Write another password to `secrets/db_password`, then `docker compose up -d --force-recreate backend` | Original password, same command |
| 3 | `docker compose stop backend`, `docker run -d --name blocker -p 127.0.0.1:8080:80 nginx`, `docker compose up -d backend` | `docker rm -f blocker`, `docker compose up -d backend` |
| 4 | `BACKEND_MEMORY=200M` in `.env`, `docker compose up -d backend` | Remove the line, same command |
| 5 | `BACKEND_MEMORY=100M` in `.env`, `docker compose up -d backend` | Remove the line, same command |
| 6 | `docker compose down -v` (deletes local data) | `docker compose up -d --wait` |
