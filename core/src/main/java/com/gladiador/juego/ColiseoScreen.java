package com.gladiador.juego;

import java.util.ArrayList;
import java.util.Iterator;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class ColiseoScreen implements Screen {

    private static final float PISO = 72f;
    private static final float POSICION_INICIAL_JUGADOR = 180f;
    private static final float LIMITE_JUGADOR = JuegoScreen.ANCHO - 180f;

    private final MainClass game;
    private final ArrayList<LeonInvocado> leones = new ArrayList<>();

    private OrthographicCamera camera;
    private Viewport viewport;
    private Texture fondo;
    private Texture pantallaGameOver;
    private Texture botonVolver;
    private Texture botonSalir;
    private Jugador jugador;
    private Vida vida;
    private BestiarioJefe jefe;
    private ShapeRenderer shapeRenderer;
    private float tiempoHitstop;
    private float tiempoTemblor;
    private boolean finDelJuego;
    private boolean victoria;
    private boolean recursosLiberados;

    public ColiseoScreen(MainClass game) {
        this.game = game;
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(JuegoScreen.ANCHO, JuegoScreen.ALTO, camera);
        viewport.apply(true);
        camera.position.set(JuegoScreen.ANCHO / 2f, JuegoScreen.ALTO / 2f, 0f);
        camera.update();

        fondo = new Texture(Gdx.files.internal("coliseo_fondo.png"));
        fondo.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pantallaGameOver = new Texture(Gdx.files.internal("gameover/pantalla.png"));
        botonVolver = new Texture(Gdx.files.internal("gameover/volver.png"));
        botonSalir = new Texture(Gdx.files.internal("gameover/salir.png"));
        jugador = new Jugador(POSICION_INICIAL_JUGADOR, PISO);
        jugador.setEscalaVisual(1.2f);
        vida = new Vida();
        jefe = new BestiarioJefe();
        shapeRenderer = new ShapeRenderer();
    }

    @Override
    public void render(float deltaReal) {
        float delta = deltaReal;
        if (!finDelJuego && tiempoHitstop > 0f) {
            tiempoHitstop = Math.max(0f, tiempoHitstop - deltaReal);
            delta = 0f;
        }
        if (tiempoTemblor > 0f) {
            tiempoTemblor = Math.max(0f, tiempoTemblor - deltaReal);
        }

        if (!finDelJuego) {
            actualizarCombate(delta);
        }
        if (victoria) {
            game.setScreen(new JuegoScreen(game));
            return;
        }

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glViewport(
            viewport.getScreenX(), viewport.getScreenY(),
            viewport.getScreenWidth(), viewport.getScreenHeight()
        );

        float temblor = tiempoTemblor > 0f ? MathUtils.random(-4f, 4f) : 0f;
        camera.position.set(JuegoScreen.ANCHO / 2f + temblor, JuegoScreen.ALTO / 2f, 0f);
        camera.update();
        game.batch.setProjectionMatrix(camera.combined);

        game.batch.begin();
        float altoFondo = JuegoScreen.ALTO;
        float anchoFondo = altoFondo * fondo.getWidth() / fondo.getHeight();
        game.batch.draw(
            fondo,
            (JuegoScreen.ANCHO - anchoFondo) / 2f,
            0f,
            anchoFondo,
            altoFondo
        );
        jefe.dibujar(game.batch);
        for (LeonInvocado leon : leones) {
            leon.dibujar(game.batch);
        }
        jugador.dibujar(game.batch);
        vida.dibujar(game.batch);
        game.batch.end();

        shapeRenderer.setProjectionMatrix(camera.combined);
        jefe.dibujarLatigo(shapeRenderer, jugador.getCentroX(), jugador.getY());
        if (finDelJuego && !victoria) {
            dibujarGameOver();
            return;
        }
        dibujarBarrasDeVida();
        revisarSalida();
    }

    private void actualizarCombate(float delta) {
        jugador.actualizar(delta, PISO, LIMITE_JUGADOR);
        jefe.actualizar(delta);

        if (jefe.consumirInvocacionLeon()) {
            float anchoLeon = 250f;
            float xLeon = jefe.getCentroX() - 165f - anchoLeon;
            leones.add(new LeonInvocado(xLeon, PISO));
            tiempoTemblor = 0.12f;
        }

        Rectangle hitboxJugador = jugador.getHitbox();
        if (jefe.estaVivo()
            && jugador.estaAtaqueActivo()
            && jugador.getAreaDeAtaque().overlaps(jefe.getHitbox())) {
            if (jefe.recibirGolpe(jugador.getNumeroAtaque())) {
                tiempoHitstop = 0.045f;
                tiempoTemblor = 0.11f;
            }
        }

        if (jefe.consumirImpactoLatigo()
            && jefe.getAreaLatigo(jugador.getCentroX(), jugador.getY()).overlaps(hitboxJugador)) {
            resolverAtaqueDelJefe(jefe.getCentroX());
        }

        Iterator<LeonInvocado> iterador = leones.iterator();
        while (iterador.hasNext()) {
            LeonInvocado leon = iterador.next();
            leon.actualizar(delta);
            if (leon.estaActivo() && leon.getHitbox().overlaps(hitboxJugador)) {
                leon.desactivar();
                resolverAtaqueDelJefe(jefe.getCentroX());
                tiempoTemblor = 0.15f;
            }
            if (!leon.estaActivo()) {
                leon.dispose();
                iterador.remove();
            }
        }

        if (!jefe.estaVivo()) {
            victoria = true;
            finDelJuego = true;
            game.registrarBestiarioDerrotado();
        } else if (!vida.estaVivo()) {
            victoria = false;
            finDelJuego = true;
        }
    }

    private void resolverAtaqueDelJefe(float origenAtaqueX) {
        if (jugador.esInvulnerable()) {
            return;
        }
        float empuje = jugador.getCentroX() >= origenAtaqueX ? 1f : -1f;
        if (jugador.bloqueaGolpeDesde(origenAtaqueX)) {
            if (jugador.estaEnVentanaParry()) {
                jefe.aturdir();
                jugador.bloquearGolpe(empuje);
                tiempoHitstop = 0.10f;
                tiempoTemblor = 0.14f;
            } else {
                jugador.bloquearGolpe(empuje);
                tiempoHitstop = 0.035f;
            }
            return;
        }

        vida.recibirGolpe();
        jugador.recibirImpacto(empuje);
        tiempoHitstop = 0.07f;
        tiempoTemblor = 0.18f;
    }

    private void dibujarBarrasDeVida() {
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        float ancho = 420f;
        float alto = 24f;
        float x = (JuegoScreen.ANCHO - ancho) / 2f;
        float y = JuegoScreen.ALTO - 78f;
        shapeRenderer.setColor(0.12f, 0.04f, 0.04f, 0.94f);
        shapeRenderer.rect(x - 4f, y - 4f, ancho + 8f, alto + 8f);
        shapeRenderer.setColor(0.35f, 0.10f, 0.08f, 1f);
        shapeRenderer.rect(x, y, ancho, alto);
        shapeRenderer.setColor(0.82f, 0.10f, 0.08f, 1f);
        shapeRenderer.rect(x, y, ancho * jefe.getVida() / jefe.getVidaMaxima(), alto);
        shapeRenderer.end();
    }

    private void dibujarGameOver() {
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.7f);
        shapeRenderer.rect(0f, 0f, JuegoScreen.ANCHO, JuegoScreen.ALTO);
        shapeRenderer.end();

        float escalaX = JuegoScreen.ANCHO / 1536f;
        float escalaY = JuegoScreen.ALTO / 1024f;
        float volverX = 417f * escalaX;
        float volverAncho = (1120f - 417f) * escalaX;
        float volverY = JuegoScreen.ALTO - 830f * escalaY;
        float volverAlto = (830f - 678f) * escalaY;
        float salirX = 473f * escalaX;
        float salirAncho = (1065f - 473f) * escalaX;
        float salirY = JuegoScreen.ALTO - 989f * escalaY;
        float salirAlto = (989f - 836f) * escalaY;

        game.batch.begin();
        game.batch.draw(pantallaGameOver, 0f, 0f, JuegoScreen.ANCHO, JuegoScreen.ALTO);
        game.batch.draw(botonVolver, volverX, volverY, volverAncho, volverAlto);
        game.batch.draw(botonSalir, salirX, salirY, salirAncho, salirAlto);
        game.batch.end();

        Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f);
        viewport.unproject(mouse);
        boolean volverSeleccionado = mouse.x >= volverX && mouse.x <= volverX + volverAncho
            && mouse.y >= volverY && mouse.y <= volverY + volverAlto;
        boolean salirSeleccionado = mouse.x >= salirX && mouse.x <= salirX + salirAncho
            && mouse.y >= salirY && mouse.y <= salirY + salirAlto;

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            if (volverSeleccionado) {
                game.setScreen(new JuegoScreen(game));
            } else if (salirSeleccionado) {
                Gdx.app.exit();
            }
        }
    }

    private void revisarSalida() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new JuegoScreen(game));
        }
    }

    @Override
    public void resize(int width, int height) {
        if (viewport != null) {
            viewport.update(width, height, true);
        }
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        if (recursosLiberados) {
            return;
        }
        recursosLiberados = true;
        if (fondo != null) {
            fondo.dispose();
        }
        if (pantallaGameOver != null) {
            pantallaGameOver.dispose();
        }
        if (botonVolver != null) {
            botonVolver.dispose();
        }
        if (botonSalir != null) {
            botonSalir.dispose();
        }
        if (jugador != null) {
            jugador.dispose();
        }
        if (vida != null) {
            vida.dispose();
        }
        if (jefe != null) {
            jefe.dispose();
        }
        for (LeonInvocado leon : leones) {
            leon.dispose();
        }
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
    }
}
