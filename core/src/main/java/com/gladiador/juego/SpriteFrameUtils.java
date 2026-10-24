package com.gladiador.juego;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

// Utileria para "recortar" sprites.
// Muchos dibijos vienen en un cuadro grande con mucho espacio transparente alrededor.
// Este helper encuentra la parte que realmente tiene dibujo y recorta solo esa zona,
// para que el personaje se vea centrado y pisando el suelo.
final class SpriteFrameUtils {

    // Privado: nadie puede crear objetos de esta clase, solo se usan sus métodos estáticos.
    private SpriteFrameUtils() {
    }

    // Recorre la imagen pixel por pixel y devuelve la region (sin transparencias) del TextureRegion.
    static TextureRegion recortarTransparencia(Texture textura, String ruta) {
        // Pixmap = la imagen ya decodificada en memoria, para poder leer sus pixels.
        Pixmap pixmap = new Pixmap(Gdx.files.internal(ruta));
        int minX = pixmap.getWidth();   // el borde izquierdo "encontrado" arranca fuera de la imagen
        int minY = pixmap.getHeight();  // idem arriba
        int maxX = -1;                  // el borde derecho arranca vacio
        int maxY = -1;

        for (int y = 0; y < pixmap.getHeight(); y++) {
            for (int x = 0; x < pixmap.getWidth(); x++) {
                // getPixel devuelve un entero con los 4 canales; & 0xff toma solo el alfa.
                // Si el alfa no es 0, el pixel tiene dibujo.
                if ((pixmap.getPixel(x, y) & 0xff) != 0) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }

        pixmap.dispose();
        if (maxX < minX || maxY < minY) {
            throw new IllegalArgumentException("El sprite no contiene pixeles visibles: " + ruta);
        }

        // Sumar uno porque ambos extremos (minimo y maximo) pertenecen al recorte.
        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        return new TextureRegion(textura, minX, minY, width, height);
    }
}
