#!/usr/bin/env bash
set -euo pipefail
project_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
config_file="$project_root/backend/.env.local"
if [[ ! -f "$config_file" ]]; then
  echo 'Primero ejecutá: bash scripts/local/setup.sh' >&2
  exit 1
fi
# This file is generated locally by setup.sh; never supplied by a remote source.
set -a
source "$config_file"
set +a
case "${1:-}" in
  backend)
    cd "$project_root/backend"
    exec java -jar "$project_root/.local/backend.jar"
    ;;
  frontend)
    cd "$project_root/frontend"
    exec npm run dev -- --host 127.0.0.1 --port 5173 --strictPort
    ;;
  backup)
    exec python3 "$project_root/scripts/local/backup.py"
    ;;
  *) echo 'Uso: run.sh backend|frontend|backup' >&2; exit 2 ;;
esac
