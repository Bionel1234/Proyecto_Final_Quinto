package com.gladiador.juego;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

// Esta clase representa a Jorge, el protagonista.
// Controla su movimiento, salto, ataque con espada, escudo con parry y animaciones.
// Las teclas: A/D mover, W o flecha arriba saltar, espacio o click izquierdo atacar,
// E o click derecho escudo.
public class Jugador {

    // --- Animaciones (una por cada accion y direccion) ---
    private Animation<TextureRegion> estaticoDerecha;
    private Animation<TextureRegion> estaticoIzquierda;
    private Animation<TextureRegion> caminarDerecha;
    private Animation<TextureRegion> caminarIzquierda;
    private Animation<TextureRegion> saltoDerecha;
    private Animation<TextureRegion> saltoIzquierda;
    private Animation<TextureRegion> dashDerecha;
    private Animation<TextureRegion> dashIzquierda;
    private Animation<TextureRegion> ataqueDerecha;
    private Animation<TextureRegion> ataqueIzquierda;
    private Animation<TextureRegion> ataqueLanzaAlternoDerecha;
    private Animation<TextureRegion> ataqueLanzaAlternoIzquierda;
    private Animation<TextureRegion> escudo;

    private static final String RUTA_ESTATICO_NORMAL_DERECHA =
        "jugador/Pj principal estatico derecha-20261029T164911Z-1-001/Pj principal estatico derecha";
    private static final String RUTA_ESTATICO_NORMAL_IZQUIERDA =
        "jugador/Pj principal estatico izquierda-20261029T164915Z-1-001/Pj principal estatico izquierda";
    private static final String RUTA_ESTATICO_LANZA_DERECHA =
        "jugador/estatico_corregido/derecha";
    private static final String RUTA_ESTATICO_LANZA_IZQUIERDA =
        "jugador/estatico_corregido/izquierda";
    private static final String RUTA_CAMINAR_LANZA_IZQUIERDA =
        "jugador/lanza_corregida/caminar_izquierda";
    private static final String RUTA_CAMINAR_LANZA_DERECHA =
        "jugador/lanza_corregida/caminar_derecha";
    private static final String RUTA_SALTO_LANZA_DERECHA =
        "jugador/lanza_corregida/salto_derecha";
    private static final String RUTA_SALTO_LANZA_IZQUIERDA =
        "jugador/lanza_corregida/salto_izquierda";
    private static final String RUTA_ATAQUE_LANZA_DERECHA =
        "jugador/lanza_corregida/ataque_derecha";
    private static final String RUTA_ATAQUE_LANZA_IZQUIERDA =
        "jugador/lanza_corregida/ataque_izquierda";
    private static final String RUTA_ATAQUE_LANZA_ALTERNO_DERECHA =
        "jugador/lanza_corregida/ataque_2_derecha";
    private static final String RUTA_ATAQUE_LANZA_ALTERNO_IZQUIERDA =
        "jugador/lanza_corregida/ataque_2_izquierda";
    private static final String RUTA_ATAQUE_ESPADA_DERECHA =
        "jugador/ataque_corregido/espada_derecha";
    private static final String RUTA_ATAQUE_ESPADA_IZQUIERDA =
        "jugador/ataque_corregido/espada_izquierda";
    private static final int FRAMES_ATAQUE_LANZA = 15;
    private static final int FRAMES_ATAQUE_LANZA_ALTERNO = 7;

    private static boolean lanzaComprada = false;
    private static boolean lanzaEquipada = false;
    private boolean tieneLanza;

    // Lista con todas las imagenes cargadas, para liberarlas al cerrar la pantalla.
    private final ArrayList<Texture> imagenes = new ArrayList<>();

    // Sonidos.
    private Sound sonidoSalto;
    private Sound sonidoAtaque;
    private Sound sonidoEscudo;
    private Sound sonidoCaminar;
    private long idSonidoCaminar = -1L; // identificador del "loop" de pasos (-1 = no suena)

    // --- Posicion y velocidad ---
    private float x;              // posicion horizontal
    private float y;              // posicion vertical de los pies
    private float velocidadX;     // velocidad horizontal actual
    private float velocidadY;     // velocidad vertical actual (salto/gravedad)

