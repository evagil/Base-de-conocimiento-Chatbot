#!/usr/bin/env bash
# Round trip on a disposable PostgreSQL cluster. No application credentials used.
set -euo pipefail
project_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
postgres_bin="${BC_PG_BIN:-$(pg_config --bindir)}"
test_directory="$(mktemp -d /tmp/bc-backup-test.XXXXXX)"
cleanup() {
  if [[ -f "$test_directory/data/postmaster.pid" ]]; then
    "$postgres_bin/pg_ctl" -D "$test_directory/data" -m fast -w stop >/dev/null
  fi
  rm -rf -- "$test_directory"
}
trap cleanup EXIT
"$postgres_bin/initdb" -D "$test_directory/data" -A trust --no-locale -E UTF8 -U bc_test >/dev/null
"$postgres_bin/pg_ctl" -D "$test_directory/data" -l "$test_directory/postgres.log" -o "-p 55441 -h 127.0.0.1 -k $test_directory" -w start >/dev/null
export PGHOST=127.0.0.1 PGPORT=55441 PGUSER=bc_test
"$postgres_bin/createdb" bc_backup_test
"$postgres_bin/createdb" bc_restore_test
"$postgres_bin/psql" -X -v ON_ERROR_STOP=1 -d bc_backup_test -c "create table prueba (id integer primary key, descripcion text); insert into prueba values (1,'Contenido recuperado');" >/dev/null
mkdir -p "$test_directory/storage/capturas"
printf 'contenido de imagen\n' > "$test_directory/storage/capturas/prueba.png"
export DB_URL=jdbc:postgresql://127.0.0.1:55441/bc_backup_test DB_USERNAME=bc_test DB_PASSWORD=''
export STORAGE_PATH="$test_directory/storage" BACKUP_PATH="$test_directory/backups"
python3 "$project_root/scripts/local/backup.py"
backup_directory=("$BACKUP_PATH"/copia-*)
python3 "$project_root/scripts/local/verify-backup.py" "${backup_directory[0]}"
"$postgres_bin/pg_restore" --dbname=bc_restore_test --no-owner --no-privileges --exit-on-error "${backup_directory[0]}/database.dump"
restored="$("$postgres_bin/psql" -X -At -d bc_restore_test -c 'select descripcion from prueba where id=1')"
[[ "$restored" == 'Contenido recuperado' ]]
cmp "$STORAGE_PATH/capturas/prueba.png" "${backup_directory[0]}/storage/capturas/prueba.png"
printf 'alterado' >> "${backup_directory[0]}/storage/capturas/prueba.png"
if python3 "$project_root/scripts/local/verify-backup.py" "${backup_directory[0]}" >/dev/null 2>&1; then
  echo 'No detectó la imagen alterada.' >&2; exit 1
fi
# A missing storage directory must not produce a successful backup.
export STORAGE_PATH="$test_directory/inexistente"
if python3 "$project_root/scripts/local/backup.py" >/dev/null 2>&1; then
  echo 'Aceptó almacenamiento inexistente.' >&2; exit 1
fi
printf 'Respaldo, restauración e integridad: correctos.\n'
