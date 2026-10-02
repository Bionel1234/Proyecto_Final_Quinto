package com.gladiador.juego;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class MapaScreen implements Screen {

    public static final int ANCHO = 1366;
    public static final int ALTO = 768;
    private static final float ANCHO_MAPA = 2089f;
    private static final float ALTURA_SUELO = 150f;
    private static final float DISTANCIA_SEGUNDA_PUERTA = 320f;
    private static final float DISTANCIA_MINIMA_APARICION = 450f;
    private static final float DISTANCIA_MINIMA_ENTRE_ENEMIGOS = 230f;
    private static final int CANTIDAD_OLEADAS = 3;
    private static final int[] ENEMIGOS_POR_OLEADA = {2, 4, 4};

    private final MainClass game;
    private OrthographicCamera camera;
    private Viewport viewport;
    private Texture fondoMapa;
    private Texture pantallaGameOver;
    private ArrayList<Enemigo> enemigos;
    private Jugador jugador;
    private Vida vida;
    private BitmapFont fuente;
    private ShapeRenderer shapeRenderer;
    private List<Rectangle> plataformas;
    private boolean finDelJuego;
    private int oleadaActual;
    private boolean oleadasCompletadas;

    // Efectos de combate: pausa breve al golpear, temblor de camara y aviso de parry.
    private float tiempoHitstop;
    private float tiempoTemblor;
    private float duracionTemblor;
    private float intensidadTemblor;
    private float tiempoTextoParry;

    public MapaScreen(MainClass game) {
        this.game = game;
    }

    @Override
    public void show() {
        configurarCamara();
        fondoMapa = new Texture(Gdx.files.internal("Mapa 1/scene_animated.png"));
        pantallaGameOver = new Texture(Gdx.files.internal("gameover/pantalla.png"));
        jugador = new Jugador(250f, 150f);
        vida = new Vida();
        enemigos = new ArrayList<>();
        fuente = new BitmapFont();
        shapeRenderer = new ShapeRenderer();
        plataformas = Arrays.asList(
            new Rectangle(850f, 260f, 220f, 24f),
            new Rectangle(1160f, 360f, 220f, 24f)
        );
        finDelJuego = false;
        oleadaActual = 1;
        oleadasCompletadas = false;
        tiempoHitstop = 0f;
        tiempoTemblor = 0f;
        tiempoTextoParry = 0f;
        iniciarOleada(jugador.getX());
    }

    private void configurarCamara() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(ANCHO, ALTO, camera);
        viewport.apply(true);
        camera.position.set(ANCHO / 2f, ALTO / 2f, 0f);
        camera.update();
    }

    @Override
    public void render(float deltaReal) {
        if (finDelJuego) {
            dibujarGameOver();
            return;
        }

        // Hitstop: el juego se "congela" una fraccion de segundo al golpear. Se siente mucho mas fuerte.
        float delta = deltaReal;
        if (tiempoHitstop > 0f) {
            tiempoHitstop -= deltaReal;
            delta = 0f;
        }
        if (tiempoTemblor > 0f) {
            tiempoTemblor = Math.max(0f, tiempoTemblor - deltaReal);
        }
        if (tiempoTextoParry > 0f) {
            tiempoTextoParry = Math.max(0f, tiempoTextoParry - deltaReal);
        }

        jugador.actualizar(delta, ALTURA_SUELO, ANCHO_MAPA - 220f, plataformas);

        float scroll = Math.min(Math.max(0f, jugador.getX() - 180f), ANCHO_MAPA - ANCHO);

        // Los enemigos muertos tambien se actualizan (para desvanecerse).
        for (Enemigo enemigo : enemigos) {
            enemigo.actualizar(delta, jugador.getX(), jugador.getY(), 0f, ANCHO_MAPA);
        }
        separarEnemigos();

        Rectangle hitboxJugador = jugador.getHitbox();
        for (Enemigo enemigo : enemigos) {
            if (!enemigo.estaVivo()) {
                continue;
            }

            // 1) Jorge golpea: solo cuenta durante los frames del tajo.
            if (jugador.estaAtaqueActivo() && jugador.getAreaDeAtaque().overlaps(enemigo.getHitbox())) {
                float fuerzaEmpuje = 300f;
                if (enemigo.recibirGolpe(jugador.getNumeroAtaque(), jugador.getDireccionAtaque(), fuerzaEmpuje)) {
                    activarHitstop(0.06f);
                    activarTemblor(0.08f, 4f);
                }
            }

            // 2) El enemigo golpea: tambien tiene una ventana de impacto.
            //    Si Jorge lo golpeo antes, el golpe del enemigo se cancela solo.
            if (enemigo.impactoActivo() && enemigo.getAreaDeAtaque().overlaps(hitboxJugador)) {
                enemigo.resolverImpacto();
                float empujeAJugador = jugador.getCentroX() >= enemigo.getCentroX() ? 1f : -1f;
                if (jugador.esInvulnerable()) {
                    // Jorge todavia esta en su tiempo de gracia: el golpe no hace nada.
                } else if (jugador.bloqueaGolpeDesde(enemigo.getCentroX())) {
                    if (jugador.estaEnVentanaParry()) {
                        // Parry: bloqueo justo, el enemigo queda aturdido y vulnerable.
                        enemigo.aturdir(1.1f, -empujeAJugador, 380f);
                        activarHitstop(0.12f);
                        activarTemblor(0.15f, 6f);
                        tiempoTextoParry = 0.7f;
                    } else {
                        jugador.bloquearGolpe(empujeAJugador);
                        activarHitstop(0.04f);
                        activarTemblor(0.08f, 3f);
                    }
                } else {
                    vida.recibirGolpe();
                    jugador.recibirImpacto(empujeAJugador);
                    activarHitstop(0.08f);
                    activarTemblor(0.2f, 8f);
                    if (!vida.estaVivo()) {
                        finDelJuego = true;
                    }
                }
            }
        }

        enemigos.removeIf(enemigo -> {
            if (!enemigo.puedeEliminarse()) {
                return false;
            }
            enemigo.dispose();
            return true;
        });
        if (enemigos.isEmpty() && !oleadasCompletadas) {
            if (oleadaActual < CANTIDAD_OLEADAS) {
                oleadaActual++;
                iniciarOleada(jugador.getX());
            } else {
                oleadasCompletadas = true;
            }
        }

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glViewport(
            viewport.getScreenX(), viewport.getScreenY(),
            viewport.getScreenWidth(), viewport.getScreenHeight()
        );
        float temblorX = 0f;
        float temblorY = 0f;
        if (tiempoTemblor > 0f && duracionTemblor > 0f) {
            float fuerza = intensidadTemblor * (tiempoTemblor / duracionTemblor);
            temblorX = MathUtils.random(-fuerza, fuerza);
            temblorY = MathUtils.random(-fuerza, fuerza);
        }
        camera.position.set(ANCHO / 2f + temblorX, ALTO / 2f + temblorY, 0f);
        camera.update();
        game.batch.setProjectionMatrix(camera.combined);

        game.batch.begin();
        game.batch.draw(fondoMapa, -scroll, 0f, ANCHO_MAPA, ALTO);
        game.batch.end();

        dibujarPlataformas(scroll);

        game.batch.begin();
        jugador.dibujar(game.batch, scroll);
        for (Enemigo enemigo : enemigos) {
            enemigo.dibujar(game.batch, scroll);
        }
        vida.dibujar(game.batch);
        fuente.draw(game.batch, "Zona 1: Las Catacumbas", 80f, 710f);
        if (oleadasCompletadas) {
            fuente.draw(game.batch, "¡Completaste las 3 oleadas!", 750f, 710f);
            if (jugador.getX() >= ANCHO_MAPA - DISTANCIA_SEGUNDA_PUERTA) {
                fuente.draw(
                    game.batch,
                    "Presiona ENTER para volver al lobby",
                    ANCHO_MAPA - 300f - scroll,
                    245f
                );
            }
        } else {
            fuente.draw(
                game.batch,
                "Oleada " + oleadaActual + "/" + CANTIDAD_OLEADAS,
                750f,
                710f
            );
        }
        if (tiempoTextoParry > 0f) {
            fuente.getData().setScale(1.6f);
            fuente.setColor(0.6f, 0.9f, 1f, 1f);
            fuente.draw(game.batch, "¡PARRY!", jugador.getX() - scroll + 35f, jugador.getY() + 200f);
            fuente.setColor(1f, 1f, 1f, 1f);
            fuente.getData().setScale(1f);
        }
        game.batch.end();

        if (oleadasCompletadas
            && jugador.getX() >= ANCHO_MAPA - DISTANCIA_SEGUNDA_PUERTA
            && Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            Monedas.recompensarPrimerMapa();
            game.setScreen(new JuegoScreen(game));
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MenuScreen(game));
        }
    }

    private void activarHitstop(float segundos) {
        tiempoHitstop = Math.max(tiempoHitstop, segundos);
    }

    private void activarTemblor(float segundos, float intensidad) {
        tiempoTemblor = segundos;
        duracionTemblor = segundos;
        intensidadTemblor = intensidad;
    }

    // Empuja a los enemigos que quedaron uno encima del otro.
    private void separarEnemigos() {
        final float distanciaMinima = 110f;
        for (int i = 0; i < enemigos.size(); i++) {
            Enemigo a = enemigos.get(i);
            if (!a.estaVivo()) {
                continue;
            }
            for (int j = i + 1; j < enemigos.size(); j++) {
                Enemigo b = enemigos.get(j);
                if (!b.estaVivo()) {
                    continue;
                }
                float diferencia = b.getCentroX() - a.getCentroX();
                float distancia = Math.abs(diferencia);
                if (distancia >= distanciaMinima) {
                    continue;
                }
                float direccion = diferencia >= 0f ? 1f : -1f;
                float empuje = (distanciaMinima - distancia) / 2f;
                a.aplicarSeparacion(-direccion * empuje);
                b.aplicarSeparacion(direccion * empuje);
            }
        }
    }

    private void iniciarOleada(float jugadorX) {
        enemigos.clear();
        int cantidadEnemigos = ENEMIGOS_POR_OLEADA[oleadaActual - 1];
        for (int i = 0; i < cantidadEnemigos; i++) {
            float x = buscarPosicionAparicion(jugadorX);
            float velocidad = 80f + MathUtils.random(0f, 20f);
            enemigos.add(new Enemigo(x, ALTURA_SUELO, velocidad));
        }
    }

    private float buscarPosicionAparicion(float jugadorX) {
        float limiteMinimo = 60f;
        float limiteMaximo = ANCHO_MAPA - 220f;
        float mejorPosicion = limiteMinimo;
        float mejorDistancia = -1f;

        for (int intento = 0; intento < 80; intento++) {
            float posicion = MathUtils.random(limiteMinimo, limiteMaximo);
            float distanciaJugador = Math.abs(posicion - jugadorX);
            float distanciaEnemigos = distanciaAlEnemigoMasCercano(posicion);
            float distanciaMinima = Math.min(distanciaJugador, distanciaEnemigos);
            if (distanciaMinima > mejorDistancia) {
                mejorDistancia = distanciaMinima;
                mejorPosicion = posicion;
            }
            if (distanciaJugador >= DISTANCIA_MINIMA_APARICION
                && distanciaEnemigos >= DISTANCIA_MINIMA_ENTRE_ENEMIGOS) {
                return posicion;
            }
        }

        return mejorPosicion;
    }

    private float distanciaAlEnemigoMasCercano(float posicion) {
        if (enemigos.isEmpty()) {
            return Float.MAX_VALUE;
        }
        float distanciaMinima = Float.MAX_VALUE;
        for (Enemigo enemigo : enemigos) {
            distanciaMinima = Math.min(distanciaMinima, Math.abs(posicion - enemigo.getX()));
        }
        return distanciaMinima;
    }

    private void dibujarPlataformas(float desplazamientoX) {
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Rectangle plataforma : plataformas) {
            float x = plataforma.x - desplazamientoX;
            shapeRenderer.setColor(0.20f, 0.20f, 0.22f, 1f);
            shapeRenderer.rect(x, plataforma.y, plataforma.width, plataforma.height);
            shapeRenderer.setColor(0.42f, 0.40f, 0.36f, 1f);
            shapeRenderer.rect(x, plataforma.y + plataforma.height - 5f, plataforma.width, 5f);
            shapeRenderer.setColor(0.10f, 0.10f, 0.11f, 1f);
            shapeRenderer.rect(x, plataforma.y, plataforma.width, 3f);
        }
        shapeRenderer.end();
    }

    private void dibujarGameOver() {
        camera.position.set(ANCHO / 2f, ALTO / 2f, 0f);
        camera.update();
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
        shapeRenderer.rect(0f, 0f, ANCHO, ALTO);
        shapeRenderer.end();

        game.batch.begin();
        game.batch.draw(pantallaGameOver, 0f, 0f, ANCHO, ALTO);
        game.batch.end();

        Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f);
        viewport.unproject(mouse);
        float escalaX = ANCHO / 1536f;
        float escalaY = ALTO / 1024f;
        float volverX = 417f * escalaX;
        float volverAncho = (1120f - 417f) * escalaX;
        float volverY = ALTO - 830f * escalaY;
        float volverAlto = (830f - 678f) * escalaY;
        float salirX = 473f * escalaX;
        float salirAncho = (1065f - 473f) * escalaX;
        float salirY = ALTO - 989f * escalaY;
        float salirAlto = (989f - 836f) * escalaY;
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
        if (pantallaGameOver != null) {
            pantallaGameOver.dispose();
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
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
    }
}
