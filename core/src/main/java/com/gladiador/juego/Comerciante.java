package com.gladiador.juego;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

// El mercader que esta parado en el lobby (pantalla JuegoScreen).
// Solo cumple dos funciones: animarse y avisar si Jorge esta lo suficientemente cerca.
public class Comerciante {

    private static final float DISTANCIA_INTERACCION = 150f; // que tan cerca hay que pararse
    private static final int CANTIDAD_FRAMES = 25;

    private final Animation<TextureRegion> animacion;
    private final float x;
    private final float y;
    private float tiempoAnimacion;

    // Las 25 imagenes de la animacion se guardan aqui para poder liberarlas al salir.
    private final Texture[] texturas;

    // Carga las imagenes numeradas del mercader y las prepara como animacion.
    public Comerciante(float x, float y) {
        this.x = x;
        this.y = y;
        texturas = new Texture[CANTIDAD_FRAMES];
        TextureRegion[] frames = new TextureRegion[CANTIDAD_FRAMES];
        for (int i = 0; i < CANTIDAD_FRAMES; i++) {
            String nombre = String.format("mercader/idle_right/frames/%04d.png", i + 1);
            texturas[i] = new Texture(Gdx.files.internal(nombre));
            frames[i] = new TextureRegion(texturas[i]);
        }
        animacion = new Animation<>(0.08f, frames); // cada imagen dura 0,08 segundos
        animacion.setPlayMode(Animation.PlayMode.LOOP); // y se repite para siempre
    }

    // true si Jorge esta al lado del mercader (para mostrar el cartel de "Pulsa F").
    public boolean estaCerca(float jugadorX) {
        return Math.abs(jugadorX - x) <= DISTANCIA_INTERACCION;
    }

    // Avanza el reloj de la animacion y dibuja el frame que corresponde.
    public void dibujar(SpriteBatch batch) {
        tiempoAnimacion += Gdx.graphics.getDeltaTime();
        batch.draw(animacion.getKeyFrame(tiempoAnimacion), x, y, 180f, 180f);
    }

    // Libera cada imagen cargada para la animacion.
    public void dispose() {
        for (Texture textura : texturas) {
            textura.dispose();
        }
    }
}
