#!/bin/bash
set -e
rm -r slickcraft || true
cd ..
wget -Oslickcraft.7z "https://cdn.glitch.com/a9068bb8-2329-44b4-8406-ddefb02d1719%2Fslickcraft.7z?v=1579382923273"
7z -oslickcraft x slickcraft.7z
