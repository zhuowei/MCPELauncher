#!/bin/sh
infile=../PocketInvEditor/src/net/zhuoweizhang/pocketinveditor/entity/EntityType.java
outfile=src/net/zhuoweizhang/mcpelauncher/EntityType.java
cat >$outfile <<EOF
package net.zhuoweizhang.mcpelauncher;

public class EntityType {
EOF
grep "	[A-Z_]*(.*)" $infile |grep -v UNKNOWN|sed -e "s/\([A-Z_]*\)(\([0-9]*\),.*/public static final int \1 = \2;/" >>$outfile

cat >>$outfile <<EOF
}
EOF
