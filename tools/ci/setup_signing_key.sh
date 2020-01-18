#!/bin/bash
rm ../mcpelauncher-app/blocklauncherkey.keystore || true
7z -o../mcpelauncher-app -p$SIGNING_KEY_7Z_PASSWORD x tools/ci/blocklauncherkey.7z
echo """key.store=blocklauncherkey.keystore
key.alias=blocklauncherkey
key.store.password=$SIGNING_KEY_PASSWORD
key.alias.password=$SIGNING_KEY_PASSWORD""" >../mcpelauncher-app/ant.properties
