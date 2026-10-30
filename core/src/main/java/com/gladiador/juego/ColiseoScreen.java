package com.gladiador.juego;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

// Combate contra el jefe "El Bestiario" en la arena del Coliseo.
// Jorge debe vaciar la barra roja de arriba esquivando latigazos y leones.
public class ColiseoScreen implements Screen {

    private static final float PISO = 72f;                     // altura del suelo de la arena
    private static final float POSICION_INICIAL_JUGADOR = 180f; // donde aparece Jorge
    private static final float LIMITE_JUGADOR = JuegoScreen.ANCHO - 180f; // pared invisible del fondo
    private static final float DURACION_CARTEL_JEFE = 2.5f;    // cuanto se ve el cartel del jefe

    private final MainClass game;
    private final boolean jefeZona2;
    private final ArrayList<LeonInvocado> leones = new ArrayList<>(); // leones invocados en pista

    private OrthographicCamera camera;
    private Viewport viewport;
    private Texture fondo;
    private ImageInputStream entradaFondoGif;
    private ImageReader lectorFondoGif;
    private Pixmap pixmapFondoGif;
    private BufferedImage lienzoFondoGif;
    private BufferedImage lienzoAnteriorFondoGif;
    private int anchoFondo;
    private int altoFondo;
    private int cantidadFotogramasFondo;
    private int indiceFotogramaFondo = -1;
    private int xFotogramaAnterior;
    private int yFotogramaAnterior;
    private int anchoFotogramaAnterior;
    private int altoFotogramaAnterior;
    private String disposicionFotogramaAnterior;
    private float tiempoFotogramaFondo;
    private float duracionFotogramaFondo;
    private Texture pantallaGameOver;
    private Texture cartelJefe;      // cartel "jefe" que se muestra al empezar
    private Jugador jugador;
    private Vida vida;
    private BestiarioJefe jefe;
    private ShapeRenderer shapeRenderer;

    // Mismos efectos de combate que en los niveles: congelamiento y temblor.
    private float tiempoHitstop;
    private float tiempoTemblor;
    private float tiempoCartelJefe;   // cuanto falta para ocultar el cartel

    // Estado del combate.
    private boolean finDelJuego;
    private boolean victoria;

    // Recibe el controlador principal para volver al lobby o registrar la victoria.
    public ColiseoScreen(MainClass game, boolean jefeZona2) {
        this.game = game;
        this.jefeZona2 = jefeZona2;
    }

    // Cargar todo al abrir la pantalla.
    // Prepara escenario, personaje, jefe e interfaz al entrar al combate.
    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(JuegoScreen.ANCHO, JuegoScreen.ALTO, camera);
        viewport.apply(true);
        camera.position.set(JuegoScreen.ANCHO / 2f, JuegoScreen.ALTO / 2f, 0f);
        camera.update();

