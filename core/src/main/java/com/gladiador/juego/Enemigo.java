package com.gladiador.juego;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Enemigo {

    private static final float ANCHO_ENEMIGO = 220f;
    private static final float ALTO_ENEMIGO = 220f;
    private static final float VELOCIDAD_BASE = 90f;
    private static final float RANGO_VISION = 420f;
    private static final float RANGO_ATAQUE = 90f;
    private static final float TIEMPO_ATAQUE = 1.2f;
    private static final float TIEMPO_RECUPERACION_GOLPE = 0.35f;
    private static final int VIDA_MAXIMA = 2;
    private static final float HITBOX_ANCHO = 120f;
    private static final float HITBOX_ALTO = 120f;

    private final Animation<TextureRegion> caminarDerecha;
    private final Animation<TextureRegion> caminarIzquierda;
    private final Animation<TextureRegion> atacarDerecha;
    private final Animation<TextureRegion> atacarIzquierda;
    private final ArrayList<Texture> imagenes = new ArrayList<>();

    private float x;
    private float y;
    private float velocidad;
    private float tiempoAnimacion = 0f;
    private float tiempoEntreAtaques = 0f;
    private float tiempoRecuperacionGolpe = 0f;
    private float patrolMinX;
    private float patrolMaxX;
    private boolean miraDerecha = false;
    private boolean atacando = false;
    private int vida = VIDA_MAXIMA;
    private boolean vivo = true;

    public Enemigo(float x, float y) {
        this(x, y, VELOCIDAD_BASE);
    }

    public Enemigo(float x, float y, float velocidad) {
        this.x = x;
        this.y = y;
        this.velocidad = velocidad;
        this.patrolMinX = x - 150f;
        this.patrolMaxX = x + 150f;

        caminarDerecha = cargarAnimacion("Enemigo 1 movimientos/Caminar derecha", 10, 0.08f);
        caminarIzquierda = cargarAnimacion("Enemigo 1 movimientos/Caminar izquierda sin fondo", 10, 0.08f);
        atacarDerecha = cargarAnimacion("Enemigo 1 movimientos/Atacar derecha", 15, 0.07f);
        atacarIzquierda = cargarAnimacion("Enemigo 1 movimientos/Atacar izquierda", 15, 0.07f);
    }

    public void actualizar(float delta, float objetivoX, float objetivoY, float limiteIzquierdo, float limiteDerecho) {
        if (!vivo) {
            return;
        }

        tiempoAnimacion += delta;
        if (tiempoEntreAtaques > 0f) {
            tiempoEntreAtaques = Math.max(0f, tiempoEntreAtaques - delta);
        }
        if (tiempoRecuperacionGolpe > 0f) {
            tiempoRecuperacionGolpe = Math.max(0f, tiempoRecuperacionGolpe - delta);
        }

        float distanciaHorizontal = objetivoX - x;
        float distanciaVertical = objetivoY - y;
        boolean enVision = Math.abs(distanciaHorizontal) <= RANGO_VISION && Math.abs(distanciaVertical) <= 80f;
        atacando = enVision && Math.abs(distanciaHorizontal) <= RANGO_ATAQUE;

        if (enVision) {
            miraDerecha = distanciaHorizontal >= 0f;
            if (!atacando) {
                x = x + Math.signum(distanciaHorizontal) * velocidad * delta;
            }
            return;
        }

        float derechaPatrulla = Math.max(limiteIzquierdo + 60f, patrolMaxX);
        float izquierdaPatrulla = Math.min(limiteDerecho - 60f, patrolMinX);

        if (x <= izquierdaPatrulla) {
            miraDerecha = true;
        } else if (x >= derechaPatrulla) {
            miraDerecha = false;
        }

        if (miraDerecha) {
            x = x + velocidad * 0.35f * delta;
            if (x > derechaPatrulla) {
                x = derechaPatrulla;
                miraDerecha = false;
            }
        } else {
            x = x - velocidad * 0.35f * delta;
            if (x < izquierdaPatrulla) {
                x = izquierdaPatrulla;
                miraDerecha = true;
            }
        }
    }

    public boolean estaEnRangoDeAtaque(float objetivoX, float objetivoY) {
        return vivo && Math.abs(objetivoX - x) <= RANGO_ATAQUE && Math.abs(objetivoY - y) <= 80f;
    }

    public boolean puedeAtacar() {
        return vivo && tiempoEntreAtaques <= 0f;
    }

    public void reiniciarAtaque() {
        tiempoEntreAtaques = TIEMPO_ATAQUE;
    }

    public boolean puedeRecibirGolpe() {
        return vivo && tiempoRecuperacionGolpe <= 0f;
    }

    public int getVida() {
        return vida;
    }

    public void recibirGolpe() {
        if (!vivo || tiempoRecuperacionGolpe > 0f) {
            return;
        }
        vida = vida - 1;
        tiempoRecuperacionGolpe = TIEMPO_RECUPERACION_GOLPE;
        if (vida <= 0) {
            vivo = false;
            vida = 0;
        }
    }

    public boolean estaVivo() {
        return vivo;
    }

    public com.badlogic.gdx.math.Rectangle getHitbox() {
        return new com.badlogic.gdx.math.Rectangle(x + 25f, y + 20f, HITBOX_ANCHO, HITBOX_ALTO);
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public void dibujar(SpriteBatch batch) {
        dibujar(batch, 0f);
    }

    public void dibujar(SpriteBatch batch, float desplazamientoX) {
        if (!vivo) {
            return;
        }
        TextureRegion frameActual = mirarFrameActual();
        float ajustePiso = 42f;
        float posicionY = y - ajustePiso / 256f * ALTO_ENEMIGO;
        batch.draw(frameActual, x - desplazamientoX, posicionY, ANCHO_ENEMIGO, ALTO_ENEMIGO);
    }

    private TextureRegion mirarFrameActual() {
        if (atacando) {
            if (miraDerecha) {
                return atacarDerecha.getKeyFrame(tiempoAnimacion, true);
            }
            return atacarIzquierda.getKeyFrame(tiempoAnimacion, true);
        }

        if (miraDerecha) {
            return caminarDerecha.getKeyFrame(tiempoAnimacion, true);
        }
        return caminarIzquierda.getKeyFrame(tiempoAnimacion, true);
    }

    public void dispose() {
        for (Texture imagen : imagenes) {
            imagen.dispose();
        }
    }

    private Animation<TextureRegion> cargarAnimacion(String ruta, int cantidad, float duracion) {
        TextureRegion[] frames = new TextureRegion[cantidad];

        for (int indice = 0; indice < cantidad; indice++) {
            String nombre = String.format("%s/%02d.png", ruta, indice + 1);
            Texture imagen = new Texture(Gdx.files.internal(nombre));
            imagenes.add(imagen);
            frames[indice] = new TextureRegion(imagen);
        }

        return new Animation<>(duracion, frames);
    }
}
