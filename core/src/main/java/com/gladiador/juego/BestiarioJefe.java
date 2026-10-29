package com.gladiador.juego;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;

// El jefe del Coliseo. En la Zona 1 usa el Bestiario; en la Zona 2 usa la version
// con un ataque de cadena y una onda expansiva.
public class BestiarioJefe {

    private static final int CANTIDAD_FRAMES_LATIGO = 12;   // frames de la animacion de ataque
    private static final int CANTIDAD_FRAMES_ESTAITCO = 13; // frames de la animacion quieto
    private static final int VIDA_MAXIMA = 16;
    private static final int VIDA_MAXIMA_ZONA_2 = 20;
    private static final float DURACION_FRAME_ESTAITCO = 0.10f;
    private static final float DURACION_ANIMACION_LATIGO = 1.75f;
    private static final float DURACION_FRAME_LATIGO = DURACION_ANIMACION_LATIGO / CANTIDAD_FRAMES_LATIGO;
    private static final float DURACION_INVOCACION = 1.4f;
    private static final float DURACION_ONDA_ESCAPE = 1.15f;
    private static final float MOMENTO_IMPACTO_LATIGO = 0.78f;
    private static final float ESPERA_ENTRE_ATAQUES_MINIMA = 2.2f;
    private static final float ESPERA_ENTRE_ATAQUES_MAXIMA = 3.4f;
    private static final float TAMANO_LIENZO = 512f;
    private static final float POSICION_CENTRO_X = 1050f;
    private static final float ALTURA_PISO = 72f;
    private static final float ALTURA_MANO_LATIGO = ALTURA_PISO + 68f;
    private static final float DISTANCIA_MANO_CENTRO = 50f;
    private static final float ALTURA_VISUAL_JEFE = 300f;
    private static final float FRACCION_BANDA_PIES = 0.18f;

    public enum Ataque {
        NINGUNO,
        LATIGO,
        LEON,
        CADENA,
        ONDA_ESCAPE
    }

    private static class Medida {
        float centroPiesX;
        float filaPiesY;
        float altoVisible;
        float canvas;
    }

    private final boolean jefeZona2;
    private final Animation<TextureRegion> estaticoDerecha;
    private final Animation<TextureRegion> estaticoIzquierda;
    private final Animation<TextureRegion> ataqueDerecha;
    private final Animation<TextureRegion> ataqueIzquierda;
    private final Animation<TextureRegion> ondaDerecha;
    private final Animation<TextureRegion> ondaIzquierda;
    private final Medida medidaEstatico;
    private final Medida medidaAtaque;
    private final Medida medidaOnda;
    private final float escalaVisual;
    private final ArrayList<Texture> texturas = new ArrayList<>();

    private int vida;
    private float tiempoAtaque;
    private float tiempoQuieto;
    private float tiempoHastaAtaque = 1.8f;
    private float tiempoDanio;
    private long ultimoAtaqueRecibido = -1L;
    private boolean impactoLatigoResuelto;
    private boolean leonInvocado;
    private boolean proximoAtaqueEsLatigo = true;
    private boolean proximoAtaqueEsCadena = true;
    private Ataque ataqueActual = Ataque.NINGUNO;

    private boolean impactoLatigoPendiente;
    private boolean invocacionPendiente;
    private boolean ondaEscapePendiente;

    public BestiarioJefe() {
        this(false);
    }