    // --- "Estados": que esta haciendo Jorge ---
    private boolean miraDerecha = true;      // hacia donde esta mirando
    private boolean estaEnElSuelo = true;    // true si pisa el piso o una plataforma
    private int saltosRealizados;
    private boolean estaAtacando = false;    // esta en mitad de un tajo
    private boolean estaProtegiendo = false; // tiene el escudo levantado
    private boolean estaDasheando;
    private float direccionDash;

    // --- Temporisadores de cada accion ---
    private float tiempoAnimacion = 0f; // tiempo de la animacion actual
    private float tiempoAtaque = 0f;    // tiempo que lleva el tajo en curso
    private float tiempoEscudo = 0f;    // tiempo que lleva el escudo levantado
    private float tiempoDash;
    private float cooldownDash;
    private long numeroAtaque;          // contador de tajos (evita que un tajo pegue dos veces)
    private boolean escudoMiraDerecha = true;   // direccion del escudo actual
    private boolean ataqueMiraDerecha = true;   // direccion del tajo actual (no cambia en el medio)
    private boolean siguienteAtaqueLanzaAlterno;
    private boolean usarAtaqueLanzaAlterno;
    private float duracionAtaqueActual = DURACION_ATAQUE;

    // --- Tiempo de espera y defensas ---
    private float cooldownEscudo = 0f;       // tiempo antes de poder usar el escudo otra vez
    private float tiempoInvulnerable = 0f;   // gracia despues de recibir un golpe
    private float tiempoAturdido = 0f;       // Jorge no puede actuar mientras dure
    private float velocidadEmpujeX = 0f;     // empuje de los golpes recibidos (se apaga solo)
    private float escalaVisual = 1f;         // tamano visual (1 = normal, en el Coliseo 1.2)

    // --- Numeros del movimiento (asi se ajustan facil las "fisicas") ---
    private static final float VELOCIDAD_CAMINAR = 260f; // pixeles por segundo
    private static final float VELOCIDAD_DASH = 600f;
    private static final float DURACION_DASH = 11 * 0.04f;
    private static final float COOLDOWN_DASH = 0.75f;
    private static final float FUERZA_SALTO = 720f;      // impulso inicial del salto
    private static final float GRAVEDAD = 1500f;         // fuerza que lo hace caer

    // Tamano "logico" del personaje (su cuadro de accion, no el del dibujo).
    private static final float ANCHO_PERSONAJE = 180f;
    private static final float ALTO_VISIBLE_PERSONAJE = 137f;

    // El golpe se registra en la segunda mitad de la animacion, cuando el arma ya se ve atacar.
    private static final float DURACION_ATAQUE = 12 * 0.07f;
    private static final float FRACCION_INICIO_GOLPE = 0.50f;
    private static final float FRACCION_FIN_GOLPE = 0.75f;
    private float inicioAtaqueActivo;
    private float finAtaqueActivo;
    // Pequeno paso hacia adelante al tajar, para que el golpe "pese".
    private static final float INICIO_IMPULSO = 0.14f;
    private static final float VELOCIDAD_IMPULSO = 150f;
    // Cuanto control de movimiento queda mientras se ataca (0 = ninguno, 1 = total).
    private static final float CONTROL_DURANTE_ATAQUE = 0.25f;

    // Escudo: duracion minima, ventana de parry y enfriamiento.
    private static final float DURACION_MINIMA_ESCUDO = 0.36f;
    private static final float VENTANA_PARRY = 0.20f;   // el bloqueo "justo" es en los primeros 0.2 s
    private static final float COOLDOWN_ESCUDO = 0.60f;

    // Cuando Jorge recibe un golpe: duracion de la gracia, del aturdimiento y del empuje.
    private static final float DURACION_INVULNERABLE = 0.90f;
    private static final float DURACION_ATURDIMIENTO = 0.22f;
    private static final float EMPUJE_AL_RECIBIR_DANIO = 420f;
    private static final float EMPUJE_AL_BLOQUEAR = 180f;

    // Crear a Jorge en la posicion inicial recibida (x, y de sus pies).
    public Jugador(float posicionInicialX, float posicionInicialY) {
        x = posicionInicialX;
        y = posicionInicialY;
        tieneLanza = lanzaComprada && lanzaEquipada;

        // Cargar las imagenes de todas las animaciones y los sonidos.
        cargarTodasLasAnimaciones();
        sonidoSalto = Gdx.audio.newSound(Gdx.files.internal("sonidos/sonido_salto.mp3"));
        sonidoAtaque = Gdx.audio.newSound(Gdx.files.internal("sonidos/sonido_ataque.mp3"));
        sonidoEscudo = Gdx.audio.newSound(Gdx.files.internal("sonidos/sonido_escudo.mp3"));
        sonidoCaminar = Gdx.audio.newSound(Gdx.files.internal("sonidos/sonido_caminar.mp3"));
    }

