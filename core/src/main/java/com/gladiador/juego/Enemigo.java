package com.gladiador.juego;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;

// Enemigo comun de las zonas.
// Hace dos cosas: patrulla cuando no ve a Jorge, y lo persigue y ataca cuando lo ve.
public class Enemigo {

    // Cada tipo de enemigo cambia su vida, velocidad, dano y tiempos de ataque.
    // Los numeros entre parentesis son los valores de ese tipo.
    public enum TipoEnemigo {
        BASICO(2, 1.10f, 1, 0.18f, 0.42f, 34f, 170f),
        AGIL(2, 1.28f, 1, 0.12f, 0.32f, 42f, 185f),
        BRUTO(4, 0.94f, 2, 0.20f, 0.48f, 28f, 160f),
        ELITE(5, 1.18f, 3, 0.14f, 0.36f, 46f, 200f);

        public final int vidaMaxima;        // golpes que aguanta
        public final float multiplicador;   // por cuanto se multiplica su velocidad base
        public final int dano;              // vida que le quita a Jorge
        public final float esperaMin;       // tiempo minimo "cargando" antes del tajo
        public final float esperaMax;       // tiempo maximo "cargando" antes del tajo
        public final float alturaAtaque;    // diferencia de altura maxima para que el tajo llegue
        public final float rangoAtaque;     // distancia a la que empieza a atacar

        TipoEnemigo(int vidaMaxima, float multiplicador, int dano,
                    float esperaMin, float esperaMax, float alturaAtaque, float rangoAtaque) {
            this.vidaMaxima = vidaMaxima;
            this.multiplicador = multiplicador;
            this.dano = dano;
            this.esperaMin = esperaMin;
            this.esperaMax = esperaMax;
            this.alturaAtaque = alturaAtaque;
            this.rangoAtaque = rangoAtaque;
        }
    }

    // --- Medidas y tiempos fijos (son las "reglas" del enemigo) ---
    private static final float ANCHO_ENEMIGO = 220f;         // ancho del "cuadro" del sprite
    private static final float ALTO_VISIBLE_ENEMIGO = 130f;  // alto real en pantalla
    private static final float VELOCIDAD_BASE = 105f;        // velocidad base de todos
    private static final float RANGO_VISION = 540f;          // a partir de aqui "ve" a Jorge
    private static final float SEPARACION_OBJETIVOS = 110f;  // espacio que guardan entre ellos
    // Distancia (centro a centro) desde la que empieza a atacar. Es menor que lo
    // que alcanza su golpe (unos 170) para que no ataque al aire.
    private static final float DISTANCIA_OBJETIVO = 120f;
    // Si Jorge se le pega demasiado mientras recarga, el enemigo retrocede.
    private static final float DISTANCIA_RETROCESO = 90f;
    private static final float COOLDOWN_ATAQUE = 0.85f;      // espera tras cada ataque
    private static final float DURACION_ATAQUE = 15 * 0.07f; // 15 frames de animacion
    private static final float DURACION_ATAQUE_ESPADA_ZONA_DOS = 13 * 0.07f;
    private static final float DURACION_LANZAMIENTO_PIEDRA = 12 * 0.07f;
    private static final float DURACION_ATAQUE_CRIATURA = 8 * 0.08f;
    private static final float RANGO_LANZAMIENTO_PIEDRA = 470f;
    private static final float ALTURA_LANZAMIENTO_PIEDRA = 320f;
    private static final float COOLDOWN_LANZAMIENTO_PIEDRA = 2.1f;
    // El golpe hace dano durante una ventana corta (2 frames), no en un solo instante.
    private static final float MOMENTO_IMPACTO = 0.34f;
    private static final float MOMENTO_LANZAMIENTO_PIEDRA = 7 * 0.07f;
    private static final float MOMENTO_ROCA_CRIATURA = 4 * 0.08f;
    private static final float VENTANA_IMPACTO = 0.18f;
    private static final float DURACION_ATURDIMIENTO = 0.35f; // al recibir un golpe
    private static final float DURACION_MUERTE = 0.50f;       // desvanecerse al morir

