#!/usr/bin/env bash
#
# Run and drive a Minecraft mod without a window ever showing up on screen. Pure Wayland, no X11,
# no XWayland.
#
# The client lives inside a sway session started on the headless backend: a real wlroots
# compositor with a virtual output, rendered by the graphics card, that nobody sees. sway rather
# than cage because its IPC lets us set the resolution and find the game window again.
#
# Native Wayland display comes from the WayGL mod, which must sit in run/mods along with
# fabric-api and cloth-config.
#
# Measured limitation, not a guess: the game under WayGL ignores virtual keyboard and pointer. A
# terminal in the same sway session receives them fine, the game never does, even when a virtual
# keyboard exists before it starts. So drive the dedicated server through `cmd`, and drive the
# client through Fabric's client gametest API rather than key/click.
#
# State lives under <mod>/build/headless/: `start` returns, later calls act on the running
# session, `stop` tears it all down.
#
# Prerequisite: sudo dnf install sway wtype wlrctl grim wf-recorder
#
# Usage: scripts/headless-test.sh start [--client|--server|--both] [--world NAME]
#                                       [--size 1280x720] [--timeout 300] [--video] [--software]
#                                       [--sound] [--env K=V]...
#        scripts/headless-test.sh cmd "time set night"
#        scripts/headless-test.sh chat "/gamemode creative"
#        scripts/headless-test.sh key Escape | click left | move 640 360
#        scripts/headless-test.sh shot after-spawn
#        scripts/headless-test.sh logs [client|server] [lines]
#        scripts/headless-test.sh status | stop
set -euo pipefail

MOD_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
STATE="$MOD_DIR/build/headless"
LOGS="$STATE/logs"
SHOTS="$STATE/shots"
OUTPUT="HEADLESS-1"
started=1  # only cmd_start flips this to 0 while it builds a session

die() { echo "headless-test: $*" >&2; exit 1; }

need() { command -v "$1" >/dev/null || die "$1 missing: sudo dnf install sway wtype wlrctl grim wf-recorder"; }

read_state() {
  # A --server run never starts sway, so the wayland file is the wrong witness of a live session:
  # it made cmd, logs and shot refuse to work on a server that was running fine.
  [ -f "$STATE/wayland" ] || [ -f "$STATE/server.pid" ] || die "no session running (call 'start' first)"
  if [ -f "$STATE/wayland" ]; then
    export WAYLAND_DISPLAY="$(cat "$STATE/wayland")"
    export SWAYSOCK="$(cat "$STATE/swaysock")"
  fi
  unset DISPLAY
}

# Wait for the success line, but give up at once on a failure line: without this second pattern a
# startup crash costs the whole delay before anyone hears about it.
FAILED_PATTERN='Failed to start|A problem occurred|FAILURE: Build|Exception in thread "main"|already locked'

wait_for_line() {
  local file="$1" pattern="$2" limit="$3" waited=0
  while [ "$waited" -lt "$limit" ]; do
    if [ -f "$file" ]; then
      grep -qE "$pattern" "$file" && return 0
      grep -qE "$FAILED_PATTERN" "$file" && return 2
    fi
    sleep 1
    waited=$((waited + 1))
  done
  return 1
}

explain_failure() {
  local file="$1"
  grep -m1 -E "$FAILED_PATTERN" "$file" 2>/dev/null | sed 's/^/  /'
  die "startup failed, full log: $file"
}

focus_game() {
  # One window normally lives in this session, but a crash dialog or ModMenu can steal focus.
  swaymsg -q '[app_id="^(minecraft|Minecraft|com.mojang).*"] focus' 2>/dev/null || true
}

# The Gradle daemon starts the game outside the script's process group, so killing the group
# leaves the server JVM alive, holding the lock on run/server/world and failing the next run with
# "already locked". Match the JVM by the mod path in its command line, which spares other mods.
mc_pids() { pgrep -f "fabric.dli.config=$MOD_DIR" 2>/dev/null || true; }

kill_mc() {
  local pids; pids="$(mc_pids)"
  [ -n "$pids" ] || return 0
  kill $pids 2>/dev/null || true
  sleep 4
  pids="$(mc_pids)"
  [ -n "$pids" ] && kill -9 $pids 2>/dev/null || true
}

