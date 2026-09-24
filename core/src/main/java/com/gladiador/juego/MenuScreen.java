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

// Menu principal del juego.
public class MenuScreen implements Screen {

    // Referencia al juego y objetos necesarios para dibujar.
    private final MainClass game;
    private OrthographicCamera camera;
    private Viewport viewport;

    // Fondo animado del menu.
    private Texture[] fondos;
    private Animation<TextureRegion> animacionFondo;
    private float tiempoFondo;

    // Botones en este orden: jugar, controles y salir.
    private final Texture[] botones = new Texture[3];
    private final float[] escalas = {1f, 1f, 1f};
    private final boolean[] hover = {false, false, false};
    private final boolean[] hoverAnterior = {false, false, false};

    // Sonidos del menu.
    private Music musica;
    private Sound sonidoHover;
    private Sound sonidoClick;
    private boolean mousePresionado;

    // Medidas y posiciones del menu.
    private static final int ANCHO = 1366;
    private static final int ALTO = 768;
    private static final int CANTIDAD_FONDOS = 16;
    private static final float ANCHO_BOTON = 420f;
    private static final float SEPARACION_BOTONES = -100f;
    private static final float ESCALA_HOVER = 1.08f;

    // Recibir el juego principal.
    public MenuScreen(MainClass game) {
        this.game = game;
    }

    @Override
    public void show() {
        // Preparar la camara y cargar todos los recursos.
        configurarCamara();
        cargarFondo();
        cargarBotones();
        cargarSonidos();
    }

