#!/usr/bin/env bash
set -euo pipefail
umask 077
project_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$project_root"
for tool in java npm python3 pg_dump systemctl; do
  command -v "$tool" >/dev/null || { echo "Falta instalar $tool." >&2; exit 1; }
done
config_file="$project_root/backend/.env.local"
if [[ ! -f "$config_file" ]]; then
  read -rsp 'Contraseña de PostgreSQL para base_conocimiento: ' local_db_password
  echo
  [[ -n "$local_db_password" ]] || { echo 'La contraseña no puede estar vacía.' >&2; exit 1; }
  local_jwt_secret="$(python3 -c 'import secrets; print(secrets.token_hex(48))')"
  local_admin_password="$(python3 -c 'import secrets; print(secrets.token_urlsafe(32))')"
  {
    printf 'DB_URL=%q\n' 'jdbc:postgresql://localhost:5432/base_conocimiento'
    printf 'DB_USERNAME=%q\n' 'base_conocimiento'
    printf 'DB_PASSWORD=%q\n' "$local_db_password"
    printf 'JWT_SECRET=%q\n' "$local_jwt_secret"
    printf 'ADMIN_INITIAL_PASSWORD=%q\n' "$local_admin_password"
    printf 'STORAGE_PATH=%q\n' "$project_root/storage"
    printf 'BACKUP_PATH=%q\n' "$project_root/backups"
    printf 'SERVER_ADDRESS=%q\n' '127.0.0.1'
    printf 'SPRING_JPA_SHOW_SQL=%q\n' 'false'
    printf 'LOGGING_LEVEL_ORG_HIBERNATE_SQL=%q\n' 'INFO'
    printf 'CORS_ALLOWED_ORIGINS=%q\n' 'http://localhost:5173,http://127.0.0.1:5173'
  } > "$config_file"
  unset local_db_password local_jwt_secret local_admin_password
fi
chmod 600 "$config_file"
# Verify the configured connection and save a recovery copy before migrations/startup.
bash scripts/local/run.sh backup
cd "$project_root/backend"
bash mvnw -DskipTests package
local_jar="$(find target -maxdepth 1 -name '*.jar' ! -name 'base-conocimiento-local.jar' -print -quit)"
[[ -n "$local_jar" ]] || { echo 'No se encontró el ejecutable del backend.' >&2; exit 1; }
mkdir -p "$project_root/.local"
cp "$local_jar" "$project_root/.local/backend.jar.tmp"
mv "$project_root/.local/backend.jar.tmp" "$project_root/.local/backend.jar"
cd "$project_root/frontend"
npm ci
cd "$project_root"
python3 scripts/local/install.py
systemctl --user daemon-reload
systemctl --user enable base-conocimiento-backend.service base-conocimiento-frontend.service base-conocimiento-backup.timer
systemctl --user restart base-conocimiento-backend.service base-conocimiento-frontend.service base-conocimiento-backup.timer
printf '\nAbrí http://localhost:5173\nPara consultar el inicio: journalctl --user -u base-conocimiento-backend -n 60 --no-pager\n'
