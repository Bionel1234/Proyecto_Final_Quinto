package com.gladiador.juego;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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

// Nivel de combate: un mapa largo donde Jorge debe sobrevivir a 3 oleadas de enemigos
// y despues puede salir por la puerta del fondo.
public class MapaScreen implements Screen {

    public static final int ANCHO = 1366;
    public static final int ALTO = 768;
    private static final float ANCHO_MAPA_BASE = 2089f;
    private static final float ALTURA_SUELO = 150f;   // altura donde todos pisan
    private static final float DISTANCIA_PUERTA = 320f; // tan cerca hay que estar de la salida
    private static final float DISTANCIA_MINIMA_APARICION = 450f; // lejos de Jorge al aparecer
    private static final float DISTANCIA_MINIMA_ENTRE_ENEMIGOS = 230f;
    private static final float DISTANCIA_APARICION_ZONA1 = 180f;
    private static final float SEPARACION_APARICION_ZONA1 = 160f;
    private static final float DURACION_AVISO_OLEADA = 2.5f; // cuanto se ve el cartel "Oleada X"
    private static final float INTERVALO_REFUERZOS_ZONA1 = 8f;
    private static final int CANTIDAD_OLEADAS = 3;
    private static final int[] ENEMIGOS_POR_OLEADA = {3, 5, 7};
    private static final int[] ENEMIGOS_ZONA1_POR_OLEADA = {5, 5, 5};
    private static final int CANTIDAD_INICIAL_ZONA1 = 3;
    private static final String[] IMAGENES_OLEADA = {"ui/oleada_1.png", "ui/oleada_2.png", "ui/oleada_3.png"};
    private final MainClass game;
    private final String nombreZona;   // "Zona 1" o "Zona 2": cambia enemigos y texto
    private final int numeroNivel;
    private final int etapaProgresion;
    private float anchoMapa = ANCHO_MAPA_BASE;

    private OrthographicCamera camera;
    private Viewport viewport;
    private Texture fondoMapa;          // fondo fijo de la Zona 1
    private Texture fondoZona2;         // fondo nuevo de la Zona 2 (o null)
    private Texture pantallaGameOver;
    private Texture[] imagenesOleada;
    private ArrayList<Enemigo> enemigos;
    private Jugador jugador;
    private Vida vida;
    private BitmapFont fuente;
    private ShapeRenderer shapeRenderer;  // para dibujar las plataformas (rectangulos)
    private List<Rectangle> plataformas;  // las dos plataformas sobre las que se puede saltar

    // Estado del nivel.
    private boolean finDelJuego;
    private int oleadaActual;
    private boolean oleadasCompletadas;
    private float tiempoAvisoOleada;
    private float tiempoRefuerzosZona1;
    private int enemigosPendientes;

    // "Jugo" del combate: el juego se congela un instante al golpear (hitstop),
    // la camara tiembla con los golpes fuertes y el parry muestra un cartel.
    private float tiempoHitstop;
    private float tiempoTemblor;
    private float duracionTemblor;
    private float intensidadTemblor;
    private float tiempoTextoParry;

    // Guarda la zona y el nivel, y convierte esa combinacion al identificador de progreso.
    public MapaScreen(MainClass game, String nombreZona, int numeroNivel) {
        this.game = game;
        this.nombreZona = nombreZona;
        this.numeroNivel = numeroNivel;
        if ("Zona 1".equals(nombreZona)) {
            etapaProgresion = numeroNivel == 2
                ? MainClass.ETAPA_ZONA1_NIVEL2 : MainClass.ETAPA_ZONA1_NIVEL1;
        } else {
            etapaProgresion = MainClass.ETAPA_ZONA2;
        }
    }

