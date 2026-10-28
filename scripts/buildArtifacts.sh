#!/bin/bash
# Exit immediately if any command or piped command fails
set -eo pipefail

# Ensure script always executes from the repository root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "${REPO_ROOT}"

# Clean and clone dependencies
rm -rf ci.ant ci.common
git clone --depth=1 git@github.com:OpenLiberty/ci.ant.git ci.ant
git clone --depth=1 -b 1.x git@github.com:OpenLiberty/ci.common.git ci.common

# Install dependencies
mvn -V clean install -f ci.ant --batch-mode --no-transfer-progress --errors -DtrimStackTrace=false -DskipTests
mvn -V clean install -f ci.common --batch-mode --no-transfer-progress --errors -DtrimStackTrace=false -DskipTests

# Build and install ci.gradle artifacts locally
chmod +x ./gradlew
./gradlew clean install -x test --no-daemon --stacktrace --info
