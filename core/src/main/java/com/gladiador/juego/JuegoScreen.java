package com.gladiador.juego;

import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

// Pantalla central del juego ("el lobby"): la mazmorra donde esta Jorge.
// Desde aca se habla con el mercader y se entra a las zonas con el menu del fondo.
public class JuegoScreen implements Screen {

    // Resolucion logica usada por todo el juego.
    public static final int ANCHO = 1366;
    public static final int ALTO = 768;

    // Animacion del fondo de la mazmorra (32 cuadros que se repiten).
    private static final int FRAMES_MAZMORRA = 32;
    private static final float DURACION_MAZMORRA = 0.12f;

    private final MainClass game;
    private OrthographicCamera camera;
    private Viewport viewport;
    private Texture[] texturasMazmorra;
    private Texture panelSelector;      // imagen del cartel donde se elige la zona
    private Texture cartelPrision;      // cartel colgante "La Prision" (titulo del lobby)
    private Animation<TextureRegion> mazmorra;
    private float tiempoMazmorra;
    private Jugador jugador;
    private Comerciante comerciante;
    private Vida vida;
    private Monedas monedas;
    private BitmapFont fuente;          // para los textos simples
    private Music musicaLobby;

    // Estado de esta pantalla.
    private boolean puedeEntrarAlMapa;          // Jorge esta parado frente a la puerta
    private boolean hablandoConComerciante;     // dialogo del mercader abierto
    private String mensajeComerciante = "";

    public JuegoScreen(MainClass game) {
        this.game = game;
    }

    // show() se llama al abrir la pantalla: aca se carga TODO.
    @Override
    public void show() {
        configurarCamara();
        cargarMazmorra();
        panelSelector = new Texture(Gdx.files.internal("menu/controles_fondo.png"));
        cartelPrision = new Texture(Gdx.files.internal("ui/prision.png"));
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
        mensajeComerciante = "";
    }

