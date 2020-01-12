#!/bin/bash
set -e
function clone() {
	git clone "$1" "$2"
	git checkout "$3"
}
cd ..
clone "https://github.com/zhuowei/aFileChooser.git" "aFileChooser" "8d9e9989f46f9ce594add8b9945464791362b9af"
clone "https://github.com/Chainfire/libsuperuser.git" "libsuperuser" "c5872be26d8fb0231cc80699595a11a19e9a6bc4"
#clone "" "mcpelauncher-app" ""
