#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

echo "📦 Staging artifacts..."
./mvnw --batch-mode --no-transfer-progress -Ppublication -DskipTests=true -Dskip.spotless=true

echo "🚀 Releasing..."
./mvnw --batch-mode --no-transfer-progress -Prelease -pl :runtimes-java jreleaser:full-release

echo "🎉 Done!"
