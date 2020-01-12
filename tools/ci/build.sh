#!/bin/bash
set -e
ndk-build
cd ../mcpelauncher-app
ant clean release
