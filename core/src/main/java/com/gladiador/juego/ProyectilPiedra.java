package com.gladiador.juego;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

public class ProyectilPiedra {

    private static final float VELOCIDAD = 430f;
    private static final float TAMANO = 30f;
    private static final float TAMANO_HITBOX = 22f;

    private final Texture textura;
    private final float velocidadX;
    private final float velocidadY;
    private float x;
    private float y;

    public ProyectilPiedra(Texture textura, float x, float y, float objetivoX, float objetivoY) {
        this.textura = textura;
        this.x = x;
        this.y = y;

        float diferenciaX = objetivoX - x;
        float tiempoVuelo = Math.max(Math.abs(diferenciaX) / VELOCIDAD, 0.12f);
        velocidadX = Math.signum(diferenciaX) * VELOCIDAD;
        velocidadY = (objetivoY - y) / tiempoVuelo;
    }

    public void actualizar(float delta) {
        x += velocidadX * delta;
        y += velocidadY * delta;
    }

    public Rectangle getHitbox() {
        float margen = (TAMANO - TAMANO_HITBOX) / 2f;
        return new Rectangle(x + margen, y + margen, TAMANO_HITBOX, TAMANO_HITBOX);
    }

    public float getCentroX() {
        return x + TAMANO / 2f;
    }

    public boolean estaFueraDelMapa(float anchoMapa) {
        return x + TAMANO < 0f || x > anchoMapa || y + TAMANO < 0f || y > MapaScreen.ALTO + 300f;
    }

    public void dibujar(SpriteBatch batch, float desplazamientoX) {
        batch.draw(textura, x - desplazamientoX, y, TAMANO, TAMANO);
    }
}
