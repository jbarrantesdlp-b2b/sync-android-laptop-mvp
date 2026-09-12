@echo off
setlocal
cd /d "%~dp0"

echo [1/3] Escribiendo settings.gradle.kts unificado...
(
echo pluginManagement {
echo     repositories {
echo         google^(^)
echo         mavenCentral^(^)
echo         gradlePluginPortal^(^)
echo     }
echo }
echo dependencyResolutionManagement {
echo     repositoriesMode.set^(RepositoriesMode.FAIL_ON_PROJECT_REPOS^)
echo     repositories {
echo         google^(^)
echo         mavenCentral^(^)
echo     }
echo }
echo rootProject.name = "sync-android-laptop-mvp"
echo include^(":app"^)
echo include^(":core"^)
echo include^(":data"^)
echo include^(":sync"^)
echo include^(":widget"^)
) > settings.gradle.kts

echo [2/3] Escribiendo build.gradle.kts raiz...
(
echo plugins {
echo     kotlin^("jvm"^) version "1.8.22" apply false
echo     id^("com.android.application"^) version "8.1.0" apply false
echo     id^("com.android.library"^) version "8.1.0" apply false
echo }
echo ext["compose_version"] = "1.4.7"
echo ext["glance_version"] = "1.0.0"
echo ext["coroutines_version"] = "1.7.3"
echo ext["datastore_version"] = "1.1.0"
echo ext["okhttp_version"] = "4.11.0"
echo ext["work_version"] = "2.8.1"
) > build.gradle.kts

echo [3/3] Configurando JDK 17 y ejecutando compilacion limpia...
set "JAVA_HOME=%~dp0jdk17\jdk-17.0.11+9"
set "PATH=%JAVA_HOME%\bin;%PATH%"

call gradlew.bat --stop
call gradlew.bat clean build --refresh-dependencies --no-daemon

echo Proceso finalizado.
pause