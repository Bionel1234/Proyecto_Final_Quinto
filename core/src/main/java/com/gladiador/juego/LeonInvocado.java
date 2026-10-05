package com.gladiador.juego;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

public class LeonInvocado {

    private static final int CANTIDAD_FRAMES = 25;
    private static final float ALTO_VISIBLE = 100f;
    private static final float VELOCIDAD = 500f;

    private final ArrayList<Texture> texturas = new ArrayList<>();
    private final Animation<TextureRegion> correr;
    private final float ancho;
    private final float piso;
    private float x;
    private float tiempoAnimacion;
    private boolean activo = true;

    public LeonInvocado(float x, float piso) {
        this.x = x;
        this.piso = piso;

        TextureRegion[] frames = new TextureRegion[CANTIDAD_FRAMES];
        for (int indice = 0; indice < CANTIDAD_FRAMES; indice++) {
            String ruta = String.format("bestiario/leon/%02d.png", indice + 1);
            Texture textura = new Texture(Gdx.files.internal(ruta));
            texturas.add(textura);
            TextureRegion frame = SpriteFrameUtils.recortarTransparencia(textura, ruta);
            frame.flip(true, false);
            frames[indice] = frame;
        }
        correr = new Animation<>(0.05f, frames);
        correr.setPlayMode(Animation.PlayMode.LOOP);
        ancho = frames[0].getRegionWidth() * ALTO_VISIBLE / frames[0].getRegionHeight();
    }

    public void actualizar(float delta) {
        if (!activo) {
            return;
        }
        tiempoAnimacion += delta;
        x -= VELOCIDAD * delta;
        if (x + ancho < 0f) {
            activo = false;
        }
    }

    public Rectangle getHitbox() {
        return new Rectangle(x + ancho * 0.12f, piso + 12f, ancho * 0.76f, ALTO_VISIBLE * 0.72f);
    }

    public void desactivar() {
        activo = false;
    }

    public boolean estaActivo() {
        return activo;
    }

    public void dibujar(SpriteBatch batch) {
        if (activo) {
            batch.draw(correr.getKeyFrame(tiempoAnimacion), x, piso, ancho, ALTO_VISIBLE);
        }
    }

    public void dispose() {
        for (Texture textura : texturas) {
            textura.dispose();
        }
    }
}
