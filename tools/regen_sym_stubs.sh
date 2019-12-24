#!/bin/bash
set -e
python3 makefakesymslib.py
cp fakesymstubs_arm32.s fakesym_ptrs.c ../jni/
