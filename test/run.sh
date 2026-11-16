#!/usr/bin/env bash
# Echter Test von Morni Angelkönig: baut das Plugin und das Test-Netzwerk, richtet einen Paper-Server ein (offline,
# eigener Port, RCON an), startet ihn, schickt Test-Spieler (mineflayer) durch alle Abläufe und hält ihn wieder an.
#
#   test/run.sh                       # alles
#   KEEP=1 test/run.sh                # Server danach laufen lassen (zum Nachschauen)
#   KEEP=1 SKIP_TEST=1 test/run.sh    # nur Server starten
#   FRESH=1 test/run.sh               # Welt vorher löschen (entsteht neu aus dem Code)
#
# Erwartet (Vorgaben für die MoinMorni-Sandbox, alles per Umgebungsvariable änderbar):
#   ANGEL_JAVA    Java 25 (Ordner; sonst die JDK 25 aus dem Cache, sonst JAVA_HOME)
#   PAPER_JAR     Paper 26.1.2 (mineflayer kann bis 26.1)
#   TEMPLATE      ein schon einmal gestarteter Paper-26.1.2-Ordner (libraries/, cache/, versions/ werden kopiert,
#                 damit nichts heruntergeladen werden muss) – optional
#   ANGEL_NODE_MODULES  node_modules mit mineflayer
#   EXTRA_PLUGINS weitere Jars für den Server (z. B. ViaVersion + ViaBackwards, damit ein Client 26.3 hineinkommt)
#   PORT, RCON_PORT, SERVER_DIR, VIEW
#
# Verfasst mit Claude 🤖
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
REPO="$(dirname "$HERE")"
CACHE="${MM_CACHE:-/home/user/mm-cache}"
CACHED_JDK="$(ls -d "$CACHE"/jdk/jdk-25* 2>/dev/null | head -1 || true)"
JAVA_HOME="${ANGEL_JAVA:-${CACHED_JDK:-${JAVA_HOME:-}}}"
export JAVA_HOME
JAVA="$JAVA_HOME/bin/java"
PAPER_JAR="${PAPER_JAR:-$CACHE/jars/paper-26.1.2.jar}"
TEMPLATE="${TEMPLATE:-$CACHE/servers/t1}"
SERVER="${SERVER_DIR:-$CACHE/servers/angelkoenig-test}"
PORT="${PORT:-25970}"
RCON_PORT="${RCON_PORT:-26070}"
VIEW="${VIEW:-6}"
export NODE_PATH="${ANGEL_NODE_MODULES:-$CACHE/node/node_modules}${NODE_PATH:+:$NODE_PATH}"

echo "== Bauen"
(cd "$REPO" && nice -n 15 mvn -q -B -DskipTests package)
(cd "$REPO" && nice -n 15 mvn -q -B -f test/fake-network/pom.xml package)

echo "== Server einrichten: $SERVER"
mkdir -p "$SERVER/plugins/MorniAngelkoenig" "$SERVER/plugins/MoinMorni"
if [ -d "$TEMPLATE" ] && [ ! -d "$SERVER/libraries" ]; then
  cp -r "$TEMPLATE/libraries" "$TEMPLATE/cache" "$TEMPLATE/versions" "$SERVER/" 2>/dev/null || true
fi
cp "$PAPER_JAR" "$SERVER/paper.jar"
echo "eula=true" > "$SERVER/eula.txt"
RCON_PASS="$(head -c 18 /dev/urandom | base64 | tr -dc 'A-Za-z0-9' | head -c 20)"
printf '%s' "$RCON_PASS" > "$SERVER/.rcon-pass"
chmod 600 "$SERVER/.rcon-pass"
cat > "$SERVER/server.properties" <<EOF
online-mode=false
enforce-secure-profile=false
server-port=$PORT
enable-rcon=true
rcon.port=$RCON_PORT
rcon.password=$RCON_PASS
level-name=world
level-type=minecraft\:flat
generator-settings={"layers"\:[{"block"\:"minecraft\:bedrock","height"\:1}],"biome"\:"minecraft\:plains"}
generate-structures=false
spawn-protection=0
view-distance=$VIEW
simulation-distance=4
max-players=20
white-list=false
enforce-whitelist=false
motd=Morni Angelkoenig Test
allow-flight=true
EOF
# Testwerte: schneller Biss, festes Wetter und feste Zeit (der Test stellt beides selbst), Turniere nur per Befehl.
cat > "$SERVER/plugins/MorniAngelkoenig/config.yml" <<'EOF'
welt: angelkoenig
welt-ablauf:
  tageszeit-laeuft: false
  wetter-wechselt: false
  wetter-min-sekunden: 360
  wetter-max-sekunden: 900
