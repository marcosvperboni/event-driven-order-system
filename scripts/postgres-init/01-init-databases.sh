#!/bin/bash
set -e

# Read the secret directly from its mounted file instead of trusting that
# $POSTGRES_PASSWORD (resolved by the entrypoint from POSTGRES_PASSWORD_FILE)
# is still exported by the time init scripts run - on this stack it silently
# came through empty, creating every app role with a blank password.
POSTGRES_PASSWORD="$(cat /run/secrets/postgres_password)"

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE DATABASE orders_db;
    CREATE DATABASE payments_db;
    CREATE DATABASE inventory_db;
    CREATE DATABASE notifications_db;

    CREATE USER orders_user WITH PASSWORD '$POSTGRES_PASSWORD';
    CREATE USER payments_user WITH PASSWORD '$POSTGRES_PASSWORD';
    CREATE USER inventory_user WITH PASSWORD '$POSTGRES_PASSWORD';
    CREATE USER notifications_user WITH PASSWORD '$POSTGRES_PASSWORD';

    GRANT ALL PRIVILEGES ON DATABASE orders_db TO orders_user;
    GRANT ALL PRIVILEGES ON DATABASE payments_db TO payments_user;
    GRANT ALL PRIVILEGES ON DATABASE inventory_db TO inventory_user;
    GRANT ALL PRIVILEGES ON DATABASE notifications_db TO notifications_user;
EOSQL

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "orders_db" \
    -c "GRANT ALL ON SCHEMA public TO orders_user;"
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "payments_db" \
    -c "GRANT ALL ON SCHEMA public TO payments_user;"
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "inventory_db" \
    -c "GRANT ALL ON SCHEMA public TO inventory_user;"
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "notifications_db" \
    -c "GRANT ALL ON SCHEMA public TO notifications_user;"
