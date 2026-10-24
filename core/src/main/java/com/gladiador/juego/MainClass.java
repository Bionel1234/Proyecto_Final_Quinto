package com.gladiador.juego;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

// Clase principal del juego: arranca aqui y guarda lo que es comun a todas las pantallas.
public class MainClass extends Game {

    // El batch es como un "pincel" que dibuja todas las imagenes del juego.
    // Hay uno solo compartido para todas las pantallas.
    public SpriteBatch batch;

    // Constantes que identifican el orden de avance de la campaña.
    public static final int ETAPA_ZONA1_NIVEL1 = 0;
    public static final int ETAPA_ZONA1_NIVEL2 = 1;
    public static final int ETAPA_JEFE_ZONA1 = 2;
    public static final int ETAPA_ZONA2 = 3;

    // Indice de la etapa mas avanzada disponible en el selector.
    private int etapaDesbloqueada = ETAPA_ZONA1_NIVEL1;

    // Se ejecuta una sola vez, al abrir el juego.
    @Override
    public void create() {
        batch = new SpriteBatch();
        setScreen(new MenuScreen(this)); // la primera pantalla es el menu
    }

    // Se ejecuta al cerrar el juego, para liberar memoria.
    @Override
    public void dispose() {
        super.dispose();
        if (batch != null) {
            batch.dispose();
        }
    }

    // Permite que el selector sepa cuantas etapas se pueden elegir.
    public int getEtapaDesbloqueada() {
        return etapaDesbloqueada;
    }

    // Completar la etapa actual avanza el progreso y retira las anteriores del selector.
    public void registrarEtapaCompletada(int etapaCompletada) {
        if (etapaCompletada == etapaDesbloqueada && etapaDesbloqueada <= ETAPA_ZONA2) {
            etapaDesbloqueada++;
        }
    }
}
