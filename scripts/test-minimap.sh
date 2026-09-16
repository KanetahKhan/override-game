#!/usr/bin/env bash
set -euo pipefail
mvn --batch-mode --no-transfer-progress test-compile dependency:build-classpath -Dmdep.outputFile=target/minimap-classpath.txt
minimap_cp="$(<target/minimap-classpath.txt)"
java --enable-native-access=ALL-UNNAMED -Dprism.order=sw \
  -cp "target/test-classes:target/classes:$minimap_cp" \
  com.override.chapter1.MinimapSmoke