    // --- Datos del enemigo ---
    private final TipoEnemigo tipo;
    private final boolean zonaDos;
    private final boolean zonaDosNivelUno;
    private final boolean criaturaAlcantarillas;
    private final float duracionAtaque;
    private final float momentoLanzamiento;
    private final float velocidad;         // velocidad ya multiplicada por la del tipo
    private int vida;                      // vida actual (empieza llena)

    // Animaciones: cada una es una secuencia de imagenes que se van mostrando solas.
    private final Animation<TextureRegion> caminarDerecha;
    private final Animation<TextureRegion> caminarIzquierda;
    private final Animation<TextureRegion> atacarDerecha;
    private final Animation<TextureRegion> atacarIzquierda;
    private final Animation<TextureRegion> atacarEspadaDerecha;
    private final Animation<TextureRegion> atacarEspadaIzquierda;
    // Todas las imagenes cargadas, para poder liberarlas al cerrar el nivel.
    private final ArrayList<Texture> imagenes = new ArrayList<>();

    // Area visible que ocupan los dibujos de CAMINAR (sin transparencias), uniendo
    // todos sus frames. Medimos UNA sola area para todas las animaciones: si
    // midieramos frame por frame, el enemigo cambiaria de tamano y de altura en
    // cada imagen y pareceria que "salta" mientras camina.
    private int unionMinX = Integer.MAX_VALUE;
    private int unionMinY = Integer.MAX_VALUE;
    private int unionMaxX = -1;
    private int unionMaxY = -1;
    private int ataqueUnionMinX = Integer.MAX_VALUE;
    private int ataqueUnionMaxX = -1;
    private int ataqueUnionMaxY = -1;
    private float escalaSprite;    // tamano fijo del dibujo en pantalla
    private float centroVisibleX;  // centro horizontal fijo del dibujo

    private float x;
    private float y;
    private boolean miraDerecha = false;

    // --- "Estados": cosas que el enemigo esta haciendo en este momento ---
    private boolean patrullandoDerecha = true; // para donde camina cuando esta libre
    private float patrolMinX = 0f;             // borde izquierda de su zona de paseo
    private float patrolMaxX = Float.MAX_VALUE; // borde derecha de su zona de paseo
    private boolean atacando = false;
    private boolean atacandoConEspada = false;
    private boolean preparandoAtaque = false;  // esta "cargando" el tajo
    private boolean impactoResuelto = false;   // el tajo actual ya pego (o lo bloquearon)
    private float esperaAtaque = 0f;           // cuanto falta para descargar el tajo
    private float tiempoAturdido = 0f;         // segundos que tarda en reaccionar
    private float tiempoRetrocesoChoque;
    private float direccionRetrocesoChoque;
    private float tiempoDanio = 0f;            // para parpadear en rojo al recibir golpe
    private float tiempoMuerte = 0f;           // progreso de la animacion de muerte
    private float velocidadEmpujeX = 0f;       // empuje que le dan los golpes de Jorge
    private long ultimoAtaqueRecibido = -1L;   // numero del ultimo golpe que le conecto

    // Timers de las animaciones.
    private float tiempoAnimacion = 0f;
    private float tiempoAnimacionAtaque = 0f;
    private float cooldownAtaque = 0f;

    // Crear un enemigo en (x, y) del tipo indicado, para la zona indicada.
    // La "zona" cambia las imagenes; todos los enemigos pueden defenderse con una pose fija.
    public Enemigo(float x, float y, float velocidadBase, TipoEnemigo tipo, String zona) {
        this(x, y, velocidadBase, tipo, zona, false);
    }

