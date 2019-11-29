with open("combined.csv", "r") as infile:
    mapping = {}
    for l in infile:
        l = l.strip().split(",")
        addr = int(l[1], 16)
        name = l[0]
        if name in mapping:
            print(name, "already in!")
        else:
            mapping[name] = addr
with open("bl_directly_used_syms.txt", "r") as infile:
    for l in infile:
        if not l.strip() in mapping:
            print(l.strip(), "not in mapping")