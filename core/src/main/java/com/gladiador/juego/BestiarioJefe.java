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
    private static final int CANTIDAD_FRAMES_EMBESTIDA = 17;
    private static final int CANTIDAD_FRAMES_MUERTE = 13;
    private static final float DURACION_FRAME_EMBESTIDA = 0.055f;
    private static final float DURACION_FRAME_MUERTE = 0.12f;
    private static final float VELOCIDAD_EMBESTIDA = 1000f;
    private static final float ESPERA_EMBESTIDA_MINIMA = 8f;
    private static final float ESPERA_EMBESTIDA_MAXIMA = 10f;
    private static final float CENTRO_X_MINIMO = 130f;
    private static final float CENTRO_X_MAXIMO = JuegoScreen.ANCHO - 130f;
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
    private static final String RUTA_EMBESTIDA_DERECHA = "bestiario/jefe1/embestir_derecha";
    private static final String RUTA_EMBESTIDA_IZQUIERDA = "bestiario/jefe1/embestir_izquierda";
    private static final String RUTA_MUERTE_DERECHA = "bestiario/jefe1/muerte_derecha";
    private static final String RUTA_MUERTE_IZQUIERDA = "bestiario/jefe1/muerte_izquierda";

    public enum Ataque {
        NINGUNO,
        LATIGO,
        LEON,
        CADENA,
        ONDA_ESCAPE,
        EMBESTIDA,
        MUERTE
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
    private final Animation<TextureRegion> embestidaDerecha;
    private final Animation<TextureRegion> embestidaIzquierda;
    private final Animation<TextureRegion> muerteDerecha;
    private final Animation<TextureRegion> muerteIzquierda;
    private final Medida medidaEstatico;
    private final Medida medidaAtaque;
    private final Medida medidaOnda;
    private final Medida medidaEmbestida;
    private final Medida medidaMuerte;
    private final float escalaVisual;
    private final ArrayList<Texture> texturas = new ArrayList<>();

    private int vida;
    private float tiempoAtaque;
    private float tiempoQuieto;
    private float tiempoHastaAtaque = 1.8f;
    private float tiempoDanio;
    private float centroX = POSICION_CENTRO_X;
    private float tiempoHastaEmbestida = 7f;
    private float direccionEmbestida;
    private long ultimoAtaqueRecibido = -1L;
    private boolean impactoLatigoResuelto;
    private boolean leonInvocado;
    private boolean proximoAtaqueEsLatigo = true;
    private boolean proximoAtaqueEsCadena = true;
    private Ataque ataqueActual = Ataque.NINGUNO;

    private boolean impactoLatigoPendiente;
    private boolean invocacionPendiente;
    private boolean ondaEscapePendiente;
    private boolean impactoEmbestidaResuelto;

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
            embestidaDerecha = null;
            embestidaIzquierda = null;
            muerteDerecha = null;
            muerteIzquierda = null;
            medidaEstatico = medirConjunto("Jefe zona 2/Ataque normal jefe zona 2 derecha", 15, 512f);
            medidaAtaque = medirConjunto("Jefe zona 2/Ataque normal jefe zona 2 derecha", 15, 512f);
            medidaOnda = medirConjunto("Jefe zona 2/Ataque onda expansiva jefe zona 2 derecha", 14, 512f);
            medidaEmbestida = null;
            medidaMuerte = null;
        } else {
            vida = VIDA_MAXIMA;
            estaticoDerecha = cargarAnimacion("jefe1/estatico_derecha", CANTIDAD_FRAMES_ESTAITCO, DURACION_FRAME_ESTAITCO);
            estaticoIzquierda = cargarAnimacion("jefe1/estatico_izquierda", CANTIDAD_FRAMES_ESTAITCO, DURACION_FRAME_ESTAITCO);
            ataqueDerecha = cargarAnimacion("jefe1/ataque_derecha", CANTIDAD_FRAMES_LATIGO, DURACION_FRAME_LATIGO);
            ataqueIzquierda = cargarAnimacion("jefe1/ataque_izquierda", CANTIDAD_FRAMES_LATIGO, DURACION_FRAME_LATIGO);
            ondaDerecha = null;
            ondaIzquierda = null;
            embestidaDerecha = cargarAnimacion(RUTA_EMBESTIDA_DERECHA,
                CANTIDAD_FRAMES_EMBESTIDA, DURACION_FRAME_EMBESTIDA);
            embestidaIzquierda = cargarAnimacion(RUTA_EMBESTIDA_IZQUIERDA,
                CANTIDAD_FRAMES_EMBESTIDA, DURACION_FRAME_EMBESTIDA);
            muerteDerecha = cargarAnimacion(RUTA_MUERTE_DERECHA,
                CANTIDAD_FRAMES_MUERTE, DURACION_FRAME_MUERTE);
            muerteIzquierda = cargarAnimacion(RUTA_MUERTE_IZQUIERDA,
                CANTIDAD_FRAMES_MUERTE, DURACION_FRAME_MUERTE);
            medidaEstatico = medirConjunto("jefe1/estatico_derecha", CANTIDAD_FRAMES_ESTAITCO, 256f);
            medidaAtaque = medirConjunto("jefe1/ataque_derecha", CANTIDAD_FRAMES_LATIGO, 512f);
            medidaOnda = medidaAtaque;
            medidaEmbestida = medirConjunto(RUTA_EMBESTIDA_DERECHA, CANTIDAD_FRAMES_EMBESTIDA, 256f);
            medidaMuerte = medirConjunto(RUTA_MUERTE_DERECHA, CANTIDAD_FRAMES_MUERTE, 256f);
        }
        escalaVisual = ALTURA_VISUAL_JEFE / medidaEstatico.altoVisible;
    }

    public void actualizar(float delta) {
        actualizar(delta, centroX - 1f);
    }

    public void actualizar(float delta, float jugadorX) {
        tiempoQuieto += delta;
        if (tiempoDanio > 0f) {
            tiempoDanio = Math.max(0f, tiempoDanio - delta);
        }

        if (!estaVivo()) {
            if (ataqueActual == Ataque.MUERTE) {
                tiempoAtaque += delta;
            }
            return;
        }

        if (ataqueActual == Ataque.NINGUNO) {
            if (!jefeZona2) {
                tiempoHastaEmbestida -= delta;
                if (tiempoHastaEmbestida <= 0f) {
                    iniciarEmbestida(jugadorX);
                    return;
                }
            }
            tiempoHastaAtaque -= delta;
            if (tiempoHastaAtaque <= 0f) {
                iniciarSiguienteAtaque();
            }
            return;
        }

        tiempoAtaque += delta;
        if (ataqueActual == Ataque.EMBESTIDA) {
            centroX += direccionEmbestida * VELOCIDAD_EMBESTIDA * delta;
            centroX = MathUtils.clamp(centroX, CENTRO_X_MINIMO, CENTRO_X_MAXIMO);
            if (centroX <= CENTRO_X_MINIMO || centroX >= CENTRO_X_MAXIMO
                    || embestidaDerecha.isAnimationFinished(tiempoAtaque)) {
                finalizarAtaque();
            }
            return;
        }
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

    private void iniciarEmbestida(float jugadorX) {
        ataqueActual = Ataque.EMBESTIDA;
        tiempoAtaque = 0f;
        direccionEmbestida = jugadorX >= centroX ? 1f : -1f;
        impactoEmbestidaResuelto = false;
        impactoLatigoPendiente = false;
        invocacionPendiente = false;
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
        if (ataqueActual == Ataque.EMBESTIDA) {
            tiempoHastaEmbestida = MathUtils.random(ESPERA_EMBESTIDA_MINIMA, ESPERA_EMBESTIDA_MAXIMA);
        }
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
        if (vida == 0) {
            ataqueActual = muerteDerecha == null ? Ataque.NINGUNO : Ataque.MUERTE;
            tiempoAtaque = 0f;
            impactoLatigoPendiente = false;
            invocacionPendiente = false;
            ondaEscapePendiente = false;
        }
        return true;
    }

    public boolean terminoAnimacionMuerte() {
        return !estaVivo() && (muerteDerecha == null
            || muerteDerecha.isAnimationFinished(tiempoAtaque));
    }

    public boolean registrarImpactoEmbestida() {
        if (ataqueActual != Ataque.EMBESTIDA || impactoEmbestidaResuelto) {
            return false;
        }
        impactoEmbestidaResuelto = true;
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
            centroX - mitadAnchoVisible,
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
        float origenX = centroX + Math.signum(jugadorX - centroX) * DISTANCIA_MANO_CENTRO;
        float objetivoY = jugadorY + 65f;
        float xMinimo = Math.min(origenX, jugadorX) - margenX;
        float yMinimo = Math.min(ALTURA_MANO_LATIGO, objetivoY) - 42f;
        float ancho = Math.max(24f, Math.abs(jugadorX - origenX) + 24f);
        float alto = Math.max(60f, Math.abs(objetivoY - ALTURA_MANO_LATIGO) + margenY);
        return new Rectangle(xMinimo, yMinimo, ancho, alto);
    }

    public float getCentroX() {
        return centroX;
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
        boolean miraDerecha = jugadorX >= centroX;
        TextureRegion frame;
        Medida medida;

        if (ataqueActual == Ataque.MUERTE && muerteDerecha != null) {
            frame = miraDerecha
                ? muerteDerecha.getKeyFrame(tiempoAtaque, false)
                : muerteIzquierda.getKeyFrame(tiempoAtaque, false);
            medida = medidaMuerte;
        } else if (ataqueActual == Ataque.EMBESTIDA) {
            frame = direccionEmbestida > 0f
                ? embestidaDerecha.getKeyFrame(tiempoAtaque, false)
                : embestidaIzquierda.getKeyFrame(tiempoAtaque, false);
            medida = medidaEmbestida;
        } else if (ataqueActual == Ataque.ONDA_ESCAPE) {
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
        float x = centroX - medida.centroPiesX * escalaVisual;
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
