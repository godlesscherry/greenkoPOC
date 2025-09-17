#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
PROJECT_ROOT=$(cd "${SCRIPT_DIR}/.." && pwd)

pushd "${PROJECT_ROOT}" > /dev/null
docker compose down -v
rm -rf .data/db
popd > /dev/null