    public BestiarioJefe(boolean jefeZona2) {
        this.jefeZona2 = jefeZona2;
        if (jefeZona2) {
            vida = VIDA_MAXIMA_ZONA_2;
            estaticoDerecha = cargarAnimacion("Jefe zona 2/Ataque normal jefe zona 2 derecha", 15, 0.08f);
            estaticoIzquierda = cargarAnimacion("Jefe zona 2/Ataque normal jefe zona 2 izquierda", 15, 0.08f);
            ataqueDerecha = cargarAnimacion("Jefe zona 2/Ataque normal jefe zona 2 derecha", 15, 0.08f);
            ataqueIzquierda = cargarAnimacion("Jefe zona 2/Ataque normal jefe zona 2 izquierda", 15, 0.08f);
            ondaDerecha = cargarAnimacion("Jefe zona 2/Ataque onda expansiva jefe zona 2 derecha", 14, 0.09f);
            ondaIzquierda = cargarAnimacion("Jefe zona 2/Ataque onda expansiva jefe zona 2 izquierda", 14, 0.09f);
            medidaEstatico = medirConjunto("Jefe zona 2/Ataque normal jefe zona 2 derecha", 15, 512f);
            medidaAtaque = medirConjunto("Jefe zona 2/Ataque normal jefe zona 2 derecha", 15, 512f);
            medidaOnda = medirConjunto("Jefe zona 2/Ataque onda expansiva jefe zona 2 derecha", 14, 512f);
        } else {
            vida = VIDA_MAXIMA;
            estaticoDerecha = cargarAnimacion("jefe1/estatico_derecha", CANTIDAD_FRAMES_ESTAITCO, DURACION_FRAME_ESTAITCO);
            estaticoIzquierda = cargarAnimacion("jefe1/estatico_izquierda", CANTIDAD_FRAMES_ESTAITCO, DURACION_FRAME_ESTAITCO);
            ataqueDerecha = cargarAnimacion("jefe1/ataque_derecha", CANTIDAD_FRAMES_LATIGO, DURACION_FRAME_LATIGO);
            ataqueIzquierda = cargarAnimacion("jefe1/ataque_izquierda", CANTIDAD_FRAMES_LATIGO, DURACION_FRAME_LATIGO);
            ondaDerecha = null;
            ondaIzquierda = null;
            medidaEstatico = medirConjunto("jefe1/estatico_derecha", CANTIDAD_FRAMES_ESTAITCO, 256f);
            medidaAtaque = medirConjunto("jefe1/ataque_derecha", CANTIDAD_FRAMES_LATIGO, 512f);
            medidaOnda = medidaAtaque;
        }
        escalaVisual = ALTURA_VISUAL_JEFE / medidaEstatico.altoVisible;
    }

    public void actualizar(float delta) {
        tiempoQuieto += delta;
        if (tiempoDanio > 0f) {
            tiempoDanio = Math.max(0f, tiempoDanio - delta);
        }

        if (ataqueActual == Ataque.NINGUNO) {
            tiempoHastaAtaque -= delta;
            if (tiempoHastaAtaque <= 0f) {
                iniciarSiguienteAtaque();
            }
            return;
        }

        tiempoAtaque += delta;
        if (jefeZona2) {
            if (ataqueActual == Ataque.CADENA) {
                if (!impactoLatigoResuelto && tiempoAtaque >= MOMENTO_IMPACTO_LATIGO) {
                    impactoLatigoResuelto = true;
                    impactoLatigoPendiente = true;
                }
                if (tiempoAtaque >= DURACION_ANIMACION_LATIGO) {
                    finalizarAtaque();
                }
            } else if (ataqueActual == Ataque.ONDA_ESCAPE) {
                if (!ondaEscapePendiente && tiempoAtaque >= 0.48f) {
                    ondaEscapePendiente = true;
                }
                if (tiempoAtaque >= DURACION_ONDA_ESCAPE) {
                    finalizarAtaque();
                }
            }
            return;
        }

        if (ataqueActual == Ataque.LATIGO) {
            if (!impactoLatigoResuelto && tiempoAtaque >= MOMENTO_IMPACTO_LATIGO) {
                impactoLatigoResuelto = true;
                impactoLatigoPendiente = true;
            }
            if (tiempoAtaque >= DURACION_ANIMACION_LATIGO) {
                finalizarAtaque();
            }
        } else {
            if (!leonInvocado && tiempoAtaque >= 0.46f) {
                leonInvocado = true;
                invocacionPendiente = true;
            }
            if (tiempoAtaque >= DURACION_INVOCACION) {
                finalizarAtaque();
            }
        }
    }

