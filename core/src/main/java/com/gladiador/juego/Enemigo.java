package com.gladiador.juego;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;

public class Enemigo {

    private static final float ANCHO_ENEMIGO = 220f;
    private static final float ALTO_VISIBLE_ENEMIGO = 130f;
    private static final float VELOCIDAD_BASE = 90f;
    private static final float RANGO_VISION = 420f;
    // Distancia (centro a centro) desde la que empieza a atacar. Tiene que ser menor
    // que lo que realmente alcanza su golpe (unos 170) para que no ataque al aire.
    private static final float RANGO_ATAQUE = 165f;
    private static final float DISTANCIA_OBJETIVO = 145f;
    // Si Jorge se pega demasiado mientras el enemigo recarga, el enemigo retrocede.
    private static final float DISTANCIA_RETROCESO = 115f;
    private static final float TOLERANCIA_ALTURA_ATAQUE = 100f;
    private static final float TOLERANCIA_ALTURA_PERSECUCION = 100f;
    private static final float COOLDOWN_ATAQUE = 1.2f;
    // Pausa antes de atacar (aleatoria) para que no sea siempre el mismo ritmo.
    private static final float ESPERA_ATAQUE_MIN = 0.25f;
    private static final float ESPERA_ATAQUE_MAX = 0.60f;
    private static final float DURACION_ANIMACION_ATAQUE = 15 * 0.07f;
    // El golpe hace daño durante una pequeña ventana (2 frames), no en un solo instante.
    private static final float MOMENTO_IMPACTO = 0.49f;
    private static final float VENTANA_IMPACTO = 0.14f;
    private static final float DURACION_ATURDIMIENTO_GOLPE = 0.35f;
    private static final float DURACION_MUERTE = 0.50f;
    private static final float HITBOX_ANCHO = 70f;
    private static final float HITBOX_ALTO = 95f;
    private static final float HITBOX_ATAQUE_ANCHO = 100f;
    private static final float HITBOX_ATAQUE_ALTO = 95f;

    private final Animation<TextureRegion> caminarDerecha;
    private final Animation<TextureRegion> caminarIzquierda;
    private final Animation<TextureRegion> atacarDerecha;
    private final Animation<TextureRegion> atacarIzquierda;
    private final ArrayList<Texture> imagenes = new ArrayList<>();
    private final Map<TextureRegion, LimitesSprite> limitesSprites = new IdentityHashMap<>();
    private final VidaEnemigo vida = new VidaEnemigo();

    private float x;
    private float y;
    private float velocidad;
    private float tiempoAnimacion = 0f;
    private float tiempoAnimacionAtaque = 0f;
    private float cooldownAtaque = 0f;
    private float patrolMinX;
    private float patrolMaxX;
    private boolean patrullandoDerecha = true;
    private boolean miraDerecha = false;
    private boolean atacando = false;
    private boolean impactoResuelto = false;
    private long ultimoAtaqueRecibido = -1L;

    // Estado nuevo del combate.
    private boolean preparandoAtaque = false;
    private float esperaAtaque = 0f;
    private float tiempoAturdido = 0f;
    private float tiempoDanio = 0f;
    private float velocidadEmpujeX = 0f;
    private float tiempoMuerte = 0f;

    public Enemigo(float x, float y) {
        this(x, y, VELOCIDAD_BASE);
    }

    public Enemigo(float x, float y, float velocidad) {
        this.x = x;
        this.y = y;
        this.velocidad = velocidad;
        this.patrolMinX = 0f;
        this.patrolMaxX = Float.MAX_VALUE;

        caminarDerecha = cargarAnimacion("Enemigo 1 movimientos/Caminar derecha", 10, 0.08f);
        caminarIzquierda = cargarAnimacion("Enemigo 1 movimientos/Caminar izquierda sin fondo", 10, 0.08f);
        atacarDerecha = cargarAnimacion("Enemigo 1 movimientos/Atacar derecha", 15, 0.07f);
        atacarIzquierda = cargarAnimacion("Enemigo 1 movimientos/Atacar izquierda", 15, 0.07f);
    }

