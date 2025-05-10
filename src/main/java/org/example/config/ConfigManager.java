package org.example.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Clase para gestionar la configuración de la aplicación desde config.ini
 */
public class ConfigManager {
    private static ConfigManager instance;
    private Properties properties;
    
    // Valores por defecto
    private static final String DEFAULT_DB_URL = "jdbc:postgresql://ep-crimson-band-a45r8g9i-pooler.us-east-1.aws.neon.tech/neondb?sslmode=require";
    private static final String DEFAULT_DB_USER = "neondb_owner";
    private static final String DEFAULT_DB_PASSWORD = "npg_t4HnmWoG7bNO";
    private static final String DEFAULT_SERVER_HOST = "localhost";
    private static final int DEFAULT_SERVER_PORT = 5000;
    
    private ConfigManager() {
        properties = new Properties();
        loadConfig();
    }
    
    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }
    
    private void loadConfig() {
        try {
            // Intentar cargar desde el directorio de trabajo
            File configFile = new File("config.ini");
            
            // Si no existe en el directorio de trabajo, buscar en resources
            if (!configFile.exists()) {
                try {
                    configFile = new File(getClass().getClassLoader().getResource("config.ini").getFile());
                } catch (NullPointerException e) {
                    System.out.println("No se encontró config.ini en resources.");
                }
            }
            
            if (configFile.exists()) {
                FileInputStream fis = new FileInputStream(configFile);
                properties.load(fis);
                fis.close();
                System.out.println("Configuración cargada desde: " + configFile.getAbsolutePath());
            } else {
                System.out.println("Archivo config.ini no encontrado, usando valores por defecto.");
            }
        } catch (IOException e) {
            System.err.println("Error al cargar la configuración: " + e.getMessage());
            System.out.println("Usando valores por defecto.");
        }
    }
    
    /**
     * Obtiene la URL de la base de datos
     * @return URL de conexión a la base de datos
     */
    public String getDbUrl() {
        return properties.getProperty("db_url", DEFAULT_DB_URL);
    }
    
    /**
     * Obtiene el usuario de la base de datos
     * @return Usuario para conectar a la base de datos
     */
    public String getDbUser() {
        return properties.getProperty("db_user", DEFAULT_DB_USER);
    }
    
    /**
     * Obtiene la contraseña de la base de datos
     * @return Contraseña para conectar a la base de datos
     */
    public String getDbPassword() {
        return properties.getProperty("db_password", DEFAULT_DB_PASSWORD);
    }
    
    /**
     * Obtiene el host del servidor
     * @return Host donde se ejecuta el servidor
     */
    public String getServerHost() {
        return properties.getProperty("server_host", DEFAULT_SERVER_HOST);
    }
    
    /**
     * Obtiene el puerto del servidor
     * @return Puerto en el que escucha el servidor
     */
    public int getServerPort() {
        try {
            return Integer.parseInt(properties.getProperty("server_port", String.valueOf(DEFAULT_SERVER_PORT)));
        } catch (NumberFormatException e) {
            return DEFAULT_SERVER_PORT;
        }
    }
} 