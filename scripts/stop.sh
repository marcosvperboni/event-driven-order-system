#!/bin/bash
# Stops and removes all containers, network and volumes for this system.
set -euo pipefail
cd "$(dirname "$0")/.."

if command -v podman >/dev/null 2>&1; then
    ENGINE="podman"
elif command -v docker >/dev/null 2>&1; then
    ENGINE="docker"
else
    echo "Neither podman nor docker was found on PATH." >&2
    exit 1
fi

echo "Using engine: $ENGINE compose"
"$ENGINE" compose down --volumes