    public void actualizar(float delta, float objetivoX, float objetivoY, float limiteIzquierdo, float limiteDerecho) {
        patrolMinX = limiteIzquierdo + 60f;
        patrolMaxX = Math.max(patrolMinX, limiteDerecho - ANCHO_ENEMIGO);

        // Muerto: solo sigue deslizandose por el golpe mientras se desvanece.
        if (!estaVivo()) {
            tiempoMuerte += delta;
            aplicarEmpuje(delta);
            return;
        }

        aplicarEmpuje(delta);
        if (tiempoDanio > 0f) {
            tiempoDanio = Math.max(0f, tiempoDanio - delta);
        }

        // Aturdido: no hace nada hasta recuperarse.
        if (tiempoAturdido > 0f) {
            tiempoAturdido = Math.max(0f, tiempoAturdido - delta);
            return;
        }

        if (!atacando && cooldownAtaque > 0f) {
            cooldownAtaque = Math.max(0f, cooldownAtaque - delta);
        }
        float centroEnemigoX = x + ANCHO_ENEMIGO / 2f;
        float centroObjetivoX = objetivoX + 90f;
        float distanciaHorizontal = centroObjetivoX - centroEnemigoX;
        float distanciaVertical = objetivoY - y;
        boolean enVision = Math.abs(distanciaHorizontal) <= RANGO_VISION
            && Math.abs(distanciaVertical) <= TOLERANCIA_ALTURA_PERSECUCION;

        if (atacando) {
            tiempoAnimacionAtaque += delta;
            if (tiempoAnimacionAtaque >= DURACION_ANIMACION_ATAQUE) {
                atacando = false;
                tiempoAnimacionAtaque = 0f;
                tiempoAnimacion = 0f;
                // Cooldown con variacion para que el ritmo no sea predecible.
                cooldownAtaque = COOLDOWN_ATAQUE * MathUtils.random(0.8f, 1.5f);
            }
            return;
        }

        if (enVision) {
            if (distanciaHorizontal != 0f) {
                miraDerecha = distanciaHorizontal > 0f;
            }

            boolean enRangoDeAtaque = Math.abs(distanciaVertical) <= TOLERANCIA_ALTURA_ATAQUE
                && Math.abs(distanciaHorizontal) <= RANGO_ATAQUE;
            if (enRangoDeAtaque) {
                if (puedeAtacar()) {
                    // Se queda mirando a Jorge un momento antes de atacar.
                    if (!preparandoAtaque) {
                        preparandoAtaque = true;
                        esperaAtaque = MathUtils.random(ESPERA_ATAQUE_MIN, ESPERA_ATAQUE_MAX);
                    }
                    esperaAtaque -= delta;
                    if (esperaAtaque <= 0f) {
                        preparandoAtaque = false;
                        atacando = true;
                        impactoResuelto = false;
                        tiempoAnimacionAtaque = 0f;
                    }
                } else if (Math.abs(distanciaHorizontal) < DISTANCIA_RETROCESO) {
                    // Recargando y Jorge demasiado pegado: dar un paso atras.
                    x -= Math.signum(distanciaHorizontal) * velocidad * 0.6f * delta;
                    limitarX();
                    tiempoAnimacion += delta;
                }
                return;
            }

            preparandoAtaque = false;
            float distancia = Math.abs(distanciaHorizontal);
            if (distancia > DISTANCIA_OBJETIVO) {
                float distanciaARecorrer = distancia - DISTANCIA_OBJETIVO;
                float avance = Math.min(velocidad * delta, distanciaARecorrer);
                x += Math.signum(distanciaHorizontal) * avance;
            }
            tiempoAnimacion += delta;
            return;
        }

        preparandoAtaque = false;
        patrullar(delta);
    }

    private void patrullar(float delta) {
        if (x <= patrolMinX) {
            x = patrolMinX;
            patrullandoDerecha = true;
        } else if (x >= patrolMaxX) {
            x = patrolMaxX;
            patrullandoDerecha = false;
        }

        miraDerecha = patrullandoDerecha;
        float distancia = velocidad * 0.5f * delta;
        if (patrullandoDerecha) {
            x += distancia;
            if (x >= patrolMaxX) {
                x = patrolMaxX;
                patrullandoDerecha = false;
            }
        } else {
            x -= distancia;
            if (x <= patrolMinX) {
                x = patrolMinX;
                patrullandoDerecha = true;
            }
        }
        tiempoAnimacion += delta;
    }

