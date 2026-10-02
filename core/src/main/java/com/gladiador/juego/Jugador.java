package com.gladiador.juego;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.Rectangle;

// Esta clase representa al personaje Jorge.
// Aqui se controla su movimiento, salto, ataque, escudo y animaciones.
public class Jugador {

    // Animacion para caminar hacia la derecha.
    private Animation<TextureRegion> caminarDerecha;

    // Animacion para caminar hacia la izquierda.
    private Animation<TextureRegion> caminarIzquierda;

    // Animacion para saltar hacia la derecha.
    private Animation<TextureRegion> saltoDerecha;

    // Animacion para saltar hacia la izquierda.
    private Animation<TextureRegion> saltoIzquierda;

    // Animacion para atacar hacia la derecha.
    private Animation<TextureRegion> ataqueDerecha;

    // Animacion para atacar hacia la izquierda.
    private Animation<TextureRegion> ataqueIzquierda;
    private Animation<TextureRegion> escudo;

    // Lista con todas las imagenes cargadas.
    // Se usa para poder liberarlas al cerrar la pantalla.
    private ArrayList<Texture> imagenes = new ArrayList<Texture>();
    private Sound sonidoSalto;
    private Sound sonidoAtaque;
    private Sound sonidoEscudo;

    // Posicion horizontal de Jorge.
    private float x;

    // Posicion vertical de los pies de Jorge.
    private float y;

    // Velocidad horizontal actual.
    private float velocidadX;

    // Velocidad vertical actual.
    private float velocidadY;

    // Indica hacia donde esta mirando Jorge.
    private boolean miraDerecha = true;

    // Indica si Jorge esta tocando el suelo.
    private boolean estaEnElSuelo = true;

    // Indica si Jorge esta atacando.
    private boolean estaAtacando = false;
    private boolean estaProtegiendo = false;

    // Tiempo que lleva reproduciendose la animacion actual.
    private float tiempoAnimacion = 0f;

    // Tiempo que lleva realizando el ataque.
    private float tiempoAtaque = 0f;
    private float tiempoEscudo = 0f;
    private long numeroAtaque;
    private boolean escudoMiraDerecha = true;

    // Direccion en la que se lanzo el ataque actual (no cambia a mitad del tajo).
    private boolean ataqueMiraDerecha = true;

    // Tiempo que falta para poder volver a usar el escudo.
    private float cooldownEscudo = 0f;

    // Tiempo restante sin poder recibir daño despues de un golpe.
    private float tiempoInvulnerable = 0f;

    // Tiempo restante en el que Jorge esta aturdido y no puede actuar.
    private float tiempoAturdido = 0f;

    // Empuje horizontal por golpes recibidos (se va apagando solo).
    private float velocidadEmpujeX = 0f;

    // Velocidad con la que Jorge camina.
    private static final float VELOCIDAD_CAMINAR = 260f;

    // Fuerza inicial del salto.
    private static final float FUERZA_SALTO = 720f;

    // Fuerza que hace caer a Jorge.
    private static final float GRAVEDAD = 1500f;

    // Tamaño normal de Jorge.
    private static final float ANCHO_PERSONAJE = 180f;
    private static final float ALTO_VISIBLE_PERSONAJE = 137f;

    // Ataque: 12 frames de 0.07s. El tajo real esta en los frames 5 a 7.
    private static final float DURACION_ATAQUE = 12 * 0.07f;
    private static final float ATAQUE_ACTIVO_DESDE = 0.28f;
    private static final float ATAQUE_ACTIVO_HASTA = 0.49f;
    // Pequeño paso hacia adelante al tajar, para que el golpe "pese".
    private static final float INICIO_IMPULSO = 0.14f;
    private static final float VELOCIDAD_IMPULSO = 150f;
    // Cuanto control de movimiento queda mientras se ataca.
    private static final float CONTROL_DURANTE_ATAQUE = 0.25f;

    // Escudo.
    private static final float DURACION_MINIMA_ESCUDO = 0.36f;
    private static final float VENTANA_PARRY = 0.20f;
    private static final float COOLDOWN_ESCUDO = 0.60f;

