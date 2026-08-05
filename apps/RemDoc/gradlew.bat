@echo off
set DIRNAME=%~dp0
set APP_HOME=%DIRNAME%
set JAVA_EXE=%JAVA_HOME%\bin\java.exe
if defined JAVA_HOME if exist "%JAVA_EXE%" goto execute
set JAVA_EXE=java.exe
:execute
set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=gradlew" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