    public Enemigo(float x, float y, float velocidadBase, TipoEnemigo tipo, String zona,
                   boolean criaturaAlcantarillas) {
        this.tipo = tipo;
        zonaDos = !"Zona 1".equalsIgnoreCase(zona);
        zonaDosNivelUno = zonaDos && !criaturaAlcantarillas;
        this.criaturaAlcantarillas = zonaDos && criaturaAlcantarillas;
        duracionAtaque = this.criaturaAlcantarillas
            ? DURACION_ATAQUE_CRIATURA
            : zonaDos ? DURACION_LANZAMIENTO_PIEDRA : DURACION_ATAQUE;
        momentoLanzamiento = this.criaturaAlcantarillas
            ? MOMENTO_ROCA_CRIATURA
            : zonaDos ? MOMENTO_LANZAMIENTO_PIEDRA : MOMENTO_IMPACTO;
        this.vida = tipo.vidaMaxima;
        this.velocidad = velocidadBase * tipo.multiplicador;
        this.x = x;
        this.y = y;

        if (this.criaturaAlcantarillas) {
            caminarDerecha = cargarAnimacion(
                "Criatura alcantarillas/Caminar derecha", 13, 0.08f, true);
            caminarIzquierda = cargarAnimacion(
                "Criatura alcantarillas/Caminar izquierda", 13, 0.08f, true);
            atacarDerecha = cargarAnimacion(
                "Criatura alcantarillas/Ataque derecha", 8, 0.08f, false, true);
            atacarIzquierda = cargarAnimacion(
                "Criatura alcantarillas/Ataque izquierda", 8, 0.08f, false, true);
            atacarEspadaDerecha = null;
            atacarEspadaIzquierda = null;
        } else if ("Zona 1".equalsIgnoreCase(zona)) {
            caminarDerecha = cargarAnimacion("Enemigo 1 movimientos/Caminar derecha", 10, 0.08f, true);
            caminarIzquierda = cargarAnimacion("Enemigo 1 movimientos/Caminar izquierda sin fondo", 10, 0.08f, true);
            atacarDerecha = cargarAnimacion("Enemigo 1 movimientos/Atacar derecha", 15, 0.07f, false);
            atacarIzquierda = cargarAnimacion("Enemigo 1 movimientos/Atacar izquierda", 15, 0.07f, false);
            atacarEspadaDerecha = null;
            atacarEspadaIzquierda = null;
        } else {
            // La defensa usa un frame quieto para mantener siempre el mismo tamano.
            caminarDerecha = cargarAnimacion("Enemigo1 zona2 movimientos/caminar derecha", 11, 0.08f, true);
            caminarIzquierda = cargarAnimacion("Enemigo1 zona2 movimientos/caminar izquierda", 11, 0.08f, true);
            atacarDerecha = cargarAnimacion(
                "Enemigo1 zona2 movimientos/lanzar piedra derecha", 12, 0.07f, false);
            atacarIzquierda = cargarAnimacion(
                "Enemigo1 zona2 movimientos/lanzar piedra izquierda", 12, 0.07f, false);
            atacarEspadaDerecha = cargarAnimacion(
                "Enemigo1 zona2 movimientos/atacando derecha", 13, 0.07f, false);
            atacarEspadaIzquierda = cargarAnimacion(
                "Enemigo1 zona2 movimientos/atacando izquierda", 13, 0.07f, false);
        }

        calcularMedidasSprite();
    }

    // Con el area visible de caminar calculamos un tamano y una posicion UNICOS
    // para todas las animaciones. Asi el enemigo nunca cambia de altura ni "salta".
    private void calcularMedidasSprite() {
        float altoUnion = unionMaxY - unionMinY + 1f;
        escalaSprite = ALTO_VISIBLE_ENEMIGO / altoUnion;
        centroVisibleX = (unionMinX + unionMaxX + 1f) / 2f;
    }

