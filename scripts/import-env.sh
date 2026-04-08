#!/usr/bin/env sh

set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DEFAULT_ENV_FILE="${SCRIPT_DIR}/env/superkiller.env"

ENV_FILE="${DEFAULT_ENV_FILE}"
if [ "${1:-}" != "" ] && [ "$1" != "--" ]; then
  ENV_FILE="$1"
  shift
fi

if [ ! -f "$ENV_FILE" ]; then
  echo "Environment file not found: $ENV_FILE" >&2
  echo "Copy scripts/env/superkiller.env.example to scripts/env/superkiller.env first." >&2
  exit 1
fi

LOADED_COUNT=0

trim() {
  printf '%s' "$1" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//'
}

strip_quotes() {
  value="$1"
  case "$value" in
    \"*\") value=${value#\"}; value=${value%\"} ;;
    \'*\') value=${value#\'}; value=${value%\'} ;;
  esac
  printf '%s' "$value"
}

while IFS= read -r raw_line || [ -n "$raw_line" ]; do
  line=$(printf '%s' "$raw_line" | sed 's/\r$//')
  case "$line" in
    ''|'#'*) continue ;;
  esac

  key_part=${line%%=*}
  if [ "$key_part" = "$line" ]; then
    continue
  fi

  value_part=${line#*=}
  key=$(trim "$key_part")
  value=$(trim "$value_part")
  value=$(strip_quotes "$value")

  if [ -z "$key" ]; then
    continue
  fi

  export "$key=$value"
  LOADED_COUNT=$((LOADED_COUNT + 1))
done < "$ENV_FILE"

echo "Loaded ${LOADED_COUNT} environment variables from ${ENV_FILE}"

if [ "${1:-}" = "--" ]; then
  shift
fi

if [ "$#" -gt 0 ]; then
  exec "$@"
fi

echo "If you want these variables to remain in the current shell, run:"
echo ". \"$0\" \"$ENV_FILE\""
