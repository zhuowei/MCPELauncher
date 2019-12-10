#!/bin/bash
arm-linux-androideabi-objcopy -j .rodata -O binary ../../libs/armeabi-v7a/libmcpelauncher.so mcpelauncher_rodata.bin
strings mcpelauncher_rodata.bin |grep _Z|sort|uniq >syms_in_strings.txt
