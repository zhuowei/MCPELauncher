# you need thesyms.txt (objdump -T libminecraftpe.so) and
# readelfout.txt (readelf -a -W libminecraftpe.so)
resetline = None
vtables = []
with open("thesyms.txt", "r") as infile:
	for l in infile:
		if "6Entity5reset" in l:
			resetline = l.split()
		elif "_ZTV" in l:
			vtables.append(l.split())
#with open("libminecraftpe.so", "rb") as infile:
#	m = infile.read()
p = set()
with open("readelfout.txt", "r") as infile:
	for l in infile:
		if not ("R_ARM_ABS32" in l and "6Entity5reset" in l):
			continue
		p.add(int(l.split()[0], 16))
resetaddr = int(resetline[0], 16) + 1
for vt in vtables:
	vtaddr = int(vt[0], 16)
	#a = vtaddr + 20 - 0x1000
	#b = m[a] | m[a+1]<<8 | m[a+2] << 16 | m[a+3] << 24
	#if b == resetaddr:
	if (vtaddr + 44) in p:
		print(vt[6])
