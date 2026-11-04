#!/usr/bin/env bash
# Deploys a published backend version and rolls back when it does not become ready.
#   docker/deploy.sh 0.2.0 | sha-abc1234 | local
# The deployed version is kept in docker/.env, so a later `docker compose up` keeps it.
set -euo pipefail

cd "$(dirname "$0")"

version="${1:-}"
timeout="${DEPLOY_TIMEOUT:-180}"
compose=(docker compose -f compose.yaml)

if [[ ! "$version" =~ ^([0-9]+\.[0-9]+\.[0-9]+|sha-[0-9a-f]{7}|local)$ ]]; then
	echo "Usage: $0 <version>, e.g. 0.2.0, sha-abc1234, or local for the image built from source" >&2
	exit 64
fi

current_version() {
	sed -n 's/^PULSIRIS_VERSION=//p' .env 2>/dev/null | tr -d '\r' | tail -n 1
}

set_version() {
	{ grep -v '^PULSIRIS_VERSION=' .env 2>/dev/null || true; echo "PULSIRIS_VERSION=$1"; } > .env.tmp
	mv .env.tmp .env
}

image() {
	PULSIRIS_VERSION="$1" "${compose[@]}" config --images backend | grep pulsiris-backend
}

# Starts the version in .env and checks the container runs exactly the image of that version.
start() {
	local version="$1" wait="$2" expected running
	expected="$(docker image inspect --format '{{.Id}}' "$(image "$version")")" || return 1
	"${compose[@]}" up -d --wait --wait-timeout "$wait" backend || return 1
	running="$(docker inspect --format '{{.Image}}' "$("${compose[@]}" ps -q backend)")"
	[[ "$running" == "$expected" ]]
}

previous="$(current_version)"
previous="${previous:-local}"

# Releases come from GHCR;
if [[ "$version" != local ]] && ! PULSIRIS_VERSION="$version" "${compose[@]}" pull backend; then
	echo "Could not pull $version; nothing was changed" >&2
	exit 3
fi
built_as="$(docker image inspect --format '{{index .Config.Labels "org.opencontainers.image.version"}}' "$(image "$version")")"

echo "Deploying $version (built as $built_as), replacing $previous"
set_version "$version"
if start "$version" "$timeout"; then
	echo "Deployed $version. Roll back with: $0 $previous"
	exit 0
fi

echo "$version did not become ready. Last backend logs:" >&2
"${compose[@]}" logs --tail 30 backend >&2 || true
echo "Rolling back to $previous" >&2
set_version "$previous"
if start "$previous" 180; then
	echo "Rolled back to $previous" >&2
	exit 1
fi
echo "Rollback to $previous failed as well; the backend needs attention" >&2
exit 2
