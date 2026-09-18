#!/usr/bin/env bash
#
# PostToolUse hook: runs Spotless on the Maven module that owns the file Claude
# just wrote, so formatting never reaches CI broken.
#
# Why this exists: spotless:check is bound to the "validate" phase, so an unused
# import or a missing final newline fails `mvn verify` before a single test runs.
# Catching it here costs ~3s; catching it in CI costs a whole pipeline run.
#
# Reads the Claude Code hook payload as JSON on stdin. Uses python3 to parse it,
# matching .github/workflows/ci.yml, which already depends on python3.
#
# Never fails the tool call: every exit path returns 0 on purpose. A formatter
# that blocks Claude's edits is worse than a formatting slip caught by CI.

set -u

payload=$(cat)

file=$(printf '%s' "$payload" | python3 -c \
  "import sys, json
try:
    d = json.load(sys.stdin)
except Exception:
    sys.exit(0)
print(d.get('tool_input', {}).get('file_path')
      or d.get('tool_response', {}).get('filePath')
      or '')" 2>/dev/null)

case "$file" in
  *.java) ;;
  *) exit 0 ;;
esac

# Windows hands over backslash paths; normalise before splitting on /src/.
module="${file//\\//}"
module="${module%%/src/*}"

[ -f "$module/pom.xml" ] || exit 0

# -o (offline) keeps this at ~3s instead of ~8s: the plugin is already in the
# local repository and this hook must never wait on the network.
cd "$module" && mvn -q -o spotless:apply >/dev/null 2>&1

exit 0
