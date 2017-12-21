#!/usr/bin/env bash

codex_inventory_instance_id=${1:-}
tenant_id=${2:-demo_tenant}

echo "Unregistering Codex Inventory Module"
./unregister-managed.sh ${codex_inventory_instance_id} ${tenant_id}
