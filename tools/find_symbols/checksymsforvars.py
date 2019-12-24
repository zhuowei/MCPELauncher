import sys
def readFakeSymInput(filename):
        fakesyms = []
        with open(filename, "r") as infile:
                for l in infile:
                        parts = l.strip().split(",")
                        fakesyms.append((parts[0], int(parts[1], 16)))
        return fakesyms
syms = readFakeSymInput(sys.argv[1])
for s in syms:
	if s[1] & 1 == 0:
		print(s[0])
