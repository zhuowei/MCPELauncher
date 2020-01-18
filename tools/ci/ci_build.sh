#!/bin/bash
# Top level script run by CI to build BlockLauncher.
# Installs SDKs, fetch dependencies, builds dependencies, builds app
# Expects to be run from the root of the build directory
set -e
parent_dir="$(cd ..; pwd)"
export ANDROID_HOME="$parent_dir/android-sdk-linux_86"
export NDK_ROOT="$parent_dir/android-ndk-r10c"
export PATH="$PATH:$parent_dir:$parent_dir/android-sdk-linux_86/tools:$parent_dir/android-sdk-linux_86/build-tools/27.0.3:$NDK_ROOT"
# Download and extract dependencies
tools/ci/setup_sdks.sh
tools/ci/setup_repositories.sh
tools/ci/download_mc.sh
tools/ci/download_resources.sh
# Extract Xbox Live lib from Minecraft
tools/ci/build_dependencies.sh
# Prepare the repositories
tools/ci/prep_repositories.sh
tools/ci/setup_signing_key.sh
# Build!
tools/ci/build.sh
tools/ci/copy_artifact.sh
