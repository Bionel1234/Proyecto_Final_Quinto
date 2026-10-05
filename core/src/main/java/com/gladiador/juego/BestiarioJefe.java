package com.gladiador.juego;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;

public class BestiarioJefe {

    private static final int CANTIDAD_FRAMES_LATIGO = 25;
    private static final int VIDA_MAXIMA = 16;
    private static final float DURACION_FRAME = 0.07f;
    private static final float DURACION_ANIMACION_LATIGO = CANTIDAD_FRAMES_LATIGO * DURACION_FRAME;
    private static final float DURACION_INVOCACION = 1.4f;
    private static final float MOMENTO_IMPACTO_LATIGO = 0.78f;
    private static final float ESPERA_ENTRE_ATAQUES_MINIMA = 2.2f;
    private static final float ESPERA_ENTRE_ATAQUES_MAXIMA = 3.4f;
    private static final float ESCALA_JEFE = 0.98f;
    private static final float TAMANO_LIENZO = 512f;
    private static final float POSICION_CENTRO_X = 1050f;
    private static final float ALTURA_PISO = 72f;
    private static final float ALTURA_MANO_LATIGO = ALTURA_PISO + 68f;
    private static final float DISTANCIA_MANO_CENTRO = 50f;

    public enum Ataque {
        NINGUNO,
        LATIGO,
        LEON
    }

    private final TextureRegion[] framesLatigo;
    private final Animation<TextureRegion> animacionLatigo;
    private final ArrayList<Texture> texturas = new ArrayList<>();

    private int vida = VIDA_MAXIMA;
    private float tiempoAtaque;
    private float tiempoHastaAtaque = 1.8f;
    private float tiempoDanio;
    private long ultimoAtaqueRecibido = -1L;
    private boolean impactoLatigoResuelto;
    private boolean leonInvocado;
    private boolean proximoAtaqueEsLatigo = true;
    private Ataque ataqueActual = Ataque.NINGUNO;

    public BestiarioJefe() {
        framesLatigo = new TextureRegion[CANTIDAD_FRAMES_LATIGO];
        for (int indice = 0; indice < CANTIDAD_FRAMES_LATIGO; indice++) {
            String ruta = String.format("bestiario/latigo/%02d.png", indice + 1);
            Texture textura = new Texture(Gdx.files.internal(ruta));
            textura.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            texturas.add(textura);
            TextureRegion frame = new TextureRegion(textura);
            frame.flip(true, false);
            framesLatigo[indice] = frame;
        }
        animacionLatigo = new Animation<>(DURACION_FRAME, framesLatigo);
    }

    public void actualizar(float delta) {
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

    private boolean impactoLatigoPendiente;
    private boolean invocacionPendiente;

    private void iniciarSiguienteAtaque() {
        ataqueActual = proximoAtaqueEsLatigo ? Ataque.LATIGO : Ataque.LEON;
        proximoAtaqueEsLatigo = !proximoAtaqueEsLatigo;
        tiempoAtaque = 0f;
        impactoLatigoResuelto = false;
        leonInvocado = false;
    }

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
        ataqueActual = Ataque.NINGUNO;
        tiempoAtaque = 0f;
        tiempoHastaAtaque = 1f;
    }

    public Rectangle getHitbox() {
        float mitadAnchoVisible = TAMANO_LIENZO * ESCALA_JEFE * 0.15f;
        return new Rectangle(
            POSICION_CENTRO_X - mitadAnchoVisible,
            ALTURA_PISO + 5f,
            mitadAnchoVisible * 2f,
            TAMANO_LIENZO * ESCALA_JEFE * 0.33f
        );
    }

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

    public void dibujarLatigo(ShapeRenderer shapeRenderer, float jugadorX, float jugadorY) {
        if (ataqueActual != Ataque.LATIGO || tiempoAtaque < 0.32f || tiempoAtaque > 1.28f) {
            return;
        }
        float progreso = MathUtils.clamp((tiempoAtaque - 0.32f) / 0.44f, 0f, 1f);
        float origenX = getOrigenLatigoX(jugadorX);
        float objetivoX = MathUtils.lerp(origenX, jugadorX, progreso);
        float objetivoY = MathUtils.lerp(ALTURA_MANO_LATIGO, jugadorY + 65f, progreso);
        float distancia = objetivoX - origenX;
        float curva = Math.min(85f, Math.abs(distancia) * 0.12f);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.12f, 0.07f, 0.045f, 1f);
        dibujarCurvaLatigo(shapeRenderer, origenX, objetivoX, objetivoY, curva, 5f);
        shapeRenderer.setColor(0.58f, 0.34f, 0.20f, 1f);
        dibujarCurvaLatigo(shapeRenderer, origenX, objetivoX, objetivoY, curva, 2f);
        shapeRenderer.end();
    }

    private void dibujarCurvaLatigo(
        ShapeRenderer shapeRenderer,
        float origenX,
        float objetivoX,
        float objetivoY,
        float curva,
        float ancho
    ) {
        if (curva == 0f) {
            shapeRenderer.rectLine(origenX, ALTURA_MANO_LATIGO, objetivoX, objetivoY, ancho);
            return;
        }

        float control1X = MathUtils.lerp(origenX, objetivoX, 0.30f);
        float control1Y = ALTURA_MANO_LATIGO + curva;
        float control2X = MathUtils.lerp(origenX, objetivoX, 0.78f);
        float control2Y = objetivoY + curva * 0.35f;
        float anteriorX = origenX;
        float anteriorY = ALTURA_MANO_LATIGO;
        for (int segmento = 1; segmento <= 12; segmento++) {
            float t = segmento / 12f;
            float inverso = 1f - t;
            float x = inverso * inverso * inverso * origenX
                + 3f * inverso * inverso * t * control1X
                + 3f * inverso * t * t * control2X
                + t * t * t * objetivoX;
            float y = inverso * inverso * inverso * ALTURA_MANO_LATIGO
                + 3f * inverso * inverso * t * control1Y
                + 3f * inverso * t * t * control2Y
                + t * t * t * objetivoY;
            shapeRenderer.rectLine(anteriorX, anteriorY, x, y, ancho);
            anteriorX = x;
            anteriorY = y;
        }
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

    public void dibujar(SpriteBatch batch) {
        TextureRegion frame = framesLatigo[0];
        if (ataqueActual == Ataque.LATIGO) {
            frame = animacionLatigo.getKeyFrame(tiempoAtaque, false);
        }
        float ancho = TAMANO_LIENZO * ESCALA_JEFE;
        float alto = TAMANO_LIENZO * ESCALA_JEFE;
        float x = POSICION_CENTRO_X - ancho / 2f;
        float y = ALTURA_PISO - (TAMANO_LIENZO - 476f) * ESCALA_JEFE;
        batch.setColor(1f, 1f, 1f, 1f);
        if (tiempoDanio > 0f && ((int) (tiempoDanio * 40f) % 2) == 0) {
            batch.setColor(1f, 0.5f, 0.5f, 1f);
        }
        batch.draw(frame, x, y, ancho, alto);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    public void dispose() {
        for (Texture textura : texturas) {
            textura.dispose();
        }
    }
}
