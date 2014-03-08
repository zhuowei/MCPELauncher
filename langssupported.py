#!/usr/bin/python3
import os

sourcefile = """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="languages_supported">{0}</string>
</resources>

"""

# open the res folder and write out all languages
def main():
	lang_list = []
	list_files = os.listdir("res")
	for i in list_files:
		if not "values-" in i:
			continue
		if "-land" in i or "-port" in i:
			continue
		langinfo = i.strip()[len("values-"):].replace("-r", "_")
		lang_list.append(langinfo)
	lang_list.append("en") # en is the default locale
	lang_list.sort()
	finalstring = sourcefile.format(","+",".join(lang_list))
	with open("res/values/langssupported.xml", "w") as outfile:
		outfile.write(finalstring)


main()