    private void iniciarSiguienteAtaque() {
        if (jefeZona2) {
            ataqueActual = proximoAtaqueEsCadena ? Ataque.CADENA : Ataque.ONDA_ESCAPE;
            proximoAtaqueEsCadena = !proximoAtaqueEsCadena;
        } else {
            ataqueActual = proximoAtaqueEsLatigo ? Ataque.LATIGO : Ataque.LEON;
            proximoAtaqueEsLatigo = !proximoAtaqueEsLatigo;
        }
        tiempoAtaque = 0f;
        impactoLatigoResuelto = false;
        leonInvocado = false;
    }

    private void finalizarAtaque() {
        ataqueActual = Ataque.NINGUNO;
        impactoLatigoPendiente = false;
        invocacionPendiente = false;
        ondaEscapePendiente = false;
        tiempoHastaAtaque = MathUtils.random(ESPERA_ENTRE_ATAQUES_MINIMA, ESPERA_ENTRE_ATAQUES_MAXIMA);
    }

    public boolean consumirImpactoLatigo() {
        if (!impactoLatigoPendiente) {
            return false;
        }
        impactoLatigoPendiente = false;
        return true;
    }

    public boolean consumirImpactoCadena() {
        return consumirImpactoLatigo();
    }

    public boolean consumirInvocacionLeon() {
        if (!invocacionPendiente) {
            return false;
        }
        invocacionPendiente = false;
        return true;
    }

    public boolean consumirImpactoOndaEscape() {
        if (!ondaEscapePendiente) {
            return false;
        }
        ondaEscapePendiente = false;
        return true;
    }

    public boolean recibirGolpe(long numeroAtaque) {
        if (!estaVivo() || numeroAtaque == ultimoAtaqueRecibido) {
            return false;
        }
        ultimoAtaqueRecibido = numeroAtaque;
        vida--;
        tiempoDanio = 0.18f;
        return true;
    }

    public void aturdir() {
        if (!estaVivo()) {
            return;
        }
        impactoLatigoPendiente = false;
        invocacionPendiente = false;
        ondaEscapePendiente = false;
        ataqueActual = Ataque.NINGUNO;
        tiempoAtaque = 0f;
        tiempoHastaAtaque = 1f;
    }

    public Rectangle getHitbox() {
        float mitadAnchoVisible = TAMANO_LIENZO * 0.98f * 0.15f;
        return new Rectangle(
            POSICION_CENTRO_X - mitadAnchoVisible,
            ALTURA_PISO + 5f,
            mitadAnchoVisible * 2f,
            TAMANO_LIENZO * 0.98f * 0.33f
        );
    }

    public Rectangle getAreaLatigo(float jugadorX, float jugadorY) {
        return getAreaAtaqueGeneral(jugadorX, jugadorY, 24f, 84f);
    }

    public Rectangle getAreaCadena(float jugadorX, float jugadorY) {
        return getAreaLatigo(jugadorX, jugadorY);
    }

    public Rectangle getAreaOndaEscape(float jugadorX, float jugadorY) {
        float origenX = POSICION_CENTRO_X;
        float xMinimo = Math.min(origenX, jugadorX) - 38f;
        float ancho = Math.max(120f, Math.abs(jugadorX - origenX) + 140f);
        return new Rectangle(xMinimo, ALTURA_PISO + 20f, ancho, 150f);
    }

    private Rectangle getAreaAtaqueGeneral(float jugadorX, float jugadorY, float margenX, float margenY) {
        float origenX = POSICION_CENTRO_X + Math.signum(jugadorX - POSICION_CENTRO_X) * DISTANCIA_MANO_CENTRO;
        float objetivoY = jugadorY + 65f;
        float xMinimo = Math.min(origenX, jugadorX) - margenX;
        float yMinimo = Math.min(ALTURA_MANO_LATIGO, objetivoY) - 42f;
        float ancho = Math.max(24f, Math.abs(jugadorX - origenX) + 24f);
        float alto = Math.max(60f, Math.abs(objetivoY - ALTURA_MANO_LATIGO) + margenY);
        return new Rectangle(xMinimo, yMinimo, ancho, alto);
    }

    public float getCentroX() {
        return POSICION_CENTRO_X;
    }

