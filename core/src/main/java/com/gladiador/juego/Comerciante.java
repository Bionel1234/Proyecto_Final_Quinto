package com.gladiador.juego;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Comerciante {

    private static final float ANCHO = 180f;
    private static final float ALTO = 180f;
    private static final float DISTANCIA_INTERACCION = 150f;
    private static final int CANTIDAD_FRAMES = 25;

    private final Texture[] texturas;
    private final Animation<TextureRegion> animacion;
    private final float x;
    private final float y;
    private float tiempoAnimacion;

    public Comerciante(float x, float y) {
        this.x = x;
        this.y = y;
        texturas = new Texture[CANTIDAD_FRAMES];
        TextureRegion[] frames = new TextureRegion[CANTIDAD_FRAMES];
        for (int i = 0; i < CANTIDAD_FRAMES; i++) {
            String nombre = String.format(
                "mercader/idle_right/frames/%04d.png",
                i + 1
            );
            texturas[i] = new Texture(Gdx.files.internal(nombre));
            frames[i] = new TextureRegion(texturas[i]);
        }
        animacion = new Animation<>(0.08f, frames);
        animacion.setPlayMode(Animation.PlayMode.LOOP);
    }

    public boolean estaCerca(float jugadorX) {
        return Math.abs(jugadorX - x) <= DISTANCIA_INTERACCION;
    }

    public void dibujar(SpriteBatch batch) {
        tiempoAnimacion += Gdx.graphics.getDeltaTime();
        batch.draw(animacion.getKeyFrame(tiempoAnimacion), x, y, ANCHO, ALTO);
    }

    public void dispose() {
        for (Texture textura : texturas) {
            textura.dispose();
        }
    }
}
