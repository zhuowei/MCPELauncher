import os
import pickle
def u32(a, i):
    return a[i] | (a[i+1] << 8) | (a[i+2] << 16) | (a[i+3] << 24)
def u64(a, i):
    return u32(a, i) | (u32(a, i + 4) << 32)
def portTable(tableStart, sourceTableSize, sourceBytes, sourceRSyms, destBytes, destTableStart):
    destTableFileOffset = -0x1000
    sourceTableFileOffset = -0x200000
    print("//", hex(tableStart), hex(sourceTableSize))
    for tableIndex in range(0, sourceTableSize // 8):
        sourceVtableAddress = u64(sourceBytes, tableStart + sourceTableFileOffset + tableIndex*8)
        if not sourceVtableAddress in sourceRSyms:
            continue
        destAddress = destTableStart + destTableFileOffset + tableIndex * 4
        sourceSymName = sourceRSyms[sourceVtableAddress]
        destSymAddress = u32(destBytes, destAddress)
        print("//", tableIndex, sourceSymName, hex(destAddress), hex(destSymAddress))
        if destSymAddress == 0:
            continue
        print("MakeNameEx({}, \"{}\", SN_NOWARN|SN_NOCHECK);".format(hex(destSymAddress & ~1), sourceSymName))

def readSyms(filepath):
    if os.path.exists("syms.pickle"):
        with open("syms.pickle", "rb") as infile:
            return pickle.load(infile)
    symtab = {}
    rsymtab = {}
    with open(filepath, "r") as infile:
        for line in infile:
            l = line.strip().split(maxsplit=7)
            if len(l) != 7:
                continue
            symtab[l[6]] = (int(l[0], 16), int(l[4], 16))
            rsymtab[int(l[0], 16)] = l[6]
    output = (symtab, rsymtab)
    with open("syms.pickle", "wb") as outfile:
        pickle.dump(output, outfile)
    return output
#print(readSyms("/Users/zhuowei/Downloads/bedrock-server-1.13.3.0/syms.txt"))
sourceSyms, sourceRsyms = readSyms("/Users/zhuowei/Downloads/bedrock-server-1.13.3.0/syms2.txt")
with open("/Users/zhuowei/Downloads/bedrock-server-1.13.3.0/bedrock_server", "rb") as infile:
    sourceData = infile.read()
with open("/Users/zhuowei/mcpe1131/lib/armeabi-v7a/libminecraftpe.so", "rb") as infile:
    destData = infile.read()
tables = {
    #"_ZTV12ServerPlayer": 0x04e2d774
    # NO "_ZTV11LocalPlayer": 0x4e08d98
    #"_ZTV4Item": 0x4e57b88
    # "_ZTV11BlockLegacy": 0x4e5e5bc,
    "_ZTV5Level": 0x4e2d618
}

print("""
#include <idc.idc>

static main(void)
{
""")

for tableName in tables:
    portTable(sourceSyms[tableName][0], sourceSyms[tableName][1], sourceData, sourceRsyms, destData, tables[tableName])
print("}")
# hex editor: 0x540B2D8
# Ghidra: 541b2d8
# hex editor: 0x4AE1E83
# Ghidra: 0x04af1e83