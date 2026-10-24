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

// El jefe "El Bestiario" del Coliseo.
// Alterna dos ataques: el latigazo y la invocacion de un leon.
public class BestiarioJefe {

    private static final int CANTIDAD_FRAMES_LATIGO = 12;   // frames de la animacion de ataque
    private static final int CANTIDAD_FRAMES_ESTAITCO = 13; // frames de la animacion quieto
    private static final int VIDA_MAXIMA = 16;
    private static final float DURACION_FRAME_ESTAITCO = 0.10f;
    // El ataque dura lo mismo que antes (para no cambiar la jugabilidad), pero ahora
    // se reparte entre 12 imagenes en vez de 25.
    private static final float DURACION_ANIMACION_LATIGO = 1.75f;
    private static final float DURACION_FRAME_LATIGO = DURACION_ANIMACION_LATIGO / CANTIDAD_FRAMES_LATIGO;
    private static final float DURACION_INVOCACION = 1.4f;
    private static final float MOMENTO_IMPACTO_LATIGO = 0.78f;
    private static final float ESPERA_ENTRE_ATAQUES_MINIMA = 2.2f;
    private static final float ESPERA_ENTRE_ATAQUES_MAXIMA = 3.4f;
    private static final float TAMANO_LIENZO = 512f;
    private static final float POSICION_CENTRO_X = 1050f;
    private static final float ALTURA_PISO = 72f;
    private static final float ALTURA_MANO_LATIGO = ALTURA_PISO + 68f;
    private static final float DISTANCIA_MANO_CENTRO = 50f;
    private static final float ALTURA_VISUAL_JEFE = 300f;  // alto del jefe en pantalla
    private static final float FRACCION_BANDA_PIES = 0.18f; // parte de abajo usada para "apoyar" los pies

    public enum Ataque {
        // El jefe esta esperando el proximo ataque.
        NINGUNO,
        // Ataque cercano con el latigo.
        LATIGO,
        // Ataque a distancia que crea un leon.
        LEON
    }

    // Medidas de un conjunto de imagenes: donde apoyar los pies y donde centrar el cuerpo.
    private static class Medida {
        float centroPiesX;  // centro horizontal de los pies (en pixeles de la imagen)
        float filaPiesY;    // fila mas baja con dibujo (en pixeles de la imagen)
        float altoVisible;  // alto del dibujo (en pixeles de la imagen)
        float canvas;       // tamano del lado de la imagen (256 o 512)
    }

    private final Animation<TextureRegion> estaticoDerecha;
    private final Animation<TextureRegion> estaticoIzquierda;
    private final Animation<TextureRegion> ataqueDerecha;
    private final Animation<TextureRegion> ataqueIzquierda;
    private final Medida medidaEstatico;
    private final Medida medidaAtaque;
    private final float escalaVisual;   // pixeles de imagen -> pixeles de pantalla
    private final ArrayList<Texture> texturas = new ArrayList<>();

    private int vida = VIDA_MAXIMA;
    private float tiempoAtaque;
    private float tiempoQuieto;         // reloj propio de la animacion "quieto"
    private float tiempoHastaAtaque = 1.8f;
    private float tiempoDanio;
    private long ultimoAtaqueRecibido = -1L;
    private boolean impactoLatigoResuelto;
    private boolean leonInvocado;
    private boolean proximoAtaqueEsLatigo = true;
    private Ataque ataqueActual = Ataque.NINGUNO;

    private boolean impactoLatigoPendiente;
    private boolean invocacionPendiente;

    public BestiarioJefe() {
        // El jefe mira hacia los dos lados: cargamos las cuatro animaciones.
        estaticoDerecha = cargarAnimacion("jefe1/estatico_derecha", CANTIDAD_FRAMES_ESTAITCO,
            DURACION_FRAME_ESTAITCO);
        estaticoIzquierda = cargarAnimacion("jefe1/estatico_izquierda", CANTIDAD_FRAMES_ESTAITCO,
            DURACION_FRAME_ESTAITCO);
        ataqueDerecha = cargarAnimacion("jefe1/ataque_derecha", CANTIDAD_FRAMES_LATIGO,
            DURACION_FRAME_LATIGO);
        ataqueIzquierda = cargarAnimacion("jefe1/ataque_izquierda", CANTIDAD_FRAMES_LATIGO,
            DURACION_FRAME_LATIGO);

        medidaEstatico = medirConjunto("jefe1/estatico_derecha", CANTIDAD_FRAMES_ESTAITCO, 256f);
        medidaAtaque = medirConjunto("jefe1/ataque_derecha", CANTIDAD_FRAMES_LATIGO, 512f);
        // Usamos la misma escala para las dos animaciones: asi el jefe no cambia
        // de tamano entre estar quieto y atacar.
        escalaVisual = ALTURA_VISUAL_JEFE / medidaEstatico.altoVisible;
    }

    // Actualiza temporizadores, decide cuando atacar y genera eventos de impacto.
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

    // Empieza el siguiente ataque de la secuencia alternada.
    private void iniciarSiguienteAtaque() {
        ataqueActual = proximoAtaqueEsLatigo ? Ataque.LATIGO : Ataque.LEON;
        proximoAtaqueEsLatigo = !proximoAtaqueEsLatigo;
        tiempoAtaque = 0f;
        impactoLatigoResuelto = false;
        leonInvocado = false;
    }