    // Crear la camara 2D del menu.
    private void configurarCamara() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(ANCHO, ALTO, camera);
        viewport.apply(true);
        camera.position.set(ANCHO / 2f, ALTO / 2f, 0f);
        camera.update();
    }

    // Cargar los 16 frames del fondo.
    private void cargarFondo() {
        fondos = new Texture[CANTIDAD_FONDOS];
        TextureRegion[] regiones = new TextureRegion[CANTIDAD_FONDOS];

        for (int i = 0; i < CANTIDAD_FONDOS; i++) {
            fondos[i] = new Texture(Gdx.files.internal("menu/frame_" + (i + 1) + ".png"));
            regiones[i] = new TextureRegion(fondos[i]);
        }

        animacionFondo = new Animation<TextureRegion>(0.15f, regiones);
    }

    // Cargar las imagenes de jugar, controles y salir.
    private void cargarBotones() {
        botones[0] = cargarTextura("menu/jugar.png");
        botones[1] = cargarTextura("menu/controles.png");
        botones[2] = cargarTextura("menu/salir.png");
    }

    // Cargar musica y efectos. Si falta un efecto, el menu sigue funcionando.
    private void cargarSonidos() {
        musica = cargarMusica("menu/musica_menu.mp3");
        // No se carga un sonido de hover porque el proyecto no tiene ese archivo.
        sonidoHover = null;
        sonidoClick = cargarSonido("menu/musca_boton_inicio.mp3");

        if (musica != null) {
            musica.setLooping(true);
            musica.setVolume(0.7f);
            musica.play();
        }
    }

    // Cargar una imagen opcional.
    private Texture cargarTextura(String ruta) {
        try {
            return new Texture(Gdx.files.internal(ruta));
        } catch (Exception error) {
            Gdx.app.error("MenuScreen", "No se pudo cargar " + ruta, error);
            return null;
        }
    }

    // Cargar un sonido opcional.
    private Sound cargarSonido(String ruta) {
        try {
            return Gdx.audio.newSound(Gdx.files.internal(ruta));
        } catch (Exception error) {
            Gdx.app.error("MenuScreen", "No se pudo cargar " + ruta, error);
            return null;
        }
    }

    // Cargar la musica del menu.
    private Music cargarMusica(String ruta) {
        try {
            return Gdx.audio.newMusic(Gdx.files.internal(ruta));
        } catch (Exception error) {
            Gdx.app.error("MenuScreen", "No se pudo cargar " + ruta, error);
            return null;
        }
    }

    @Override
    public void render(float delta) {
        // Avanzar la animacion y limpiar toda la ventana.
        tiempoFondo += delta;
        limpiarPantalla();

        // Dibujar dentro del viewport logico.
        camera.update();
        game.batch.setProjectionMatrix(camera.combined);
        TextureRegion fondo = animacionFondo.getKeyFrame(tiempoFondo, true);

        game.batch.begin();
        game.batch.draw(fondo, 0f, 0f, ANCHO, ALTO);

        // Calcular la posicion de cada boton y detectar el mouse.
        float[] alturas = obtenerAlturas();
        float[] posicionesY = obtenerPosicionesY(alturas);
        actualizarHover(posicionesY, alturas);

        // Dibujar los botones con su animacion de hover.
        for (int i = 0; i < botones.length; i++) {
            escalas[i] = actualizarEscala(escalas[i], hover[i]);
            dibujarBoton(i, posicionesY[i], alturas[i]);
        }
        game.batch.end();

        // Revisar si el jugador presiono un boton.
        procesarClick();
    }

    // Limpiar toda la pantalla y aplicar el viewport del juego.
    private void limpiarPantalla() {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glViewport(
            viewport.getScreenX(), viewport.getScreenY(),
            viewport.getScreenWidth(), viewport.getScreenHeight()
        );
    }

    // Obtener la altura proporcional de cada boton.
    private float[] obtenerAlturas() {
        float[] alturas = new float[botones.length];
        for (int i = 0; i < botones.length; i++) {
            alturas[i] = botones[i] == null
                ? 60f
                : botones[i].getHeight() * ANCHO_BOTON / botones[i].getWidth();
        }
        return alturas;
    }

    // Calcular la posicion vertical de cada boton.
    private float[] obtenerPosicionesY(float[] alturas) {
        float[] y = new float[3];
        float primerBoton = ALTO * 0.68f - 12f - alturas[0];
        y[0] = primerBoton;
        y[1] = y[0] - alturas[1] - SEPARACION_BOTONES;
        y[2] = y[1] - alturas[2] - SEPARACION_BOTONES;
        return y;
    }

    // Detectar el boton que esta debajo del cursor.
    private void actualizarHover(float[] posicionesY, float[] alturas) {
        Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f);
        viewport.unproject(mouse);

        for (int i = 0; i < botones.length; i++) {
            hover[i] = dentroDelBoton(mouse.x, mouse.y, posicionesY[i], alturas[i]);
            if (i > 0) hover[i] = hover[i] && !hover[i - 1];

            if (hover[i] && !hoverAnterior[i] && sonidoHover != null) {
                sonidoHover.play(0.9f);
            }
            hoverAnterior[i] = hover[i];
        }
    }

    // Comprobar si una posicion esta dentro de un boton.
    private boolean dentroDelBoton(float x, float y, float posicionY, float altura) {
        float posicionX = (ANCHO - ANCHO_BOTON) / 2f;
        return x >= posicionX && x <= posicionX + ANCHO_BOTON
            && y >= posicionY && y <= posicionY + altura;
    }

    // Suavizar el aumento de tamaño al pasar el cursor.
    private float actualizarEscala(float actual, boolean estaEnHover) {
        float objetivo = estaEnHover ? ESCALA_HOVER : 1f;
        return actual + (objetivo - actual) * 0.2f;
    }

    // Dibujar un boton centrado.
    private void dibujarBoton(int indice, float y, float altura) {
        if (botones[indice] == null) return;

        float escala = escalas[indice];
        float ancho = ANCHO_BOTON * escala;
        float alto = altura * escala;
        float x = (ANCHO - ancho) / 2f;
        float dibujarY = y - (alto - altura) / 2f;
        game.batch.draw(botones[indice], x, dibujarY, ancho, alto);
    }

    // Activar la opcion presionada.
    private void procesarClick() {
        boolean presionado = Gdx.input.isTouched()
            || Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        boolean clickNuevo = presionado && !mousePresionado;

        if (clickNuevo) {
            if (hover[0]) abrirPantalla(new JuegoScreen(game));
            else if (hover[1]) abrirPantalla(new ControlesScreen(game));
            else if (hover[2]) {
                if (sonidoClick != null) sonidoClick.play(0.8f);
                Gdx.app.exit();
                return;
            }
        }
        mousePresionado = presionado;
    }

    // Reproducir click, detener musica y cambiar de pantalla.
    private void abrirPantalla(Screen pantalla) {
        if (sonidoClick != null) sonidoClick.play(0.8f);
        if (musica != null) musica.stop();
        game.setScreen(pantalla);
    }

    @Override
    public void resize(int width, int height) {
        // Adaptar el viewport al nuevo tamaño.
        if (viewport != null) viewport.update(width, height, true);
    }

    @Override
    public void pause() {
        // No hay una accion especial al pausar.
    }

    @Override
    public void resume() {
        // No hay una accion especial al reanudar.
    }

    @Override
    public void hide() {
        // Detener la musica al salir del menu.
        if (musica != null) musica.stop();
    }

    @Override
    public void dispose() {
        // Liberar fondos, botones y sonidos.
        if (fondos != null) for (Texture textura : fondos) textura.dispose();
        for (Texture boton : botones) if (boton != null) boton.dispose();
        if (musica != null) musica.dispose();
        if (sonidoHover != null) sonidoHover.dispose();
        if (sonidoClick != null) sonidoClick.dispose();
    }
}
