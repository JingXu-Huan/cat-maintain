#!/usr/bin/env bash
# 在 Linux 服务器执行。显式补充或重置演示数据，不启动或停止应用服务。
set -Eeuo pipefail
umask 077

base=$(realpath -- "${1:?Usage: bash deploy/seed-demo.sh DEPLOYMENT_ROOT [SQL_FILE] [--reset] [--no-backup]}")
shift
script_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
seed_sql="$script_root/db/seed/demo.sql"
if [[ $# -gt 0 && "$1" != --* ]]; then
    seed_sql="$1"
    shift
fi
reset_data=0
create_backup=true
for argument in "$@"; do
    case "$argument" in
        --reset) reset_data=1 ;;
        --no-backup) create_backup=false ;;
        *) printf 'Unknown argument: %s\n' "$argument" >&2; exit 2 ;;
    esac
done
seed_sql=$(realpath -- "$seed_sql")
credentials="$base/demo.env"
container=cat-maintain-mysql-1
[[ -d "$base" && -f "$seed_sql" && -f "$credentials" ]]

# 与生产部署共用锁，导入期间不会并发升级数据库或替换 MySQL 容器。
exec 9> "$base/deploy.lock"
flock -w 60 9
[[ $(docker inspect -f '{{ index .Config.Labels "com.docker.compose.project" }}' "$container") == cat-maintain ]]
[[ $(stat -c '%a' "$credentials") == 600 ]]
# 此文件必须由服务器所有者维护，不能来源于用户输入或构建产物。
source "$credentials"
[[ ${DEMO_PASSWORD_HASH:-} =~ ^\$2[aby]\$[0-9]{2}\$[./A-Za-z0-9]{53}$ ]]

mysql() {
    docker exec -i "$container" sh -c \
        'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot --default-character-set=utf8mb4 --batch "$MYSQL_DATABASE"'
}
[[ $(printf 'SELECT DATABASE();\n' | mysql | sed -n '2p') == cat_maintain ]]

if [[ "$create_backup" == true ]]; then
    mkdir -p -- "$base/backups"
    backup=$(mktemp "$base/backups/demo-seed-$(date -u +%Y%m%dT%H%M%SZ)-XXXXXX.sql.gz")
    docker exec "$container" sh -c \
        'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysqldump -uroot --single-transaction --no-tablespaces "$MYSQL_DATABASE"' \
        | gzip > "$backup"
    printf 'Backup: %s\n' "$backup"
else
    printf 'Backup: skipped (--no-backup)\n'
fi

# 哈希已通过字符集校验；明文密码不进入 SQL、日志或进程参数。
{
    printf "SET @demo_password_hash = '%s';\n" "$DEMO_PASSWORD_HASH"
    printf 'SET @reset_demo_data = %s;\n' "$reset_data"
    cat -- "$seed_sql"
} | mysql

printf 'Imported: %s\n' "$seed_sql"
