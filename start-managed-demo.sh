#!/usr/bin/env bash

okapi_proxy_address=${1:-http://localhost:9130}

echo "Check if Okapi is contactable"
curl -w '\n' -X GET -D -   \
     "${okapi_proxy_address}/_/env" || exit 1

echo "Building Codex Inventory"

mvn clean package -q -Dmaven.test.skip=true || exit 1

echo "Running Inventory module"
./register-managed.sh