    public static void comprarLanza() {
        lanzaComprada = true;
        lanzaEquipada = true;
    }

    public static boolean tieneLanzaComprada() {
        return lanzaComprada;
    }

    public static void resetLanza() {
        lanzaComprada = false;
        lanzaEquipada = false;
    }

    public void setLanzaComprada(boolean comprada) {
        tieneLanza = comprada;
        lanzaComprada = comprada;
        lanzaEquipada = comprada;
        estaAtacando = false;
        estaProtegiendo = false;
        tiempoAtaque = 0f;
        tiempoEscudo = 0f;
        siguienteAtaqueLanzaAlterno = false;
        usarAtaqueLanzaAlterno = false;
        duracionAtaqueActual = DURACION_ATAQUE;
        cargarTodasLasAnimaciones();
    }

    public boolean tieneLanza() {
        return tieneLanza;
    }

    // Este metodo se llama una vez por frame desde la pantalla que corresponda.
    // delta = segundos desde el frame anterior; posicionDelSuelo = altura del piso;
    // limiteMaximoX = hasta donde puede caminar; plataformas = plataformas donde puede aterrizar.
    public void actualizar(float delta, float posicionDelSuelo, float limiteMaximoX, List<Rectangle> plataformas) {
        if (lanzaComprada && Gdx.input.isKeyJustPressed(Input.Keys.L)) {
            alternarArma();
        }
        tiempoAnimacion = tiempoAnimacion + delta;
        cooldownDash = Math.max(0f, cooldownDash - delta);

        // La invulnerabilidad post-golpe se va apagando sola.
        if (tiempoInvulnerable > 0f) {
            tiempoInvulnerable = Math.max(0f, tiempoInvulnerable - delta);
        }

        if (tiempoAturdido > 0f) {
            // Aturdido: no se puede atacar, protegerse ni caminar.
            tiempoAturdido = Math.max(0f, tiempoAturdido - delta);
            velocidadX = 0f;
            estaDasheando = false;
            tiempoDash = 0f;
        } else {
            actualizarEscudo(delta);
            revisarAtaque(delta);
            revisarMovimiento(delta);
        }

        aplicarEmpuje(delta);
        aplicarGravedad(delta, posicionDelSuelo, plataformas);
        limitarPosicionHorizontal(limiteMaximoX);
        actualizarSonidoCaminar();
    }

    private void alternarArma() {
        tieneLanza = !tieneLanza;
        lanzaEquipada = tieneLanza;
        estaAtacando = false;
        estaProtegiendo = false;
        tiempoAtaque = 0f;
        tiempoEscudo = 0f;
        siguienteAtaqueLanzaAlterno = false;
        usarAtaqueLanzaAlterno = false;
        duracionAtaqueActual = DURACION_ATAQUE;
        tiempoAnimacion = 0f;
        cargarTodasLasAnimaciones();
    }

