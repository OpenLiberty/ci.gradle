#!/bin/bash
git clone --depth=1 git@github.com:OpenLiberty/ci.ant.git ci.ant
git clone --depth=1 -b 1.x git@github.com:OpenLiberty/ci.common.git ci.common

mvn -V clean install -f ci.ant --batch-mode --no-transfer-progress --errors -DtrimStackTrace=false -DskipTests
mvn -V clean install -f ci.common --batch-mode --no-transfer-progress --errors -DtrimStackTrace=false -DskipTests
timeout 120m ./gradlew clean install check -P"test.exclude"="**/DevContainerTest*,**/DevEarlyQuitTest*,**/Devc*" -Druntime=${{ matrix.RUNTIME }} -DruntimeVersion="${{ matrix.RUNTIME_VERSION }}" --stacktrace --info --warning-mode=all