    // Este metodo se llama una vez por frame desde MapaScreen.
    // "objetivoX/objetivoY" es donde esta Jorge y los limites hasta donde puede caminar.
    public void actualizar(float delta, float objetivoX, float objetivoY,
                           float limiteIzquierdo, float limiteDerecho, float desplazamientoObjetivoX) {
        patrolMinX = limiteIzquierdo + 60f;
        patrolMaxX = Math.max(patrolMinX, limiteDerecho - ANCHO_ENEMIGO);

        // Si esta muerto, solo termina de desvanecerse y ya no hace nada.
        if (!estaVivo()) {
            tiempoMuerte += delta;
            aplicarEmpuje(delta);
            return;
        }

        aplicarEmpuje(delta);
        if (tiempoDanio > 0f) {
            tiempoDanio = Math.max(0f, tiempoDanio - delta);
        }
        // Aturdido: congelado, no puede hacer nada hasta recuperarse.
        if (tiempoAturdido > 0f) {
            tiempoAturdido = Math.max(0f, tiempoAturdido - delta);
            return;
        }

        if (tiempoRetrocesoChoque > 0f) {
            tiempoRetrocesoChoque = Math.max(0f, tiempoRetrocesoChoque - delta);
            miraDerecha = direccionRetrocesoChoque > 0f;
            x += direccionRetrocesoChoque * velocidad * delta;
            limitarX();
            tiempoAnimacion += delta;
            return;
        }

        if (!atacando && cooldownAtaque > 0f) {
            cooldownAtaque = Math.max(0f, cooldownAtaque - delta);
        }

        // Distancias respecto a Jorge (desplazamientoObjetivoX acomoda la fila de enemigos).
        float centroEnemigoX = x + ANCHO_ENEMIGO / 2f;
        float centroObjetivoX = objetivoX + 90f + desplazamientoObjetivoX;
        float distanciaHorizontal = centroObjetivoX - centroEnemigoX;
        float distanciaJugadorHorizontal = objetivoX + 90f - centroEnemigoX;
        float distanciaVertical = objetivoY - y;
        boolean enVision = Math.abs(distanciaJugadorHorizontal) <= RANGO_VISION;

        // 1) Si esta atacando, la animacion sigue sola y no se puede interrumpir.
        if (atacando) {
            tiempoAnimacionAtaque += delta;
            if (tiempoAnimacionAtaque >= obtenerDuracionAtaqueActual()) {
                boolean terminoAtaqueEspada = atacandoConEspada;
                atacando = false;
                atacandoConEspada = false;
                tiempoAnimacionAtaque = 0f;
                tiempoAnimacion = 0f;
                float esperaBase = zonaDos && !terminoAtaqueEspada
                    ? COOLDOWN_LANZAMIENTO_PIEDRA : COOLDOWN_ATAQUE;
                cooldownAtaque = esperaBase * tipo.multiplicador * MathUtils.random(0.8f, 1.35f);
            }
            return;
        }

        // Mirar siempre hacia Jorge.
        if (distanciaHorizontal != 0f) {
            miraDerecha = distanciaHorizontal > 0f;
        }

        // 3) Si no ve a Jorge, pasea de un lado a otro.
        if (!enVision) {
            preparandoAtaque = false;
            atacandoConEspada = false;
            patrullar(delta);
            return;
        }

        // 4) Si Jorge esta a su alcance, prepara y lanza el ataque.
        boolean ataqueEspada = zonaDosNivelUno
            && Math.abs(distanciaJugadorHorizontal) <= tipo.rangoAtaque;
        if (preparandoAtaque && atacandoConEspada != ataqueEspada) {
            atacandoConEspada = ataqueEspada;
            esperaAtaque = MathUtils.random(tipo.esperaMin, tipo.esperaMax);
        }
        float rangoAtaque = zonaDos && !ataqueEspada
            ? RANGO_LANZAMIENTO_PIEDRA : tipo.rangoAtaque;
        float alturaAtaque = zonaDos && !ataqueEspada
            ? ALTURA_LANZAMIENTO_PIEDRA : tipo.alturaAtaque;
        if (Math.abs(distanciaVertical) <= alturaAtaque
                && Math.abs(distanciaJugadorHorizontal) <= rangoAtaque) {
            if (puedeAtacar()) {
                if (!preparandoAtaque) {
                    // Primero "carga" un tiempo random, para que no sea siempre igual.
                    preparandoAtaque = true;
                    atacandoConEspada = ataqueEspada;
                    esperaAtaque = MathUtils.random(tipo.esperaMin, tipo.esperaMax);
                }
                esperaAtaque -= delta;
                if (esperaAtaque <= 0f) {
                    preparandoAtaque = false;
                    atacando = true;
                    impactoResuelto = false;
                    tiempoAnimacionAtaque = 0f;
                }
            } else if (!zonaDos && Math.abs(distanciaHorizontal) < DISTANCIA_RETROCESO) {
                // Si esta recargando y Jorge se le pega, retrocede un poco.
                x -= Math.signum(distanciaHorizontal) * velocidad * 0.6f * delta;
                limitarX();
                tiempoAnimacion += delta;
            }
            return;
        }

        // 5) Ve a Jorge pero esta lejos: camina hacia el hasta la distancia de ataque.
        preparandoAtaque = false;
        atacandoConEspada = false;
        float distancia = Math.abs(distanciaHorizontal);
        if (distancia > DISTANCIA_OBJETIVO) {
            float distanciaARecorrer = distancia - DISTANCIA_OBJETIVO;
            float avance = Math.min(velocidad * delta, distanciaARecorrer);
            x += Math.signum(distanciaHorizontal) * avance;
        }
        tiempoAnimacion += delta;
    }

