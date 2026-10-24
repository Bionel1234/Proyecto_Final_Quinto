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

// Pantalla de controles: solo muestra una imagen grande con la explicacion.
// Se vuelve al menu con ESC o haciendo click en el boton dibujado en la imagen.
public class ControlesScreen implements Screen {

    private final MainClass game;

    private Texture fondo;
    private Music musica;
    private OrthographicCamera camera;
    private Viewport viewport;
    private boolean mousePresionado;

    // Caja del boton "volver", medida sobre la propia imagen del fondo
    // (x, y del borde inferior izquierdo, ancho y alto).
    private static final float BOTON_X = 1060f;
    private static final float BOTON_Y = 20f;
    private static final float BOTON_ANCHO = 290f;
    private static final float BOTON_ALTO = 95f;

    // Recibe el juego principal para poder volver al menu.
    public ControlesScreen(MainClass game) {
        this.game = game;
    }

    // Inicializa camara, imagen y musica al entrar en la pantalla.
    @Override
    public void show() {
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

    // Dibuja la pantalla y detecta si el jugador pide regresar.
    @Override
    public void render(float delta) {
        // Limpiar la ventana y dibujar la imagen ocupando todo el ancho.
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

        // Volver con ESC o con un click sobre la caja del boton.
        boolean presionado = Gdx.input.isTouched() || Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        boolean clickNuevo = presionado && !mousePresionado;
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || (clickNuevo && sobreBoton())) {
            game.setScreen(new MenuScreen(game));
            return;
        }
        mousePresionado = presionado;
    }

    // Convertir la posicion del mouse al plano del juego y ver si cae dentro del boton.
    private boolean sobreBoton() {
        Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f);
        viewport.unproject(mouse);
        return mouse.x >= BOTON_X && mouse.x <= BOTON_X + BOTON_ANCHO
            && mouse.y >= BOTON_Y && mouse.y <= BOTON_Y + BOTON_ALTO;
    }

    // Ajusta el area de dibujo cuando cambia el tamano de la ventana.
    @Override
    public void resize(int width, int height) {
        if (viewport != null) {
            viewport.update(width, height, true);
        }
    }

    // LibGDX llama estos metodos al pausar o reanudar la aplicacion.
    // Esta pantalla no necesita modificar su estado en esos momentos.
    @Override
    public void pause() {
    }

    // La pantalla de controles no tiene temporizadores que deban reiniciarse.
    @Override
    public void resume() {
    }

    // Detiene la musica al cambiar a otra pantalla.
    @Override
    public void hide() {
        // Detener la musica al salir de esta pantalla.
        if (musica != null) {
            musica.stop();
        }
    }

    // Libera imagen y musica creadas por esta pantalla.
    @Override
    public void dispose() {
        if (fondo != null) {
            fondo.dispose();
        }
        if (musica != null) {
            musica.dispose();
        }
    }
}
