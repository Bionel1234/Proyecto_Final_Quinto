// Indica el paquete al que pertenece esta clase y coincide con las carpetas
// lwjgl3/com/gladiador/juego dentro de la estructura de codigo fuente.
package com.gladiador.juego.lwjgl3;

// Clase de LibGDX que crea y ejecuta la aplicacion usando la ventana LWJGL3.
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
// Configuracion que define el aspecto y el comportamiento de esa ventana.
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
// Clase principal del juego: contiene las pantallas y la logica general.
import com.gladiador.juego.MainClass;

// Clase de arranque para ejecutar el juego como aplicacion de escritorio.
public class Lwjgl3Launcher {

    // Punto de entrada que Java ejecuta primero al iniciar el programa.
    // "public" permite que Java encuentre el metodo; "static" permite llamarlo
    // sin crear antes un objeto Lwjgl3Launcher; "void" indica que no devuelve
    // un resultado. "args" recibe los argumentos opcionales de la terminal.
    public static void main(String[] args) {
        // Crea un objeto de configuracion con los valores iniciales de LibGDX.
        Lwjgl3ApplicationConfiguration configuracion = new Lwjgl3ApplicationConfiguration();

        // Define el texto que aparece en la barra de titulo de la ventana.
        configuracion.setTitle("Jorge el ultimo Gladiador");

        // Abre la ventana en modo normal (no pantalla completa), inicialmente
        // con 640 pixeles de ancho y 480 de alto.
        configuracion.setWindowedMode(640, 480);

        // Sincroniza el dibujo con la frecuencia del monitor para reducir
        // los cortes o desgarros visibles durante el movimiento.
        configuracion.useVsync(true);

        // Limita a 60 los cuadros por segundo cuando la ventana esta activa,
        // evitando que el juego dibuje mas cuadros de los necesarios.
        configuracion.setForegroundFPS(60);

        // Carga los iconos de la ventana en varios tamanos. El sistema operativo
        // elige el tamano adecuado segun donde muestre el icono. Estos archivos
        // deben estar disponibles en la carpeta de recursos de escritorio.
        configuracion.setWindowIcon("libgdx128.png", "libgdx64.png",
            "libgdx32.png", "libgdx16.png");

        // Construye el juego principal y se lo entrega al backend de escritorio
        // junto con la configuracion. LibGDX abre la ventana y llama a los
        // metodos del ciclo de vida del juego, como create() y render().
        // La llamada permanece activa hasta que se cierra la ventana.
        new Lwjgl3Application(new MainClass(), configuracion);
    }
}
