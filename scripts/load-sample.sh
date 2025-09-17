#!/usr/bin/env bash
set -euo pipefail

MINUTES=${1:-180}
SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
PROJECT_ROOT=$(cd "${SCRIPT_DIR}/.." && pwd)

pushd "${PROJECT_ROOT}/server" > /dev/null
WINDFARM_SEED_ENABLED=true \
WINDFARM_SEED_MINUTES=${MINUTES} \
./gradlew bootRun --args='--spring.main.web-application-type=none --windfarm.emitter.enabled=false --windfarm.listener.enabled=false'
popd > /dev/null
