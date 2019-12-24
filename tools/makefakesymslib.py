asm_header = """
        .arch armv5te
        .fpu softvfp
        .eabi_attribute 20, 1
        .eabi_attribute 21, 1
        .eabi_attribute 23, 3
        .eabi_attribute 24, 1
        .eabi_attribute 25, 1
        .eabi_attribute 26, 2
        .eabi_attribute 30, 4
        .eabi_attribute 34, 0
        .eabi_attribute 18, 4
        .file   "fakesymstubs_arm32.c"
        .text
        .align  2
"""
asm_perfunc_template = """
        .global {name} 
        .type   {name}, %function
{name}:
        ldr     ip, .L{name}2
.L{name}PIC0:
        add     ip, pc, ip 
        ldr     ip, [ip]
        bx      ip 
.L{name}3:
        .align  2
.L{name}2:
        .word   {varname}-(.L{name}PIC0+8)
        .size   {name}, .-{name}
        .hidden {name}
"""

asm_footer = """
        .ident  "GCC: (GNU) 4.8"
        .section        .note.GNU-stack,"",%progbits
"""
def makeFakeSymLibArm(fakeSyms, outfile):
	print(asm_header, file=outfile)
	for sym in fakeSyms:
		if (sym[1] & 1) == 0:
			continue
		print(asm_perfunc_template.format(name=sym[0], varname="bl_addr_" + sym[0]), file=outfile)
	print(asm_footer, file=outfile)

def makeFakeSymPtrs(fakeSyms, outfile):
        print("#include <stdint.h>", file=outfile)
        for sym in fakeSyms:
                if (sym[1] & 1) == 0:
                        continue
                print("void* bl_addr_{};".format(sym[0]), file=outfile)
        print("struct bl_sym_pair { const char* name; uintptr_t offset; };", file=outfile);
        print("struct bl_sym_pair bl_sym_pairs[] = {", file=outfile)
        for sym in fakeSyms:
            print("{\"" + sym[0] + "\", " + hex(sym[1]) + "},", file=outfile)
        print("};", file=outfile)
        print("int bl_sym_pairs_count = " + str(len(fakeSyms)) + ";", file=outfile)
        print("void bl_fakeSyms_initOneAddress(void**, uintptr_t, uintptr_t);", file=outfile);
        print("void bl_fakeSyms_initStubs(uintptr_t baseAddr) {", file=outfile)
        for sym in fakeSyms:
                if (sym[1] & 1) == 0:
                        continue
                print("bl_fakeSyms_initOneAddress(&bl_addr_{}, baseAddr, {});".format(sym[0], hex(sym[1])), file=outfile)
        print("}", file=outfile)
import sys
def readFakeSymInput(filename):
        fakesyms = []
        with open(filename, "r") as infile:
                for l in infile:
                        parts = l.strip().split(",")
                        fakesyms.append((parts[0], int(parts[1], 16)))
        return fakesyms
fakesyms = readFakeSymInput("find_symbols/combined_check2_out.csv")
fakesyms.sort(key=lambda a:a[0])
with open("fakesymstubs_arm32.s", "w") as outfile:
        makeFakeSymLibArm(fakesyms, outfile)
with open("fakesym_ptrs.c", "w") as outfile:
        makeFakeSymPtrs(fakesyms, outfile)
