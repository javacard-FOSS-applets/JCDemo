@echo off

SET Package=com\jcdemo\jcapplet
SET Project=jcapplet

SET JC_HOME=C:\GeneralJavaApplet\java_card_kit-2_2_2
SET JAVA_HOME=C:\GeneralJavaApplet\jdk1.5.0_16
SET JC_EXT=C:\work\JC\JCDemo\Tool

DEL .\dist\%Package%\*.class

:Question
cls

echo.
echo  LIBRARY GENERATION MENU:
echo.
echo  [0] Compile and Generate package for javacard 
echo  [1] Generate Scr file for package
echo  [2] Install applet for CREF
echo  [3] Run APDUTOOL command
echo  [4] Run for Test (seq = 0, 1, 2)
echo  [Q] Quit.
echo.

set choiceOUT=x
set /P choiceOUT= Enter your choice:

:DISPATCH_CHOICE
if "%choiceOUT%" == "0" goto CompileLabel
if "%choiceOUT%" == "1" goto GenScrLabel 
if "%choiceOUT%" == "2" goto InstallCrefLabel
if "%choiceOUT%" == "3" goto RunApdutoolLable 
if "%choiceOUT%" == "4" goto CompileLabel
if "%choiceOUT%" == "q" goto END
if "%choiceOUT%" == "Q" goto END

echo  ------------------- illegal choice --------------------
goto END


rem ******************Compile Start********************************
:CompileLabel

@echo on
%JAVA_HOME%\bin\javac -g -classpath %JC_HOME%\lib\api.jar;%JC_HOME%\lib\installer.jar .\src\*.java -d .\dist
%JAVA_HOME%\bin\java -classpath %JC_HOME%\lib\offcardverifier.jar;%JC_HOME%\lib\converter.jar; com.sun.javacard.converter.Converter -config $build.opt

@echo off
if "%choiceOUT%" == "4" goto GenScrLabel 
PAUSE

goto Question
rem ******************Compile End********************************






rem ****************** SCR Start********************************
:GenScrLabel

%JAVA_HOME%\bin\java -classpath %JC_HOME%\lib\offcardverifier.jar;%JC_HOME%\lib\scriptgen.jar; com.sun.javacard.scriptgen.Main -o dist\%Package%\javacard\%Project%.scr dist\%Package%\javacard\%Project%.cap

type test\Header > test\temp
type dist\%Package%\javacard\%Project%.scr >> test\temp 
type test\Footer >> test\temp
type test\temp > dist\%Package%\javacard\%Project%.scr
del test\temp

if "%choiceOUT%" == "4" goto InstallCrefLabel 
PAUSE

goto Question
rem ******************SCR End********************************




rem ****************** Install Cref Start********************************
:InstallCrefLabel

set _CLASSES=%JC_HOME%\lib\apduio.jar;%JC_HOME%\lib\apdutool.jar;%JC_HOME%\lib\jcwde.jar;%JC_HOME%\lib\converter.jar;%JC_HOME%\lib\scriptgen.jar;%JC_HOME%\lib\offcardverifier.jar;%JC_HOME%\lib\api.jar;%JC_HOME%\lib\installer.jar;%JC_HOME%\lib\capdump.jar;%JC_HOME%\samples\classes;%CLASSPATH%;

%JAVA_HOME%\bin\java -classpath %_CLASSES% com.sun.javacard.apdutool.Main dist\%Package%\javacard\%Project%.scr 

echo *******************************

PAUSE

goto Question
rem ****************** Install Cref End********************************


rem ****************** Apdutool Start********************************
:RunApdutoolLable

echo !!! Notice !!!
echo please Run CREF Load Image
echo Then run your test script

set apduTestFile=x
set /P apduTestFile= Enter your test file(full path):

set _CLASSES=%JC_HOME%\lib\apduio.jar;%JC_HOME%\lib\apdutool.jar;%JC_HOME%\lib\jcwde.jar;%JC_HOME%\lib\converter.jar;%JC_HOME%\lib\scriptgen.jar;%JC_HOME%\lib\offcardverifier.jar;%JC_HOME%\lib\api.jar;%JC_HOME%\lib\installer.jar;%JC_HOME%\lib\capdump.jar;%JC_HOME%\samples\classes;%CLASSPATH%;

%JAVA_HOME%\bin\java -classpath %_CLASSES% com.sun.javacard.apdutool.Main %apduTestFile%

PAUSE

goto Question
rem ******************Apdutool End********************************




:END
