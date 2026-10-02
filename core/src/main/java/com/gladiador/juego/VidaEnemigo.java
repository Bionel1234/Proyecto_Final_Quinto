package com.gladiador.juego;

public class VidaEnemigo {

    private static final int VIDA_MAXIMA = 2;

    private int vidaActual = VIDA_MAXIMA;

    public int getActual() {
        return vidaActual;
    }

    public int getMaxima() {
        return VIDA_MAXIMA;
    }

    public void recibirGolpe() {
        if (vidaActual <= 0) {
            return;
        }

        vidaActual--;
    }

    public boolean estaVivo() {
        return vidaActual > 0;
    }
}
