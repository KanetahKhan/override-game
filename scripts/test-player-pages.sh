#!/usr/bin/env bash
set -euo pipefail
mvn --batch-mode --no-transfer-progress test-compile dependency:build-classpath -Dmdep.outputFile=target/player-classpath.txt
player_cp="target/test-classes:target/classes:$(<target/player-classpath.txt)"
java -cp "$player_cp" com.override.shared.service.PlayerStorageSmoke
player_glass=()
if [[ "$(uname -s)" == "Linux" && -z "${DISPLAY:-}" ]]; then player_glass=(-Dglass.platform=headless); fi
java --enable-native-access=ALL-UNNAMED "${player_glass[@]}" -Dprism.order=sw -cp "$player_cp" com.override.shared.ui.PlayerPagesSmoke
