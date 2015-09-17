SET Package=com\jcdemo\jcapdu
SET Project=jcapdu
REM SET Lib_Path=C:\GeneralJavaApplet\LibGeneration
SET Lib_Path=E:\GeneralJavaApplet\java_card_kit-2_2_2\lib\api.jar;E:\GeneralJavaApplet\java_card_kit-2_2_2\lib\installer.jar
SET JavaCard_Kit=E:\GeneralJavaApplet\java_card_kit-2_2_2\lib
SET JAVA_HOME=E:\GeneralJavaApplet\jdk1.5.0_16

DEL .\dist\%Package%\*.class

%JAVA_HOME%\bin\javac -g -classpath %Lib_Path% .\src\*.java -d .\dist
%JAVA_HOME%\bin\java -classpath %JavaCard_Kit%\offcardverifier.jar;%JavaCard_Kit%\converter.jar; com.sun.javacard.converter.Converter -config $build.opt
%JAVA_HOME%\bin\java -classpath %JavaCard_Kit%\offcardverifier.jar;%JavaCard_Kit%\scriptgen.jar; com.sun.javacard.scriptgen.Main -o dist\%Package%\javacard\%Project%.scr dist\%Package%\javacard\%Project%.cap

PAUSE
