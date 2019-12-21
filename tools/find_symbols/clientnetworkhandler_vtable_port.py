import os
import pickle

ida = True

def u32(a, i):
    return a[i] | (a[i+1] << 8) | (a[i+2] << 16) | (a[i+3] << 24)
def u64(a, i):
    return u32(a, i) | (u32(a, i + 4) << 32)
def portTable(tableStart, sourceTableSize, sourceBytes, sourceRSyms, destBytes, destTableStart, destTableLegacyClientHandlerStart, destTableClientHandlerStart):
    destTableFileOffset = -0x1000
    sourceTableFileOffset = -0x200000

    if ida:
        print("//", hex(tableStart), hex(sourceTableSize))
    for tableIndex in range(0, sourceTableSize // 8):
        sourceVtableAddress = u64(sourceBytes, tableStart + sourceTableFileOffset + tableIndex*8)
        if not sourceVtableAddress in sourceRSyms:
            continue
        sourceSymName = sourceRSyms[sourceVtableAddress]
        # Now we have either a Classroom symbol or a NetEventCallback one.

        destAddress = destTableStart + destTableFileOffset + tableIndex * 4
        destSymAddress = u32(destBytes, destAddress)

        destAddressClientHandler = destTableClientHandlerStart + destTableFileOffset + tableIndex * 4
        destSymAddressClientHandler = u32(destBytes, destAddressClientHandler)

        destAddressLegacyClientHandler = destTableLegacyClientHandlerStart + destTableFileOffset + tableIndex * 4
        destSymAddressLegacyClientHandler = u32(destBytes, destAddressLegacyClientHandler)


        # If the ClientHandler doesn't override, then the Classroom impl and the ClientHandler would be same.

        isNetHandler = destSymAddressLegacyClientHandler == destSymAddress
        isLegacy = destSymAddressLegacyClientHandler != destSymAddressClientHandler
        newSymName = sourceSymName
        if not isNetHandler:
            replacement = "26LegacyClientNetworkHandler" if isLegacy else "20ClientNetworkHandler"
            newSymName = sourceSymName.replace("27ClassroomModeNetworkHandler", replacement).replace("16NetEventCallback", replacement)

        if ida:
            print("//", tableIndex, newSymName, hex(destAddressLegacyClientHandler), hex(destSymAddressLegacyClientHandler))
        if destSymAddressLegacyClientHandler == 0:
            continue
        if ida:
            #print("MakeNameEx({}, \"{}\", SN_NOWARN|SN_NOCHECK);".format(hex(destSymAddressClientHandler & ~1), "sub_" + hex(destSymAddressClientHandler & ~1)[2:]))
            print("MakeNameEx({}, \"{}\", SN_NOWARN|SN_NOCHECK);".format(hex(destSymAddressLegacyClientHandler & ~1), newSymName))

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
    "_ZTV27ClassroomModeNetworkHandler": 0x4f9e758,
    "_ZTV26LegacyClientNetworkHandler": 0x4f28488,
    "_ZTV20ClientNetworkHandler": 0x4f4319c,
}

if ida:
    print("""
#include <idc.idc>

static main(void)
{
""")

tableName = "_ZTV27ClassroomModeNetworkHandler"
portTable(sourceSyms[tableName][0], sourceSyms[tableName][1], sourceData, sourceRsyms, destData, tables[tableName], tables["_ZTV26LegacyClientNetworkHandler"], tables["_ZTV20ClientNetworkHandler"])

if ida:
    print("}")