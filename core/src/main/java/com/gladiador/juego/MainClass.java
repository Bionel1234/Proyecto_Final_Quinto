package com.gladiador.juego;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

// Clase principal: crea el dibujador y muestra el menu.
public class MainClass extends Game {

    // Un solo SpriteBatch es compartido por todas las pantallas.
    public SpriteBatch batch;
    private boolean zona1Completada;
    private boolean bestiarioDerrotado;

    @Override
    public void create() {
        // Crear el objeto que dibuja imagenes y textos.
        batch = new SpriteBatch();

        // Abrir el juego en el menu principal.
        setScreen(new MenuScreen(this));
    }

    @Override
    public void render() {
        // Game ejecuta el render de la pantalla activa.
        super.render();
    }

    public boolean isZona1Completada() {
        return zona1Completada;
    }

    public void registrarZona1Completada() {
        zona1Completada = true;
    }

    public boolean isBestiarioDerrotado() {
        return bestiarioDerrotado;
    }

    public void registrarBestiarioDerrotado() {
        bestiarioDerrotado = true;
    }

    @Override
    public void dispose() {
        if (getScreen() != null) {
            getScreen().dispose();
        }
        // Liberar el dibujador al cerrar el juego.
        if (batch != null) {
            batch.dispose();
        }
    }
}
