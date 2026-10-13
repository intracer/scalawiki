#!/usr/bin/env bash
# Weekly cache maintenance, independent of daily report publishing.
# Uses the same directory, jar, environment and lock as wlx-daily.sh.
# No reports or wiki writes are performed. Print only on failure for cron mail.
set -uo pipefail
export TZ=Europe/Kyiv
base="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$base" || exit 1
exec 9>"$base/wlx-daily.lock"
if ! flock -n 9; then
  echo "wlx-resync: another stats run is still going; retry later" >&2
  exit 1
fi
echo 1000 > "/proc/$$/oom_score_adj" 2>/dev/null || true
if [[ -f "$base/secrets.env" ]]; then
  set -a
  . "$base/secrets.env"
  set +a
fi
year=$(date +%Y)
(( $(date +%-m) < 10 )) && year=$((year - 1))
mkdir -p "$base/logs"
log="$base/logs/resync-$(date +%F-%H%M%S).txt"
export SW_JAR="${SW_JAR:-$base/scalawiki-wlx.jar}"
export JAVA_OPTS="${JAVA_OPTS:--Xmx1g} -XX:+ExitOnOutOfMemoryError"
nice -n 10 ionice -c3 "$base/run-stats.sh" \
  --campaign wlm-ua --start-year 2012 --year "$year" \
  --resync-only --no-progress "$@" > "$log" 2>&1
status=$?
if (( status != 0 )); then
  echo "wlx-resync: cache maintenance failed (exit $status); daily reports can use the existing caches; full output in $log"
  tail -n 60 "$log"
fi
exit "$status"
