#!/usr/bin/env bash
set -euo pipefail

MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-admin}"
SQL_PATH="${1:-$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/database/schema.sql}"

if [[ ! -f "$SQL_PATH" ]]; then
  echo "SQL file not found: $SQL_PATH" >&2
  exit 1
fi

echo "Running SQL script: $SQL_PATH"
mysql -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" < "$SQL_PATH"
