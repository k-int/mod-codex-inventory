#!/usr/bin/env bash

module_direct_address=${1}
module_instance_id=${2}
okapi_proxy_address=${3:-http://localhost:9130}
tenant_id=${4:-demo_tenant}

discovery_json=$(cat ./target/Discovery.json)

discovery_json="${discovery_json/directaddresshere/$module_direct_address}"
discovery_json="${discovery_json/instanceidhere/$module_instance_id}"

curl -w '\n' -X POST -D -   \
     -H "Content-type: application/json"   \
     -d "${discovery_json}" \
     "${okapi_proxy_address}/_/discovery/modules"

curl -w '\n' -D - -s \
     -X POST \
     -H "Content-type: application/json" \
     -d @./target/ModuleDescriptor.json \
     "${okapi_proxy_address}/_/proxy/modules"

curl -w '\n' -X POST -D - \
     -H "Content-type: application/json" \
     -d @./target/Activate.json \
     "${okapi_proxy_address}/_/proxy/tenants/${tenant_id}/modules"

