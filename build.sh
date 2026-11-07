#!/usr/bin/env bash
set -e
ID=com.tywstudio.example.java
OUT="$ID.jar"
rm -rf build && mkdir -p build
find src -name '*.java' > sources.txt
javac -encoding UTF-8 -d build @sources.txt
mkdir -p build/META-INF
cp META-INF/tml-extension.json build/META-INF/
jar cf "$OUT" -C build .
rm -f sources.txt
echo "已打包: $OUT"