    // Camara 2D que mantiene la proporcion de la pantalla (FitViewport).
    private void configurarCamara() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(ANCHO, ALTO, camera);
        viewport.apply(true);
        camera.position.set(ANCHO / 2f, ALTO / 2f, 0f);
        camera.update();
    }

    // Cargar frame_00.png hasta frame_31.png del fondo animado.
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

    // render() se llama muchas veces por segundo (una por cada imagen mostrada).
    @Override
    public void render(float delta) {
        // delta indica cuantos segundos pasaron desde el frame anterior.
        tiempoMazmorra += delta;

        // 1) Limpiar la ventana y preparar la camara.
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glViewport(
            viewport.getScreenX(), viewport.getScreenY(),
            viewport.getScreenWidth(), viewport.getScreenHeight()
        );
        camera.update();
        game.batch.setProjectionMatrix(camera.combined);

        // 2) Calcular el tamano proporcional del fondo (que se use de "suelo").
        TextureRegion fondo = mazmorra.getKeyFrame(tiempoMazmorra, true);
        float ancho = ANCHO;
        float alto = ancho * fondo.getRegionHeight() / fondo.getRegionWidth();
        float x = (ANCHO - ancho) / 2f;
        float y = (ALTO - alto) / 2f;

        // 3) Mover a Jorge y ver donde esta ( puerta / mercader ).
        jugador.actualizar(delta, y, ANCHO - 180f, List.of());
        puedeEntrarAlMapa = jugador.getX() >= ANCHO - 260f;
        boolean cercaDelComerciante = comerciante.estaCerca(jugador.getX());
        if (!cercaDelComerciante) {
            hablandoConComerciante = false; // alejarse cierra el dialogo
        }

        // 4) Dibujar todo.
        game.batch.begin();
        game.batch.draw(fondo, x, y, ancho, alto);
        comerciante.dibujar(game.batch);
        jugador.dibujar(game.batch, 0f);
        vida.dibujar(game.batch);
        monedas.dibujar(game.batch);

        if (cercaDelComerciante && !hablandoConComerciante) {
            fuente.draw(game.batch, "Pulsa F para hablar con el mercader", 420f, 650f);
        }
        if (hablandoConComerciante) {
            fuente.draw(game.batch, "Mercader: Puedo curarte por 50 monedas.", 430f, 650f);
            fuente.draw(game.batch, "Pulsa R para curarte por completo.", 430f, 620f);
            fuente.draw(game.batch, mensajeComerciante, 430f, 590f);
            fuente.draw(game.batch, "Pulsa F para cerrar el diálogo", 430f, 560f);
        }

        // 5) Si esta frente a la puerta, mostrar el selector de zona.
        if (puedeEntrarAlMapa) {
            dibujarSelectorMapa();
            if (game.getEtapaDesbloqueada() <= MainClass.ETAPA_ZONA2
                    && Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                entrarEnMapaSeleccionado();
                game.batch.end();
                return;
            }
        }

        game.batch.end();

        // 6) Teclas que no dibujan: F (dialogo) y ESC (volver al menu).
        if (cercaDelComerciante && Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            hablandoConComerciante = !hablandoConComerciante;
            mensajeComerciante = "";
        }
        if (cercaDelComerciante && hablandoConComerciante
                && Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            if (vida.estaCompleta()) {
                mensajeComerciante = "Ya tienes la vida completa.";
            } else if (Monedas.gastar(50)) {
                vida.curarCompleto();
                mensajeComerciante = "Te has curado por completo.";
            } else {
                mensajeComerciante = "No tienes suficientes monedas.";
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MenuScreen(game));
        }
    }

    // Cartel oscuro con el siguiente combate disponible.
    private void dibujarSelectorMapa() {
        float panelX = 300f;
        float panelY = 320f;
        float panelAncho = 760f;
        float panelAlto = 240f;

        // Cartel colgante "La Prision" como titulo, arriba del selector.
        float cartelAncho = 560f;
        float cartelAlto = cartelAncho * cartelPrision.getHeight() / cartelPrision.getWidth();
        game.batch.draw(cartelPrision, (ANCHO - cartelAncho) / 2f, ALTO - cartelAlto - 20f,
            cartelAncho, cartelAlto);

        game.batch.setColor(0.07f, 0.09f, 0.14f, 0.86f);
        game.batch.draw(panelSelector, panelX, panelY, panelAncho, panelAlto);
        game.batch.setColor(1f, 1f, 1f, 1f);

        if (game.getEtapaDesbloqueada() > MainClass.ETAPA_ZONA2) {
            fuente.draw(game.batch, "¡Campaña completada!", panelX + 40f, panelY + 150f);
            return;
        }

        fuente.draw(game.batch, "Siguiente combate", panelX + 40f, panelY + 180f);
        fuente.draw(game.batch, "> " + obtenerTextoOpcion(game.getEtapaDesbloqueada()),
            panelX + 50f, panelY + 110f);
        fuente.draw(game.batch, "Pulsa Enter para entrar", panelX + 40f, panelY + 28f);
    }

    // Texto de cada opcion del selector.
    private String obtenerTextoOpcion(int indice) {
        // Los indices siguen el orden de progresion definido en MainClass.
        if (indice == 0) {
            return "Zona 1: Nivel 1";
        }
        if (indice == 1) {
            return "Zona 1: Nivel 2";
        }
        if (indice == 2) {
            return "Jefe de Zona 1: El Bestiario";
        }
        return "Zona 2: Nivel 1";
    }

    // Cambiar a la pantalla de la zona elegida.
    private void entrarEnMapaSeleccionado() {
        int etapa = game.getEtapaDesbloqueada();
        if (etapa == MainClass.ETAPA_ZONA1_NIVEL1) {
            game.setScreen(new MapaScreen(game, "Zona 1", 1));
        } else if (etapa == MainClass.ETAPA_ZONA1_NIVEL2) {
            game.setScreen(new MapaScreen(game, "Zona 1", 2));
        } else if (etapa == MainClass.ETAPA_JEFE_ZONA1) {
            game.setScreen(new ColiseoScreen(game));
        } else if (etapa == MainClass.ETAPA_ZONA2) {
            game.setScreen(new MapaScreen(game, "Zona 2", 1));
        }
    }

    // El "suelo" del lobby es la parte de arriba de la franja inferior del fondo.
    private float calcularSuelo() {
        TextureRegion fondo = mazmorra.getKeyFrame(0f);
        float alto = ANCHO * fondo.getRegionHeight() / fondo.getRegionWidth();
        return (ALTO - alto) / 2f;
    }

    @Override
    public void resize(int width, int height) {
        // Recalcular el viewport cuando cambia el tamano de la ventana.
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
        if (musicaLobby != null) {
            musicaLobby.stop();
        }
    }

    // dispose() libera TODAS las imagenes y sonidos para no llenar la memoria.
    @Override
    public void dispose() {
        // Evita fugas liberando los recursos graficos, de audio y tipografia.
        if (texturasMazmorra != null) {
            for (Texture textura : texturasMazmorra) {
                textura.dispose();
            }
        }
        if (panelSelector != null) panelSelector.dispose();
        if (cartelPrision != null) cartelPrision.dispose();
        if (jugador != null) jugador.dispose();
        if (comerciante != null) comerciante.dispose();
        if (vida != null) vida.dispose();
        if (monedas != null) monedas.dispose();
        if (fuente != null) fuente.dispose();
        if (musicaLobby != null) musicaLobby.dispose();
    }
}
