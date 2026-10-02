package com.gladiador.juego;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

final class SpriteFrameUtils {

    private SpriteFrameUtils() {
    }

    static TextureRegion recortarTransparencia(Texture textura, String ruta) {
        Pixmap pixmap = new Pixmap(Gdx.files.internal(ruta));
        int minX = pixmap.getWidth();
        int minY = pixmap.getHeight();
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < pixmap.getHeight(); y++) {
            for (int x = 0; x < pixmap.getWidth(); x++) {
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

        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        return new TextureRegion(textura, minX, minY, width, height);
    }
}
