package com.gladiador.juego;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

// Esta clase muestra las vidas de Jorge en la esquina superior derecha.
public class Vida {

    // Cantidad de vidas con la que comienza la partida.
    private int cantidad = 3;

    // Imagen que muestra tres corazones.
    private Texture tresVidas;

    // Imagen que muestra dos corazones.
    private Texture dosVidas;

    // Imagen que muestra un corazon.
    private Texture unaVida;

    // Crear las imagenes de las vidas.
    public Vida() {
        tresVidas = new Texture(Gdx.files.internal("vidas/vidas_1.png"));
        dosVidas = new Texture(Gdx.files.internal("vidas/vidas_2.png"));
        unaVida = new Texture(Gdx.files.internal("vidas/vidas_3.png"));
    }

    // Dibujar en pantalla la imagen que corresponde a la cantidad actual.
    public void dibujar(SpriteBatch batch) {
        if (cantidad <= 0) {
            return;
        }

        Texture imagenActual = tresVidas;

        if (cantidad == 2) {
            imagenActual = dosVidas;
        }

        if (cantidad == 1) {
            imagenActual = unaVida;
        }

        // Ancho maximo del dibujo.
        float ancho = 200f;

        // Calcular la altura manteniendo la proporcion de la imagen.
        float alto = ancho * imagenActual.getHeight() / imagenActual.getWidth();

        // Colocar la imagen arriba a la derecha.
        float x = JuegoScreen.ANCHO - ancho - 20f;
        float y = JuegoScreen.ALTO - alto - 20f;

        batch.draw(imagenActual, x, y, ancho, alto);
    }

    // Cambiar la cantidad de vidas.
    public void establecerVidas(int nuevaCantidad) {
        if (nuevaCantidad < 0) {
            cantidad = 0;
            return;
        }

        if (nuevaCantidad > 3) {
            cantidad = 3;
            return;
        }

        cantidad = nuevaCantidad;
    }

    // Devolver la cantidad actual de vidas.
    public int obtenerVidas() {
        return cantidad;
    }

    // Liberar las imagenes al cerrar la pantalla.
    public void dispose() {
        tresVidas.dispose();
        dosVidas.dispose();
        unaVida.dispose();
    }
}
