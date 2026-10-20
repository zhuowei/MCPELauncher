cmake_minimum_required(VERSION 3.22.1)
project(nexnative LANGUAGES CXX)

add_library(nexnative SHARED native_bridge.cpp)
target_compile_features(nexnative PRIVATE cxx_std_17)
target_compile_options(nexnative PRIVATE -O2 -Wall -Wextra -Wpedantic -fvisibility=hidden)

find_library(ANDROID_LOG_LIBRARY log REQUIRED)
find_library(ANDROID_LIBRARY android REQUIRED)

target_link_libraries(nexnative PRIVATE ${ANDROID_LOG_LIBRARY} ${ANDROID_LIBRARY})

# This library validates user-selected pack files only. It does not hook or patch
# Minecraft. GLES/EGL are intentionally omitted because this module does not render.