angeln:
  biss-min-sekunden: 1
  biss-max-sekunden: 2
  startgeld: 25
turnier:
  an: true
  alle-minuten: 600
  dauer-minuten: 5
  vorwarnung-sekunden: 60
  mindestens-spieler: 2
  preise: [250, 120, 60]
  teilnahme: 15
EOF
# Test-Netzwerk: Bert spricht Englisch, Anna und Carl sind eine Party (Anna führt).
cat > "$SERVER/plugins/MoinMorni/fake.properties" <<'EOF'
lang.Bert=en
party.Anna=Anna,Carl
EOF
rm -f "$SERVER/plugins/MoinMorni/calls.log"
rm -rf "$SERVER/plugins/MorniAngelkoenig/spieler" "$SERVER/plugins/MorniAngelkoenig/bestenliste.yml"
rm -rf "$SERVER/world/playerdata" "$SERVER/world/players" 2>/dev/null || true
find "$SERVER" -maxdepth 3 -type d -name playerdata -path '*world*' -exec rm -rf {} + 2>/dev/null || true
if [ "${FRESH:-0}" = "1" ]; then
  rm -rf "$SERVER/angelkoenig" "$SERVER/plugins/MorniAngelkoenig/welt.yml"
fi
cp "$REPO/target/Morni-Angelkoenig.jar" "$SERVER/plugins/"
cp "$REPO/test/fake-network/target/MoinMorni-FakeNetwork.jar" "$SERVER/plugins/"
for jar in ${EXTRA_PLUGINS:-}; do
  cp "$jar" "$SERVER/plugins/"
done

echo "== Server starten (Port $PORT, RCON $RCON_PORT)"
cd "$SERVER"
: > console.log
nice -n 15 "$JAVA" -Xms1G -Xmx2G -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -jar paper.jar --nogui < /dev/null > console.log 2>&1 &
PID=$!
echo "$PID" > server.pid
stop_server() {
  if kill -0 "$PID" 2>/dev/null; then
    node "$HERE/rcon.cjs" "127.0.0.1:$RCON_PORT" "$SERVER/.rcon-pass" stop >/dev/null 2>&1 || kill "$PID" || true
    for _ in $(seq 1 60); do kill -0 "$PID" 2>/dev/null || break; sleep 1; done
    kill -9 "$PID" 2>/dev/null || true
  fi
}
if [ "${KEEP:-0}" != "1" ]; then
  trap stop_server EXIT
fi

for _ in $(seq 1 300); do
  if grep -q "ist bereit: " console.log && grep -q "Done (" console.log; then break; fi
  if ! kill -0 "$PID" 2>/dev/null; then echo "Server ist abgestürzt:"; tail -40 console.log; exit 1; fi
  sleep 1
done
grep -q "ist bereit: " console.log || { echo "Angelkoenig wurde nicht bereit:"; tail -40 console.log; exit 1; }
grep -E "Tal bereit|ist bereit: |Netzwerk|Unbekannte" console.log || true

if [ "${SKIP_TEST:-0}" = "1" ]; then
  echo "Server läuft (PID $PID) – anhalten mit: node $HERE/rcon.cjs 127.0.0.1:$RCON_PORT $SERVER/.rcon-pass stop"
  exit 0
fi

echo "== Test-Spieler"
STATUS=0
node "$HERE/angelkoenig-test.cjs" 127.0.0.1 "$PORT" "127.0.0.1:$RCON_PORT" "$SERVER/.rcon-pass" "$SERVER" || STATUS=$?

echo "== Fehler im Server-Log:"
if grep -qE "ERROR|Exception" console.log; then
  grep -E "ERROR|Exception" console.log | head -20
else
  echo "keine"
fi
exit $STATUS
