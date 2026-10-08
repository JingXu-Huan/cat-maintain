#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

base=$(realpath -- "${1:?Deployment root required}")
release=$(realpath -- "${2:?Release directory required}")
[[ "$release" == "$base/releases/"* ]]
[[ ${release##*/} =~ ^[0-9a-f]{40}-[0-9]+-[0-9]+$ ]]
[[ -f "$base/.env" && -f "$release/backend.jar" && -f "$release/front/dist/index.html" ]]

exec 9> "$base/deploy.lock"
flock -w 600 9
set -a
# Only the trusted, server-owned configuration is sourced.
source "$base/.env"
set +a
[[ ${ADMIN_PASSWORD_HASH:-} =~ ^\$2[aby]\$[0-9]{2}\$[./A-Za-z0-9]{53}$ ]]
backup_database=${DEPLOY_BACKUP_DATABASE:-true}
[[ "$backup_database" == true || "$backup_database" == false ]]
export APP_VERSION=${release##*/}
previous=''
if [[ -L "$base/current" ]]; then
    previous=$(readlink -f "$base/current")
fi
services_stopped=false

compose() {
    docker compose --project-name cat-maintain --env-file "$base/.env" \
        -f "$release/deploy/compose.production.yaml" "$@"
}

recover() {
    result=$?
    trap - ERR
    compose ps >&2 || true
    compose logs --tail 80 backend web >&2 || true
    if [[ "$services_stopped" == true && -n "$previous" && -d "$previous" ]]; then
        echo "Restoring application release: ${previous##*/}" >&2
        APP_VERSION=${previous##*/} docker compose --project-name cat-maintain \
            --env-file "$base/.env" -f "$previous/deploy/compose.production.yaml" \
            up -d --no-build --wait --wait-timeout 180 backend web || true
    elif [[ "$services_stopped" == true ]]; then
        compose stop web backend || true
    fi
    echo 'Deployment failed. Schema is not automatically reverted.' >&2
    exit "$result"
}
trap recover ERR

# Build runtime images before interrupting the currently running application.
compose build --pull backend web
compose up -d --wait --wait-timeout 180 mysql
if [[ "$backup_database" == true ]]; then
    mkdir -p -- "$base/backups"
    compose exec -T mysql sh -c \
        'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysqldump -uroot --single-transaction --no-tablespaces "$MYSQL_DATABASE"' \
        | gzip > "$base/backups/$APP_VERSION.sql.gz"
else
    echo 'Database backup skipped (DEPLOY_BACKUP_DATABASE=false).'
fi

services_stopped=true
compose stop web backend
# This repository maintains its schema as an idempotent MySQL script.
compose exec -T mysql sh -c \
    'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot --default-character-set=utf8mb4 "$MYSQL_DATABASE"' \
    < "$release/db/init/init.sql"
compose exec -T mysql sh -c \
    'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot "$MYSQL_DATABASE"' <<SQL
UPDATE accounts
SET password_hash = '$ADMIN_PASSWORD_HASH'
WHERE username = 'admin'
  AND password_hash = '\$2a\$10\$tNxozLajZ1BFLANvBdV4d.tv0ebhpDWP.NbCV9ci/tgGk2xOA2MES';
SQL

compose up -d --no-build --wait --wait-timeout 180 backend web
curl --fail --silent --show-error --max-time 15 "http://127.0.0.1:${HTTP_PORT:-80}/api/health" \
    | grep -q '"status":"UP"'
curl --fail --silent --show-error --max-time 15 "http://127.0.0.1:${HTTP_PORT:-80}/api/products" > /dev/null
curl --fail --silent --show-error --max-time 15 "http://127.0.0.1:${HTTP_PORT:-80}/" > /dev/null

if [[ -n "$previous" ]]; then
    ln -sfn -- "$previous" "$base/previous"
fi
ln -sfn -- "$release" "$base/current"
trap - ERR
compose ps
echo "Deployed release: $APP_VERSION"