    // Daño recibido.
    private static final float DURACION_INVULNERABLE = 0.90f;
    private static final float DURACION_ATURDIMIENTO = 0.22f;
    private static final float EMPUJE_AL_RECIBIR_DANIO = 420f;
    private static final float EMPUJE_AL_BLOQUEAR = 180f;

    // Crear a Jorge en la posicion inicial recibida.
    public Jugador(float posicionInicialX, float posicionInicialY) {
        x = posicionInicialX;
        y = posicionInicialY;

        // Cargar las imagenes de todas las animaciones.
        cargarTodasLasAnimaciones();
        sonidoSalto = Gdx.audio.newSound(Gdx.files.internal("sonidos/sonido_salto.mp3"));
        sonidoAtaque = Gdx.audio.newSound(Gdx.files.internal("sonidos/sonido_ataque.mp3"));
        sonidoEscudo = Gdx.audio.newSound(Gdx.files.internal("sonidos/sonido_escudo.mp3"));
    }

    // Este metodo se llama una vez por cada imagen del juego.
    public void actualizar(float delta, float posicionDelSuelo) {
        actualizar(delta, posicionDelSuelo, JuegoScreen.ANCHO - ANCHO_PERSONAJE);
    }

    public void actualizar(float delta, float posicionDelSuelo, float limiteMaximoX) {
        actualizar(delta, posicionDelSuelo, limiteMaximoX, Collections.<Rectangle>emptyList());
    }

    public void actualizar(
        float delta,
        float posicionDelSuelo,
        float limiteMaximoX,
        List<Rectangle> plataformas
    ) {
        // Aumentar el tiempo de la animacion actual.
        tiempoAnimacion = tiempoAnimacion + delta;

        if (tiempoInvulnerable > 0f) {
            tiempoInvulnerable = Math.max(0f, tiempoInvulnerable - delta);
        }

        if (tiempoAturdido > 0f) {
            // Aturdido: no se puede atacar, protegerse ni caminar.
            tiempoAturdido = Math.max(0f, tiempoAturdido - delta);
            velocidadX = 0f;
        } else {
            actualizarEscudo(delta);

            // Revisar si se pulso la tecla de ataque.
            revisarAtaque(delta);

            // Revisar movimiento y salto.
            revisarMovimiento(delta);
        }

        aplicarEmpuje(delta);

        // Aplicar la gravedad y apoyar los pies en el suelo.
        aplicarGravedad(delta, posicionDelSuelo, plataformas);

        // Evitar que Jorge salga por los lados del mundo.
        limitarPosicionHorizontal(limiteMaximoX);
    }

    // Leer las teclas A, D, W y flecha arriba.
    private void revisarMovimiento(float delta) {
        // Primero suponemos que Jorge esta quieto.
        velocidadX = 0f;
        if (estaProtegiendo) {
            return;
        }

        float direccion = 0f;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            direccion = -1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            direccion = 1f;
        }

        if (estaAtacando) {
            // Durante el ataque Jorge no gira y casi no se puede mover,
            // pero el tajo lo empuja un poco hacia adelante.
            velocidadX = direccion * VELOCIDAD_CAMINAR * CONTROL_DURANTE_ATAQUE;
            if (tiempoAtaque >= INICIO_IMPULSO && tiempoAtaque <= ATAQUE_ACTIVO_HASTA) {
                velocidadX += (ataqueMiraDerecha ? 1f : -1f) * VELOCIDAD_IMPULSO;
            }
        } else if (direccion != 0f) {
            velocidadX = direccion * VELOCIDAD_CAMINAR;
            miraDerecha = direccion > 0f;
        }

        // Moverse usando la velocidad y el tiempo de este frame.
        x = x + velocidadX * delta;

        // Detectar el inicio de un salto.
        boolean sePulsoSalto = Gdx.input.isKeyJustPressed(Input.Keys.W)
            || Gdx.input.isKeyJustPressed(Input.Keys.UP);

