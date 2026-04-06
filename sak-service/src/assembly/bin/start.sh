#!/usr/bin/env sh

set -eu

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
APP_HOME="$(CDPATH= cd -- "${SCRIPT_DIR}/.." && pwd)"
APP_DIR="${APP_HOME}/app"
LIB_DIR="${APP_HOME}/lib"
CONFIG_DIR="${APP_HOME}/config"
LOG_DIR="${APP_HOME}/logs"

mkdir -p "${LOG_DIR}"

APP_JAR="$(find "${APP_DIR}" -maxdepth 1 -name "*.jar" | head -n 1)"
if [ -z "${APP_JAR}" ]; then
  echo "No application jar found under ${APP_DIR}" >&2
  exit 1
fi

JAVA_BIN="${JAVA_HOME:-}"
if [ -n "${JAVA_BIN}" ]; then
  JAVA_BIN="${JAVA_BIN}/bin/java"
else
  JAVA_BIN="java"
fi

JAVA_OPTS="${JAVA_OPTS:-}"
SPRING_OPTS="${SPRING_OPTS:-}"

exec "${JAVA_BIN}" ${JAVA_OPTS} \
  -cp "${APP_JAR}:${LIB_DIR}/*" \
  com.superkiller.backend.BackendApplication \
  --spring.config.additional-location="optional:file:${CONFIG_DIR}/" \
  ${SPRING_OPTS} \
  "$@"
