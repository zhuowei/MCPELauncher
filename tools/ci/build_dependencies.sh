#!/bin/bash
set -e
cd ..
rm -r mcpe/mcpe180 || true
create-xbox-interop-lib/create-xbox-interop-lib.sh mcpe/mcpe180.apk mcpe/mcpe180