kill_group() {
  local file="$1" sig="${2:--TERM}"
  [ -f "$file" ] || return 0
  local pid; pid="$(cat "$file")"
  kill "$sig" -- "-$pid" 2>/dev/null || true
}

cmd_start() {
  local mode=both world="" size=1280x720 video=0 software=0 limit=300 sound=0
  local -a extra_env=()
  while [ $# -gt 0 ]; do
    case "$1" in
      --client)   mode=client ;;
      --server)   mode=server ;;
      --both)     mode=both ;;
      --world)    world="$2"; shift ;;
      --size)     size="$2"; shift ;;
      --video)    video=1 ;;
      --timeout)  limit="$2"; shift ;;
      --software) software=1 ;;
      --sound)    sound=1 ;;
      --env)      extra_env+=("$2"); shift ;;
      *) die "unknown option: $1" ;;
    esac
    shift
  done

  { [ -f "$STATE/wayland" ] || [ -f "$STATE/server.pid" ]; } && die "a session is already running (see 'status', or 'stop')"
  [ -n "$(mc_pids)" ] && die "an instance of this mod is still alive ($(mc_pids | tr '\n' ' ')): call 'stop'"
  [ -x "$MOD_DIR/gradlew" ] || die "no gradlew in $MOD_DIR"
  [ "$mode" = server ] || { need sway; need grim; }

  rm -rf "$STATE"
  mkdir -p "$LOGS" "$SHOTS"

  # A daemon left over from an earlier build carries the environment it was started with, and Loom
  # launches the game FROM the daemon: the client then connects to the desktop compositor instead of
  # the headless one and its window opens on the real screen. --no-daemon does not help, since an
  # existing compatible daemon is still reused. Stopping them first is the only reliable fix.
  "$MOD_DIR/gradlew" --stop >/dev/null 2>&1 || true

  # The guard is armed BEFORE anything is launched, not after. Armed at the end, a client that
  # fails to start makes the script exit early and leaves the server and sway running with no
  # limit at all, until someone calls stop by hand.
  setsid bash -c "sleep $limit; \"$0\" stop >> \"$LOGS/watchdog.log\" 2>&1" &
  echo $! > "$STATE/watchdog.pid"
  # And a failed startup takes the whole session down with it right away, instead of waiting out
  # the timeout with a half-built session on the machine.
  trap '[ "$started" = 1 ] || cmd_stop >/dev/null 2>&1 || true' EXIT
  started=0

  # A test that starts playing zombie noises through the speakers while someone works is
  # unbearable. OpenAL on the null driver: nothing comes out, and options.txt is left alone so
  # normal play keeps its volume.
  [ "$sound" = 1 ] || extra_env+=(ALSOFT_DRIVERS=null)

  # OpenCL on Fedora needs two nudges, neither of which needs root, or a mod doing GPU compute
  # silently falls back to the CPU and its GPU checks report as skipped rather than failed:
  # Mesa's rusticl exposes no device unless RUSTICL_ENABLE names the driver, and JOCL dlopens the
  # unversioned libOpenCL.so, which Fedora ships only in the -devel package.
  local loader
  for loader in /usr/lib64/libOpenCL.so.1 /usr/lib/x86_64-linux-gnu/libOpenCL.so.1; do
    [ -e "$loader" ] || continue
    mkdir -p "$STATE/opencl"
    ln -sf "$loader" "$STATE/opencl/libOpenCL.so"
    extra_env+=("RUSTICL_ENABLE=${RUSTICL_ENABLE:-radeonsi}"
                "LD_LIBRARY_PATH=$STATE/opencl${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}")
    break
  done

  if [ "$mode" != server ]; then
    ls "$MOD_DIR"/run/mods/waygl-*.jar >/dev/null 2>&1 \
      || echo "headless-test: WayGL missing from run/mods, the client will look for X11 and fail" >&2

    # sway writes its own socket name: wlroots picks wayland-N by itself and nothing else tells
    # us which one for sure.
    cat > "$STATE/sway.conf" <<CONF
