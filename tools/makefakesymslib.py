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
        .file   "test.c"
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
        blx     ip 
.L{name}3:
        .align  2
.L{name}2:
        .word   {varname}-(.L{name}PIC0+8)
        .size   {name}, .-{name}
        .hidden {varname}
"""

asm_footer = """
        .ident  "GCC: (GNU) 4.8"
        .section        .note.GNU-stack,"",%progbits
"""
def makeFakeSymLibArm(fakeSyms, outfile):
	print(asm_header, file=outfile)
	for sym in fakeSyms:
		print(asm_perfunc_template.format(name=sym, varname="bl_addr_" + sym), file=outfile)
	print(asm_footer, file=outfile)
import sys
makeFakeSymLibArm(["aaa", "bbb"], sys.stdout)
