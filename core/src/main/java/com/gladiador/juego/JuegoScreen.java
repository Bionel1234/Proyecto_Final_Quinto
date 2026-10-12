package com.gladiador.juego;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
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
    private Texture panelSelector;
    private Animation<TextureRegion> mazmorra;
    private float tiempoMazmorra;
    private Jugador jugador;
    private Comerciante comerciante;
    private Vida vida;
    private Monedas monedas;
    private BitmapFont fuente;
    private Music musicaLobby;
    private boolean puedeEntrarAlMapa;
    private boolean hablandoConComerciante;
    private int indiceMapaSeleccionado = 0;

    // Recibir la referencia al juego.
    public JuegoScreen(MainClass game) {
        this.game = game;
    }

    @Override
    public void show() {
        // Preparar la vista y cargar todos los objetos.
        configurarCamara();
        cargarMazmorra();
        panelSelector = new Texture(Gdx.files.internal("menu/controles_fondo.png"));
        jugador = new Jugador((ANCHO - 180f) / 2f, calcularSuelo());
        comerciante = new Comerciante(250f, calcularSuelo() + 1f);
        vida = new Vida();
        monedas = new Monedas();
        fuente = new BitmapFont();
        musicaLobby = Gdx.audio.newMusic(Gdx.files.internal("sonidos/musica_lobby.mp3"));
        musicaLobby.setLooping(true);
        musicaLobby.setVolume(0.08f);
        musicaLobby.play();
        puedeEntrarAlMapa = false;
        hablandoConComerciante = false;
        indiceMapaSeleccionado = 0;
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
        boolean cercaDelComerciante = comerciante.estaCerca(jugador.getX());
        if (!cercaDelComerciante) {
            hablandoConComerciante = false;
        }

        game.batch.begin();
        game.batch.draw(fondo, x, y, ancho, alto);
        comerciante.dibujar(game.batch);
        jugador.dibujar(game.batch);
        vida.dibujar(game.batch);
        monedas.dibujar(game.batch);

        if (cercaDelComerciante && !hablandoConComerciante) {
            fuente.draw(game.batch, "Pulsa F para hablar con el mercader", 420f, 650f);
        }
        if (hablandoConComerciante) {
            fuente.draw(game.batch, "Mercader: Bienvenido, gladiador.", 430f, 650f);
            fuente.draw(game.batch, "Pronto tendré mercancía para ti.", 430f, 620f);
            fuente.draw(game.batch, "Pulsa F para cerrar el diálogo", 430f, 590f);
        }

        if (puedeEntrarAlMapa) {
            dibujarSelectorMapa();
            gestionarSeleccionMapa();
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                entrarEnMapaSeleccionado();
                game.batch.end();
                return;
            }
        }

        game.batch.end();

        if (cercaDelComerciante && Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            hablandoConComerciante = !hablandoConComerciante;
        }

        // ESC vuelve al menu.
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MenuScreen(game));
        }
    }

    private void dibujarSelectorMapa() {
        float panelX = 300f;
        float panelY = 320f;
        float panelAncho = 760f;
        float panelAlto = 240f;

        game.batch.setColor(0.07f, 0.09f, 0.14f, 0.86f);
        game.batch.draw(panelSelector, panelX, panelY, panelAncho, panelAlto);
        game.batch.setColor(1f, 1f, 1f, 1f);

        fuente.draw(game.batch, "Elige tu próximo combate", panelX + 40f, panelY + 180f);
        int opciones = obtenerCantidadOpciones();
        for (int i = 0; i < opciones; i++) {
            String texto = obtenerTextoOpcion(i);
            float y = panelY + 130f - i * 56f;
            if (i == indiceMapaSeleccionado) {
                fuente.draw(game.batch, "> " + texto, panelX + 50f, y);
            } else {
                fuente.draw(game.batch, texto, panelX + 70f, y);
            }
        }
        fuente.draw(game.batch, "Flechas o W/S para cambiar • Enter para entrar", panelX + 40f, panelY + 28f);
    }

    private void gestionarSeleccionMapa() {
        if (!puedeEntrarAlMapa) {
            return;
        }
        int opciones = obtenerCantidadOpciones();
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            indiceMapaSeleccionado = Math.max(0, indiceMapaSeleccionado - 1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            indiceMapaSeleccionado = Math.min(opciones - 1, indiceMapaSeleccionado + 1);
        }
    }

    private int obtenerCantidadOpciones() {
        return game.isZona1Completada() ? 3 : 2;
    }

    private String obtenerTextoOpcion(int indice) {
        if (indice == 0) {
            return "Zona 1: Nivel 1";
        }
        if (indice == 1) {
            return "Zona 2: Nivel 1";
        }
        return "Coliseo — El Bestiario";
    }

    private void entrarEnMapaSeleccionado() {
        if (!puedeEntrarAlMapa) {
            return;
        }
        int opcion = indiceMapaSeleccionado;
        if (opcion == 0) {
            game.setScreen(new MapaScreen(game, "Zona 1", 1));
            return;
        }
        if (opcion == 1) {
            game.setScreen(new MapaScreen(game, "Zona 2", 1, "Mapa 1/scene_animated.png"));
            return;
        }
        game.setScreen(new ColiseoScreen(game));
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
        if (musicaLobby != null) {
            musicaLobby.stop();
        }
    }

    @Override
    public void dispose() {
        // Liberar las texturas del fondo.
        if (texturasMazmorra != null) {
            for (Texture textura : texturasMazmorra) textura.dispose();
        }
        if (panelSelector != null) panelSelector.dispose();
        if (jugador != null) jugador.dispose();
        if (comerciante != null) comerciante.dispose();
        if (vida != null) vida.dispose();
        if (monedas != null) monedas.dispose();
        if (fuente != null) fuente.dispose();
        if (musicaLobby != null) musicaLobby.dispose();
    }
}