    public int getVida() {
        return vida;
    }

    public int getVidaMaxima() {
        return jefeZona2 ? VIDA_MAXIMA_ZONA_2 : VIDA_MAXIMA;
    }

    public boolean estaVivo() {
        return vida > 0;
    }

    public Ataque getAtaqueActual() {
        return ataqueActual;
    }

    public void dibujar(SpriteBatch batch, float jugadorX) {
        boolean miraDerecha = jugadorX >= POSICION_CENTRO_X;
        TextureRegion frame;
        Medida medida;

        if (ataqueActual == Ataque.ONDA_ESCAPE) {
            frame = miraDerecha
                ? ondaDerecha.getKeyFrame(tiempoAtaque, false)
                : ondaIzquierda.getKeyFrame(tiempoAtaque, false);
            medida = medidaOnda;
        } else if (ataqueActual == Ataque.CADENA || ataqueActual == Ataque.LATIGO || ataqueActual == Ataque.LEON) {
            frame = miraDerecha
                ? ataqueDerecha.getKeyFrame(tiempoAtaque, false)
                : ataqueIzquierda.getKeyFrame(tiempoAtaque, false);
            medida = medidaAtaque;
        } else {
            frame = miraDerecha
                ? estaticoDerecha.getKeyFrame(tiempoQuieto, true)
                : estaticoIzquierda.getKeyFrame(tiempoQuieto, true);
            medida = medidaEstatico;
        }

        float tamano = medida.canvas * escalaVisual;
        float x = POSICION_CENTRO_X - medida.centroPiesX * escalaVisual;
        float y = ALTURA_PISO - (medida.canvas - 1f - medida.filaPiesY) * escalaVisual;

        batch.setColor(1f, 1f, 1f, 1f);
        if (tiempoDanio > 0f && ((int) (tiempoDanio * 40f) % 2) == 0) {
            batch.setColor(1f, 0.5f, 0.5f, 1f);
        }
        batch.draw(frame, x, y, tamano, tamano);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    private Animation<TextureRegion> cargarAnimacion(String ruta, int cantidad, float duracionFrame) {
        TextureRegion[] frames = new TextureRegion[cantidad];
        for (int indice = 0; indice < cantidad; indice++) {
            String nombre = String.format("%s/%02d.png", ruta, indice + 1);
            Texture textura = new Texture(Gdx.files.internal(nombre));
            texturas.add(textura);
            frames[indice] = new TextureRegion(textura);
        }
        return new Animation<>(duracionFrame, frames);
    }

    private Medida medirConjunto(String ruta, int cantidad, float canvas) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = -1;
        int maxY = -1;

        for (int indice = 0; indice < cantidad; indice++) {
            Pixmap pixmap = new Pixmap(Gdx.files.internal(String.format("%s/%02d.png", ruta, indice + 1)));
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
        }

        int banda = Math.max(1, Math.round((maxY - minY + 1) * FRACCION_BANDA_PIES));
        int piesMinX = Integer.MAX_VALUE;
        int piesMaxX = -1;
        for (int indice = 0; indice < cantidad; indice++) {
            Pixmap pixmap = new Pixmap(Gdx.files.internal(String.format("%s/%02d.png", ruta, indice + 1)));
            for (int pixelY = maxY - banda; pixelY <= maxY; pixelY++) {
                for (int pixelX = 0; pixelX < pixmap.getWidth(); pixelX++) {
                    if ((pixmap.getPixel(pixelX, pixelY) & 0xff) != 0) {
                        piesMinX = Math.min(piesMinX, pixelX);
                        piesMaxX = Math.max(piesMaxX, pixelX);
                    }
                }
            }
            pixmap.dispose();
        }

        Medida medida = new Medida();
        medida.centroPiesX = (piesMinX + piesMaxX + 1f) / 2f;
        medida.filaPiesY = maxY;
        medida.altoVisible = maxY - minY + 1f;
        medida.canvas = canvas;
        return medida;
    }

    public void dispose() {
        for (Texture textura : texturas) {
            textura.dispose();
        }
    }
}