    // Vuelve al estado de espera y sortea cuanto tarda el siguiente ataque.
    private void finalizarAtaque() {
        ataqueActual = Ataque.NINGUNO;
        tiempoHastaAtaque = MathUtils.random(ESPERA_ENTRE_ATAQUES_MINIMA, ESPERA_ENTRE_ATAQUES_MAXIMA);
    }

    public boolean consumirImpactoLatigo() {
        if (!impactoLatigoPendiente) {
            return false;
        }
        impactoLatigoPendiente = false;
        return true;
    }

    public boolean consumirInvocacionLeon() {
        if (!invocacionPendiente) {
            return false;
        }
        invocacionPendiente = false;
        return true;
    }

    // Aplica como maximo un dano por tajo del jugador y activa el destello visual.
    public boolean recibirGolpe(long numeroAtaque) {
        if (!estaVivo() || numeroAtaque == ultimoAtaqueRecibido) {
            return false;
        }
        ultimoAtaqueRecibido = numeroAtaque;
        vida--;
        tiempoDanio = 0.18f;
        return true;
    }

    // Cancela el ataque actual, por ejemplo cuando Jorge consigue un parry.
    public void aturdir() {
        if (!estaVivo()) {
            return;
        }
        impactoLatigoPendiente = false;
        invocacionPendiente = false;
        ataqueActual = Ataque.NINGUNO;
        tiempoAtaque = 0f;
        tiempoHastaAtaque = 1f;
    }

    // Devuelve el rectangulo de colision del cuerpo del jefe.
    public Rectangle getHitbox() {
        float mitadAnchoVisible = TAMANO_LIENZO * 0.98f * 0.15f;
        return new Rectangle(
            POSICION_CENTRO_X - mitadAnchoVisible,
            ALTURA_PISO + 5f,
            mitadAnchoVisible * 2f,
            TAMANO_LIENZO * 0.98f * 0.33f
        );
    }

    // Zona que cubre el latigazo: va desde la mano del jefe hasta donde esta Jorge.
    public Rectangle getAreaLatigo(float jugadorX, float jugadorY) {
        float origenX = getOrigenLatigoX(jugadorX);
        float objetivoY = jugadorY + 65f;
        float xMinimo = Math.min(origenX, jugadorX) - 12f;
        float yMinimo = Math.min(ALTURA_MANO_LATIGO, objetivoY) - 42f;
        float ancho = Math.max(24f, Math.abs(jugadorX - origenX) + 24f);
        float alto = Math.max(60f, Math.abs(objetivoY - ALTURA_MANO_LATIGO) + 84f);
        return new Rectangle(xMinimo, yMinimo, ancho, alto);
    }

    public float getCentroX() {
        return POSICION_CENTRO_X;
    }

    private float getOrigenLatigoX(float jugadorX) {
        return POSICION_CENTRO_X
            + Math.signum(jugadorX - POSICION_CENTRO_X) * DISTANCIA_MANO_CENTRO;
    }

    public int getVida() {
        return vida;
    }

    public int getVidaMaxima() {
        return VIDA_MAXIMA;
    }

    public boolean estaVivo() {
        return vida > 0;
    }

    public Ataque getAtaqueActual() {
        return ataqueActual;
    }

    // Dibujar al jefe mirando hacia el lado donde esta Jorge.
    // Elige la animacion segun direccion y estado y la dibuja alineada al piso.
    public void dibujar(SpriteBatch batch, float jugadorX) {
        boolean miraDerecha = jugadorX >= POSICION_CENTRO_X;
        TextureRegion frame;
        Medida medida;
        if (ataqueActual == Ataque.LATIGO) {
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

        // Apoyamos los pies en el piso y centramos el cuerpo en la misma posicion
        // para las dos animaciones, asi el jefe no "salta" ni se corre al atacar.
        float tamano = medida.canvas * escalaVisual;
        float x = POSICION_CENTRO_X - medida.centroPiesX * escalaVisual;
        float y = ALTURA_PISO - (medida.canvas - 1f - medida.filaPiesY) * escalaVisual;

        batch.setColor(1f, 1f, 1f, 1f);
        if (tiempoDanio > 0f && ((int) (tiempoDanio * 40f) % 2) == 0) {
            batch.setColor(1f, 0.5f, 0.5f, 1f); // parpadeo rojo al recibir un golpe
        }
        batch.draw(frame, x, y, tamano, tamano);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    // Cargar una animacion: los archivos son 01.png, 02.png... dentro de una carpeta.
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

    // Recorrer todas las imagenes del conjunto para saber donde estan los pies y
    // cuanto mide el dibujo. Miramos la franja de abajo (los pies) para centrar el
    // cuerpo: el latigo se estira hacia un costado y no debe mover al jefe.
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

        // Segunda pasada: centro horizontal de los pixeles de la franja de los pies.
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

    // Libera todas las texturas usadas por las animaciones del jefe.
    public void dispose() {
        for (Texture textura : texturas) {
            textura.dispose();
        }
    }
}
