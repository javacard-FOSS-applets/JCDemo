SET JC_HOME=C:\GeneralJavaApplet\java_card_kit-2_2_2
SET JAVA_HOME=C:\GeneralJavaApplet\jdk1.5.0_16


@echo off

:Question
cls

echo.
echo  LIBRARY GENERATION MENU:
echo.
echo  [1] Run CAPDUMP 
echo  [Q] Quit.
echo.

set choiceOUT=x
set /P choiceOUT= Enter your choice:

:DISPATCH_CHOICE
if "%choiceOUT%" == "1" goto CapDumpLabel 
if "%choiceOUT%" == "q" goto END
if "%choiceOUT%" == "Q" goto END

echo  ------------------- illegal choice --------------------
goto END



rem ******************CapDump Start********************************
:CapDumpLabel
set capFile=x
set /P capFile= Enter your test file(full path):

set _CLASSES=%JC_HOME%\lib\apduio.jar;%JC_HOME%\lib\apdutool.jar;%JC_HOME%\lib\jcwde.jar;%JC_HOME%\lib\converter.jar;%JC_HOME%\lib\scriptgen.jar;%JC_HOME%\lib\offcardverifier.jar;%JC_HOME%\lib\api.jar;%JC_HOME%\lib\installer.jar;%JC_HOME%\lib\capdump.jar;%JC_HOME%\samples\classes;%CLASSPATH%;

%JAVA_HOME%\bin\java -classpath %_CLASSES% com.sun.javacard.capdump.CapDump %capFile%

PAUSE
goto Question
rem ******************CapDump End********************************



rem ******************Load Start********************************
:CrefLoadLabel
%JC_HOME%/bin/cref -i HelloWorldImage

goto Question
rem ******************Load End********************************


:END
