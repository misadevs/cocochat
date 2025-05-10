@echo off
echo Iniciando cliente de chat...
echo.

rem Compilar el proyecto (esto asume que Maven está instalado y configurado)
call mvn clean package -DskipTests

echo.
echo === OPCIONES DE EJECUCIÓN ===
echo 1. Ejecutar usando Java directamente (con módulos JavaFX)
echo 2. Ejecutar usando el plugin JavaFX de Maven (recomendado)
echo.
set /p opcion="Selecciona una opción (1 o 2): "

if "%opcion%"=="1" (
    echo Ejecutando con Java directamente...
    java --module-path %USERPROFILE%\.m2\repository\org\openjfx\javafx-controls\17.0.2\javafx-controls-17.0.2-win.jar;%USERPROFILE%\.m2\repository\org\openjfx\javafx-fxml\17.0.2\javafx-fxml-17.0.2-win.jar;%USERPROFILE%\.m2\repository\org\openjfx\javafx-graphics\17.0.2\javafx-graphics-17.0.2-win.jar;%USERPROFILE%\.m2\repository\org\openjfx\javafx-base\17.0.2\javafx-base-17.0.2-win.jar --add-modules javafx.controls,javafx.fxml -cp target/CocoChat-1.0-SNAPSHOT.jar org.example.App
) else (
    echo Ejecutando con el plugin JavaFX de Maven...
    call mvn javafx:run
)

pause 