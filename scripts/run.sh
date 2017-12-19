#!/bin/sh
# Script that runs this module against the folio/testing box
# We run this module locally and calls ther other modules inside the box
set -x
OKAPIURL=http://localhost:9130

java -jar ../target/mod-codex-inventory-fat.jar >fat.log 2>&1 &
PID=$!
sleep 2
tail -20 fat.log
cat > login.json <<END
{
  "username" : "diku_admin",
  "password" : "admin"
} 
END
curl -s -D login.res -o login.txt -HContent-Type:application/json -HX-Okapi-Tenant:diku -XPOST -d@login.json http://localhost:9130/authn/login
TOK=`grep -i x-okapi-token login.res`
H1=$TOK
H2=X-Okapi-Tenant:diku
H3=X-Okapi-URL:$OKAPIURL
curl "-H$H1" -H$H2 -H$H3 'http://localhost:8081/codex-instances?query=title%3Dwater'
sleep 1
curl "-H$H1" -H$H2 -H$H3 'http://localhost:8081/codex-instances'

kill -9 $PID
