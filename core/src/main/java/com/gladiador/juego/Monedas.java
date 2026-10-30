package com.gladiador.juego;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

// Contador de monedas que se muestra junto al corazon de vida.
// La cantidad es "static": es una sola bolsa de monedas para toda la partida,
// no importa en que pantalla este Jorge.
public class Monedas {

    private static int cantidad=1;                   // monedas que tiene el jugador

    private final Texture imagen;
    private final BitmapFont fuente;

    // Carga los recursos graficos y de texto necesarios para mostrar el contador.
    public Monedas() {
        imagen = new Texture(Gdx.files.internal("moneda.png"));
        fuente = new BitmapFont(); // fuente por defecto de LibGDX, para textos simples
    }

    public static void recompensarNivelCompletado() {
        cantidad += 50;
    }

    public static void recompensarJefeDerrotado() {
        cantidad += 100;
    }

    public static void reiniciar() {
        cantidad = 0;
    }

    public static boolean gastar(int costo) {
        if (cantidad < costo) {
            return false;
        }
        cantidad -= costo;
        return true;
    }

    // Dibujar la moneda y "x NNN" arriba a la izquierda.
    public void dibujar(SpriteBatch batch) {
        float x = 70f;
        float y = JuegoScreen.ALTO - 76f;
        batch.draw(imagen, x, y - 8f, 54f, 48f);
        fuente.draw(batch, "x " + cantidad, x + 66f, y + 26f);
    }

    // Libera la textura y la fuente que esta instancia creo.
    public void dispose() {
        imagen.dispose();
        fuente.dispose();
    }
}
