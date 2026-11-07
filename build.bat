@echo off
setlocal
set ID=com.tywstudio.example.java
set OUT=%ID%.jar
if exist build rmdir /s /q build
mkdir build
dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -d build @sources.txt
if not exist build\META-INF mkdir build\META-INF
copy META-INF\tml-extension.json build\META-INF\ >nul
jar cf "%OUT%" -C build .
del sources.txt
echo 已打包: %OUT%
endlocal
