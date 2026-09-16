# Temporary dev launcher for testing the WebView-embedded 3D chapter (Chapter One -> "3D Preview").
#
# Why this exists: `mvn javafx:run` currently fails on JDK 26 with
#   "Module jdk.jsobject not found, required by javafx.web"
# JDK 26 dropped the jdk.jsobject module that javafx.web's module-info requires.
# OpenJFX 26.0.1 ships a replacement org.openjfx:jdk-jsobject artifact, but
# javafx-maven-plugin 0.0.8 (the latest release as of writing) doesn't know to
# put it on the module path, so it ends up on the classpath instead, where it
# can't satisfy javafx.web's `requires jdk.jsobject`.
#
# This script puts jdk-jsobject on the module path alongside the other JavaFX
# jars manually. Delete this script once javafx-maven-plugin ships a fix, or
# switch back to `mvn javafx:run` if you're on JDK <= 25 (which still has
# jdk.jsobject built in and doesn't need this).

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

Write-Host "Compiling..."
mvn -q compile
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

# Assumes the default local repository location. If you've customized
# <localRepository> in settings.xml, change this path to match.
$m2 = "$env:USERPROFILE\.m2\repository"
$fxVersion = "26.0.1"
$fx = "$m2\org\openjfx"

$modulePath = @(
    "$fx\javafx-base\$fxVersion\javafx-base-$fxVersion-win.jar",
    "$fx\javafx-controls\$fxVersion\javafx-controls-$fxVersion-win.jar",
    "$fx\javafx-fxml\$fxVersion\javafx-fxml-$fxVersion-win.jar",
    "$fx\javafx-graphics\$fxVersion\javafx-graphics-$fxVersion-win.jar",
    "$fx\javafx-media\$fxVersion\javafx-media-$fxVersion-win.jar",
    "$fx\javafx-web\$fxVersion\javafx-web-$fxVersion-win.jar",
    "$fx\jdk-jsobject\$fxVersion\jdk-jsobject-$fxVersion-win.jar"
) -join ";"

$addModules = "javafx.base,javafx.controls,javafx.fxml,javafx.graphics,javafx.media,javafx.web,jdk.jsobject"

Write-Host "Building classpath..."
mvn -q dependency:build-classpath "-Dmdep.outputFile=target/cp.txt"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
$classpath = "target\classes;" + (Get-Content target/cp.txt -Raw).Trim()

Write-Host "Launching..."
& java --module-path $modulePath --add-modules $addModules -cp $classpath com.override.Main
