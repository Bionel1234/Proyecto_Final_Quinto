package com.gladiador.juego;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class MainClass extends ApplicationAdapter {
    private SpriteBatch batch;

    @Override
    public void create() {
        // Inicializamos el SpriteBatch (encargado de enviar las imágenes a la GPU)
        batch = new SpriteBatch();
    }

    @Override
    public void render() {
        // Limpiamos la pantalla pintándola de color azul oscuro / gris
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Bloque de renderizado gráfico
        batch.begin();
        
        // Aquí dibujaremos a Jorge, el escenario y los enemigos
        
        batch.end();
    }

    @Override
    public void dispose() {
        // Liberamos la memoria RAM/GPU asignada al SpriteBatch
        batch.dispose();
    }
}