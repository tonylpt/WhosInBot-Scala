#!/bin/bash

set -eo pipefail

: ${TELEGRAM_TOKEN:?"TELEGRAM_TOKEN was not specified."}

if [[ -z ${JDBC_DATABASE_URL} ]]; then
    if [[ -z ${DATABASE_URL} ]]; then
        >&2 echo "Neither JDBC_DATABASE_URL nor DATABASE_URL was specified."
        exit 1
    fi

    JDBC_DATABASE_URL=$(./database_url_to_jdbc.py ${DATABASE_URL})
fi

exec java $JAVA_OPTS -jar WhosInBot-Scala.jar "$@"
