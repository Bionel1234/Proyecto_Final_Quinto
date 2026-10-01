package com.gladiador.juego;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class MapaScreen implements Screen {

    public static final int ANCHO = 1366;
    public static final int ALTO = 768;

    private final MainClass game;
    private OrthographicCamera camera;
    private Viewport viewport;
    private Texture fondoMapa;
    private ArrayList<Enemigo> enemigos;
    private Jugador jugador;
    private Vida vida;
    private BitmapFont fuente;
    private BitmapFont fuenteGrande;
    private ShapeRenderer shapeRenderer;
    private boolean finDelJuego;

    public MapaScreen(MainClass game) {
        this.game = game;
    }

    @Override
    public void show() {
        configurarCamara();
        fondoMapa = new Texture(Gdx.files.internal("Mapa 1/scene_animated.png"));
        jugador = new Jugador(250f, 150f);
        vida = new Vida();
        enemigos = new ArrayList<>();
        enemigos.add(new Enemigo(420f, 150f, 80f));
        enemigos.add(new Enemigo(740f, 150f, 90f));
        enemigos.add(new Enemigo(1280f, 150f, 95f));
        enemigos.add(new Enemigo(1680f, 150f, 100f));
        fuente = new BitmapFont();
        fuenteGrande = new BitmapFont();
        shapeRenderer = new ShapeRenderer();
        finDelJuego = false;
    }

    private void configurarCamara() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(ANCHO, ALTO, camera);
        viewport.apply(true);
        camera.position.set(ANCHO / 2f, ALTO / 2f, 0f);
        camera.update();
    }

    @Override
    public void render(float delta) {
        if (finDelJuego) {
            dibujarGameOver();
            return;
        }

        float mapaAncho = 2089f;
        jugador.actualizar(delta, 150f, mapaAncho - 220f);

        float scroll = Math.min(Math.max(0f, jugador.getX() - 180f), mapaAncho - ANCHO);

        for (Enemigo enemigo : enemigos) {
            if (!enemigo.estaVivo()) {
                continue;
            }
            enemigo.actualizar(delta, jugador.getX(), jugador.getY(), 0f, mapaAncho);
            if (enemigo.estaEnRangoDeAtaque(jugador.getX(), jugador.getY()) && enemigo.puedeAtacar()) {
                int vidasActuales = vida.obtenerVidas();
                if (vidasActuales > 0) {
                    vida.establecerVidas(vidasActuales - 1);
                }
                enemigo.reiniciarAtaque();
                if (vida.obtenerVidas() <= 0) {
                    finDelJuego = true;
                }
            }
        }

        for (Enemigo enemigo : enemigos) {
            if (!enemigo.estaVivo()) {
                continue;
            }
            Rectangle hitboxJugador = jugador.getHitbox();
            Rectangle hitboxAtaque = jugador.getAreaDeAtaque();
            Rectangle hitboxEnemigo = enemigo.getHitbox();
            if (jugador.estaAtacando()) {
                boolean golpea = hitboxAtaque.overlaps(hitboxEnemigo);
                if (golpea && enemigo.puedeRecibirGolpe()) {
                    enemigo.recibirGolpe();
                }
            }
            if (hitboxJugador.overlaps(hitboxEnemigo) && enemigo.puedeAtacar()) {
                int vidasActuales = vida.obtenerVidas();
                if (vidasActuales > 0) {
                    vida.establecerVidas(vidasActuales - 1);
                }
                enemigo.reiniciarAtaque();
                if (vida.obtenerVidas() <= 0) {
                    finDelJuego = true;
                }
            }
        }

        enemigos.removeIf(enemigo -> !enemigo.estaVivo());

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glViewport(
            viewport.getScreenX(), viewport.getScreenY(),
            viewport.getScreenWidth(), viewport.getScreenHeight()
        );
        camera.update();
        game.batch.setProjectionMatrix(camera.combined);

        game.batch.begin();
        game.batch.draw(fondoMapa, -scroll, 0f, mapaAncho, ALTO);
        jugador.dibujar(game.batch, scroll);
        for (Enemigo enemigo : enemigos) {
            enemigo.dibujar(game.batch, scroll);
        }
        vida.dibujar(game.batch);
        for (Enemigo enemigo : enemigos) {
            if (enemigo.estaVivo()) {
                fuente.draw(game.batch, "HP: " + enemigo.getVida() + "/2", enemigo.getX() - scroll - 20f, enemigo.getY() + 180f);
            }
        }
        fuente.draw(game.batch, "Zona 1: Las Catacumbas", 80f, 710f);
        game.batch.end();

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MenuScreen(game));
        }
    }

    private void dibujarGameOver() {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glViewport(
            viewport.getScreenX(), viewport.getScreenY(),
            viewport.getScreenWidth(), viewport.getScreenHeight()
        );

        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();
        game.batch.draw(fondoMapa, 0f, 0f, ANCHO, ALTO);
        game.batch.end();

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.7f);
        shapeRenderer.rect(150f, 140f, ANCHO - 300f, ALTO - 260f);
        shapeRenderer.end();

        game.batch.begin();
        fuenteGrande.setColor(1f, 1f, 1f, 1f);
        fuenteGrande.getData().setScale(2.4f);
        fuenteGrande.draw(game.batch, "GAME OVER", ANCHO / 2f - 160f, ALTO / 2f + 110f);
        fuenteGrande.getData().setScale(1.3f);
        fuenteGrande.draw(game.batch, "Te derrotaron en las catacumbas", ANCHO / 2f - 260f, ALTO / 2f + 40f);

        float botonX = ANCHO / 2f - 150f;
        float botonY = ALTO / 2f - 90f;
        float ancho = 300f;
        float alto = 60f;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.18f, 0.42f, 0.75f, 1f);
        shapeRenderer.rect(botonX, botonY, ancho, alto);
        shapeRenderer.end();

        Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f);
        viewport.unproject(mouse);
        boolean hover = mouse.x >= botonX && mouse.x <= botonX + ancho && mouse.y >= botonY && mouse.y <= botonY + alto;
        if (hover) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0.26f, 0.57f, 0.9f, 1f);
            shapeRenderer.rect(botonX, botonY, ancho, alto);
            shapeRenderer.end();
        }
        if (hover && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            game.setScreen(new MenuScreen(game));
        }

        fuenteGrande.setColor(1f, 1f, 1f, 1f);
        fuenteGrande.getData().setScale(1.4f);
        fuenteGrande.draw(game.batch, "Volver a jugar", botonX + 25f, botonY + 42f);
        game.batch.end();
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
    }

    @Override
    public void dispose() {
        if (fondoMapa != null) {
            fondoMapa.dispose();
        }
        for (Enemigo enemigo : enemigos) {
            if (enemigo != null) {
                enemigo.dispose();
            }
        }
        if (jugador != null) {
            jugador.dispose();
        }
        if (vida != null) {
            vida.dispose();
        }
        if (fuente != null) {
            fuente.dispose();
        }
        if (fuenteGrande != null) {
            fuenteGrande.dispose();
        }
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
    }
}
