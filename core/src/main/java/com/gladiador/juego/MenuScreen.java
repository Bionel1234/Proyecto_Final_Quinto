package com.gladiador.juego;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

// Menu principal: fondo animado y tres botones (Jugar, Controles, Salir).
// Los botones son imagenes; se eligen con el mouse y "crecen" al pasar por encima.
public class MenuScreen implements Screen {

    private final MainClass game;
    private OrthographicCamera camera;
    private Viewport viewport;

    // Fondo animado (16 cuadros) y su timer.
    private Texture[] fondos;
    private Animation<TextureRegion> animacionFondo;
    private float tiempoFondo;

    // Los tres botones, en orden: jugar, controles y salir.
    private final Texture[] botones = new Texture[3];
    private final float[] escalas = {1f, 1f, 1f};      // tamano actual de cada boton
    private final boolean[] hover = {false, false, false};      // el mouse esta encima?
    private final boolean[] hoverAnterior = {false, false, false};

    // Musica de fondo y sonido del click.
    private Music musica;
    private Sound sonidoClick;
    private boolean mousePresionado;

    // Medidas del menu.
    private static final int ANCHO = 1366;
    private static final int ALTO = 768;
    private static final int CANTIDAD_FONDOS = 16;
    private static final float ANCHO_BOTON = 420f;
    private static final float SEPARACION_BOTONES = -100f; // negativa: se solapan un poco
    private static final float ESCALA_HOVER = 1.08f;       // cuanto crece al pasar el mouse

    // Guarda una referencia al juego para poder cambiar de pantalla o salir.
    public MenuScreen(MainClass game) {
        this.game = game;
    }

    // Cargar camara, fondos, botones y sonidos.
    // Carga los recursos y prepara el sistema de coordenadas del menu.
    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(ANCHO, ALTO, camera);
        viewport.apply(true);
        camera.position.set(ANCHO / 2f, ALTO / 2f, 0f);
        camera.update();

        // Los 16 cuadros del fondo animado.
        fondos = new Texture[CANTIDAD_FONDOS];
        TextureRegion[] regiones = new TextureRegion[CANTIDAD_FONDOS];
        for (int i = 0; i < CANTIDAD_FONDOS; i++) {
            fondos[i] = new Texture(Gdx.files.internal("menu/frame_" + (i + 1) + ".png"));
            regiones[i] = new TextureRegion(fondos[i]);
        }
        animacionFondo = new Animation<TextureRegion>(0.15f, regiones);

        // Las imagenes de los botones.
        botones[0] = new Texture(Gdx.files.internal("menu/jugar.png"));
        botones[1] = new Texture(Gdx.files.internal("menu/controles.png"));
        botones[2] = new Texture(Gdx.files.internal("menu/salir.png"));

