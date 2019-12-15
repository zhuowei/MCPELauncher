import os
import pickle
import sqlite3
def readSyms(filepath):
    if os.path.exists("syms_1130b6.pickle"):
        with open("syms_1130b6.pickle", "rb") as infile:
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
    with open("syms_1130b6.pickle", "wb") as outfile:
        pickle.dump(output, outfile)
    return output
sourceSyms, sourceRsyms = readSyms("/Users/zhuowei/mcpe1130b6/lib/armeabi-v7a/syms2.txt")
conn = sqlite3.connect("diffout_1140.diaphora")
for a in conn.execute("select address, address2, name2 from results where ratio >= 0.8 and not description like '%same name%' and not name2 like 'sub_%'"):
    print(sourceRsyms[int(a[1], 16)] + "," + hex(int(a[0], 16) | 1))