output $OUTPUT resolution $size
default_border none
focus_follows_mouse no
exec sh -c 'printf "%s" "\$WAYLAND_DISPLAY" > "$STATE/wayland"; printf "%s" "\$SWAYSOCK" > "$STATE/swaysock"'
CONF
    # WLR_BACKENDS, plural: that is the name wlroots 0.19 reads. With the old WLR_BACKEND alone,
    # sway falls back to the Wayland backend and opens a real "wlroots - WL-1" window on screen,
    # exactly what this script exists to avoid.
    # Without an explicit render node the headless backend has no DRM fd, wlroots logs "Failed to get
  # backend DRM FD" and the game surface is never mapped: sway shows an empty workspace and every
  # screenshot comes back black while the game itself runs fine.
  local render_node=/dev/dri/renderD128
  [ -e "$render_node" ] || render_node="$(ls /dev/dri/renderD* 2>/dev/null | head -1)"
  local -a sway_env=(WLR_BACKENDS=headless WLR_LIBINPUT_NO_DEVICES=1 WLR_HEADLESS_OUTPUTS=1)
  [ -n "$render_node" ] && sway_env+=(WLR_RENDER_DRM_DEVICE="$render_node")
    [ "$software" = 1 ] && sway_env+=(WLR_RENDERER_ALLOW_SOFTWARE=1 LIBGL_ALWAYS_SOFTWARE=1)
    setsid env "${sway_env[@]}" sway -c "$STATE/sway.conf" >"$LOGS/sway.log" 2>&1 &
    echo $! > "$STATE/sway.pid"
    wait_for_line "$STATE/wayland" '.' 20 || die "sway did not start (see $LOGS/sway.log)"
    export WAYLAND_DISPLAY="$(cat "$STATE/wayland")"
    export SWAYSOCK="$(cat "$STATE/swaysock")"
    # A virtual keyboard kept alive: a headless seat owns no device, and a client that starts on
    # a seat with no keyboard may never subscribe to keys. Shift pressed then released changes
    # nothing in game. It does not make the game read keys either, see the header note; it costs
    # nothing and rules the seat out as a cause.
    setsid bash -c 'while true; do wtype -s 60000 -k Shift_L -k Shift_L; done' \
      >"$LOGS/keyboard.log" 2>&1 &
    echo $! > "$STATE/keyboard.pid"
    sleep 1
  fi

  if [ "$mode" != client ]; then
    # The keeper process holds the pipe open: without it the server reads end-of-file on the
    # first command written and shuts itself down.
    mkfifo "$STATE/server.in"
    setsid sleep infinity > "$STATE/server.in" &
    echo $! > "$STATE/holder.pid"
    setsid env "${extra_env[@]}" "$MOD_DIR/gradlew" --no-daemon -p "$MOD_DIR" runServer --console=plain \
      < "$STATE/server.in" > "$LOGS/server.log" 2>&1 &
    echo $! > "$STATE/server.pid"
    wait_for_line "$LOGS/server.log" 'Done \(|For help, type' 240 \
      || { [ $? = 2 ] && explain_failure "$LOGS/server.log"; die "server never finished starting (see $LOGS/server.log)"; }
  fi

  if [ "$mode" != server ]; then
    local -a args=()
    # A single "--args=..." token: split in two, Gradle rejects the option. The inner quotes are
    # for the world name, which contains spaces.
    [ -n "$world" ] && args=("--args=--quickPlaySingleplayer \"$world\"")
    # DISPLAY removed and XDG_SESSION_TYPE=wayland: if anything still reaches for X11 we want a
    # blunt failure in the log rather than a window opening on the real screen.
    # -PheadlessWayland duplicates WAYLAND_DISPLAY on purpose: a project property travels with the
    # build request, while the daemon keeps the environment it was first started with and hands the
    # game the desktop compositor. A mod whose build script ignores the property still opens its
    # window on the real screen.
    setsid env -u DISPLAY WAYLAND_DISPLAY="$WAYLAND_DISPLAY" XDG_SESSION_TYPE=wayland \
      "${extra_env[@]}" \
      "$MOD_DIR/gradlew" --no-daemon -p "$MOD_DIR" runClient --console=plain -PheadlessWayland="$WAYLAND_DISPLAY" "${args[@]}" \
      > "$LOGS/client.log" 2>&1 &
    echo $! > "$STATE/client.pid"
    wait_for_line "$LOGS/client.log" 'Sound engine started|Starting Integrated|Created:.*minecraft:textures' 300 \
      || { [ $? = 2 ] && explain_failure "$LOGS/client.log"; die "client never reached the menu (see $LOGS/client.log)"; }
  fi

  if [ "$video" = 1 ]; then
    need wf-recorder
    setsid wf-recorder -o "$OUTPUT" -f "$STATE/session.mp4" >"$LOGS/wf-recorder.log" 2>&1 &
    echo $! > "$STATE/video.pid"
  fi

  started=1
  echo "session ready (mode $mode), automatic shutdown in ${limit}s -> $STATE"
}

