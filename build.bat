rmdir /s /q tmp
mkdir tmp
del bdjstack.jar
copy bdjstack.original.jar bdjstack.jar
"%JAVA_HOME%/javac" -source 1.3 -target 1.3 -d tmp -classpath ./bdjstack.original.jar ./src/com/sony/bdjstack/security/*.java ./src/com/sony/bdjstack/system/*.java
"C:\Program Files\7-Zip\7z.exe" a bdjstack.jar ./tmp/*
rmdir /s /q tmp
pause