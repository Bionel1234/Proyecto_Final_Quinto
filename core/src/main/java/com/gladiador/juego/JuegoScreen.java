package com.gladiador.juego;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

// Pantalla principal: muestra la mazmorra, a Jorge y sus vidas.
public class JuegoScreen implements Screen {

    // Resolucion logica usada por todo el juego.
    public static final int ANCHO = 1366;
    public static final int ALTO = 768;

    // Datos de la animacion de la mazmorra.
    private static final int FRAMES_MAZMORRA = 32;
    private static final float DURACION_MAZMORRA = 0.12f;

    // Objetos principales de la pantalla.
    private final MainClass game;
    private OrthographicCamera camera;
    private Viewport viewport;
    private Texture[] texturasMazmorra;
    private Animation<TextureRegion> mazmorra;
    private float tiempoMazmorra;
    private Jugador jugador;
    private Vida vida;
    private BitmapFont fuente;
    private boolean puedeEntrarAlMapa;

    // Recibir la referencia al juego.
    public JuegoScreen(MainClass game) {
        this.game = game;
    }

    @Override
    public void show() {
        // Preparar la vista y cargar todos los objetos.
        configurarCamara();
        cargarMazmorra();
        jugador = new Jugador(180f, calcularSuelo());
        vida = new Vida();
        fuente = new BitmapFont();
        puedeEntrarAlMapa = false;
    }

    // Crear una camara 2D que mantiene la proporcion de la pantalla.
    private void configurarCamara() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(ANCHO, ALTO, camera);
        viewport.apply(true);
        camera.position.set(ANCHO / 2f, ALTO / 2f, 0f);
        camera.update();
    }

    // Cargar frame_00.png hasta frame_31.png.
    private void cargarMazmorra() {
        texturasMazmorra = new Texture[FRAMES_MAZMORRA];
        TextureRegion[] regiones = new TextureRegion[FRAMES_MAZMORRA];

        for (int i = 0; i < FRAMES_MAZMORRA; i++) {
            String nombre = String.format("mazmorra/frame_%02d.png", i);
            texturasMazmorra[i] = new Texture(Gdx.files.internal(nombre));
            regiones[i] = new TextureRegion(texturasMazmorra[i]);
        }

        mazmorra = new Animation<TextureRegion>(DURACION_MAZMORRA, regiones);
    }

    @Override
    public void render(float delta) {
        // Actualizar la animacion del fondo.
        tiempoMazmorra += delta;

        // Limpiar los bordes negros y preparar la camara.
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glViewport(
            viewport.getScreenX(), viewport.getScreenY(),
            viewport.getScreenWidth(), viewport.getScreenHeight()
        );
        camera.update();
        game.batch.setProjectionMatrix(camera.combined);

        // Calcular el tamano proporcional del fondo.
        TextureRegion fondo = mazmorra.getKeyFrame(tiempoMazmorra, true);
        float ancho = ANCHO;
        float alto = ancho * fondo.getRegionHeight() / fondo.getRegionWidth();
        float x = (ANCHO - ancho) / 2f;
        float y = (ALTO - alto) / 2f;

        // Actualizar y dibujar todos los elementos.
        jugador.actualizar(delta, y);
        puedeEntrarAlMapa = jugador.getX() >= ANCHO - 260f;

        game.batch.begin();
        game.batch.draw(fondo, x, y, ancho, alto);
        jugador.dibujar(game.batch);
        vida.dibujar(game.batch);

        if (puedeEntrarAlMapa) {
            fuente.draw(game.batch, "Pulsa E para entrar al Mapa 1", 420f, 650f);
            if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                game.setScreen(new MapaScreen(game));
                game.batch.end();
                return;
            }
        }

        game.batch.end();

        // ESC vuelve al menu.
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MenuScreen(game));
        }
    }

    // Calcular el suelo usando el primer frame del fondo.
    private float calcularSuelo() {
        TextureRegion fondo = mazmorra.getKeyFrame(0f);
        float alto = ANCHO * fondo.getRegionHeight() / fondo.getRegionWidth();
        return (ALTO - alto) / 2f;
    }

    @Override
    public void resize(int width, int height) {
        // Recalcular el viewport cuando cambia la ventana.
        if (viewport != null) viewport.update(width, height, true);
    }

    @Override
    public void pause() {
        // No hay una accion especial al pausar por ahora.
    }

    @Override
    public void resume() {
        // No hay una accion especial al reanudar por ahora.
    }

    @Override
    public void hide() {
        // No hay recursos de audio que detener en esta pantalla.
    }

    @Override
    public void dispose() {
        // Liberar las texturas del fondo.
        if (texturasMazmorra != null) {
            for (Texture textura : texturasMazmorra) textura.dispose();
        }
        if (jugador != null) jugador.dispose();
        if (vida != null) vida.dispose();
        if (fuente != null) fuente.dispose();
    }
}
