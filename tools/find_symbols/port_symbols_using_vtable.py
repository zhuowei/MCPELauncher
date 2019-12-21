import os
import pickle

ida = False

def u32(a, i):
    return a[i] | (a[i+1] << 8) | (a[i+2] << 16) | (a[i+3] << 24)
def u64(a, i):
    return u32(a, i) | (u32(a, i + 4) << 32)
def portTable(tableStart, sourceTableSize, sourceBytes, sourceRSyms, destBytes, destTableStart):
    destTableFileOffset = -0x1000
    sourceTableFileOffset = -0x200000
    if ida:
        print("//", hex(tableStart), hex(sourceTableSize))
    for tableIndex in range(0, sourceTableSize // 8):
        sourceVtableAddress = u64(sourceBytes, tableStart + sourceTableFileOffset + tableIndex*8)
        if not sourceVtableAddress in sourceRSyms:
            continue
        destAddress = destTableStart + destTableFileOffset + tableIndex * 4
        sourceSymName = sourceRSyms[sourceVtableAddress]
        destSymAddress = u32(destBytes, destAddress)
        if ida:
            print("//", tableIndex, sourceSymName, hex(destAddress), hex(destSymAddress))
        if destSymAddress == 0:
            continue
        if ida:
            print("MakeNameEx({}, \"{}\", SN_NOWARN|SN_NOCHECK);".format(hex(destSymAddress & ~1), sourceSymName))
        else:
            print(sourceSymName, hex(destSymAddress), sep=",")

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
sourceSyms, sourceRsyms = readSyms("/Users/zhuowei/Downloads/bedrock-server-1.14.1.4/syms.txt")
with open("/Users/zhuowei/Downloads/bedrock-server-1.14.1.4/bedrock_server", "rb") as infile:
    sourceData = infile.read()
with open("/Users/zhuowei/mcpe1141/lib/armeabi-v7a/libminecraftpe.so", "rb") as infile:
    destData = infile.read()
tables = {
    "_ZTV12ServerPlayer": 0x4f4ebc8,
    ## NO "_ZTV11LocalPlayer": 0x4e08d98
    "_ZTV4Item": 0x4f79038,
    "_ZTV11BlockLegacy": 0x4f7fda4,
    "_ZTV11ServerLevel": 0x4f4ea6c,
    ## "_ZTV5Level": 0x4f0f9dc,
    "_ZTV19ServerCommandOrigin": 0x4f4e0b0,
    ## "_ZTV18ItemSpriteRenderer": 0x4e40518,
    ##"_ZTV10WitherBoss": 0x4e3d504,
    "_ZTV8GameMode": 0x4f76658,
    ##"_ZTV12ShapedRecipe": 0x4e56c24,
    ##"_ZTV11BannerBlock": 0x4e89cb4,
    ##"_ZTV12EmptyMapItem": 0x4e56ed8,
    ##"_ZTV9ArmorItem": 0x4e555c4,
    "_ZTV3Mob": 0x4f643a8,
    "_ZTV5Actor": 0x4f4fd90,
    "_ZTV20ServerNetworkHandler": 0x4f4d0c0,
}

if ida:
    print("""
#include <idc.idc>

static main(void)
{
""")

for tableName in tables:
    portTable(sourceSyms[tableName][0], sourceSyms[tableName][1], sourceData, sourceRsyms, destData, tables[tableName])
    if not ida:
        print(tableName, hex(tables[tableName]), sep=",")
if ida:
    print("}")
# hex editor: 0x540B2D8
# Ghidra: 541b2d8
# hex editor: 0x4AE1E83
# Ghidra: 0x04af1e83
