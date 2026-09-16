#!/usr/bin/env bash
set -euo pipefail
mvn --batch-mode --no-transfer-progress test-compile dependency:build-classpath -Dmdep.outputFile=target/opening-classpath.txt
opening_cp="$(<target/opening-classpath.txt)"
java --enable-native-access=ALL-UNNAMED -Dprism.order=sw \
  -cp "target/test-classes:target/classes:$opening_cp" \
  com.override.shared.ui.OpeningVisualSmoke "$@"
