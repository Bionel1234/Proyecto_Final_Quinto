package com.gladiador.juego;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

// Pantalla que muestra la imagen con todos los controles.
public class ControlesScreen implements Screen {

    // Referencia para cambiar nuevamente al menu.
    private final MainClass game;

    // Imagen, musica y vista de esta pantalla.
    private Texture fondo;
    private Music musica;
    private OrthographicCamera camera;
    private Viewport viewport;
    private boolean mousePresionado;

    // Coordenadas del boton incluido dentro de la imagen.
    private static final float BOTON_X = 1060f;
    private static final float BOTON_Y = 20f;
    private static final float BOTON_ANCHO = 290f;
    private static final float BOTON_ALTO = 95f;

    // Recibir el juego principal.
    public ControlesScreen(MainClass game) {
        this.game = game;
    }

    @Override
    public void show() {
        // Preparar camara, imagen y musica.
        camera = new OrthographicCamera();
        viewport = new FitViewport(JuegoScreen.ANCHO, JuegoScreen.ALTO, camera);
        viewport.apply(true);
        camera.position.set(JuegoScreen.ANCHO / 2f, JuegoScreen.ALTO / 2f, 0f);
        camera.update();

        fondo = new Texture(Gdx.files.internal("menu/controles_fondo.png"));
        musica = Gdx.audio.newMusic(Gdx.files.internal("menu/musica_menu.mp3"));
        musica.setLooping(true);
        musica.setVolume(0.7f);
        musica.play();
    }

    @Override
    public void render(float delta) {
        // Limpiar la ventana y dibujar la imagen completa.
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
        game.batch.draw(fondo, 0f, 0f, JuegoScreen.ANCHO, JuegoScreen.ALTO);
        game.batch.end();

        // Volver con ESC o haciendo click en el boton de la imagen.
        boolean presionado = Gdx.input.isTouched()
            || Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        boolean clickNuevo = presionado && !mousePresionado;
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
            || (clickNuevo && sobreBoton())) {
            game.setScreen(new MenuScreen(game));
            return;
        }
        mousePresionado = presionado;
    }

    // Comprobar si el mouse esta sobre el boton dibujado.
    private boolean sobreBoton() {
        Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f);
        viewport.unproject(mouse);
        return mouse.x >= BOTON_X && mouse.x <= BOTON_X + BOTON_ANCHO
            && mouse.y >= BOTON_Y && mouse.y <= BOTON_Y + BOTON_ALTO;
    }

    @Override
    public void resize(int width, int height) {
        // Adaptar la vista al nuevo tamaño de la ventana.
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
        // Detener la musica al salir de esta pantalla.
        if (musica != null) musica.stop();
    }

    @Override
    public void dispose() {
        // Liberar imagen y musica.
        if (fondo != null) fondo.dispose();
        if (musica != null) {
            musica.stop();
            musica.dispose();
        }
    }
}
