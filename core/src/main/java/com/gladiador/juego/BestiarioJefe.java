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

// El jefe del Coliseo. En la Zona 2 combina ataques de cadena, onda y salto.
public class BestiarioJefe {

    private static final int CANTIDAD_FRAMES_LATIGO = 12;   // frames de la animacion de ataque
    private static final int CANTIDAD_FRAMES_ESTAITCO = 13; // frames de la animacion quieto
    private static final int VIDA_MAXIMA = 16;
    private static final int VIDA_MAXIMA_ZONA_2 = 20;
    private static final float DURACION_FRAME_ESTAITCO = 0.10f;
    private static final float DURACION_ANIMACION_LATIGO = 1.75f;
    private static final float DURACION_FRAME_LATIGO = DURACION_ANIMACION_LATIGO / CANTIDAD_FRAMES_LATIGO;
    private static final int CANTIDAD_FRAMES_ATAQUE_ZONA_2 = 15;
    private static final float DURACION_FRAME_ATAQUE_ZONA_2 = 0.08f;
    private static final float DURACION_ATAQUE_ZONA_2 =
        CANTIDAD_FRAMES_ATAQUE_ZONA_2 * DURACION_FRAME_ATAQUE_ZONA_2;
    private static final int CANTIDAD_FRAMES_ONDA_ZONA_2 = 14;
    private static final float DURACION_FRAME_ONDA_ZONA_2 = 0.08f;
    private static final float DURACION_ONDA_ESCAPE =
        CANTIDAD_FRAMES_ONDA_ZONA_2 * DURACION_FRAME_ONDA_ZONA_2;
    private static final int CANTIDAD_FRAMES_SALTO_ONDA = 13;
    private static final float DURACION_FRAME_SALTO_ONDA = 0.075f;
    private static final float DURACION_SALTO_ONDA =
        CANTIDAD_FRAMES_SALTO_ONDA * DURACION_FRAME_SALTO_ONDA;
    private static final float MOMENTO_IMPACTO_SALTO_ONDA = 0.42f;
    private static final int CANTIDAD_FRAMES_MUERTE_ZONA_2 = 12;
    private static final float DURACION_INVOCACION = 1.4f;
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
        SALTO_ONDA,
        EMBESTIDA,
        MUERTE
    }

    private static final Ataque[] CICLO_ATAQUES_ZONA_2 = {
        Ataque.CADENA, Ataque.ONDA_ESCAPE, Ataque.SALTO_ONDA
    };

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
    private final Animation<TextureRegion> saltoOndaDerecha;
    private final Animation<TextureRegion> saltoOndaIzquierda;
    private final Animation<TextureRegion> embestidaDerecha;
    private final Animation<TextureRegion> embestidaIzquierda;
    private final Animation<TextureRegion> muerteDerecha;
    private final Animation<TextureRegion> muerteIzquierda;
    private final Medida medidaEstatico;
    private final Medida medidaAtaque;
    private final Medida medidaOnda;
    private final Medida medidaSaltoOnda;
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
    private Ataque ataqueActual = Ataque.NINGUNO;

    private boolean impactoLatigoPendiente;
    private boolean invocacionPendiente;
    private boolean ondaEscapePendiente;
    private boolean impactoEmbestidaResuelto;
    private boolean impactoSaltoOndaResuelto;
    private boolean saltoOndaPendiente;
    private boolean miraAtaqueDerecha = true;
    private int proximoAtaqueZona2;

    public BestiarioJefe() {
        this(false);
    }

    public BestiarioJefe(boolean jefeZona2) {
        this.jefeZona2 = jefeZona2;
        if (jefeZona2) {
            vida = VIDA_MAXIMA_ZONA_2;
            estaticoDerecha = cargarAnimacion("Jefe zona 2/Ataque normal jefe zona 2 derecha", 15, 0.08f);
            estaticoIzquierda = cargarAnimacion("Jefe zona 2/Ataque normal jefe zona 2 izquierda", 15, 0.08f);
            ataqueDerecha = cargarAnimacion("Jefe zona 2/Ataque normal jefe zona 2 derecha",
                CANTIDAD_FRAMES_ATAQUE_ZONA_2, DURACION_FRAME_ATAQUE_ZONA_2);
            ataqueIzquierda = cargarAnimacion("Jefe zona 2/Ataque normal jefe zona 2 izquierda",
                CANTIDAD_FRAMES_ATAQUE_ZONA_2, DURACION_FRAME_ATAQUE_ZONA_2);
            ondaDerecha = cargarAnimacion("Jefe zona 2/Ataque onda expansiva jefe zona 2 derecha",
                CANTIDAD_FRAMES_ONDA_ZONA_2, DURACION_FRAME_ONDA_ZONA_2);
            ondaIzquierda = cargarAnimacion("Jefe zona 2/Ataque onda expansiva jefe zona 2 izquierda",
                CANTIDAD_FRAMES_ONDA_ZONA_2, DURACION_FRAME_ONDA_ZONA_2);
            saltoOndaDerecha = cargarAnimacion(
                "Jefe zona 2/Jefe zona 2 ataque salto onda expansiva derecha",
                CANTIDAD_FRAMES_SALTO_ONDA, DURACION_FRAME_SALTO_ONDA);
            saltoOndaIzquierda = cargarAnimacion(
                "Jefe zona 2/Jefe zona 2 ataque salto onda expansiva izquierda",
                CANTIDAD_FRAMES_SALTO_ONDA, DURACION_FRAME_SALTO_ONDA);
            embestidaDerecha = null;
            embestidaIzquierda = null;
            muerteDerecha = cargarAnimacion("Jefe zona 2/Jefe zona 2 muerte derecha",
                CANTIDAD_FRAMES_MUERTE_ZONA_2, DURACION_FRAME_MUERTE);
            muerteIzquierda = cargarAnimacion("Jefe zona 2/Jefe zona 2 muerte izquierda",
                CANTIDAD_FRAMES_MUERTE_ZONA_2, DURACION_FRAME_MUERTE);
            medidaEstatico = medirConjunto("Jefe zona 2/Ataque normal jefe zona 2 derecha", 15, 512f);
            medidaAtaque = medirConjunto("Jefe zona 2/Ataque normal jefe zona 2 derecha", 15, 512f);
            medidaOnda = medirConjunto("Jefe zona 2/Ataque onda expansiva jefe zona 2 derecha",
                CANTIDAD_FRAMES_ONDA_ZONA_2, 512f);
            medidaSaltoOnda = medirConjunto(
                "Jefe zona 2/Jefe zona 2 ataque salto onda expansiva derecha",
                CANTIDAD_FRAMES_SALTO_ONDA, 512f);
            medidaEmbestida = null;
            medidaMuerte = medirConjunto("Jefe zona 2/Jefe zona 2 muerte derecha",
                CANTIDAD_FRAMES_MUERTE_ZONA_2, 512f);
        } else {
            vida = VIDA_MAXIMA;
            estaticoDerecha = cargarAnimacion("jefe1/estatico_derecha", CANTIDAD_FRAMES_ESTAITCO, DURACION_FRAME_ESTAITCO);
            estaticoIzquierda = cargarAnimacion("jefe1/estatico_izquierda", CANTIDAD_FRAMES_ESTAITCO, DURACION_FRAME_ESTAITCO);
            ataqueDerecha = cargarAnimacion("jefe1/ataque_derecha", CANTIDAD_FRAMES_LATIGO, DURACION_FRAME_LATIGO);
            ataqueIzquierda = cargarAnimacion("jefe1/ataque_izquierda", CANTIDAD_FRAMES_LATIGO, DURACION_FRAME_LATIGO);
            ondaDerecha = null;
            ondaIzquierda = null;
            saltoOndaDerecha = null;
            saltoOndaIzquierda = null;
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
            medidaSaltoOnda = null;
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
            miraAtaqueDerecha = jugadorX >= centroX;
            if (!jefeZona2) {
                tiempoHastaEmbestida -= delta;
                if (tiempoHastaEmbestida <= 0f) {
                    iniciarEmbestida(jugadorX);
                    return;
                }
            }
            tiempoHastaAtaque -= delta;
            if (tiempoHastaAtaque <= 0f) {
                iniciarSiguienteAtaque(jugadorX);
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
                if (tiempoAtaque >= DURACION_ATAQUE_ZONA_2) {
                    finalizarAtaque();
                }
            } else if (ataqueActual == Ataque.ONDA_ESCAPE) {
                if (!ondaEscapePendiente && tiempoAtaque >= 0.48f) {
                    ondaEscapePendiente = true;
                }
                if (tiempoAtaque >= DURACION_ONDA_ESCAPE) {
                    finalizarAtaque();
                }
            } else if (ataqueActual == Ataque.SALTO_ONDA) {
                if (!impactoSaltoOndaResuelto && tiempoAtaque >= MOMENTO_IMPACTO_SALTO_ONDA) {
                    impactoSaltoOndaResuelto = true;
                    saltoOndaPendiente = true;
                }
                if (tiempoAtaque >= DURACION_SALTO_ONDA) {
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

    private void iniciarSiguienteAtaque(float jugadorX) {
        if (jefeZona2) {
            ataqueActual = CICLO_ATAQUES_ZONA_2[proximoAtaqueZona2];
            proximoAtaqueZona2 = (proximoAtaqueZona2 + 1) % CICLO_ATAQUES_ZONA_2.length;
        } else {
            ataqueActual = proximoAtaqueEsLatigo ? Ataque.LATIGO : Ataque.LEON;
            proximoAtaqueEsLatigo = !proximoAtaqueEsLatigo;
        }
        miraAtaqueDerecha = jugadorX >= centroX;
        tiempoAtaque = 0f;
        impactoLatigoResuelto = false;
        impactoSaltoOndaResuelto = false;
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
        saltoOndaPendiente = false;
        impactoSaltoOndaResuelto = false;
        tiempoHastaAtaque = MathUtils.random(ESPERA_ENTRE_ATAQUES_MINIMA, ESPERA_ENTRE_ATAQUES_MAXIMA);
    }

    public boolean consumirImpactoLatigo() {
        if (!impactoLatigoPendiente) {
            return false;
        }
        impactoLatigoPendiente = false;
        return true;
    }

    public boolean consumirImpactoSaltoOnda() {
        if (!saltoOndaPendiente) {
            return false;
        }
        saltoOndaPendiente = false;
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
            saltoOndaPendiente = false;
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
        saltoOndaPendiente = false;
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

    public Rectangle getAreaSaltoOnda() {
        return new Rectangle(centroX - 500f, ALTURA_PISO + 5f, 1000f, 60f);
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
        boolean miraDerecha = ataqueActual == Ataque.NINGUNO
            ? jugadorX >= centroX : miraAtaqueDerecha;
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
        } else if (ataqueActual == Ataque.SALTO_ONDA) {
            frame = miraDerecha
                ? saltoOndaDerecha.getKeyFrame(tiempoAtaque, false)
                : saltoOndaIzquierda.getKeyFrame(tiempoAtaque, false);
            medida = medidaSaltoOnda;
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
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -1f;
        float maxY = -1f;

        for (int indice = 0; indice < cantidad; indice++) {
            Pixmap pixmap = new Pixmap(Gdx.files.internal(String.format("%s/%02d.png", ruta, indice + 1)));
            float escalaLienzo = canvas / pixmap.getWidth();
            for (int pixelY = 0; pixelY < pixmap.getHeight(); pixelY++) {
                for (int pixelX = 0; pixelX < pixmap.getWidth(); pixelX++) {
                    if ((pixmap.getPixel(pixelX, pixelY) & 0xff) != 0) {
                        float pixelMinX = (float) Math.floor(pixelX * escalaLienzo);
                        float pixelMinY = (float) Math.floor(pixelY * escalaLienzo);
                        float pixelMaxX = (float) Math.ceil((pixelX + 1f) * escalaLienzo) - 1f;
                        float pixelMaxY = (float) Math.ceil((pixelY + 1f) * escalaLienzo) - 1f;
                        minX = Math.min(minX, pixelMinX);
                        minY = Math.min(minY, pixelMinY);
                        maxX = Math.max(maxX, pixelMaxX);
                        maxY = Math.max(maxY, pixelMaxY);
                    }
                }
            }
            pixmap.dispose();
        }

        float banda = Math.max(1f, (maxY - minY + 1f) * FRACCION_BANDA_PIES);
        float piesMinX = Float.MAX_VALUE;
        float piesMaxX = -1f;
        for (int indice = 0; indice < cantidad; indice++) {
            Pixmap pixmap = new Pixmap(Gdx.files.internal(String.format("%s/%02d.png", ruta, indice + 1)));
            float escalaLienzo = canvas / pixmap.getWidth();
            for (int pixelY = 0; pixelY < pixmap.getHeight(); pixelY++) {
                for (int pixelX = 0; pixelX < pixmap.getWidth(); pixelX++) {
                    if ((pixmap.getPixel(pixelX, pixelY) & 0xff) != 0) {
                        float pixelMaxY = (float) Math.ceil((pixelY + 1f) * escalaLienzo) - 1f;
                        if (pixelMaxY >= maxY - banda) {
                            float pixelMinX = (float) Math.floor(pixelX * escalaLienzo);
                            float pixelMaxX = (float) Math.ceil((pixelX + 1f) * escalaLienzo) - 1f;
                            piesMinX = Math.min(piesMinX, pixelMinX);
                            piesMaxX = Math.max(piesMaxX, pixelMaxX);
                        }
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