    // Empuje por golpes recibidos: se apaga rapido.
    private void aplicarEmpuje(float delta) {
        if (velocidadEmpujeX == 0f) {
            return;
        }
        x += velocidadEmpujeX * delta;
        velocidadEmpujeX *= Math.max(0f, 1f - 8f * delta);
        if (Math.abs(velocidadEmpujeX) < 5f) {
            velocidadEmpujeX = 0f;
        }
        limitarX();
    }

    private void limitarX() {
        x = Math.max(patrolMinX, Math.min(x, patrolMaxX));
    }

    // Se usa para que los enemigos no se amontonen en el mismo lugar.
    public void aplicarSeparacion(float desplazamientoX) {
        x += desplazamientoX;
        limitarX();
    }

    // Interrumpe el ataque en curso.
    private void cancelarAtaque() {
        atacando = false;
        impactoResuelto = false;
        tiempoAnimacionAtaque = 0f;
        preparandoAtaque = false;
        cooldownAtaque = Math.max(cooldownAtaque, 0.5f);
    }

    public boolean estaEnRangoDeAtaque(float objetivoX, float objetivoY) {
        return estaVivo() && Math.abs(objetivoX - x) <= RANGO_ATAQUE && Math.abs(objetivoY - y) <= 80f;
    }

    public boolean puedeAtacar() {
        return estaVivo() && !atacando && cooldownAtaque <= 0f;
    }

    // true mientras el golpe del enemigo puede dañar (unos 2 frames de la animacion).
    public boolean impactoActivo() {
        return estaVivo()
            && atacando
            && !impactoResuelto
            && tiempoAnimacionAtaque >= MOMENTO_IMPACTO
            && tiempoAnimacionAtaque <= MOMENTO_IMPACTO + VENTANA_IMPACTO;
    }

    // Se llama cuando el golpe ya conecto (o fue bloqueado) para que no pegue dos veces.
    public void resolverImpacto() {
        impactoResuelto = true;
    }

    public int getVida() {
        return vida.getActual();
    }

    public int getVidaMaxima() {
        return vida.getMaxima();
    }

    // Devuelve true si el golpe se registro (false si ya lo habia recibido o esta muerto).
    // direccionEmpuje: 1 = lo empuja a la derecha, -1 a la izquierda.
    public boolean recibirGolpe(long numeroAtaque, float direccionEmpuje, float fuerzaEmpuje) {
        if (!estaVivo() || numeroAtaque == ultimoAtaqueRecibido) {
            return false;
        }

        ultimoAtaqueRecibido = numeroAtaque;
        vida.recibirGolpe();
        tiempoDanio = 0.25f;
        velocidadEmpujeX = direccionEmpuje * fuerzaEmpuje;
        if (estaVivo()) {
            // El golpe corta su ataque y lo deja aturdido un momento.
            cancelarAtaque();
            tiempoAturdido = DURACION_ATURDIMIENTO_GOLPE;
        } else {
            tiempoMuerte = 0f;
        }
        return true;
    }

    // Aturdimiento largo (lo usa el parry de Jorge).
    public void aturdir(float segundos, float direccionEmpuje, float fuerzaEmpuje) {
        if (!estaVivo()) {
            return;
        }
        cancelarAtaque();
        tiempoAturdido = segundos;
        velocidadEmpujeX = direccionEmpuje * fuerzaEmpuje;
    }

    public boolean estaVivo() {
        return vida.estaVivo();
    }

    // true cuando ya termino de desvanecerse y se puede sacar de la lista.
    public boolean puedeEliminarse() {
        return !estaVivo() && tiempoMuerte >= DURACION_MUERTE;
    }

    public com.badlogic.gdx.math.Rectangle getHitbox() {
        return new com.badlogic.gdx.math.Rectangle(x + 75f, y + 10f, HITBOX_ANCHO, HITBOX_ALTO);
    }

