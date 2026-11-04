#!/bin/bash
# Exit immediately if any command or piped command fails
set -eo pipefail

# Ensure script always executes from the repository root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "${REPO_ROOT}"

RUNTIME="${RUNTIME:-ol}"
RUNTIME_VERSION="${RUNTIME_VERSION:-26.0.0.6}"

chmod +x ./gradlew
timeout 180m ./gradlew check \
    -P"test.exclude"="**/DevContainerTest*,**/DevEarlyQuitTest*,**/Devc*" \
    -Druntime="${RUNTIME}" \
    -DruntimeVersion="${RUNTIME_VERSION}" \
    --no-daemon \
    --stacktrace --info --warning-mode=all
