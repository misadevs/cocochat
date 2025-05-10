package org.example;

/**
 * Clase lanzadora para solucionar problemas con el empaquetado de JavaFX
 * Esta clase sirve como punto de entrada principal cuando se empaqueta la aplicación
 * en un único JAR con todas las dependencias.
 */
public class Launcher {
    
    /**
     * Método principal que delega a la clase App
     * @param args Argumentos de línea de comandos
     */
    public static void main(String[] args) {
        App.main(args);
    }
} 