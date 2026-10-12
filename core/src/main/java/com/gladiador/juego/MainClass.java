package com.gladiador.juego;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class MainClass extends Game {
    public SpriteBatch batch;
    private boolean zona1Completada;
    private boolean zona2Completada;
    private boolean bestiarioDerrotado;

    @Override
    public void create() {
        batch = new SpriteBatch();
        setScreen(new MenuScreen(this));
    }

    @Override
    public void dispose() {
        super.dispose();
        if (batch != null) {
            batch.dispose();
        }
    }

    public boolean isZona1Completada() {
        return zona1Completada;
    }

    public boolean isZona2Completada() {
        return zona2Completada;
    }

    public boolean isBestiarioDerrotado() {
        return bestiarioDerrotado;
    }

    public void registrarZona1Completada() {
        zona1Completada = true;
    }

    public void registrarZona2Completada() {
        zona2Completada = true;
    }

    public void registrarBestiarioDerrotado() {
        bestiarioDerrotado = true;
    }
}
