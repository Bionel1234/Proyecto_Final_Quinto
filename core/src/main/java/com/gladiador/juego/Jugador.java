package com.gladiador.juego;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

// Esta clase representa al personaje Jorge.
// Aqui se controla su movimiento, salto, ataque y animaciones.
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

    // Lista con todas las imagenes cargadas.
    // Se usa para poder liberarlas al cerrar la pantalla.
    private ArrayList<Texture> imagenes = new ArrayList<Texture>();

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

    // Tiempo que lleva reproduciendose la animacion actual.
    private float tiempoAnimacion = 0f;

    // Tiempo que lleva realizando el ataque.
    private float tiempoAtaque = 0f;

    // Velocidad con la que Jorge camina.
    private static final float VELOCIDAD_CAMINAR = 260f;

    // Fuerza inicial del salto.
    private static final float FUERZA_SALTO = 720f;

    // Fuerza que hace caer a Jorge.
    private static final float GRAVEDAD = 1500f;

    // Tamaño normal de Jorge.
    private static final float ANCHO_PERSONAJE = 180f;
    private static final float ALTO_PERSONAJE = 180f;

    // Escalas necesarias porque los sprites no tienen todos el mismo espacio transparente.
    private static final float ESCALA_NORMAL = 1f;
    private static final float ESCALA_SALTO = 1.20f;
    private static final float ESCALA_ATAQUE = 1.35f;

    // Tiempo que dura un ataque.
    private static final float DURACION_ATAQUE = 0.75f;

    // Crear a Jorge en la posicion inicial recibida.
    public Jugador(float posicionInicialX, float posicionInicialY) {
        x = posicionInicialX;
        y = posicionInicialY;

        // Cargar las imagenes de todas las animaciones.
        cargarTodasLasAnimaciones();
    }

    // Este metodo se llama una vez por cada imagen del juego.
    public void actualizar(float delta, float posicionDelSuelo) {
        // Aumentar el tiempo de la animacion actual.
        tiempoAnimacion = tiempoAnimacion + delta;

        // Revisar si se pulso la tecla de ataque.
        revisarAtaque(delta);

        // Revisar movimiento y salto.
        revisarMovimiento(delta);

        // Aplicar la gravedad y apoyar los pies en el suelo.
        aplicarGravedad(delta, posicionDelSuelo);

        // Evitar que Jorge salga por los lados de la pantalla.
        limitarPosicionHorizontal();
    }

    // Leer las teclas A, D, W y flecha arriba.
    private void revisarMovimiento(float delta) {
        // Primero suponemos que Jorge esta quieto.
        velocidadX = 0f;

        // Si se mantiene presionada la tecla A, caminar hacia la izquierda.
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            velocidadX = -VELOCIDAD_CAMINAR;
            miraDerecha = false;
        }

        // Si se mantiene presionada la tecla D, caminar hacia la derecha.
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            velocidadX = VELOCIDAD_CAMINAR;
            miraDerecha = true;
        }

        // Moverse usando la velocidad y el tiempo de este frame.
        x = x + velocidadX * delta;

        // Detectar el inicio de un salto.
        boolean sePulsoSalto = Gdx.input.isKeyJustPressed(Input.Keys.W)
            || Gdx.input.isKeyJustPressed(Input.Keys.UP);

        // Solo se puede saltar si esta en el suelo y no esta atacando.
        if (sePulsoSalto && estaEnElSuelo && !estaAtacando) {
            velocidadY = FUERZA_SALTO;
            estaEnElSuelo = false;
            tiempoAnimacion = 0f;
        }
    }

    // Aplicar la gravedad y detectar cuando Jorge vuelve al suelo.
    private void aplicarGravedad(float delta, float posicionDelSuelo) {
        // La velocidad vertical disminuye por la gravedad.
        velocidadY = velocidadY - GRAVEDAD * delta;

        // Cambiar la posicion vertical.
        y = y + velocidadY * delta;

        // Si bajo del suelo, volver a colocarlo exactamente sobre el suelo.
        if (y <= posicionDelSuelo) {
            y = posicionDelSuelo;
            velocidadY = 0f;
            estaEnElSuelo = true;
        }
    }

    // Revisar el ataque con la tecla espacio o el boton izquierdo del mouse.
    private void revisarAtaque(float delta) {
        boolean sePulsoAtaque = Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
            || Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);

        // Comenzar un ataque solamente si no hay otro ataque activo.
        if (sePulsoAtaque && !estaAtacando) {
            estaAtacando = true;
            tiempoAtaque = 0f;
            tiempoAnimacion = 0f;
        }

        // Si esta atacando, contar el tiempo transcurrido.
        if (estaAtacando) {
            tiempoAtaque = tiempoAtaque + delta;
        }

        // Terminar el ataque cuando pasa su duracion.
        if (tiempoAtaque >= DURACION_ATAQUE) {
            estaAtacando = false;
            tiempoAtaque = 0f;
            tiempoAnimacion = 0f;
        }
    }

    // No permitir que Jorge salga del ancho de la pantalla.
    private void limitarPosicionHorizontal() {
        float posicionMaxima = JuegoScreen.ANCHO - ANCHO_PERSONAJE * ESCALA_ATAQUE;

        if (x < 0f) {
            x = 0f;
        }

        if (x > posicionMaxima) {
            x = posicionMaxima;
        }
    }

    // Dibujar el frame correcto de Jorge.
    public void dibujar(SpriteBatch batch) {
        // Obtener la imagen que corresponde al estado actual.
        TextureRegion imagenActual = obtenerImagenActual();

        // Elegir el tamaño segun la accion.
        float escalaActual = ESCALA_NORMAL;
        if (!estaEnElSuelo) {
            escalaActual = ESCALA_SALTO;
        }
        if (estaAtacando) {
            escalaActual = ESCALA_ATAQUE;
        }

        // Calcular el tamaño final de la imagen.
        float ancho = ANCHO_PERSONAJE * escalaActual;
        float alto = ALTO_PERSONAJE * escalaActual;

        // Centrar los sprites que tienen diferente espacio transparente.
        float posicionX = x - (ancho - ANCHO_PERSONAJE) / 2f;

        // Corregir el espacio transparente que queda debajo de algunos sprites.
        float espacioInferior = 30f;
        if (!estaEnElSuelo) {
            espacioInferior = 57f;
        }
        if (estaAtacando) {
            espacioInferior = 56f;
        }

        float posicionY = y - espacioInferior / 256f * alto;

        // Dibujar la imagen en pantalla.
        batch.draw(imagenActual, posicionX, posicionY, ancho, alto);
    }

    // Elegir una imagen segun la accion y la direccion.
    private TextureRegion obtenerImagenActual() {
        // Si esta atacando, devolver la animacion de ataque.
        if (estaAtacando) {
            if (miraDerecha) {
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

    // Cargar las seis animaciones de Jorge.
    private void cargarTodasLasAnimaciones() {
        caminarDerecha = cargarAnimacion("caminar_derecha", 10, 0.08f);
        caminarIzquierda = cargarAnimacion("caminar_izquierda", 10, 0.08f);
        saltoDerecha = cargarAnimacion("salto_derecha", 13, 0.10f);
        saltoIzquierda = cargarAnimacion("salto_izquierda", 13, 0.10f);
        ataqueDerecha = cargarAnimacion("ataque_derecha", 12, 0.07f);
        ataqueIzquierda = cargarAnimacion("ataque_izquierda", 12, 0.07f);
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
            frames[numero] = new TextureRegion(imagen);
        }

        // Crear y devolver la animacion.
        return new Animation<TextureRegion>(duracionDeCadaImagen, frames);
    }

    // Liberar todas las imagenes cuando se cierra la pantalla.
    public void dispose() {
        for (Texture imagen : imagenes) {
            imagen.dispose();
        }
    }
}
