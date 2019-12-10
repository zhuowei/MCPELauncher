#!/bin/bash
set -e
curl 192.168.1.4:8000/combined_check2_out.csv |sort|uniq >combined_check2_out.csv
python3 makefakesymslib.py
cp fakesymstubs_arm32.s fakesym_ptrs.c ../jni/