        cargarFondoAnimado();
        pantallaGameOver = new Texture(Gdx.files.internal("ui/gameover.png"));
        cartelJefe = new Texture(Gdx.files.internal("ui/oleada_jefe.png"));
        jugador = new Jugador(POSICION_INICIAL_JUGADOR, PISO);
        jugador.setEscalaVisual(1.2f); // en el Coliseo se lo ve un poco mas grande
        vida = new Vida();
        jefe = new BestiarioJefe(jefeZona2);
        shapeRenderer = new ShapeRenderer();
        tiempoCartelJefe = DURACION_CARTEL_JEFE;
    }

    // Actualiza el combate y dibuja el estado actual del Coliseo.
    @Override
    public void render(float deltaReal) {
        actualizarFondoAnimado(deltaReal);

        // Congelar el combate durante el hitstop (los efectos siguen corriendo).
        float delta = deltaReal;
        if (!finDelJuego && tiempoHitstop > 0f) {
            tiempoHitstop = Math.max(0f, tiempoHitstop - deltaReal);
            delta = 0f;
        }
        if (tiempoTemblor > 0f) {
            tiempoTemblor = Math.max(0f, tiempoTemblor - deltaReal);
        }
        if (tiempoCartelJefe > 0f) {
            tiempoCartelJefe = Math.max(0f, tiempoCartelJefe - deltaReal);
        }

        if (!finDelJuego) {
            actualizarCombate(delta);
        }
        if (victoria) {
            int etapaGanada = jefeZona2 ? MainClass.ETAPA_JEFE_ZONA2 : MainClass.ETAPA_JEFE_ZONA1;
            Monedas.recompensarJefeDerrotado();
            game.registrarEtapaCompletada(etapaGanada);
            game.setScreen(new JuegoScreen(game));
            return;
        }

        // Limpiar y preparar la camara con el temblor.
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glViewport(
            viewport.getScreenX(), viewport.getScreenY(),
            viewport.getScreenWidth(), viewport.getScreenHeight()
        );

        float temblor = tiempoTemblor > 0f ? MathUtils.random(-4f, 4f) : 0f;
        camera.position.set(JuegoScreen.ANCHO / 2f + temblor, JuegoScreen.ALTO / 2f, 0f);
        camera.update();
        game.batch.setProjectionMatrix(camera.combined);

        // Dibujar la escena: fondo, jefe, leones, Jorge y la interfaz.
        game.batch.begin();
        float altoFondoEscalado = JuegoScreen.ALTO;
        float anchoFondoEscalado = altoFondoEscalado * anchoFondo / altoFondo;
        game.batch.draw(fondo, (JuegoScreen.ANCHO - anchoFondoEscalado) / 2f, 0f,
            anchoFondoEscalado, altoFondoEscalado);
        jefe.dibujar(game.batch, jugador.getCentroX());
        for (LeonInvocado leon : leones) {
            leon.dibujar(game.batch);
        }
        jugador.dibujar(game.batch, 0f);
        vida.dibujar(game.batch);
        if (tiempoCartelJefe > 0f) {
            // Cartel de presentacion del jefe, centrado en la pantalla.
            float ancho = 700f;
            float alto = ancho * cartelJefe.getHeight() / cartelJefe.getWidth();
            game.batch.draw(cartelJefe, (JuegoScreen.ANCHO - ancho) / 2f,
                (JuegoScreen.ALTO - alto) / 2f, ancho, alto);
        }
        game.batch.end();

        if (finDelJuego && !victoria) {
            dibujarGameOver();
            return;
        }
        dibujarBarrasDeVida();

        // ESC sirve para rendirse y volver al lobby.
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new JuegoScreen(game));
        }
    }

    // Todo lo que pasa en un frame de combate.
    private void actualizarCombate(float delta) {
        jugador.actualizar(delta, PISO, LIMITE_JUGADOR, List.of());
        jefe.actualizar(delta);

        // El jefe aviso que invoco un leon: crearlo a su lado, saliendo de la derecha.
        if (jefe.consumirInvocacionLeon()) {
            float anchoLeon = 250f;
            float xLeon = jefe.getCentroX() - 165f - anchoLeon;
            leones.add(new LeonInvocado(xLeon, PISO));
            tiempoTemblor = 0.12f; // la invocacion se "siente"
        }

        // Jorge le pega al jefe: solo durante los frames activos del tajo.
        if (jefe.estaVivo()
            && jugador.estaAtaqueActivo()
            && jugador.getAreaDeAtaque().overlaps(jefe.getHitbox())) {
            if (jefe.recibirGolpe(jugador.getNumeroAtaque())) {
                tiempoHitstop = 0.045f;
                tiempoTemblor = 0.11f;
            }
        }

        // Los ataques del jefe: cadena (latigo) y onda expansiva. En la Zona 2 el
        // enemigo usa dos tipos distintos de golpe de area.
        if (jefe.consumirImpactoLatigo()
            && jefe.getAreaLatigo(jugador.getCentroX(), jugador.getY()).overlaps(jugador.getHitbox())) {
            resolverAtaqueDelJefe(jefe.getCentroX());
        }
        if (jefeZona2 && jefe.consumirImpactoCadena()
            && jefe.getAreaCadena(jugador.getCentroX(), jugador.getY()).overlaps(jugador.getHitbox())) {
            resolverAtaqueDelJefe(jefe.getCentroX());
        }
        if (jefeZona2 && jefe.consumirImpactoOndaEscape()
            && jefe.getAreaOndaEscape(jugador.getCentroX(), jugador.getY()).overlaps(jugador.getHitbox())) {
            resolverAtaqueDelJefe(jefe.getCentroX());
        }

        // Los leones corren hacia la izquierda; si tocan a Jorge, lo lastiman y desaparecen.
        Iterator<LeonInvocado> iterador = leones.iterator();
        while (iterador.hasNext()) {
            LeonInvocado leon = iterador.next();
            leon.actualizar(delta);
            if (leon.estaActivo() && leon.getHitbox().overlaps(jugador.getHitbox())) {
                leon.desactivar();
                resolverAtaqueDelJefe(jefe.getCentroX());
                tiempoTemblor = 0.15f;
            }
            if (!leon.estaActivo()) {
                leon.dispose();
                iterador.remove(); // salir de la lista
            }
        }

        // ¿Termino el combate?
        if (!jefe.estaVivo()) {
            victoria = true;
            finDelJuego = true;
        } else if (!vida.estaVivo()) {
            victoria = false;
            finDelJuego = true;
        }
    }

    // Un ataque del jefe agarro a Jorge: decidir si es golpe, bloqueo o parry.
    private void resolverAtaqueDelJefe(float origenAtaqueX) {
        if (jugador.esInvulnerable()) {
            return; // en el tiempo de gracia no pasa nada
        }
        float empuje = jugador.getCentroX() >= origenAtaqueX ? 1f : -1f;
        if (jugador.bloqueaGolpeDesde(origenAtaqueX)) {
            if (jugador.estaEnVentanaParry()) {
                // PARRY: el jefe queda aturdido y pierde su ataque.
                jefe.aturdir();
                jugador.bloquearGolpe(empuje);
                tiempoHitstop = 0.10f;
                tiempoTemblor = 0.14f;
            } else {
                // Bloqueo comun: retrocede pero no pierde vida.
                jugador.bloquearGolpe(empuje);
                tiempoHitstop = 0.035f;
            }
            return;
        }

        // Sin defensa: un punto de vida menos y Jorge queda aturdido.
        vida.recibirGolpe(1);
        jugador.recibirImpacto(empuje);
        tiempoHitstop = 0.07f;
        tiempoTemblor = 0.18f;
    }

    // Barra roja gigante arriba con la vida del jefe.
    private void dibujarBarrasDeVida() {
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        float ancho = 420f;
        float alto = 24f;
        float x = (JuegoScreen.ANCHO - ancho) / 2f;
        float y = JuegoScreen.ALTO - 78f;
        shapeRenderer.setColor(0.12f, 0.04f, 0.04f, 0.94f);                 // marco
        shapeRenderer.rect(x - 4f, y - 4f, ancho + 8f, alto + 8f);
        shapeRenderer.setColor(0.35f, 0.10f, 0.08f, 1f);                    // fondo vacio
        shapeRenderer.rect(x, y, ancho, alto);
        shapeRenderer.setColor(0.82f, 0.10f, 0.08f, 1f);                    // parte llena
        shapeRenderer.rect(x, y, ancho * jefe.getVida() / jefe.getVidaMaxima(), alto);
        shapeRenderer.end();
    }

    // Pantalla de derrota con dos botones: volver al lobby o salir del juego.
    private void dibujarGameOver() {
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.7f); // oscurecer la escena
        shapeRenderer.rect(0f, 0f, JuegoScreen.ANCHO, JuegoScreen.ALTO);
        shapeRenderer.end();

        // La imagen de game over mide 1671x941 y trae dos botones dibujados:
        // "REINTENTAR" (pelear de nuevo) y "VOLVER AL MENU" (ir al lobby).
        float escalaX = JuegoScreen.ANCHO / 1671f;
        float escalaY = JuegoScreen.ALTO / 941f;
        float reintentarX = 600f * escalaX;
        float reintentarAncho = (1065f - 600f) * escalaX;
        float reintentarY = JuegoScreen.ALTO - 580f * escalaY;
        float reintentarAlto = (580f - 485f) * escalaY;
        float volverX = 600f * escalaX;
        float volverAncho = (1065f - 600f) * escalaX;
        float volverY = JuegoScreen.ALTO - 692f * escalaY;
        float volverAlto = (692f - 598f) * escalaY;

        game.batch.begin();
        game.batch.draw(pantallaGameOver, 0f, 0f, JuegoScreen.ANCHO, JuegoScreen.ALTO);
        game.batch.end();

        // ?Donde esta el mouse? Comparar con las cajas de los botones.
        Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f);
        viewport.unproject(mouse);
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

    // Adapta el viewport a los cambios de tamano de la ventana.
    @Override
    public void resize(int width, int height) {
        if (viewport != null) {
            viewport.update(width, height, true);
        }
    }

    // No se necesita una accion especial al pausar o reanudar el combate.
    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    // Libera los recursos del combate al cambiar de pantalla.
    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        if (fondo != null) {
            fondo.dispose();
            fondo = null;
        }
        if (pixmapFondoGif != null) {
            pixmapFondoGif.dispose();
            pixmapFondoGif = null;
        }
        if (lectorFondoGif != null) {
            lectorFondoGif.dispose();
            lectorFondoGif = null;
        }
        if (entradaFondoGif != null) {
            try {
                entradaFondoGif.close();
            } catch (IOException error) {
                throw new GdxRuntimeException("No se pudo cerrar el GIF del Coliseo.", error);
            } finally {
                entradaFondoGif = null;
            }
        }
        if (pantallaGameOver != null) {
            pantallaGameOver.dispose();
        }
        if (cartelJefe != null) {
            cartelJefe.dispose();
        }
        if (jugador != null) {
            jugador.dispose();
        }
        if (vida != null) {
            vida.dispose();
        }
        if (jefe != null) {
            jefe.dispose();
        }
        for (LeonInvocado leon : leones) {
            leon.dispose();
        }
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
    }

    private void cargarFondoAnimado() {
        Pixmap nuevoPixmap = null;
        try {
            Iterator<ImageReader> lectores = ImageIO.getImageReadersByFormatName("gif");
            if (!lectores.hasNext()) {
                throw new IOException("No hay un lector GIF disponible.");
            }
            lectorFondoGif = lectores.next();
            entradaFondoGif = ImageIO.createImageInputStream(
                Gdx.files.internal("coliseo_fondo.gif").read()
            );
            if (entradaFondoGif == null) {
                throw new IOException("No se pudo abrir el GIF del Coliseo.");
            }
            lectorFondoGif.setInput(entradaFondoGif, false, false);
            cantidadFotogramasFondo = lectorFondoGif.getNumImages(true);
            if (cantidadFotogramasFondo == 0) {
                throw new IOException("El GIF del Coliseo no contiene fotogramas.");
            }

            Node pantallaLogica = exigirNodo(
                lectorFondoGif.getStreamMetadata().getAsTree("javax_imageio_gif_stream_1.0"),
                "LogicalScreenDescriptor"
            );
            NamedNodeMap atributos = pantallaLogica.getAttributes();
            anchoFondo = Integer.parseInt(atributos.getNamedItem("logicalScreenWidth").getNodeValue());
            altoFondo = Integer.parseInt(atributos.getNamedItem("logicalScreenHeight").getNodeValue());

            lienzoFondoGif = new BufferedImage(anchoFondo, altoFondo, BufferedImage.TYPE_INT_ARGB);
            nuevoPixmap = new Pixmap(anchoFondo, altoFondo, Pixmap.Format.RGBA8888);
            pixmapFondoGif = nuevoPixmap;
            leerSiguienteFotogramaFondo();
            fondo = new Texture(pixmapFondoGif);
            fondo.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        } catch (IOException | RuntimeException error) {
            if (nuevoPixmap != null) {
                nuevoPixmap.dispose();
                pixmapFondoGif = null;
            }
            if (lectorFondoGif != null) {
                lectorFondoGif.dispose();
                lectorFondoGif = null;
            }
            if (entradaFondoGif != null) {
                try {
                    entradaFondoGif.close();
                } catch (IOException errorDeCierre) {
                    error.addSuppressed(errorDeCierre);
                }
                entradaFondoGif = null;
            }
            throw new GdxRuntimeException("No se pudo cargar el GIF animado del Coliseo.", error);
        }
    }

    private void actualizarFondoAnimado(float delta) {
        tiempoFotogramaFondo += delta;
        while (tiempoFotogramaFondo >= duracionFotogramaFondo) {
            tiempoFotogramaFondo -= duracionFotogramaFondo;
            leerSiguienteFotogramaFondo();
            fondo.draw(pixmapFondoGif, 0, 0);
        }
    }

    private void leerSiguienteFotogramaFondo() {
        int siguienteIndice = (indiceFotogramaFondo + 1) % cantidadFotogramasFondo;
        try {
            if (siguienteIndice == 0 && indiceFotogramaFondo >= 0) {
                lienzoFondoGif = new BufferedImage(anchoFondo, altoFondo, BufferedImage.TYPE_INT_ARGB);
                disposicionFotogramaAnterior = null;
                lienzoAnteriorFondoGif = null;
            }

            if (disposicionFotogramaAnterior != null) {
                if ("restoreToBackgroundColor".equals(disposicionFotogramaAnterior)) {
                    Graphics2D graficos = lienzoFondoGif.createGraphics();
                    graficos.setComposite(AlphaComposite.Clear);
                    graficos.fillRect(xFotogramaAnterior, yFotogramaAnterior,
                        anchoFotogramaAnterior, altoFotogramaAnterior);
                    graficos.dispose();
                } else if ("restoreToPrevious".equals(disposicionFotogramaAnterior)
                    && lienzoAnteriorFondoGif != null) {
                    lienzoFondoGif = lienzoAnteriorFondoGif;
                }
            }

            IIOMetadata metadatos = lectorFondoGif.getImageMetadata(siguienteIndice);
            Node raiz = metadatos.getAsTree("javax_imageio_gif_image_1.0");
            Node descriptor = exigirNodo(raiz, "ImageDescriptor");
            NamedNodeMap atributos = descriptor.getAttributes();
            Node control = exigirNodo(raiz, "GraphicControlExtension");
            NamedNodeMap atributosControl = control.getAttributes();
            int x = Integer.parseInt(atributos.getNamedItem("imageLeftPosition").getNodeValue());
            int y = Integer.parseInt(atributos.getNamedItem("imageTopPosition").getNodeValue());
            int ancho = Integer.parseInt(atributos.getNamedItem("imageWidth").getNodeValue());
            int alto = Integer.parseInt(atributos.getNamedItem("imageHeight").getNodeValue());
            String disposicion = atributosControl.getNamedItem("disposalMethod").getNodeValue();
            float duracion = Integer.parseInt(atributosControl.getNamedItem("delayTime").getNodeValue()) / 100f;

            lienzoAnteriorFondoGif = "restoreToPrevious".equals(disposicion)
                ? copiarImagen(lienzoFondoGif) : null;
            BufferedImage imagen = lectorFondoGif.read(siguienteIndice);
            Graphics2D graficos = lienzoFondoGif.createGraphics();
            graficos.setComposite(AlphaComposite.SrcOver);
            graficos.drawImage(imagen, x, y, null);
            graficos.dispose();

            int[] pixeles = lienzoFondoGif.getRGB(0, 0, anchoFondo, altoFondo, null, 0, anchoFondo);
            java.nio.ByteBuffer destino = pixmapFondoGif.getPixels();
            destino.clear();
            for (int pixel : pixeles) {
                destino.put((byte) (pixel >>> 16));
                destino.put((byte) (pixel >>> 8));
                destino.put((byte) pixel);
                destino.put((byte) (pixel >>> 24));
            }
            destino.flip();

            indiceFotogramaFondo = siguienteIndice;
            xFotogramaAnterior = x;
            yFotogramaAnterior = y;
            anchoFotogramaAnterior = ancho;
            altoFotogramaAnterior = alto;
            disposicionFotogramaAnterior = disposicion;
            duracionFotogramaFondo = Math.max(0.02f, duracion);
        } catch (IOException error) {
            throw new GdxRuntimeException("No se pudo decodificar el fotograma " + siguienteIndice
                + " del GIF del Coliseo.", error);
        }
    }

    private static Node exigirNodo(Node raiz, String nombre) {
        if (nombre.equals(raiz.getNodeName())) {
            return raiz;
        }
        for (Node hijo = raiz.getFirstChild(); hijo != null; hijo = hijo.getNextSibling()) {
            Node encontrado = buscarNodo(hijo, nombre);
            if (encontrado != null) {
                return encontrado;
            }
        }
        throw new GdxRuntimeException("Falta el metadato GIF requerido: " + nombre);
    }

    private static Node buscarNodo(Node raiz, String nombre) {
        if (nombre.equals(raiz.getNodeName())) {
            return raiz;
        }
        for (Node hijo = raiz.getFirstChild(); hijo != null; hijo = hijo.getNextSibling()) {
            Node encontrado = buscarNodo(hijo, nombre);
            if (encontrado != null) {
                return encontrado;
            }
        }
        return null;
    }

    private static BufferedImage copiarImagen(BufferedImage original) {
        BufferedImage copia = new BufferedImage(original.getWidth(), original.getHeight(),
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D graficos = copia.createGraphics();
        graficos.drawImage(original, 0, 0, null);
        graficos.dispose();
        return copia;
    }
}