    // Caminar de un lado a otro dentro de sus limites.
    private void patrullar(float delta) {
        if (x <= patrolMinX) {
            x = patrolMinX;
            patrullandoDerecha = true;
        } else if (x >= patrolMaxX) {
            x = patrolMaxX;
            patrullandoDerecha = false;
        }

        miraDerecha = patrullandoDerecha;
        float distancia = velocidad * 0.5f * delta; // patrulla a mitad de velocidad
        if (patrullandoDerecha) {
            x += distancia;
            if (x >= patrolMaxX) {
                x = patrolMaxX;
                patrullandoDerecha = false;
            }
        } else {
            x -= distancia;
            if (x <= patrolMinX) {
                x = patrolMinX;
                patrullandoDerecha = true;
            }
        }
        tiempoAnimacion += delta;
    }

    // Empuje de los golpes recibidos: mueve al enemigo y se apaga solo.
    private void aplicarEmpuje(float delta) {
        if (velocidadEmpujeX == 0f) {
            return;
        }
        x += velocidadEmpujeX * delta;
        velocidadEmpujeX *= Math.max(0f, 1f - 8f * delta);
        if (Math.abs(velocidadEmpujeX) < 5f) {
            velocidadEmpujeX = 0f;
        }
        limitarX();
    }

    // No dejar que el enemigo salga de su zona de paseo.
    private void limitarX() {
        x = Math.max(patrolMinX, Math.min(x, patrolMaxX));
    }

    // Espacio ideal entre enemigos (lo usa MapaScreen para acomodarlos).
    public static float separacionObjetivos() {
        return SEPARACION_OBJETIVOS;
    }

    // Mover al enemigo para que no se superponga con otro.
    public void aplicarSeparacion(float desplazamientoX) {
        x += desplazamientoX;
        limitarX();
    }

    public void reaccionarAlChoque(float direccion) {
        direccionRetrocesoChoque = Math.signum(direccion);
        tiempoRetrocesoChoque = 0.22f;
        atacando = false;
        atacandoConEspada = false;
        preparandoAtaque = false;
        tiempoAnimacionAtaque = 0f;
    }

