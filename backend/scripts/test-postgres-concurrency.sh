#!/usr/bin/env bash
set -euo pipefail

# A separate local PostgreSQL instance. Never connects to DB_URL or modifies application data.
project_backend="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
postgres_bin="${BC_PG_BIN:-$(pg_config --bindir)}"
postgres_port="${BC_TEST_PORT:-55439}"
test_directory="$(mktemp -d "${TMPDIR:-/tmp}/bc-concurrency.XXXXXX")"
cleanup() {
  if [[ -f "$test_directory/data/postmaster.pid" ]]; then
    "$postgres_bin/pg_ctl" -D "$test_directory/data" -m fast -w stop >/dev/null
  fi
  # Keep diagnostics on failure; a successful test run removes only this temporary cluster.
  if [[ "${test_success:-false}" == true ]]; then
    rm -rf -- "$test_directory"
  else
    echo "Diagnóstico de PostgreSQL: $test_directory" >&2
  fi
}
trap cleanup EXIT
"$postgres_bin/initdb" -D "$test_directory/data" -A trust --no-locale -E UTF8 -U bc_test > "$test_directory/init.log"
"$postgres_bin/pg_ctl" -D "$test_directory/data" -l "$test_directory/server.log" \
  -o "-p $postgres_port -h 127.0.0.1 -k $test_directory" -w start
"$postgres_bin/createdb" -h 127.0.0.1 -p "$postgres_port" -U bc_test bc_concurrency_test

export BC_TEST_DB_URL="jdbc:postgresql://127.0.0.1:$postgres_port/bc_concurrency_test"
export BC_TEST_DB_USER=bc_test
export BC_TEST_DB_PASSWORD=''
cd "$project_backend"
bash mvnw clean verify "$@"
test_success=true
