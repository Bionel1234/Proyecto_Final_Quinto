package com.gladiador.juego;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class Vida {

    private static final int SALUD_MAXIMA = 8;
    private static final int DANIO_POR_GOLPE = 1;
    private static final int ANCHO_BARRA = 320;
    private static final int ALTO_BARRA = 36;
    private static final int CANTIDAD_SEGMENTOS = 8;

    private final Texture corazon;
    private final Texture barra;
    private int salud = SALUD_MAXIMA;
    private int saludDibujada = -1;

    public Vida() {
        corazon = crearCorazon();
        barra = new Texture(ANCHO_BARRA, ALTO_BARRA, Pixmap.Format.RGBA8888);
        barra.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        actualizarBarra();
    }

    public void dibujar(SpriteBatch batch) {
        if (saludDibujada != salud) {
            actualizarBarra();
        }

        float x = JuegoScreen.ANCHO - 390f;
        float y = JuegoScreen.ALTO - 76f;
        batch.draw(corazon, x, y - 8f, 54f, 48f);
        batch.draw(barra, x + 66f, y - 1f, 300f, 34f);
    }

    public void recibirGolpe() {
        salud = Math.max(0, salud - DANIO_POR_GOLPE);
    }

    public int obtenerSalud() {
        return salud;
    }

    public boolean estaVivo() {
        return salud > 0;
    }

    private void actualizarBarra() {
        Pixmap pixmap = new Pixmap(ANCHO_BARRA, ALTO_BARRA, Pixmap.Format.RGBA8888);
        pixmap.setColor(0.04f, 0.04f, 0.04f, 1f);
        pixmap.fill();

        int rellenoInterior = ANCHO_BARRA - 8;
        int anchoRelleno = Math.round(rellenoInterior * salud / SALUD_MAXIMA);
        int anchoSegmento = rellenoInterior / CANTIDAD_SEGMENTOS;
        for (int x = 4; x < ANCHO_BARRA - 4; x++) {
            boolean lleno = x - 4 < anchoRelleno;
            pixmap.setColor(lleno ? 0.85f : 0.48f, lleno ? 0.04f : 0.48f, lleno ? 0.05f : 0.48f, 1f);
            pixmap.drawLine(x, 4, x, ALTO_BARRA - 5);
        }

        pixmap.setColor(0.12f, 0.12f, 0.12f, 1f);
        for (int segmento = 1; segmento < CANTIDAD_SEGMENTOS; segmento++) {
            int x = 4 + segmento * anchoSegmento;
            pixmap.fillRectangle(x - 2, 4, 4, ALTO_BARRA - 8);
        }

        barra.draw(pixmap, 0, 0);
        pixmap.dispose();
        saludDibujada = salud;
    }

    private Texture crearCorazon() {
        String[] borde = {
            "00111100111100",
            "01111111111110",
            "11111111111111",
            "11111111111111",
            "01111111111110",
            "00111111111100",
            "00011111111000",
            "00001111110000",
            "00000111100000"
        };
        String[] relleno = {
            "00011000011000",
            "00111100111100",
            "01111111111110",
            "01111111111110",
            "00111111111100",
            "00011111111000",
            "00001111110000",
            "00000111100000",
            "00000011000000"
        };
        Pixmap pixmap = new Pixmap(borde[0].length(), borde.length, Pixmap.Format.RGBA8888);
        pixmap.setColor(0f, 0f, 0f, 1f);
        for (int y = 0; y < borde.length; y++) {
            for (int x = 0; x < borde[y].length(); x++) {
                if (borde[y].charAt(x) == '1') {
                    pixmap.drawPixel(x, y);
                }
            }
        }
        pixmap.setColor(0.9f, 0.02f, 0.04f, 1f);
        for (int y = 0; y < relleno.length; y++) {
            for (int x = 0; x < relleno[y].length(); x++) {
                if (relleno[y].charAt(x) == '1') {
                    pixmap.drawPixel(x, y);
                }
            }
        }
        pixmap.setColor(1f, 0.68f, 0.65f, 1f);
        pixmap.drawPixel(2, 6);
        pixmap.drawPixel(3, 6);
        Texture textura = new Texture(pixmap);
        textura.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pixmap.dispose();
        return textura;
    }

    public void dispose() {
        corazon.dispose();
        barra.dispose();
    }
}
