package com.gladiador.juego.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.gladiador.juego.MainClass;

// Punto de entrada de la version de escritorio.
public class Lwjgl3Launcher {

    // Metodo que ejecuta Java al iniciar el juego.
    public static void main(String[] args) {
        // Ajustes especiales necesarios en algunos sistemas operativos.
        if (StartupHelper.startNewJvmIfRequired()) return;

        // Abrir la aplicacion LibGDX.
        new Lwjgl3Application(new MainClass(), configurarVentana());
    }

    // Configurar la ventana del juego.
    private static Lwjgl3ApplicationConfiguration configurarVentana() {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("JorgeElUltimoGladiador");
        config.useVsync(true);
        config.setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate + 1);
        config.setWindowedMode(1366, 768);
        config.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");
        return config;
    }
}
