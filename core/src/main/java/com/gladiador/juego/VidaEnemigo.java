package com.gladiador.juego;

public class VidaEnemigo {

    private static final int VIDA_MAXIMA_POR_DEFECTO = 2;

    private final int vidaMaxima;
    private int vidaActual;

    public VidaEnemigo() {
        this(VIDA_MAXIMA_POR_DEFECTO);
    }

    public VidaEnemigo(int vidaMaxima) {
        this.vidaMaxima = Math.max(1, vidaMaxima);
        this.vidaActual = this.vidaMaxima;
    }

    public int getActual() {
        return vidaActual;
    }

    public int getMaxima() {
        return vidaMaxima;
    }

    public void recibirGolpe() {
        recibirGolpe(1);
    }

    public void recibirGolpe(int danio) {
        if (vidaActual <= 0 || danio <= 0) {
            return;
        }
        vidaActual = Math.max(0, vidaActual - danio);
    }

    public boolean estaVivo() {
        return vidaActual > 0;
    }
}
