@echo off
set DIR=%~dp0
set JAVA_EXE=java.exe
if exist "%DIR%\gradle\wrapper\gradle-wrapper.jar" (
    "%JAVA_EXE%" -classpath "%DIR%\gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
) else (
    echo Gradle wrapper JAR not found.
)