        // Musica y sonido del click.
        musica = Gdx.audio.newMusic(Gdx.files.internal("menu/musica_menu.mp3"));
        musica.setLooping(true);
        musica.setVolume(0.7f);
        musica.play();
        sonidoClick = Gdx.audio.newSound(Gdx.files.internal("menu/musca_boton_inicio.mp3"));
    }

    // Actualiza la animacion, dibuja el menu y procesa la seleccion del usuario.
    @Override
    public void render(float delta) {
        tiempoFondo += delta;

        // Limpiar toda la ventana y encerrar el dibujo dentro del viewport.
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glViewport(
            viewport.getScreenX(), viewport.getScreenY(),
            viewport.getScreenWidth(), viewport.getScreenHeight()
        );

        camera.update();
        game.batch.setProjectionMatrix(camera.combined);

        // Dibujar el fondo animado (true = se repite siempre).
        TextureRegion fondo = animacionFondo.getKeyFrame(tiempoFondo, true);
        game.batch.begin();
        game.batch.draw(fondo, 0f, 0f, ANCHO, ALTO);

        // Calcular alto y posicion de cada boton segun su imagen.
        float[] alturas = new float[3];
        for (int i = 0; i < 3; i++) {
            alturas[i] = botones[i].getHeight() * ANCHO_BOTON / botones[i].getWidth();
        }
        // El primero se fija y los otros cuelgan debajo (con la separacion).
        float[] posicionesY = new float[3];
        posicionesY[0] = ALTO * 0.68f - 12f - alturas[0];
        posicionesY[1] = posicionesY[0] - alturas[1] - SEPARACION_BOTONES;
        posicionesY[2] = posicionesY[1] - alturas[2] - SEPARACION_BOTONES;

        // ?El mouse esta sobre algun boton? (si dos se solapan, gana el de arriba)
        Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f);
        viewport.unproject(mouse);
        for (int i = 0; i < 3; i++) {
            float x = (ANCHO - ANCHO_BOTON) / 2f;
            hover[i] = mouse.x >= x && mouse.x <= x + ANCHO_BOTON
                && mouse.y >= posicionesY[i] && mouse.y <= posicionesY[i] + alturas[i];
            if (i > 0) {
                hover[i] = hover[i] && !hover[i - 1];
            }
        }

        // Dibujar los botones con la animacion de "crecer" cuando hay hover.
        for (int i = 0; i < 3; i++) {
            // Mover la escala un 20% del camino hacia su objetivo, cada frame: queda suave.
            float objetivo = hover[i] ? ESCALA_HOVER : 1f;
            escalas[i] = escalas[i] + (objetivo - escalas[i]) * 0.2f;

            float ancho = ANCHO_BOTON * escalas[i];
            float alto = alturas[i] * escalas[i];
            float x = (ANCHO - ancho) / 2f;
            float y = posicionesY[i] - (alto - alturas[i]) / 2f; // crecer hacia los costados
            game.batch.draw(botones[i], x, y, ancho, alto);
        }
        game.batch.end();

        // Revisar si hicieron click en algun boton.
        boolean presionado = Gdx.input.isTouched() || Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        boolean clickNuevo = presionado && !mousePresionado; // "recien" presionado

        if (clickNuevo) {
            if (hover[0]) {
                abrirPantalla(new JuegoScreen(game));
            } else if (hover[1]) {
                abrirPantalla(new ControlesScreen(game));
            } else if (hover[2]) {
                sonidoClick.play(0.8f);
                Gdx.app.exit();
            }
        }
        mousePresionado = presionado;
    }

    // Reproducir click, cortar la musica y cambiar de pantalla.
    private void abrirPantalla(Screen pantalla) {
        sonidoClick.play(0.8f);
        if (musica != null) {
            musica.stop();
        }
        game.setScreen(pantalla);
    }

    // LibGDX invoca este metodo al cambiar el tamano de la ventana.
    @Override
    public void resize(int width, int height) {
        // Adaptar el viewport al nuevo tamano de la ventana.
        if (viewport != null) {
            viewport.update(width, height, true);
        }
    }

    // El menu no necesita acciones adicionales al pausar o reanudar.
    @Override
    public void pause() {
    }

    // Al volver a primer plano, el siguiente render continua dibujando el menu.
    @Override
    public void resume() {
    }

    // La musica del menu se detiene cuando se muestra otra pantalla.
    @Override
    public void hide() {
        // Detener la musica al salir del menu.
        if (musica != null) {
            musica.stop();
        }
    }

    // Libera imagenes, musica y sonido creados por el menu.
    @Override
    public void dispose() {
        // Liberar fondos, botones y sonidos.
        if (fondos != null) {
            for (Texture textura : fondos) {
                textura.dispose();
            }
        }
        for (Texture boton : botones) {
            if (boton != null) {
                boton.dispose();
            }
        }
        if (musica != null) {
            musica.dispose();
        }
        if (sonidoClick != null) {
            sonidoClick.dispose();
        }
    }
}
