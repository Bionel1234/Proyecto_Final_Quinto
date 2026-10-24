package com.gladiador.juego;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

// Barra de vida de Jorge, dibujada arriba a la derecha.
// Usa cuatro imagenes ya dibujadas: llena, con 2 vidas, con 1 vida y vacia.
public class Vida {

    private static final int SALUD_MAXIMA = 8;        // puntos de vida internos
    private static final int VIDAS_VISUALES = 3;      // cuantas "vidas" muestra la barra
    private static final float ANCHO_BARRA = 430f;    // ancho en pantalla

    // barras[0] = vacia, barras[1] = 1 vida, barras[2] = 2 vidas, barras[3] = llena.
    private final Texture[] barras = new Texture[VIDAS_VISUALES + 1];
    private static int salud = SALUD_MAXIMA;

    // Carga los cuatro dibujos posibles de la barra, desde vacia hasta llena.
    public Vida() {
        barras[3] = new Texture(Gdx.files.internal("ui/barra_vida_3.png"));
        barras[2] = new Texture(Gdx.files.internal("ui/barra_vida_2.png"));
        barras[1] = new Texture(Gdx.files.internal("ui/barra_vida_1.png"));
        barras[0] = new Texture(Gdx.files.internal("ui/barra_vida_0.png"));
    }

    // Pintar la barra que corresponde a la vida actual.
    public void dibujar(SpriteBatch batch) {
        Texture barra = barras[vidasVisibles()];
        float alto = ANCHO_BARRA * barra.getHeight() / barra.getWidth();
        batch.draw(barra, JuegoScreen.ANCHO - ANCHO_BARRA - 16f, JuegoScreen.ALTO - alto - 10f,
            ANCHO_BARRA, alto);
    }

    // Jorge perdio un punto de vida (los enemigos mas fuertes quitan mas).
    public void recibirGolpe(int danio) {
        salud = Math.max(0, salud - danio);
    }

    public boolean estaCompleta() {
        return salud == SALUD_MAXIMA;
    }

    public void curarCompleto() {
        salud = SALUD_MAXIMA;
    }

    public static void restablecer() {
        salud = SALUD_MAXIMA;
    }

    // Indica si queda al menos un punto de salud.
    public boolean estaVivo() {
        return salud > 0;
    }

    // Traduce los puntos internos (0 a 8) a vidas visibles (0 a 3),
    // redondeando hacia arriba para que la barra no se vacie demasiado pronto.
    private int vidasVisibles() {
        return (salud * VIDAS_VISUALES + SALUD_MAXIMA - 1) / SALUD_MAXIMA;
    }

    // Libera las cuatro texturas que forman la barra.
    public void dispose() {
        for (Texture barra : barras) {
            barra.dispose();
        }
    }
}
