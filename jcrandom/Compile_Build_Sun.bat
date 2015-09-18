SET Package=com\jcdemo\jcrandom
SET Project=jcrandom
SET JavaCard_Kit=C:\GeneralJavaApplet\java_card_kit-2_2_2\lib
SET JAVA_HOME=C:\GeneralJavaApplet\jdk1.5.0_16

DEL .\dist\%Package%\*.class

%JAVA_HOME%\bin\javac -g -classpath %JavaCard_Kit%\api.jar;%JavaCard_Kit%installer.jar .\src\*.java -d .\dist
%JAVA_HOME%\bin\java -classpath %JavaCard_Kit%\offcardverifier.jar;%JavaCard_Kit%\converter.jar; com.sun.javacard.converter.Converter -config $build.opt
%JAVA_HOME%\bin\java -classpath %JavaCard_Kit%\offcardverifier.jar;%JavaCard_Kit%\scriptgen.jar; com.sun.javacard.scriptgen.Main -o dist\%Package%\javacard\%Project%.scr dist\%Package%\javacard\%Project%.cap

PAUSE
