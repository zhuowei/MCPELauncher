#!/bin/bash
set -e
git submodule init
git submodule update

function clone() {
	git clone "$1" "$2"
	(cd "$2" && git checkout "$3")
}
curdirname="$(basename "$PWD")"
cd ..
clone "https://github.com/zhuowei/aFileChooser.git" "aFileChooser" "8d9e9989f46f9ce594add8b9945464791362b9af"
clone "https://github.com/Chainfire/libsuperuser.git" "libsuperuser" "c5872be26d8fb0231cc80699595a11a19e9a6bc4"
clone "https://github.com/zhuowei/MCPELauncher-app.git" "mcpelauncher-app" "5dc50213323c3116853a243cf8698b59a14e5e07"
clone "https://github.com/BlockLauncherApp/blocklauncher-create-xbox-interop-lib.git" "create-xbox-interop-lib" "6fe940cfca0659209be79902b32f6ec897e93bfb"
if [ "$curdirname" != "MCPELauncher" ]
then
	ln -s "$curdirname" "MCPELauncher"
fi
