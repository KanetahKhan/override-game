#!/usr/bin/env bash
set -euo pipefail
mvn --batch-mode --no-transfer-progress test-compile dependency:build-classpath -Dmdep.outputFile=target/player-classpath.txt

# build-classpath writes the platform's own separator, so the entries we prepend
# have to match it or the whole classpath is read as one bogus path. CI is Linux,
# but this also has to run from Git Bash on the Windows dev machines.
case "$(uname -s)" in
    MINGW*|MSYS*|CYGWIN*) player_sep=';' ;;
    *)                    player_sep=':' ;;
esac
player_cp="target/test-classes${player_sep}target/classes${player_sep}$(<target/player-classpath.txt)"

java -cp "$player_cp" com.override.shared.service.PlayerStorageSmoke
player_glass=()
if [[ "$(uname -s)" == "Linux" && -z "${DISPLAY:-}" ]]; then player_glass=(-Dglass.platform=headless); fi
java --enable-native-access=ALL-UNNAMED "${player_glass[@]}" -Dprism.order=sw -cp "$player_cp" com.override.shared.ui.PlayerPagesSmoke
java --enable-native-access=ALL-UNNAMED "${player_glass[@]}" -Dprism.order=sw -cp "$player_cp" com.override.shared.ui.OpeningVisualSmoke