    // Cortar cualquier accion de ataque/defensa que estuviera haciendo.
    private void cancelarAtaque() {
        atacando = false;
        atacandoConEspada = false;
        impactoResuelto = false;
        tiempoAnimacionAtaque = 0f;
        preparandoAtaque = false;
        cooldownAtaque = Math.max(cooldownAtaque, 0.5f);
    }

    // Puedo atacar si estoy vivo, libre y sin espera pendiente.
    public boolean puedeAtacar() {
        return estaVivo() && !atacando && cooldownAtaque <= 0f;
    }

    // true durante los 2 frames en los que el golpe puede hacer dano.
    public boolean impactoActivo() {
        return estaVivo()
            && atacando
            && !impactoResuelto
            && tiempoAnimacionAtaque >= obtenerMomentoImpactoActual()
            && tiempoAnimacionAtaque <= obtenerMomentoImpactoActual() + VENTANA_IMPACTO;
    }

    // Avisar que el golpe ya conecto (o fue bloqueado) para que no pegue dos veces.
    public void resolverImpacto() {
        impactoResuelto = true;
    }

    // Cuanta vida le quita a Jorge el golpe de este enemigo.
    public int getDano() {
        return tipo.dano;
    }

    // Altura maxima a la que puede golpear (la usa MapaScreen para verificar el salto).
    public float getAlturaAtaqueMax() {
        return tipo.alturaAtaque;
    }

    public boolean esZonaDos() {
        return zonaDos;
    }

    public boolean esAtaqueDeLanzamientoPiedra() {
        return zonaDos && !(zonaDosNivelUno && atacandoConEspada);
    }

    private float obtenerDuracionAtaqueActual() {
        return zonaDosNivelUno && atacandoConEspada
            ? DURACION_ATAQUE_ESPADA_ZONA_DOS : duracionAtaque;
    }

    private float obtenerMomentoImpactoActual() {
        return zonaDosNivelUno && atacandoConEspada ? MOMENTO_IMPACTO : momentoLanzamiento;
    }

    // Jorge le pego. Devuelve true si el golpe registro.
    // numeroAtaque evita que UN mismo tajo haga dano en mas de un frame.
    // direccionEmpuje: 1 empuja a la derecha, -1 a la izquierda.
    public boolean recibirGolpe(long numeroAtaque, float direccionEmpuje, float fuerzaEmpuje, int dano) {
        if (!estaVivo() || numeroAtaque == ultimoAtaqueRecibido) {
            return false;
        }
        if (dano < 1) {
            throw new IllegalArgumentException("El dano debe ser al menos 1.");
        }

        ultimoAtaqueRecibido = numeroAtaque;

        // Recibe el dano.
        vida = Math.max(0, vida - dano);
        tiempoDanio = 0.25f;
        velocidadEmpujeX = direccionEmpuje * fuerzaEmpuje;
        if (estaVivo()) {
            // El golpe corta su ataque y lo deja aturdido un momento.
            cancelarAtaque();
            tiempoAturdido = DURACION_ATURDIMIENTO;
        } else {
            tiempoMuerte = 0f; // recien ahora empieza a desvanecerse
        }
        return true;
    }

    // Aturdimiento largo (lo usa el parry de Jorge).
    // Interrumpe al enemigo durante el tiempo indicado y aplica un empujon.
    public void aturdir(float segundos, float direccionEmpuje, float fuerzaEmpuje) {
        if (!estaVivo()) {
            return;
        }
        cancelarAtaque();
        tiempoAturdido = segundos;
        velocidadEmpujeX = direccionEmpuje * fuerzaEmpuje;
    }

    // Consultas de estado, posicion y alcance utilizadas por MapaScreen.
    public boolean estaVivo() {
        return vida > 0;
    }

    // true cuando ya termino de desvanecerse y lo podemos sacar de la lista.
    public boolean puedeEliminarse() {
        return !estaVivo() && tiempoMuerte >= DURACION_MUERTE;
    }

