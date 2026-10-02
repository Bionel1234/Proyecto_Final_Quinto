package com.gladiador.juego;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class Monedas {

    private static int cantidad;
    private static boolean recompensaPrimerMapaReclamada;

    private final Texture imagen;
    private final BitmapFont fuente;

    public Monedas() {
        imagen = new Texture(Gdx.files.internal("moneda.png"));
        fuente = new BitmapFont();
    }

    public static void recompensarPrimerMapa() {
        if (!recompensaPrimerMapaReclamada) {
            cantidad += 100;
            recompensaPrimerMapaReclamada = true;
        }
    }

    public int obtenerCantidad() {
        return cantidad;
    }

    public void dibujar(SpriteBatch batch) {
        float x = 70f;
        float y = JuegoScreen.ALTO - 76f;
        batch.draw(imagen, x, y - 8f, 54f, 48f);
        fuente.draw(batch, "x " + cantidad, x + 66f, y + 26f);
    }

    public void dispose() {
        imagen.dispose();
        fuente.dispose();
    }
}