    // El sonido de pasos es un loop que se prende al caminar y se apaga al parar.
    private void actualizarSonidoCaminar() {
        boolean caminando = estaEnElSuelo
            && tiempoAturdido <= 0f
            && !estaDasheando
            && !estaProtegiendo
            && (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.D))
            && Math.abs(velocidadX) > 1f;
        if (caminando && idSonidoCaminar < 0L) {
            idSonidoCaminar = sonidoCaminar.loop(0.22f);
        } else if (!caminando) {
            detenerSonidoCaminar();
        }
    }

    public void detenerSonidoCaminar() {
        if (idSonidoCaminar >= 0L) {
            sonidoCaminar.stop(idSonidoCaminar);
            idSonidoCaminar = -1L;
        }
    }

    // Cambiar el tamano visual (lo usa el Coliseo para agrandarlo un poco).
    public void setEscalaVisual(float escala) {
        escalaVisual = escala;
    }

    // Leer las teclas A, D, W, flecha arriba y Q para el dash.
    private void revisarMovimiento(float delta) {
        // Primero suponemos que Jorge esta quieto.
        velocidadX = 0f;
        if (estaProtegiendo) {
            return; // con el escudo no se camina
        }

        float direccion = 0f;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            direccion = -1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            direccion = 1f;
        }

        if (estaDasheando) {
            tiempoDash += delta;
            if (tiempoDash >= DURACION_DASH) {
                estaDasheando = false;
                tiempoDash = 0f;
                tiempoAnimacion = 0f;
                return;
            }
            velocidadX = direccionDash * VELOCIDAD_DASH;
            x += velocidadX * delta;
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)
                && cooldownDash <= 0f && !estaAtacando) {
            direccionDash = direccion != 0f ? direccion : (miraDerecha ? 1f : -1f);
            miraDerecha = direccionDash > 0f;
            estaDasheando = true;
            tiempoDash = 0f;
            cooldownDash = COOLDOWN_DASH;
            tiempoAnimacion = 0f;
            velocidadX = direccionDash * VELOCIDAD_DASH;
            x += velocidadX * delta;
            return;
        }

        if (estaAtacando) {
            // Durante el ataque Jorge no gira y casi no se puede mover,
            // pero el tajo lo empuja un poco hacia adelante.
            velocidadX = direccion * VELOCIDAD_CAMINAR * CONTROL_DURANTE_ATAQUE;
            if (tiempoAtaque >= INICIO_IMPULSO && tiempoAtaque <= finAtaqueActivo) {
                velocidadX += (ataqueMiraDerecha ? 1f : -1f) * VELOCIDAD_IMPULSO;
            }
        } else if (direccion != 0f) {
            velocidadX = direccion * VELOCIDAD_CAMINAR;
            miraDerecha = direccion > 0f;
        }

        // Moverse usando la velocidad y el tiempo de este frame.
        x = x + velocidadX * delta;

        // Detectar el inicio de un salto.
        boolean sePulsoSalto = Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
            || Gdx.input.isKeyJustPressed(Input.Keys.UP);

        // Jorge puede saltar una vez en el suelo y una segunda vez en el aire.
        if (sePulsoSalto && saltosRealizados < 2 && !estaAtacando && !estaProtegiendo) {
            velocidadY = FUERZA_SALTO;
            estaEnElSuelo = false;
            saltosRealizados++;
            tiempoAnimacion = 0f;
            sonidoSalto.play();
        }
    }

    // Escudo: tocar E = bloqueo corto. Mantener E = bloqueo largo.
    // Los primeros instantes son un "parry": si el golpe llega justo ahi, aturde al enemigo.
    private void actualizarEscudo(float delta) {
        if (cooldownEscudo > 0f) {
            cooldownEscudo = Math.max(0f, cooldownEscudo - delta);
        }

        boolean mantiene = Gdx.input.isKeyPressed(Input.Keys.E)
            || Gdx.input.isButtonPressed(Input.Buttons.RIGHT);

        if (estaProtegiendo) {
            // Levantar el escudo hasta: soltar la tecla (pasado el minimo) o que termine la animacion.
            tiempoEscudo = tiempoEscudo + delta;
            boolean terminoAnimacion = tiempoEscudo >= escudo.getAnimationDuration();
            boolean soltoElBoton = !mantiene && tiempoEscudo >= DURACION_MINIMA_ESCUDO;
            if (terminoAnimacion || soltoElBoton) {
                estaProtegiendo = false;
                tiempoEscudo = 0f;
                cooldownEscudo = COOLDOWN_ESCUDO;
            }
            return;
        }

        boolean inicioEscudo = Gdx.input.isKeyJustPressed(Input.Keys.E)
            || Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT);
        // Solo se puede cancelar un ataque con el escudo cuando ya esta en recuperacion.
        boolean puedeCancelarAtaque = !estaAtacando || tiempoAtaque >= finAtaqueActivo;
        if (inicioEscudo && cooldownEscudo <= 0f && puedeCancelarAtaque) {
            // Con A o D presionadas, el escudo se levanta mirando para ese lado.
            boolean izquierda = Gdx.input.isKeyPressed(Input.Keys.A);
            boolean derecha = Gdx.input.isKeyPressed(Input.Keys.D);
            if (izquierda != derecha) {
                miraDerecha = derecha;
            }
            estaProtegiendo = true;
            escudoMiraDerecha = miraDerecha;
            tiempoEscudo = 0f;
            sonidoEscudo.play();
            estaAtacando = false;
            tiempoAtaque = 0f;
        }
    }

    // Aplicar la gravedad y detectar cuando Jorge vuelve al suelo o aterriza en una plataforma.
    private void aplicarGravedad(float delta, float posicionDelSuelo, List<Rectangle> plataformas) {
        float posicionAnteriorY = y;
        velocidadY = velocidadY - GRAVEDAD * delta;
        y = y + velocidadY * delta;

        // Cayendo (velocidadY negativa): revisar si cruza la parte superior de alguna plataforma.
        if (velocidadY <= 0f) {
            float jugadorIzquierda = x + 40f;
            float jugadorDerecha = x + ANCHO_PERSONAJE - 40f;
            for (Rectangle plataforma : plataformas) {
                float alturaPlataforma = plataforma.y + plataforma.height;
                boolean cruzaPlataforma = posicionAnteriorY >= alturaPlataforma
                    && y <= alturaPlataforma;
                boolean estaSobrePlataforma = jugadorDerecha > plataforma.x
                    && jugadorIzquierda < plataforma.x + plataforma.width;
                if (cruzaPlataforma && estaSobrePlataforma) {
                    y = alturaPlataforma;
                    velocidadY = 0f;
                    estaEnElSuelo = true;
                    saltosRealizados = 0;
                    return;
                }
            }
        }

        // Si paso el nivel del piso, apoyarse.
        if (y <= posicionDelSuelo) {
            y = posicionDelSuelo;
            velocidadY = 0f;
            estaEnElSuelo = true;
            saltosRealizados = 0;
        }
    }

    // Empuje por golpes: mueve a Jorge y se apaga rapido.
    private void aplicarEmpuje(float delta) {
        if (velocidadEmpujeX == 0f) {
            return;
        }
        x = x + velocidadEmpujeX * delta;
        velocidadEmpujeX = velocidadEmpujeX * Math.max(0f, 1f - 9f * delta);
        if (Math.abs(velocidadEmpujeX) < 5f) {
            velocidadEmpujeX = 0f;
        }
    }

    // Revisar el ataque con la tecla espacio o el boton izquierdo del mouse.
    private void revisarAtaque(float delta) {
        boolean sePulsoAtaque = Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);

        if (sePulsoAtaque && !estaProtegiendo && !estaAtacando) {
            iniciarAtaque();
        }

        if (estaAtacando) {
            tiempoAtaque = tiempoAtaque + delta;
            if (tiempoAtaque >= duracionAtaqueActual) {
                estaAtacando = false;
                tiempoAtaque = 0f;
                tiempoAnimacion = 0f;
            }
        }
    }

    // Empezar un tajo nuevo. Cada pulsacion inicia uno distinto.
    private void iniciarAtaque() {
        // Al empezar el tajo se puede elegir hacia donde apuntar con A/D.
        boolean izquierda = Gdx.input.isKeyPressed(Input.Keys.A);
        boolean derecha = Gdx.input.isKeyPressed(Input.Keys.D);
        if (izquierda && !derecha) {
            miraDerecha = false;
        } else if (derecha && !izquierda) {
            miraDerecha = true;
        }
        ataqueMiraDerecha = miraDerecha;
        usarAtaqueLanzaAlterno = tieneLanza && siguienteAtaqueLanzaAlterno;
        if (tieneLanza) {
            siguienteAtaqueLanzaAlterno = !siguienteAtaqueLanzaAlterno;
        }
        duracionAtaqueActual = tieneLanza && usarAtaqueLanzaAlterno
            ? FRAMES_ATAQUE_LANZA_ALTERNO * 0.08f
            : DURACION_ATAQUE;
        inicioAtaqueActivo = duracionAtaqueActual * FRACCION_INICIO_GOLPE;
        finAtaqueActivo = duracionAtaqueActual * FRACCION_FIN_GOLPE;

        estaAtacando = true;
        tiempoAtaque = 0f;
        numeroAtaque++;
        tiempoAnimacion = 0f;
        sonidoAtaque.play();
    }

    // --- Danio y defensa ---

    // Jorge recibe un golpe sin defensa. direccionEmpuje: 1 = lo empuja a la derecha, -1 a la izquierda.
    public void recibirImpacto(float direccionEmpuje) {
        detenerSonidoCaminar();
        tiempoInvulnerable = DURACION_INVULNERABLE;
        tiempoAturdido = DURACION_ATURDIMIENTO;
        velocidadEmpujeX = direccionEmpuje * EMPUJE_AL_RECIBIR_DANIO;

        // El golpe interrumpe lo que estaba haciendo.
        estaAtacando = false;
        tiempoAtaque = 0f;
        estaProtegiendo = false;
        tiempoEscudo = 0f;
    }

    // Jorge bloqueo un golpe con el escudo: retrocede un poco pero no pierde vida.
    public void bloquearGolpe(float direccionEmpuje) {
        velocidadEmpujeX = direccionEmpuje * EMPUJE_AL_BLOQUEAR;
    }

    // El escudo solo protege de lo que viene de frente.
    // centroEnemigoX = desde donde vino el golpe.
    public boolean bloqueaGolpeDesde(float centroEnemigoX) {
        if (!estaProtegiendo) {
            return false;
        }
        float centroJugadorX = x + ANCHO_PERSONAJE / 2f;
        if (miraDerecha) {
            return centroEnemigoX >= centroJugadorX - 20f;
        }
        return centroEnemigoX <= centroJugadorX + 20f;
    }

    // true durante el primer cuarto de segundo del escudo: ahi el bloqueo es "parry".
    public boolean estaEnVentanaParry() {
        return estaProtegiendo && tiempoEscudo <= VENTANA_PARRY;
    }

    // Informa si aun esta activa la proteccion temporal despues de un impacto.
    public boolean esInvulnerable() {
        return tiempoInvulnerable > 0f;
    }

    // --- Consultas que usan las pantallas ---

    // Devuelven la posicion y el centro horizontal del personaje en el mundo.
    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getCentroX() {
        return x + ANCHO_PERSONAJE / 2f;
    }

    // true solo durante los frames en los que la espada realmente corta.
    public boolean estaAtaqueActivo() {
        return estaAtacando
            && tiempoAtaque >= inicioAtaqueActivo
            && tiempoAtaque <= finAtaqueActivo;
    }

    // Identificador del tajo actual: sirve para que un mismo tajo no pegue dos veces.
    // Datos del tajo actual que las pantallas usan para resolver las colisiones.
    public long getNumeroAtaque() {
        return numeroAtaque;
    }

    // 1 si el tajo actual va hacia la derecha, -1 si va hacia la izquierda.
    public float getDireccionAtaque() {
        return ataqueMiraDerecha ? 1f : -1f;
    }

    // No permitir que Jorge salga del ancho del mundo.
    private void limitarPosicionHorizontal(float posicionMaxima) {
        if (x < 0f) {
            x = 0f;
        }
        if (x > posicionMaxima) {
            x = posicionMaxima;
        }
    }

    // Dibujar el frame correcto de Jorge. desplazamientoX = cuanto se movio la camara.
    public void dibujar(SpriteBatch batch, float desplazamientoX) {
        // Obtener la imagen que corresponde al estado actual.
        TextureRegion imagenActual = obtenerImagenActual();

        // El alto se fija con escalaVisual; el ancho mantiene la proporcion del dibujo.
        float escala = ALTO_VISIBLE_PERSONAJE * escalaVisual / imagenActual.getRegionHeight();
        float ancho = imagenActual.getRegionWidth() * escala;
        float alto = ALTO_VISIBLE_PERSONAJE * escalaVisual;
        float posicionX = x - desplazamientoX + (ANCHO_PERSONAJE - ancho) / 2f;
        float posicionY = y;

        // El escudo siempre se dibujo mirando a la derecha; si toca mirar a la izquierda
        // se dibuja "al reves" (con ancho negativo).
        boolean voltearEscudo = estaProtegiendo && !escudoMiraDerecha;
        if (voltearEscudo) {
            posicionX += ancho;
        }

        // Colores de aviso: amarillo al estar aturdido, celeste mientras el parry esta activo,
        // y parpadeo transparente cuando es invulnerable.
        float rojo = 1f;
        float verde = 1f;
        float azul = 1f;
        float alfa = 1f;
        if (tiempoAturdido > 0f) {
            verde = 0.45f;
            azul = 0.45f;
        } else if (estaEnVentanaParry()) {
            rojo = 0.65f;
            verde = 0.9f;
        }
        if (tiempoInvulnerable > 0f && ((int) (tiempoInvulnerable * 20f)) % 2 == 0) {
            alfa = 0.35f;
        }
        batch.setColor(rojo, verde, azul, alfa);
        batch.draw(imagenActual, posicionX, posicionY, voltearEscudo ? -ancho : ancho, alto);
        batch.setColor(1f, 1f, 1f, 1f); // dejar el color como estaba
    }

    // Elegir una imagen segun la accion y la direccion.
    private TextureRegion obtenerImagenActual() {
        // Prioridad: escudo > ataque > salto > caminar > quieto.
        if (estaProtegiendo) {
            return escudo.getKeyFrame(tiempoEscudo, false);
        }

        if (estaDasheando) {
            return miraDerecha
                ? dashDerecha.getKeyFrame(tiempoDash, false)
                : dashIzquierda.getKeyFrame(tiempoDash, false);
        }

        // Si esta atacando, la animacion mira SIEMPRE hacia donde empezo el tajo.
        if (estaAtacando) {
            Animation<TextureRegion> ataqueDerechaActual = usarAtaqueLanzaAlterno
                ? ataqueLanzaAlternoDerecha : ataqueDerecha;
            Animation<TextureRegion> ataqueIzquierdaActual = usarAtaqueLanzaAlterno
                ? ataqueLanzaAlternoIzquierda : ataqueIzquierda;
            if (ataqueMiraDerecha) {
                return ataqueDerechaActual.getKeyFrame(tiempoAtaque, false);
            }
            return ataqueIzquierdaActual.getKeyFrame(tiempoAtaque, false);
        }

        if (!estaEnElSuelo) {
            if (miraDerecha) {
                return saltoDerecha.getKeyFrame(tiempoAnimacion, false);
            }
            return saltoIzquierda.getKeyFrame(tiempoAnimacion, false);
        }

        if (velocidadX != 0f) {
            if (miraDerecha) {
                return caminarDerecha.getKeyFrame(tiempoAnimacion, true); // true = se repite
            }
            return caminarIzquierda.getKeyFrame(tiempoAnimacion, true);
        }

        if (miraDerecha) {
            return estaticoDerecha.getKeyFrame(tiempoAnimacion, true);
        }
        return estaticoIzquierda.getKeyFrame(tiempoAnimacion, true);
    }

    // Caja del cuerpo: lo que pueden tocar los golpes enemigos.
    public Rectangle getHitbox() {
        float ancho = 70f * escalaVisual;
        float alto = 115f * escalaVisual;
        return new Rectangle(x + (ANCHO_PERSONAJE - ancho) / 2f, y + 5f, ancho, alto);
    }

    // Caja del tajo: el area por delante de Jorge donde la espada hace dano.
    public Rectangle getAreaDeAtaque() {
        float ancho = 130f * escalaVisual;
        float alto = 100f * escalaVisual;
        boolean haciaDerecha = estaAtacando ? ataqueMiraDerecha : miraDerecha;
        float centro = x + ANCHO_PERSONAJE / 2f;
        float xAtaque = haciaDerecha
            ? centro + 20f * escalaVisual
            : centro - 20f * escalaVisual - ancho;
        float yAtaque = y + 25f * escalaVisual;
        return new Rectangle(xAtaque, yAtaque, ancho, alto);
    }

    // Cargar las animaciones de Jorge (carpeta, cantidad de imagenes, duracion de cada una).
    private void cargarTodasLasAnimaciones() {
        liberarImagenesAnimacion();
        boolean conLanza = tieneLanza;
        estaticoDerecha = cargarAnimacionDesdeRuta(
            conLanza ? RUTA_ESTATICO_LANZA_DERECHA : RUTA_ESTATICO_NORMAL_DERECHA,
            conLanza ? 18 : 12, 0.10f);
        estaticoIzquierda = cargarAnimacionDesdeRuta(
            conLanza ? RUTA_ESTATICO_LANZA_IZQUIERDA : RUTA_ESTATICO_NORMAL_IZQUIERDA,
            conLanza ? 18 : 12, 0.10f);
        dashDerecha = cargarAnimacionDesdeRuta("jugador/dash/derecha", 11, 0.04f);
        dashIzquierda = cargarAnimacionDesdeRuta("jugador/dash/izquierda", 11, 0.04f);
        if (conLanza) {
            caminarDerecha = cargarAnimacionDesdeRuta(RUTA_CAMINAR_LANZA_DERECHA, 13, 0.08f);
            caminarIzquierda = cargarAnimacionDesdeRuta(RUTA_CAMINAR_LANZA_IZQUIERDA, 13, 0.08f);
            saltoDerecha = cargarAnimacionDesdeRuta(RUTA_SALTO_LANZA_DERECHA, 14, 0.10f);
            saltoIzquierda = cargarAnimacionDesdeRuta(RUTA_SALTO_LANZA_IZQUIERDA, 14, 0.10f);
            ataqueDerecha = cargarAnimacionDesdeRuta(
                RUTA_ATAQUE_LANZA_DERECHA, FRAMES_ATAQUE_LANZA,
                DURACION_ATAQUE / FRAMES_ATAQUE_LANZA);
            ataqueIzquierda = cargarAnimacionDesdeRuta(
                RUTA_ATAQUE_LANZA_IZQUIERDA, FRAMES_ATAQUE_LANZA,
                DURACION_ATAQUE / FRAMES_ATAQUE_LANZA);
            ataqueLanzaAlternoDerecha = cargarAnimacionDesdeRuta(
                RUTA_ATAQUE_LANZA_ALTERNO_DERECHA, FRAMES_ATAQUE_LANZA_ALTERNO, 0.08f);
            ataqueLanzaAlternoIzquierda = cargarAnimacionDesdeRuta(
                RUTA_ATAQUE_LANZA_ALTERNO_IZQUIERDA, FRAMES_ATAQUE_LANZA_ALTERNO, 0.08f);
        } else {
            caminarDerecha = cargarAnimacionDesdeRuta("jugador/caminar_derecha", 10, 0.08f);
            caminarIzquierda = cargarAnimacionDesdeRuta("jugador/caminar_izquierda", 10, 0.08f);
            saltoDerecha = cargarAnimacionDesdeRuta("jugador/salto_derecha", 13, 0.10f);
            saltoIzquierda = cargarAnimacionDesdeRuta("jugador/salto_izquierda", 13, 0.10f);
            ataqueDerecha = cargarAnimacionDesdeRuta(RUTA_ATAQUE_ESPADA_DERECHA, 12, 0.07f);
            ataqueIzquierda = cargarAnimacionDesdeRuta(RUTA_ATAQUE_ESPADA_IZQUIERDA, 12, 0.07f);
            ataqueLanzaAlternoDerecha = null;
            ataqueLanzaAlternoIzquierda = null;
        }
        escudo = cargarAnimacion("escudo", 8, 0.12f);
    }

    private void liberarImagenesAnimacion() {
        for (Texture imagen : imagenes) {
            imagen.dispose();
        }
        imagenes.clear();
    }

    // Cargar una animacion desde una carpeta.
    // Los archivos deben llamarse 01.png, 02.png, 03.png, etc.
    private Animation<TextureRegion> cargarAnimacion(String nombreDeCarpeta,
                                                     int cantidadDeImagenes,
                                                     float duracionDeCadaImagen) {
        return cargarAnimacionDesdeRuta(String.format("jugador/%s", nombreDeCarpeta),
            cantidadDeImagenes, duracionDeCadaImagen);
    }

    private Animation<TextureRegion> cargarAnimacionDesdeRuta(String rutaBase,
                                                            int cantidadDeImagenes,
                                                            float duracionDeCadaImagen) {
        TextureRegion[] frames = new TextureRegion[cantidadDeImagenes];

        for (int numero = 0; numero < cantidadDeImagenes; numero++) {
            String nombreDelArchivo = String.format("%s/%02d.png", rutaBase, numero + 1);

            Texture imagen = new Texture(Gdx.files.internal(nombreDelArchivo));
            imagenes.add(imagen);
            // Recortar las transparencias para que el personaje quede bien apoyado.
            frames[numero] = SpriteFrameUtils.recortarTransparencia(imagen, nombreDelArchivo);
        }

        return new Animation<TextureRegion>(duracionDeCadaImagen, frames);
    }

    // Liberar todas las imagenes y sonidos cuando se cierra la pantalla.
    public void dispose() {
        detenerSonidoCaminar();
        sonidoCaminar.dispose();
        liberarImagenesAnimacion();
        sonidoSalto.dispose();
        sonidoAtaque.dispose();
        sonidoEscudo.dispose();
    }
}