    // El area de ataque es simetrica respecto al centro del enemigo (x + 110):
    // mirando a la izquierda cubre [x-25, x+75] y mirando a la derecha [x+145, x+245].
    public com.badlogic.gdx.math.Rectangle getAreaDeAtaque() {
        float xAtaque = miraDerecha ? x + 145f : x - 25f;
        return new com.badlogic.gdx.math.Rectangle(xAtaque, y + 10f, HITBOX_ATAQUE_ANCHO, HITBOX_ATAQUE_ALTO);
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getCentroX() {
        return x + ANCHO_ENEMIGO / 2f;
    }

    public void dibujar(SpriteBatch batch) {
        dibujar(batch, 0f);
    }

    public void dibujar(SpriteBatch batch, float desplazamientoX) {
        if (puedeEliminarse()) {
            return;
        }
        TextureRegion frameActual = mirarFrameActual();
        LimitesSprite limites = limitesSprites.get(frameActual);
        float escala = ALTO_VISIBLE_ENEMIGO / limites.alto;
        float centroVisibleX = (limites.minX + limites.maxX + 1f) / 2f;
        float centroMarcoX = ANCHO_ENEMIGO / 2f;
        float posicionX = x - desplazamientoX + centroMarcoX - centroVisibleX * escala;
        float posicionY = y - (256f - limites.maxY - 1f) * escala;
        float tamanoMarco = 256f * escala;

        // Colores de aviso:
        // - se pone rojizo al recibir un golpe y se desvanece al morir
        // - azulado cuando esta aturdido (es el momento de castigarlo)
        // - anaranjado mientras prepara el tajo (para que Jorge pueda reaccionar)
        float rojo = 1f;
        float verde = 1f;
        float azul = 1f;
        float alfa = 1f;
        if (!estaVivo()) {
            verde = 0.4f;
            azul = 0.4f;
            alfa = Math.max(0f, 1f - tiempoMuerte / DURACION_MUERTE);
        } else if (tiempoDanio > 0f) {
            verde = 0.4f;
            azul = 0.4f;
        } else if (tiempoAturdido > 0f) {
            rojo = 0.6f;
            verde = 0.8f;
        } else if (atacando && tiempoAnimacionAtaque < MOMENTO_IMPACTO) {
            verde = 0.75f;
            azul = 0.45f;
        }
        batch.setColor(rojo, verde, azul, alfa);
        batch.draw(
            frameActual,
            posicionX,
            posicionY,
            tamanoMarco,
            tamanoMarco
        );
        batch.setColor(1f, 1f, 1f, 1f);
    }

    private TextureRegion mirarFrameActual() {
        if (atacando) {
            if (miraDerecha) {
                return atacarDerecha.getKeyFrame(tiempoAnimacionAtaque, false);
            }
            return atacarIzquierda.getKeyFrame(tiempoAnimacionAtaque, false);
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
            limitesSprites.put(frames[indice], medirLimitesSprite(nombre));
        }

        return new Animation<>(duracion, frames);
    }

    private LimitesSprite medirLimitesSprite(String ruta) {
        Pixmap pixmap = new Pixmap(Gdx.files.internal(ruta));
        int minX = pixmap.getWidth();
        int minY = pixmap.getHeight();
        int maxX = -1;
        int maxY = -1;
        for (int pixelY = 0; pixelY < pixmap.getHeight(); pixelY++) {
            for (int pixelX = 0; pixelX < pixmap.getWidth(); pixelX++) {
                if ((pixmap.getPixel(pixelX, pixelY) & 0xff) != 0) {
                    minX = Math.min(minX, pixelX);
                    minY = Math.min(minY, pixelY);
                    maxX = Math.max(maxX, pixelX);
                    maxY = Math.max(maxY, pixelY);
                }
            }
        }
        pixmap.dispose();
        if (maxX < minX || maxY < minY) {
            throw new IllegalArgumentException("El sprite no contiene pixeles visibles: " + ruta);
        }
        return new LimitesSprite(minX, minY, maxX, maxY);
    }

    private static class LimitesSprite {
        private final int minX;
        private final int maxX;
        private final int maxY;
        private final int alto;

        private LimitesSprite(int minX, int minY, int maxX, int maxY) {
            this.minX = minX;
            this.maxX = maxX;
            this.maxY = maxY;
            alto = maxY - minY + 1;
        }
    }
}
