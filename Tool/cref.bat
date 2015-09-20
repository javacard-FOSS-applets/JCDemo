SET JC_HOME=C:\GeneralJavaApplet\java_card_kit-2_2_2

@echo off

:Question
cls

echo.
echo  LIBRARY GENERATION MENU:
echo.
echo  [0] Run CREF for Init
echo  [1] Run CREF for Load
echo  [Q] Quit.
echo.

set choiceOUT=x
set /P choiceOUT= Enter your choice:

:DISPATCH_CHOICE
if "%choiceOUT%" == "0" goto CrefInitLabel 
if "%choiceOUT%" == "1" goto CrefLoadLabel 
if "%choiceOUT%" == "q" goto END
if "%choiceOUT%" == "Q" goto END

echo  ------------------- illegal choice --------------------
goto END



rem ******************Init Start********************************
:CrefInitLabel
%JC_HOME%/bin/cref -o HelloWorldImage

goto Question
rem ******************Init End********************************



rem ******************Load Start********************************
:CrefLoadLabel
%JC_HOME%/bin/cref -i HelloWorldImage

goto Question
rem ******************Load End********************************


:END
