def addSymsFromFile(filename, mapping):
    print("loading", filename)
    with open(filename, "r") as infile:
        for l in infile:
            if l.startswith("#"):
                continue
            l = l.strip().split(",")
            addr = int(l[1], 16)
            name = l[0]
            if name in mapping:
                print(name, "already in!")
            mapping[name] = addr


def readSyms(filepath):
    symtab = {}
    with open(filepath, "r") as infile:
        for line in infile:
            l = line.strip().split(maxsplit=7)
            if len(l) > 0 and l[0] == "00000000":
                continue
            if len(l) == 7:
                symtab[l[6]] = int(l[0], 16)
            elif len(l) == 6:
                symtab[l[5]] = int(l[0], 16)
    return symtab
    
def readSymsOnlyUndef(filepath):
    symtab = {}
    with open(filepath, "r") as infile:
        for line in infile:
            if not "*UND*" in line or not "_Z" in line:
                continue
            l = line.strip().split(maxsplit=6)
            if len(l) != 5:
                continue
            symtab[l[4]] = int(l[0], 16)
    return symtab

def getDlsymedSyms(filepath):
    symtab = {}
    with open(filepath, "r") as infile:
        for line in infile:
            l = line.strip()
            if len(l) == 0:
                continue
            symtab[l] = "00000000"
    return symtab

mapping = {}
addSymsFromFile("diaphora_outsyms.csv", mapping)
addSymsFromFile("hopeless_out.csv", mapping)
addSymsFromFile("manually_found_syms.csv", mapping)

mcpeSyms = {}
mcpeSyms.update(readSyms("mcpe1140_ownsyms.txt"))
mcpeSyms.update(readSyms("fmod1140_ownsyms.txt"))
mcpeSyms.update(readSyms("stdc++_ownsyms.txt"))

blSyms = readSymsOnlyUndef("bl_syms.txt") # todo: hand-grabbed syms
blSyms.update(getDlsymedSyms("syms_in_strings.txt"))
with open("combined_check2_out.csv", "w") as outfile:
    for sym in blSyms:
        if sym in mcpeSyms:
            continue
        if sym in mapping:
            print(sym, hex(mapping[sym]), sep=",", file=outfile)
        else:
            print(sym, "missing")
