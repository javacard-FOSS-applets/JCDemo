#JCDmo

## how to compiler JC Applet
1. create *dist* fold at the same level with *src*
2. get Tool/GeneralJavaApplet.7z and unzip. put it your C:
3. run Compile_Build_Sun.bat
4. choose "0" to "Compile and Generate package for javacard" 

if want to change GeneralJavaApplet fold, you should

1. modify *JC_HOME* in Compile_Build_Sun.bat
2. modify *JAVA_HOME* in Compile_Build_Sun.bat
3. modify *JC_EXT* in Compile_Build_Sun.bat
4. modify *-exportpath* in $build.opt

## how to test JC Applet
1. Run cref.bat in Tool folder
   - choose "0" for "Run CREF for Init"
2. Run Compile_Build_Sun.bat
   - choose "4" for "Run for Test"
3. cref.bat shell
    - choose "1" for "Run CREF for load"
4. Compile_Build_Sun.bat shell 
   - choose "3" for "Run APDUTOOL command"

