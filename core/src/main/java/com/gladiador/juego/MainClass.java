package com.gladiador.juego;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

// Cambiamos ApplicationAdapter por Game para poder gestionar pantallas
public class MainClass extends Game {

    // SpriteBatch público para que las pantallas puedan compartir el mismo dibujador
    public SpriteBatch batch;

    @Override
    public void create() {
        // Inicializamos el SpriteBatch (envía los gráficos a la GPU)
        batch = new SpriteBatch();

        // Le decimos al juego que inicie mostrando la pantalla del menú
        this.setScreen(new MenuScreen(this));
    }

    @Override
    public void render() {
        // Ejecuta el render() de la pantalla que esté activa
        super.render();
    }

    @Override
    public void dispose() {
        // Liberamos la memoria del SpriteBatch al cerrar el juego
        batch.dispose();
    }
}