    // Caja del cuerpo: lo que ocupan los golpes de Jorge.
    public Rectangle getHitbox() {
        return new Rectangle(x + 75f, y + 10f, 70f, 95f);
    }

    // Caja del golpe: la espada sale por el lado al que mira.
    // Mirando a la izquierda cubre [x-25, x+75]; mirando a la derecha [x+145, x+245].
    public Rectangle getAreaDeAtaque() {
        float xAtaque = miraDerecha ? x + 145f : x - 25f;
        return new Rectangle(xAtaque, y + 10f, 100f, 95f);
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getCentroX() {
        return x + ANCHO_ENEMIGO / 2f;
    }

    // Dibujar al enemigo con la animacion que corresponde a lo que esta haciendo.
    public void dibujar(SpriteBatch batch, float desplazamientoX) {
        if (puedeEliminarse()) {
            return;
        }
        TextureRegion frameActual = mirarFrameActual();

        // El tamano y la posicion son FIJOS (se calcularon al cargar las imagenes),
        // para que el enemigo no cambie de altura ni "salte" entre frames.
        boolean ataqueCriatura = criaturaAlcantarillas && atacando;
        float centroVisible = ataqueCriatura
            ? (ataqueUnionMinX + ataqueUnionMaxX + 1f) / 2f
            : centroVisibleX;
        float maxVisibleY = ataqueCriatura ? ataqueUnionMaxY : unionMaxY;
        float posicionX = x - desplazamientoX + ANCHO_ENEMIGO / 2f - centroVisible * escalaSprite;
        float posicionY = y - (frameActual.getRegionHeight() - maxVisibleY - 1f) * escalaSprite;
        float tamanoAncho = frameActual.getRegionWidth() * escalaSprite;
        float tamanoAlto = frameActual.getRegionHeight() * escalaSprite;

        // Colores de aviso:
        // - al morir se pone oscuro y transparente
        // - rojizo al recibir un golpe
        // - violeta cuando esta aturdido (es el momento de castigarlo)
        // - anaranjado mientras prepara el tajo (para que Jorge pueda reaccionar)
        float rojo = 1f;
        float verde = 1f;
        float azul = 1f;
        float alfa = 1f;
        if (!estaVivo()) {
            verde = 0.4f;
            azul = 0.4f;
            alfa = Math.max(0f, 1f - tiempoMuerte / DURACION_MUERTE);
        } else if (tiempoDanio > 0f) {
            verde = 0.4f;
            azul = 0.4f;
        } else if (tiempoAturdido > 0f) {
            rojo = 0.8f;
            verde = 0.6f;
        } else if (atacando && tiempoAnimacionAtaque < momentoLanzamiento) {
            verde = 0.75f;
            azul = 0.45f;
        }
        batch.setColor(rojo, verde, azul, alfa);
        batch.draw(frameActual, posicionX, posicionY, tamanoAncho, tamanoAlto);
        batch.setColor(1f, 1f, 1f, 1f); // dejar el color como estaba
    }

    // Elegir la animacion segun lo que esta haciendo y hacia donde mira.
    private TextureRegion mirarFrameActual() {
        if (atacando) {
            Animation<TextureRegion> animacion = atacandoConEspada
                ? (miraDerecha ? atacarEspadaDerecha : atacarEspadaIzquierda)
                : (miraDerecha ? atacarDerecha : atacarIzquierda);
            return animacion.getKeyFrame(tiempoAnimacionAtaque, false);
        }

        if (miraDerecha) {
            return caminarDerecha.getKeyFrame(tiempoAnimacion, true); // true = se repite
        }
        return caminarIzquierda.getKeyFrame(tiempoAnimacion, true);
    }

    // Cargar una animacion: los archivos son 01.png, 02.png, 03.png... dentro de una carpeta.
    // Si "medirParaUnion" es true, las imagenes se suman al area visible comun.
    private Animation<TextureRegion> cargarAnimacion(String ruta, int cantidad,
                                                       float duracionDeCadaImagen,
                                                       boolean medirParaUnion) {
        return cargarAnimacion(ruta, cantidad, duracionDeCadaImagen, medirParaUnion, false);
    }

    private Animation<TextureRegion> cargarAnimacion(String ruta, int cantidad,
                                                       float duracionDeCadaImagen,
                                                       boolean medirParaUnion,
                                                       boolean medirParaAtaque) {
        TextureRegion[] frames = new TextureRegion[cantidad];

        for (int indice = 0; indice < cantidad; indice++) {
            String nombre = String.format("%s/%02d.png", ruta, indice + 1);
            Texture imagen = new Texture(Gdx.files.internal(nombre));
            imagenes.add(imagen);
            frames[indice] = new TextureRegion(imagen);
            if (medirParaUnion) {
                acumularLimites(nombre);
            }
            if (medirParaAtaque) {
                acumularLimitesAtaque(nombre);
            }
        }

        return new Animation<>(duracionDeCadaImagen, frames);
    }

    // Recorrer pixel por pixel la imagen para saber que parte tiene dibujo
    // (el resto es transparente) y agrandar el area visible comun con ese dato.
    private void acumularLimites(String ruta) {
        Pixmap pixmap = new Pixmap(Gdx.files.internal(ruta));
        int minX = pixmap.getWidth();
        int minY = pixmap.getHeight();
        int maxX = -1;
        int maxY = -1;

        for (int pixelY = 0; pixelY < pixmap.getHeight(); pixelY++) {
            for (int pixelX = 0; pixelX < pixmap.getWidth(); pixelX++) {
                // Si el pixel no es transparente (alfa != 0) forma parte del dibujo.
                if ((pixmap.getPixel(pixelX, pixelY) & 0xff) != 0) {
                    minX = Math.min(minX, pixelX);
                    minY = Math.min(minY, pixelY);
                    maxX = Math.max(maxX, pixelX);
                    maxY = Math.max(maxY, pixelY);
                }
            }
        }
        pixmap.dispose();

        if (maxX < minX || maxY < minY) {
            throw new IllegalArgumentException("El sprite no contiene pixeles visibles: " + ruta);
        }
        unionMinX = Math.min(unionMinX, minX);
        unionMinY = Math.min(unionMinY, minY);
        unionMaxX = Math.max(unionMaxX, maxX);
        unionMaxY = Math.max(unionMaxY, maxY);
    }

    private void acumularLimitesAtaque(String ruta) {
        Pixmap pixmap = new Pixmap(Gdx.files.internal(ruta));
        int minX = pixmap.getWidth();
        int maxX = -1;
        int maxY = -1;

        for (int pixelY = 0; pixelY < pixmap.getHeight(); pixelY++) {
            for (int pixelX = 0; pixelX < pixmap.getWidth(); pixelX++) {
                if ((pixmap.getPixel(pixelX, pixelY) & 0xff) != 0) {
                    minX = Math.min(minX, pixelX);
                    maxX = Math.max(maxX, pixelX);
                    maxY = Math.max(maxY, pixelY);
                }
            }
        }
        pixmap.dispose();

        if (maxX < minX || maxY < 0) {
            throw new IllegalArgumentException("El sprite no contiene pixeles visibles: " + ruta);
        }
        ataqueUnionMinX = Math.min(ataqueUnionMinX, minX);
        ataqueUnionMaxX = Math.max(ataqueUnionMaxX, maxX);
        ataqueUnionMaxY = Math.max(ataqueUnionMaxY, maxY);
    }

    // Liberar las imagenes cuando se cierra el nivel.
    public void dispose() {
        for (Texture imagen : imagenes) {
            imagen.dispose();
        }
    }
}
