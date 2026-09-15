#!/bin/bash
# Builds and starts the whole system. Works with either Podman or Docker.
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

mkdir -p secrets
[ -f secrets/postgres_password.txt ] || openssl rand -hex 16 > secrets/postgres_password.txt 2>/dev/null || echo "orders_pass_$(date +%s)" > secrets/postgres_password.txt

echo "Using engine: $ENGINE compose"
"$ENGINE" compose up --build -d

echo
echo "Services starting. Check health with: $ENGINE compose ps"