        // Solo se puede saltar si esta en el suelo y no esta atacando.
        if (sePulsoSalto && estaEnElSuelo && !estaAtacando && !estaProtegiendo) {
            velocidadY = FUERZA_SALTO;
            estaEnElSuelo = false;
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
        boolean puedeCancelarAtaque = !estaAtacando || tiempoAtaque >= ATAQUE_ACTIVO_HASTA;
        if (inicioEscudo && cooldownEscudo <= 0f && puedeCancelarAtaque) {
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

    // Aplicar la gravedad y detectar cuando Jorge vuelve al suelo.
    private void aplicarGravedad(float delta, float posicionDelSuelo, List<Rectangle> plataformas) {
        float posicionAnteriorY = y;
        velocidadY = velocidadY - GRAVEDAD * delta;
        y = y + velocidadY * delta;

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
                    return;
                }
            }
        }

        if (y <= posicionDelSuelo) {
            y = posicionDelSuelo;
            velocidadY = 0f;
            estaEnElSuelo = true;
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
        boolean sePulsoAtaque = Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
            || Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);

        if (sePulsoAtaque && !estaProtegiendo && !estaAtacando) {
            iniciarAtaque();
        }

        if (estaAtacando) {
            tiempoAtaque = tiempoAtaque + delta;
            if (tiempoAtaque >= DURACION_ATAQUE) {
                estaAtacando = false;
                tiempoAtaque = 0f;
                tiempoAnimacion = 0f;
            }
        }
    }

    // Empezar un tajo individual; cada nueva pulsacion inicia otro ataque.
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

        estaAtacando = true;
        tiempoAtaque = 0f;
        numeroAtaque++;
        tiempoAnimacion = 0f;
        sonidoAtaque.play();
    }

    // --- Daño y defensa ---

    // Jorge recibe un golpe sin defensa. direccionEmpuje: 1 = lo empuja a la derecha, -1 a la izquierda.
    public void recibirImpacto(float direccionEmpuje) {
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

    public boolean estaEnVentanaParry() {
        return estaProtegiendo && tiempoEscudo <= VENTANA_PARRY;
    }

    public boolean esInvulnerable() {
        return tiempoInvulnerable > 0f;
    }

    // --- Consultas ---

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getCentroX() {
        return x + ANCHO_PERSONAJE / 2f;
    }

    public boolean estaAtacando() {
        return estaAtacando;
    }

    // true solo durante los frames en los que la espada realmente corta.
    public boolean estaAtaqueActivo() {
        return estaAtacando
            && tiempoAtaque >= ATAQUE_ACTIVO_DESDE
            && tiempoAtaque <= ATAQUE_ACTIVO_HASTA;
    }

    public long getNumeroAtaque() {
        return numeroAtaque;
    }

    // 1 si el tajo actual va hacia la derecha, -1 si va hacia la izquierda.
    public float getDireccionAtaque() {
        return ataqueMiraDerecha ? 1f : -1f;
    }

    public boolean estaProtegiendo() {
        return estaProtegiendo;
    }

    public boolean miraDerecha() {
        return miraDerecha;
    }

    public com.badlogic.gdx.math.Rectangle getHitbox() {
        return new com.badlogic.gdx.math.Rectangle(x + 55f, y + 5f, 70f, 115f);
    }

    // El area se mide desde el centro de Jorge, igual de larga hacia los dos lados.
    public com.badlogic.gdx.math.Rectangle getAreaDeAtaque() {
        float ancho = 130f;
        float alto = 100f;
        boolean haciaDerecha = estaAtacando ? ataqueMiraDerecha : miraDerecha;
        float centro = x + ANCHO_PERSONAJE / 2f;
        float xAtaque = haciaDerecha ? centro + 20f : centro - 20f - ancho;
        float yAtaque = y + 25f;
        return new com.badlogic.gdx.math.Rectangle(xAtaque, yAtaque, ancho, alto);
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

    // Dibujar el frame correcto de Jorge.
    public void dibujar(SpriteBatch batch) {
        dibujar(batch, 0f);
    }

    public void dibujar(SpriteBatch batch, float desplazamientoX) {
        // Obtener la imagen que corresponde al estado actual.
        TextureRegion imagenActual = obtenerImagenActual();

        float escala = ALTO_VISIBLE_PERSONAJE / imagenActual.getRegionHeight();
        float ancho = imagenActual.getRegionWidth() * escala;
        float alto = ALTO_VISIBLE_PERSONAJE;
        float posicionX = x - desplazamientoX + (ANCHO_PERSONAJE - ancho) / 2f;
        float posicionY = y;
        boolean voltearEscudo = estaProtegiendo && !escudoMiraDerecha;
        if (voltearEscudo) {
            posicionX += ancho;
        }

        // Colores de aviso: rojo al ser golpeado, parpadeo al ser invulnerable,
        // celeste mientras el parry esta activo.
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
        batch.setColor(1f, 1f, 1f, 1f);
    }

    // Elegir una imagen segun la accion y la direccion.
    private TextureRegion obtenerImagenActual() {
        if (estaProtegiendo) {
            return escudo.getKeyFrame(tiempoEscudo, false);
        }

        // Si esta atacando, devolver la animacion de ataque (siempre hacia donde empezo el tajo).
        if (estaAtacando) {
            if (ataqueMiraDerecha) {
                return ataqueDerecha.getKeyFrame(tiempoAtaque, false);
            }
            return ataqueIzquierda.getKeyFrame(tiempoAtaque, false);
        }

        // Si esta en el aire, devolver la animacion de salto.
        if (!estaEnElSuelo) {
            if (miraDerecha) {
                return saltoDerecha.getKeyFrame(tiempoAnimacion, false);
            }
            return saltoIzquierda.getKeyFrame(tiempoAnimacion, false);
        }

        // Si se mueve, devolver la animacion de caminar.
        if (velocidadX != 0f) {
            if (miraDerecha) {
                return caminarDerecha.getKeyFrame(tiempoAnimacion, true);
            }
            return caminarIzquierda.getKeyFrame(tiempoAnimacion, true);
        }

        // Si esta quieto, mostrar el primer frame de caminar.
        if (miraDerecha) {
            return caminarDerecha.getKeyFrame(0f);
        }
        return caminarIzquierda.getKeyFrame(0f);
    }

    // Cargar las animaciones de Jorge.
    private void cargarTodasLasAnimaciones() {
        caminarDerecha = cargarAnimacion("caminar_derecha", 10, 0.08f);
        caminarIzquierda = cargarAnimacion("caminar_izquierda", 10, 0.08f);
        saltoDerecha = cargarAnimacion("salto_derecha", 13, 0.10f);
        saltoIzquierda = cargarAnimacion("salto_izquierda", 13, 0.10f);
        ataqueDerecha = cargarAnimacion("ataque_derecha", 12, 0.07f);
        ataqueIzquierda = cargarAnimacion("ataque_izquierda", 12, 0.07f);
        escudo = cargarAnimacion("escudo", 8, 0.12f);
    }

    // Cargar una animacion desde una carpeta.
    // Los archivos deben llamarse 01.png, 02.png, 03.png, etc.
    private Animation<TextureRegion> cargarAnimacion(
        String nombreDeCarpeta,
        int cantidadDeImagenes,
        float duracionDeCadaImagen
    ) {
        // Crear un arreglo para guardar los frames.
        TextureRegion[] frames = new TextureRegion[cantidadDeImagenes];

        // Cargar cada archivo de imagen.
        for (int numero = 0; numero < cantidadDeImagenes; numero++) {
            String nombreDelArchivo = String.format(
                "jugador/%s/%02d.png",
                nombreDeCarpeta,
                numero + 1
            );

            Texture imagen = new Texture(Gdx.files.internal(nombreDelArchivo));
            imagenes.add(imagen);
            frames[numero] = SpriteFrameUtils.recortarTransparencia(imagen, nombreDelArchivo);
        }

        // Crear y devolver la animacion.
        return new Animation<TextureRegion>(duracionDeCadaImagen, frames);
    }

    // Liberar todas las imagenes cuando se cierra la pantalla.
    public void dispose() {
        for (Texture imagen : imagenes) {
            imagen.dispose();
        }
        if (sonidoSalto != null) {
            sonidoSalto.dispose();
        }
        if (sonidoAtaque != null) {
            sonidoAtaque.dispose();
        }
        if (sonidoEscudo != null) {
            sonidoEscudo.dispose();
        }
    }
}
