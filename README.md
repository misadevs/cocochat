# CocoChat

Aplicación de chat similar a WhatsApp desarrollada en Java con JavaFX, sockets, hilos y base de datos PostgreSQL.

## Características

- **Login y registro de usuarios**: Los usuarios pueden crear una cuenta o iniciar sesión con una cuenta existente.
- **Chats temporales**: Conversaciones que se eliminan al salir del chat.
- **Chat con amigos**: Se pueden agregar amigos mediante solicitudes de amistad, y estos chats mantienen el historial de conversaciones.
- **Chats grupales**: Se pueden crear grupos con al menos 3 personas, que requieren aceptación de las invitaciones. Estos chats también mantienen el historial.

## Requisitos previos

- Java 11 o superior
- Maven
- PostgreSQL o Supabase

## Configuración

1. **Base de datos**:

   ### Opción A: PostgreSQL local
   - Crea una base de datos PostgreSQL llamada `cocochat`.
   - Edita el archivo `src/main/java/org/example/database/DatabaseManager.java` para configurar la URL, usuario y contraseña.

   ### Opción B: Supabase
   - Crea una cuenta en [Supabase](https://supabase.com) y un nuevo proyecto.
   - Encuentra las credenciales de conexión en la sección "Settings > Database".
   - Actualiza el archivo `src/main/java/org/example/database/DatabaseManager.java` con tu URL, usuario y contraseña de Supabase.

   ```java
   private static final String DB_URL = "jdbc:postgresql://db.supabase.co:5432/your_database_name";
   private static final String DB_USER = "postgres";
   private static final String DB_PASSWORD = "your_password";
   ```

   - La aplicación creará todas las tablas necesarias automáticamente.

2. **Compilación**:
   ```bash
   mvn clean package
   ```

## Ejecución

### Servidor

1. Ejecuta el servidor primero:

   ```bash
   java -cp target/CocoChat-1.0-SNAPSHOT.jar org.example.network.ChatServer
   ```

   (Alternativa) En Windows, puedes usar el archivo batch incluido:

   ```bash
   run_server.bat
   ```

2. El servidor se iniciará en el puerto 5000 por defecto. Si deseas usar otro puerto, ejecútalo con el parámetro del puerto:
   ```bash
   java -cp target/CocoChat-1.0-SNAPSHOT.jar org.example.network.ChatServer 6000
   ```

### Cliente

1. Una vez que el servidor está en ejecución, puedes iniciar uno o más clientes:

   ```bash
   java -cp target/CocoChat-1.0-SNAPSHOT.jar org.example.App
   ```

   (Alternativa) En Windows, puedes usar el archivo batch incluido:

   ```bash
   run_client.bat
   ```

2. Si necesitas conectarte a un servidor en otra máquina o puerto, edita la dirección del servidor en `src/main/java/org/example/controller/MainController.java` (busca la línea con `chatClient = new ChatClient("localhost", 5000, ...)`).

## Solución a errores comunes

### Error: "JavaFX runtime components are missing"

Si al ejecutar la aplicación obtienes el mensaje "Error: JavaFX runtime components are missing and are required to run this application", hay varias formas de solucionarlo:

1. **Usar los scripts batch modificados**:

   - Los archivos `run_client.bat` y `run_server.bat` han sido actualizados para incluir las rutas a los módulos de JavaFX.
   - Ejecuta estos scripts en lugar de ejecutar directamente los comandos java.

2. **Ejecutar con Maven**:

   - Usa el comando `mvn javafx:run` para ejecutar la aplicación cliente.
   - Esta es la forma más sencilla y es gestionada por el plugin JavaFX de Maven.

3. **Ejecutar manualmente con módulos**:

   ```bash
   java --module-path /ruta/a/javafx/libs --add-modules javafx.controls,javafx.fxml -jar target/CocoChat-1.0-SNAPSHOT.jar
   ```

   - Reemplaza `/ruta/a/javafx/libs` con la ruta a tu instalación de JavaFX o a las bibliotecas en el repositorio de Maven.

4. **Crear un classpath manualmente**:
   ```bash
   java -cp target/CocoChat-1.0-SNAPSHOT.jar;/ruta/a/javafx/libs/* org.example.App
   ```

## Uso

1. **Registro e inicio de sesión**:

   - En la pantalla de inicio, ingresa un nombre de usuario y contraseña.
   - Haz clic en "Registrarse" para crear una nueva cuenta o "Iniciar sesión" si ya tienes una.

2. **Chats temporales**:

   - Busca un usuario y selecciona "Chat Temporal" para iniciar una conversación que se eliminará al salir.

3. **Amigos**:

   - Busca un usuario y selecciona "Solicitud de Amistad" para enviar una invitación.
   - El otro usuario debe aceptar la solicitud desde la sección "Solicitudes".
   - Una vez aceptada, se creará un chat permanente entre ambos.

4. **Grupos**:
   - Haz clic en "Nuevo Grupo", ingresa un nombre y selecciona al menos 2 participantes.
   - Los usuarios invitados deben aceptar la solicitud desde la sección "Solicitudes".
   - Si el número de participantes cae por debajo de 3, el grupo se eliminará automáticamente.
   - Si el administrador abandona el grupo, también se eliminará.

## Arquitectura

- **Modelo**: Representación de los datos (Usuario, Mensaje, Chat, etc.).
- **Vista**: Interfaces de usuario en JavaFX.
- **Controlador**: Lógica para manejar eventos y actualizar la vista y el modelo.
- **Base de datos**: Almacenamiento persistente con PostgreSQL/Supabase.
- **Red**: Comunicación cliente-servidor usando sockets y JSON.

## Desarrollo

Este proyecto sigue el patrón MVC (Modelo-Vista-Controlador) y utiliza las siguientes tecnologías:

- **JavaFX**: Para la interfaz de usuario.
- **JDBC**: Para la conexión a la base de datos PostgreSQL.
- **Sockets Java**: Para la comunicación en red.
- **Gson**: Para la serialización/deserialización de mensajes en formato JSON.
- **Maven**: Para la gestión de dependencias y construcción del proyecto.