    // Se llama una sola vez al abrir el nivel: crear todo.
    @Override
    public void show() {
        // Inicializa los recursos y el estado cuando LibGDX muestra este nivel.
        configurarCamara();
        fondoMapa = new Texture(Gdx.files.internal("Mapa 1/scene_animated.png"));
        pantallaGameOver = new Texture(Gdx.files.internal("ui/gameover.png"));
        if ("Zona 2".equals(nombreZona)) {
            fondoZona2 = new Texture(Gdx.files.internal("Zona 2/fondo.png"));
            fondoZona2.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }
        imagenesOleada = new Texture[CANTIDAD_OLEADAS];
        for (int i = 0; i < CANTIDAD_OLEADAS; i++) {
            imagenesOleada[i] = new Texture(Gdx.files.internal(IMAGENES_OLEADA[i]));
        }
        jugador = new Jugador(250f, 150f);
        vida = new Vida();
        enemigos = new ArrayList<>();
        fuente = new BitmapFont();
        shapeRenderer = new ShapeRenderer();
        // Dos plataformas flotantes: x, y (borde de abajo), ancho, alto.
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
        // Avanza el juego y dibuja un frame usando el tiempo desde el frame anterior.
        if (finDelJuego) {
            dibujarGameOver();
            return;
        }

        // Hitstop: durante esas fracciones de segundo el delta pasa a ser 0
        // (todo se "congela" menos los timers de efecto, que usan el tiempo real).
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
        if (tiempoAvisoOleada > 0f) {
            tiempoAvisoOleada = Math.max(0f, tiempoAvisoOleada - deltaReal);
        }
        actualizarRefuerzosZona1(delta, jugador.getX());

        // 1) Jorge se mueve, salta y puede caer sobre las plataformas.
        jugador.actualizar(delta, ALTURA_SUELO, anchoMapa - 220f, plataformas);

        // 2) La camara sigue a Jorge: el fondo se desplaza ("scroll") con el.
        float scroll = Math.min(Math.max(0f, jugador.getX() - 180f), anchoMapa - ANCHO);

        // 3) Actualizar enemigos. Los muertos tambien (para terminar de desvanecerse).
        //    Se los ordena por posicion para que cada uno "apunte" a un lugar distinto
        //    alrededor de Jorge y no se amontonen todos en el mismo punto.
        ArrayList<Enemigo> enemigosVivos = new ArrayList<>();
        for (Enemigo enemigo : enemigos) {
            if (enemigo.estaVivo()) {
                enemigosVivos.add(enemigo);
            }
        }
        enemigosVivos.sort(Comparator.comparingDouble(Enemigo::getCentroX));
        int cantidadVivos = enemigosVivos.size();

        for (Enemigo enemigo : enemigos) {
            float desplazamientoObjetivoX = 0f;
            if (enemigo.estaVivo()) {
                int posicion = enemigosVivos.indexOf(enemigo);
                desplazamientoObjetivoX = (posicion - (cantidadVivos - 1) / 2f)
                    * Enemigo.separacionObjetivos();
            }
            enemigo.actualizar(delta, jugador.getX(), jugador.getY(), 0f, anchoMapa, desplazamientoObjetivoX);
        }
        separarEnemigos();

        // 4) Resolver los golpes de este frame.
        Rectangle hitboxJugador = jugador.getHitbox();
        for (Enemigo enemigo : enemigos) {
            if (!enemigo.estaVivo()) {
                continue;
            }

            // 4a) Jorge golpea: solo cuenta durante los frames activos del tajo.
            if (jugador.estaAtaqueActivo()
                    && jugador.getAreaDeAtaque().overlaps(enemigo.getHitbox())) {
                if (enemigo.recibirGolpe(jugador.getNumeroAtaque(), jugador.getDireccionAtaque(), 300f)) {
                    activarHitstop(0.06f);
                    activarTemblor(0.08f, 4f);
                }
            }

            // 4b) El enemigo golpea: tambien tiene una ventana de impacto.
            //     Si Jorge lo golpeo antes, el ataque del enemigo se cancelo solo.
            boolean jugadorRecibiendo = enemigo.impactoActivo()
                && enemigo.getAreaDeAtaque().overlaps(hitboxJugador)
                && Math.abs(jugador.getY() - enemigo.getY()) <= enemigo.getAlturaAtaqueMax() + 18f;
            if (jugadorRecibiendo) {
                enemigo.resolverImpacto();
                float empujeAJugador = jugador.getCentroX() >= enemigo.getCentroX() ? 1f : -1f;
                if (jugador.esInvulnerable()) {
                    // Tiempo de gracia: el golpe no hace nada.
                } else if (jugador.bloqueaGolpeDesde(enemigo.getCentroX())) {
                    if (jugador.estaEnVentanaParry()) {
                        // PARRY: bloqueo justo, el enemigo queda aturdido y vulnerable.
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
                    // Golpe limpio: baja la vida y Jorge queda aturdido un momento.
                    vida.recibirGolpe(enemigo.getDano());
                    jugador.recibirImpacto(empujeAJugador);
                    activarHitstop(0.08f);
                    activarTemblor(0.2f, 8f);
                    if (!vida.estaVivo()) {
                        finDelJuego = true;
                    }
                }
            }
        }

        // 5) Sacar de la lista a los enemigos que ya terminaron de desvanecerse.
        enemigos.removeIf(enemigo -> {
            if (!enemigo.puedeEliminarse()) {
                return false;
            }
            enemigo.dispose();
            return true;
        });

        // 6) Si no queda ninguno, arrancar la proxima oleada (o dar el nivel por superado).
        if (enemigos.isEmpty() && enemigosPendientes == 0 && !oleadasCompletadas) {
            if (oleadaActual < CANTIDAD_OLEADAS) {
                oleadaActual++;
                iniciarOleada(jugador.getX());
            } else {
                oleadasCompletadas = true;
            }
        }

        // 7) Limpiar la pantalla y aplicar el temblor de camara.
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
            // El temblor se va apagando: usa la fraccion de tiempo que resta.
            float fuerza = intensidadTemblor * (tiempoTemblor / duracionTemblor);
            temblorX = MathUtils.random(-fuerza, fuerza);
            temblorY = MathUtils.random(-fuerza, fuerza);
        }
        camera.position.set(ANCHO / 2f + temblorX, ALTO / 2f + temblorY, 0f);
        camera.update();
        game.batch.setProjectionMatrix(camera.combined);

        // 8) Dibujar: fondo desplazado, plataformas, personajes e interfaz.
        game.batch.begin();
        Texture fondoActual = mirarFondoActual();
        float altoFondo = anchoMapa * fondoActual.getHeight() / fondoActual.getWidth();
        game.batch.draw(fondoActual, -scroll, 0f, anchoMapa, altoFondo);
        game.batch.end();

        dibujarPlataformas(scroll);

        game.batch.begin();
        jugador.dibujar(game.batch, scroll);
        for (Enemigo enemigo : enemigos) {
            enemigo.dibujar(game.batch, scroll);
        }
        vida.dibujar(game.batch);
        fuente.draw(game.batch, nombreZona + ": Nivel " + numeroNivel, 80f, 710f);
        if (oleadasCompletadas) {
            fuente.draw(game.batch, "¡Has superado las 3 oleadas!", 700f, 710f);
            if (jugador.getX() >= anchoMapa - DISTANCIA_PUERTA) {
                fuente.draw(game.batch, "Pulsa ENTER para volver al lobby",
                    anchoMapa - 300f - scroll, 245f);
            }
        } else {
            fuente.draw(game.batch, "Oleada " + oleadaActual + "/" + CANTIDAD_OLEADAS, 80f, 680f);
            if (tiempoAvisoOleada > 0f) {
                Texture imagenOleada = imagenesOleada[oleadaActual - 1];
                float anchoImagen = 780f;
                float altoImagen = anchoImagen * imagenOleada.getHeight() / imagenOleada.getWidth();
                game.batch.draw(imagenOleada, (ANCHO - anchoImagen) / 2f, ALTO - altoImagen - 30f,
                    anchoImagen, altoImagen);
            }
        }
        if (tiempoTextoParry > 0f) {
            // El cartel "PARRY!" flotando sobre Jorge.
            fuente.getData().setScale(1.6f);
            fuente.setColor(0.6f, 0.9f, 1f, 1f);
            fuente.draw(game.batch, "¡PARRY!", jugador.getX() - scroll + 35f, jugador.getY() + 200f);
            fuente.setColor(1f, 1f, 1f, 1f);
            fuente.getData().setScale(1f);
        }
        game.batch.end();

        // 9) Salir del nivel o del juego.
        if (oleadasCompletadas
            && jugador.getX() >= anchoMapa - DISTANCIA_PUERTA
            && Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            Monedas.recompensarNivelCompletado();
            game.registrarEtapaCompletada(etapaProgresion);
            game.setScreen(new JuegoScreen(game));
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MenuScreen(game));
        }
    }

    // Conserva la pausa de combate mas larga si varios impactos coinciden.
    private void activarHitstop(float segundos) {
        tiempoHitstop = Math.max(tiempoHitstop, segundos);
    }

    // Guarda duracion e intensidad para que render aplique el temblor de camara.
    private void activarTemblor(float segundos, float intensidad) {
        tiempoTemblor = segundos;
        duracionTemblor = segundos;
        intensidadTemblor = intensidad;
    }

    // Devolver el fondo fijo que corresponde a la zona.
    private Texture mirarFondoActual() {
        return fondoZona2 == null ? fondoMapa : fondoZona2;
    }

    // Si dos enemigos se superponen, empujarlos en direcciones opuestas.
    // Revisa pares de enemigos vivos y separa los que tengan hitboxes superpuestas.
    private void separarEnemigos() {
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
                Rectangle hitboxA = a.getHitbox();
                Rectangle hitboxB = b.getHitbox();
                if (!hitboxA.overlaps(hitboxB)) {
                    continue; // no se estan tocando
                }

                float diferencia = b.getCentroX() - a.getCentroX();
                float direccion = diferencia == 0f
                    ? (a.getX() <= jugador.getX() ? -1f : 1f)
                    : Math.signum(diferencia);
                float distanciaMinima = (hitboxA.width + hitboxB.width) / 2f + 2f;
                float distancia = Math.abs(diferencia);
                float separacion = Math.max(0f, (distanciaMinima - distancia) / 2f);

                a.aplicarSeparacion(-direccion * separacion);
                b.aplicarSeparacion(direccion * separacion);
            }
        }
    }

    // Crear los enemigos de la oleada actual.
    private void iniciarOleada(float jugadorX) {
        // Vacía la ronda anterior y crea la cantidad de enemigos de esta oleada.
        enemigos.clear();
        tiempoAvisoOleada = DURACION_AVISO_OLEADA;
        int[] cantidadesPorOleada = "Zona 1".equals(nombreZona)
            ? ENEMIGOS_ZONA1_POR_OLEADA : ENEMIGOS_POR_OLEADA;
        int cantidadEnemigos = cantidadesPorOleada[oleadaActual - 1];
        int cantidadInicial = "Zona 1".equals(nombreZona)
            ? Math.min(CANTIDAD_INICIAL_ZONA1, cantidadEnemigos) : cantidadEnemigos;
        enemigosPendientes = cantidadEnemigos - cantidadInicial;
        tiempoRefuerzosZona1 = 0f;
        for (int i = 0; i < cantidadInicial; i++) {
            agregarEnemigoOleada(jugadorX, i);
        }
    }

    private void actualizarRefuerzosZona1(float delta, float jugadorX) {
        if (!"Zona 1".equals(nombreZona) || enemigosPendientes == 0) {
            return;
        }
        tiempoRefuerzosZona1 += delta;
        if (tiempoRefuerzosZona1 < INTERVALO_REFUERZOS_ZONA1) {
            return;
        }
        int primerIndice = ENEMIGOS_ZONA1_POR_OLEADA[oleadaActual - 1] - enemigosPendientes;
        for (int i = 0; i < enemigosPendientes; i++) {
            agregarEnemigoOleada(jugadorX, primerIndice + i);
        }
        enemigosPendientes = 0;
        tiempoRefuerzosZona1 = 0f;
    }

    private void agregarEnemigoOleada(float jugadorX, int indiceEnemigo) {
            float x = buscarPosicionAparicion(jugadorX);
            Enemigo.TipoEnemigo tipo = seleccionarTipoEnemigo(indiceEnemigo);
            float velocidadBase = "Zona 1".equals(nombreZona)
                ? obtenerVelocidadBaseZona1(tipo)
                : 80f + MathUtils.random(0f, 24f) + oleadaActual * 4f;
            enemigos.add(new Enemigo(x, ALTURA_SUELO, velocidadBase, tipo, nombreZona));
    }

    // Elegir el tipo al azar, con mas probabilidad de tipos duros en las oleadas avanzadas.
    private Enemigo.TipoEnemigo seleccionarTipoEnemigo(int indiceEnemigo) {
        if ("Zona 1".equals(nombreZona)) {
            if (oleadaActual == 1) {
                return indiceEnemigo == 1
                    ? Enemigo.TipoEnemigo.AGIL : Enemigo.TipoEnemigo.BASICO;
            }
            if (oleadaActual == 2) {
                if (indiceEnemigo == 0) {
                    return Enemigo.TipoEnemigo.BRUTO;
                }
                return indiceEnemigo == 1
                    ? Enemigo.TipoEnemigo.AGIL : Enemigo.TipoEnemigo.BASICO;
            }
            Enemigo.TipoEnemigo[] tiposOleadaTres = {
                Enemigo.TipoEnemigo.ELITE,
                Enemigo.TipoEnemigo.AGIL,
                Enemigo.TipoEnemigo.BRUTO,
                Enemigo.TipoEnemigo.BASICO,
                Enemigo.TipoEnemigo.AGIL
            };
            return tiposOleadaTres[indiceEnemigo];
        }

        float aleatorio = MathUtils.random(); // numero entre 0 y 1
        if (oleadaActual >= 3 && aleatorio < 0.18f + (oleadaActual - 1) * 0.10f) {
            return Enemigo.TipoEnemigo.ELITE;
        }
        if (oleadaActual >= 2 && aleatorio < 0.38f + (oleadaActual - 1) * 0.12f) {
            return Enemigo.TipoEnemigo.BRUTO;
        }
        if (aleatorio < 0.25f + oleadaActual * 0.14f) {
            return Enemigo.TipoEnemigo.AGIL;
        }
        return Enemigo.TipoEnemigo.BASICO;
    }

    private float obtenerVelocidadBaseZona1(Enemigo.TipoEnemigo tipo) {
        float variacionPorOleada = (oleadaActual - 1) * 3f;
        if (tipo == Enemigo.TipoEnemigo.AGIL) {
            return (MathUtils.random(165f, 185f) + variacionPorOleada) / tipo.multiplicador;
        }
        if (tipo == Enemigo.TipoEnemigo.BRUTO) {
            return (MathUtils.random(70f, 85f) + variacionPorOleada) / tipo.multiplicador;
        }
        if (tipo == Enemigo.TipoEnemigo.ELITE) {
            return (MathUtils.random(130f, 150f) + variacionPorOleada) / tipo.multiplicador;
        }
        return (MathUtils.random(105f, 120f) + variacionPorOleada) / tipo.multiplicador;
    }

    // Probar hasta 80 posiciones al azar y quedarse con la primera "buena":
    // lejos de Jorge y lejos de otros enemigos.
    // Busca un sitio de aparicion suficientemente lejos del jugador y otros enemigos.
    private float buscarPosicionAparicion(float jugadorX) {
        float limiteMinimo = 60f;
        float limiteMaximo = anchoMapa - 220f;
        float distanciaMinimaJugador = DISTANCIA_MINIMA_APARICION;
        float distanciaMinimaEnemigos = DISTANCIA_MINIMA_ENTRE_ENEMIGOS;
        if ("Zona 1".equals(nombreZona)) {
            float scroll = Math.min(Math.max(0f, jugadorX - 180f), anchoMapa - ANCHO);
            limiteMinimo = Math.max(limiteMinimo, scroll + 150f);
            limiteMaximo = Math.min(limiteMaximo, scroll + ANCHO - 150f);
            distanciaMinimaJugador = DISTANCIA_APARICION_ZONA1;
            distanciaMinimaEnemigos = SEPARACION_APARICION_ZONA1;
        }
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
            if (distanciaJugador >= distanciaMinimaJugador
                && distanciaEnemigos >= distanciaMinimaEnemigos) {
                return posicion; // encontramos un lugar bueno, no seguimos buscando
            }
        }

        return mejorPosicion; // si no hubo una perfecta, usar la mejor encontrada
    }

    // Devuelve la distancia al enemigo mas proximo a una posicion candidata.
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

    // Las plataformas se dibujan con rectangulos (no tienen imagen).
    private void dibujarPlataformas(float desplazamientoX) {
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Rectangle plataforma : plataformas) {
            float x = plataforma.x - desplazamientoX;
            shapeRenderer.setColor(0.20f, 0.20f, 0.22f, 1f);   // cuerpo oscuro
            shapeRenderer.rect(x, plataforma.y, plataforma.width, plataforma.height);
            shapeRenderer.setColor(0.42f, 0.40f, 0.36f, 1f);   // borde de arriba clarito
            shapeRenderer.rect(x, plataforma.y + plataforma.height - 5f, plataforma.width, 5f);
            shapeRenderer.setColor(0.10f, 0.10f, 0.11f, 1f);   // sombra debajo
            shapeRenderer.rect(x, plataforma.y, plataforma.width, 3f);
        }
        shapeRenderer.end();
    }

    // Pantalla de GAME OVER con dos botones clickeables sobre la imagen.
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
        game.batch.draw(mirarFondoActual(), 0f, 0f, ANCHO, ALTO);
        game.batch.end();

        // Oscurecer el fondo para que se lea el cartel.
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.7f);
        shapeRenderer.rect(0f, 0f, ANCHO, ALTO);
        shapeRenderer.end();

        game.batch.begin();
        game.batch.draw(pantallaGameOver, 0f, 0f, ANCHO, ALTO);
        game.batch.end();

        // La imagen de game over mide 1671x941 y trae dos botones dibujados:
        // "REINTENTAR" (repetir el nivel) y "VOLVER AL MENU" (ir al lobby).
        // Sus cajas estan medidas sobre esa imagen y se escalan a la pantalla.
        Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f);
        viewport.unproject(mouse); // pasar la posicion del mouse al plano del juego
        float escalaX = ANCHO / 1671f;
        float escalaY = ALTO / 941f;
        float reintentarX = 600f * escalaX;
        float reintentarAncho = (1065f - 600f) * escalaX;
        float reintentarY = ALTO - 580f * escalaY;
        float reintentarAlto = (580f - 485f) * escalaY;
        float volverX = 600f * escalaX;
        float volverAncho = (1065f - 600f) * escalaX;
        float volverY = ALTO - 692f * escalaY;
        float volverAlto = (692f - 598f) * escalaY;
        boolean reintentarSeleccionado = mouse.x >= reintentarX && mouse.x <= reintentarX + reintentarAncho
            && mouse.y >= reintentarY && mouse.y <= reintentarY + reintentarAlto;
        boolean volverSeleccionado = mouse.x >= volverX && mouse.x <= volverX + volverAncho
            && mouse.y >= volverY && mouse.y <= volverY + volverAlto;
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            if (reintentarSeleccionado) {
                game.resetProgreso();
                game.setScreen(new JuegoScreen(game)); // reinicia la campaña y restaura la vida
            } else if (volverSeleccionado) {
                game.setScreen(new JuegoScreen(game)); // volver al lobby
            }
        }
    }

    // LibGDX notifica cambios de tamano para que el viewport conserve la proporcion.
    @Override
    public void resize(int width, int height) {
        if (viewport != null) {
            viewport.update(width, height, true);
        }
    }

    // El nivel no requiere tareas adicionales al pausar, reanudar o esconderse.
    @Override
    public void pause() {
    }

    // No hay datos que necesiten restaurarse al volver de una pausa.
    @Override
    public void resume() {
    }

    // Esta pantalla no mantiene musica propia; al ocultarse no hace nada adicional.
    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        // Libera todos los recursos que esta pantalla cargo o que sus personajes poseen.
        if (fondoMapa != null) {
            fondoMapa.dispose();
        }
        if (fondoZona2 != null) {
            fondoZona2.dispose();
        }
        if (pantallaGameOver != null) {
            pantallaGameOver.dispose();
        }
        if (imagenesOleada != null) {
            for (Texture imagenOleada : imagenesOleada) {
                if (imagenOleada != null) {
                    imagenOleada.dispose();
                }
            }
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
