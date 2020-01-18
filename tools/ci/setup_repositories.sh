#!/bin/bash
set -e
function clone() {
	git clone "$1" "$2"
	git checkout "$3"
}
cd ..
clone "https://github.com/zhuowei/aFileChooser.git" "aFileChooser" "8d9e9989f46f9ce594add8b9945464791362b9af"
clone "https://github.com/Chainfire/libsuperuser.git" "libsuperuser" "c5872be26d8fb0231cc80699595a11a19e9a6bc4"
clone "git@github.com:zhuowei/mcpelauncher-app-staging.git" "mcpelauncher-app" "6b2f3e33b6df3e8eb3215248fb721604964ef4f4"
clone "https://github.com/BlockLauncherApp/blocklauncher-create-xbox-interop-lib.git" "create-xbox-interop-lib" "c7d1ac8724f33efd7740d471ba052626cb3b26c4"