cmd_cmd() {
  read_state
  [ -p "$STATE/server.in" ] || die "no server in this session"
  printf '%s\n' "$*" > "$STATE/server.in"
}

cmd_chat() {
  read_state
  need wtype
  echo "headless-test: WayGL ignores virtual input, this will most likely do nothing" >&2
  focus_game
  wtype -k t
  sleep 0.3
  wtype -d 12 -- "$*"
  wtype -k Return
}

cmd_shot() {
  read_state
  local name="${1:-shot}"
  local out="$SHOTS/$(date +%H%M%S)-$name.png"
  grim -o "$OUTPUT" "$out"
  echo "$out"
}

cmd_logs() {
  read_state
  local which="${1:-client}" lines="${2:-60}"
  local f="$LOGS/$which.log"
  [ -f "$f" ] || die "no $which log"
  echo "== $f"
  tail -n "$lines" "$f"
}

cmd_status() {
  if [ ! -f "$STATE/wayland" ] && [ ! -f "$STATE/server.pid" ]; then echo "no session"; return; fi
  [ -f "$STATE/wayland" ] && echo "socket   $(cat "$STATE/wayland")"
  local p
  for p in sway server client video watchdog; do
    [ -f "$STATE/$p.pid" ] || continue
    local pid; pid="$(cat "$STATE/$p.pid")"
    kill -0 "$pid" 2>/dev/null && echo "$p alive ($pid)" || echo "$p dead ($pid)"
  done
  ls "$SHOTS" 2>/dev/null | sed 's/^/shot     /'
}

cmd_stop() {
  [ -d "$STATE" ] || { echo "no session"; return; }
  if [ -p "$STATE/server.in" ]; then
    # Writing into a pipe nobody reads blocks forever, and the server may already be dead by the
    # time someone asks for a shutdown.
    timeout 3 sh -c "printf 'stop\n' > '$STATE/server.in'" 2>/dev/null || true
    sleep 5
  fi
  [ -f "$STATE/video.pid" ] && kill -INT "$(cat "$STATE/video.pid")" 2>/dev/null || true
  kill_group "$STATE/client.pid"
  kill_group "$STATE/server.pid"
  sleep 3
  kill_group "$STATE/client.pid" -KILL
  kill_group "$STATE/server.pid" -KILL
  kill_mc
  [ -f "$STATE/holder.pid" ] && kill "$(cat "$STATE/holder.pid")" 2>/dev/null || true
  kill_group "$STATE/watchdog.pid"
  kill_group "$STATE/keyboard.pid"
  kill_group "$STATE/sway.pid"
  rm -f "$STATE/wayland" "$STATE/swaysock" "$STATE/server.in" "$STATE"/*.pid
  echo "session stopped, logs kept in $LOGS"
}

action="${1:-}"; shift || true
case "$action" in
  start)  cmd_start "$@" ;;
  cmd)    cmd_cmd "$@" ;;
  chat)   cmd_chat "$@" ;;
  key)    read_state; need wtype; focus_game; wtype -k "$1" ;;
  click)  read_state; need wlrctl; wlrctl pointer click "${1:-left}" ;;
  move)   read_state; need wlrctl; wlrctl pointer move "$1" "$2" ;;
  shot)   cmd_shot "$@" ;;
  logs)   cmd_logs "$@" ;;
  status) cmd_status ;;
  stop)   cmd_stop ;;
  *)      die "expected action: start | cmd | chat | key | click | move | shot | logs | status | stop" ;;
esac
