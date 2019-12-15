# How to update symbols for a new version

Generate the list of symbols that BlockLauncher actually needs:

Go to Android.mk
Remove the fakesyms generated stubs
Uncomment the line which enables linking with missing symbols

```
ndk-build
cd libs/armeabi-v7a
arm-linux-androideabi-objdump -T libmcpelauncher.so >bl_syms.txt
cd ../../tools/find_symbols
bash pull_syms_from_dlsym_calls.sh
```

Copy syms_in_strings.txt and bl_syms.txt.

Use Diaphora to do a diff.

Diaphora needs 12GB of RAM, lots of disk space.

Still not enough: use removeSomeFunctions to wipe out everything from the first zlib symbol to the end of .text.

Export the sqlite and copy that to `diffout_1140.diaphora`

edit `export_mangled_syms_diaphora.py` to the new path and run it:

```
python3 export_mangled_syms_diaphora.py |sort >diaphora_outsyms.csv
```

```
aarch64-none-elf-objdump -T libminecraftpe.so >~/MCPELauncher/tools/find_symbols/mcpe1140_ownsyms.txt
aarch64-none-elf-objdump -T libfmod.so >~/MCPELauncher/tools/find_symbols/fmod1140_ownsyms.txt
```

```
aarch64-none-elf-objdump -T bedrock_server >syms.txt
```

Edit `combined_check2.py` to take the new ownsyms files

Edit `port_symbols_using_vtable.py` to point to the right libs and dedicated servers

delete `syms.pickle`

Start updating:

- vtables in `port_symbols_using_vtable.py`

```
python3 port_symbols_using_vtable.py >hopeless_out.csv
```

or:

change `ida = False` to True

```
python3 port_symbols_using_vtable.py >ported_symbols_for_ida.idc
```

- addresses in `manually_found_syms.csv`
- headers' sizes/offsets

Save in IDA often.

To find which symbols are left:

```
python3 combined_check2.py
```