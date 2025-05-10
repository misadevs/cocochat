@echo off
echo Iniciando servidor de chat...
echo.

rem Compilar el proyecto (esto asume que Maven está instalado y configurado)
call mvn clean package -DskipTests

rem Ejecutar el servidor con los módulos JavaFX
java --module-path %USERPROFILE%\.m2\repository\org\openjfx\javafx-controls\17.0.2\javafx-controls-17.0.2-win.jar;%USERPROFILE%\.m2\repository\org\openjfx\javafx-fxml\17.0.2\javafx-fxml-17.0.2-win.jar;%USERPROFILE%\.m2\repository\org\openjfx\javafx-graphics\17.0.2\javafx-graphics-17.0.2-win.jar;%USERPROFILE%\.m2\repository\org\openjfx\javafx-base\17.0.2\javafx-base-17.0.2-win.jar --add-modules javafx.controls,javafx.fxml -cp target/CocoChat-1.0-SNAPSHOT.jar org.example.network.ChatServer

pause 