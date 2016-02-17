@echo off
@
@
setlocal

set app_home=.

rem specify the JVM heap size
set memory=1024m

:start
if "%java_home%"=="" (set java_exec=java) else (set java_exec=%java_home%\bin\java)

set java_opts=-Xms%memory% -Xmx%memory% 

rem "%java_exec%" -server -showversion %java_opts% -cp "%app_home%\target\*;%app_home%\target\lib\*" com.yahoo.ycsb.Client -load -threads 20 -P workloada
"%java_exec%" -server -showversion %java_opts% -cp "%app_home%\target\*;%app_home%\target\lib\*" com.yahoo.ycsb.Client -load -s -P workloada

rem perform queries loopig by user count
for /l %%x in (5, 1, 10) do (
rem 	"%java_exec%" -server %java_opts% -cp "%app_home%\target\*;%app_home%\target\lib\*" com.yahoo.ycsb.Client -threads %%x -P workloada  
)

"%java_exec%" -server %java_opts% -cp "%app_home%\target\*;%app_home%\target\lib\*" com.yahoo.ycsb.Client -s -P workloada


goto exit

:instructions

echo Usage:
echo %app_home%\ycsb-loada.cmd
goto exit

:exit
endlocal
@echo on
