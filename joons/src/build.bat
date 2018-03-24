@echo off

cd %cd%
javac -cp "C:\Program Files\processing\core\library\core.jar";"..\library\sunflow73.jar";"..\library\janino.jar" joons\*.java
move /y joons\*.class build\joons\
cd build
jar cvfm ..\joons.jar manifest.txt joons\*.class

@pause