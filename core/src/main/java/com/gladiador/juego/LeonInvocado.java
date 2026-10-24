package com.gladiador.juego;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

// El leon que invoca el Bestiario en el combate del Coliseo.
// Corre en linea recta hacia la izquierda y se desactiva cuando choca con Jorge
// o cuando sale de la pantalla.
public class LeonInvocado {

    private static final int CANTIDAD_FRAMES = 25;
    private static final float ALTO_VISIBLE = 100f; // alto con el que se dibuja
    private static final float VELOCIDAD = 500f;    // pixeles por segundo

    private final ArrayList<Texture> texturas = new ArrayList<>();
    private final Animation<TextureRegion> correr;
    private final float ancho; // ancho calculado a partir del sprite recortado
    private final float piso;  // altura del suelo donde corre
    private float x;
    private float tiempoAnimacion;
    private boolean activo = true; // false cuando ya choco o salio

    public LeonInvocado(float x, float piso) {
        this.x = x;
        this.piso = piso;

        // Cargar y recortar los 25 frames del leon corriendo.
        TextureRegion[] frames = new TextureRegion[CANTIDAD_FRAMES];
        for (int indice = 0; indice < CANTIDAD_FRAMES; indice++) {
            String ruta = String.format("bestiario/leon/%02d.png", indice + 1);
            Texture textura = new Texture(Gdx.files.internal(ruta));
            texturas.add(textura);
            TextureRegion frame = SpriteFrameUtils.recortarTransparencia(textura, ruta);
            frame.flip(true, false); // los dibujos originales miran al otro lado
            frames[indice] = frame;
        }
        correr = new Animation<>(0.05f, frames);
        correr.setPlayMode(Animation.PlayMode.LOOP);

        // Mantener la proporcion: con el alto elegido, calcular cuanto ocupa de ancho.
        ancho = frames[0].getRegionWidth() * ALTO_VISIBLE / frames[0].getRegionHeight();
    }

    // Avanza el tiempo de la animacion y mueve el leon hacia Jorge.
    public void actualizar(float delta) {
        if (!activo) {
            return;
        }
        tiempoAnimacion += delta;
        x -= VELOCIDAD * delta; // avanza hacia la izquierda
        if (x + ancho < 0f) {
            activo = false; // ya salio de la pantalla
        }
    }

    // Caja del leon para detectar si choca con Jorge (un poco mas chica que el dibujo).
    public Rectangle getHitbox() {
        return new Rectangle(x + ancho * 0.12f, piso + 12f, ancho * 0.76f, ALTO_VISIBLE * 0.72f);
    }

    // Marca al leon como inactivo para que la pantalla lo quite de su lista.
    public void desactivar() {
        activo = false;
    }

    // Consulta si aun debe dibujarse y comprobarse su colision.
    public boolean estaActivo() {
        return activo;
    }

    public void dibujar(SpriteBatch batch) {
        if (activo) {
            batch.draw(correr.getKeyFrame(tiempoAnimacion), x, piso, ancho, ALTO_VISIBLE);
        }
    }

    // Libera los frames usados por la animacion de carrera.
    public void dispose() {
        for (Texture textura : texturas) {
            textura.dispose();
        }
    }
}
