# Jorge el ultimo Gladiador (version simplificada)

Juego 2D de accion hecho en **Java** con la libreria **LibGDX**, para el Proyecto Final
de 5to ano (Escuela Tecnica N 35).

Esta version tiene el mismo juego que el original (mismas mecnicas, mismos dibujos),
pero con el codigo simplificado y comentado paso a paso para entenderlo en clase.

## Como abrirlo y ejecutarlo en IntelliJ IDEA

1. Abrir IntelliJ IDEA y elegir **File > Open**, seleccionando esta carpeta
   (la que tiene el archivo `settings.gradle`).
2. Esperar a que termine la "sync" de Gradle (barra abajo, unos minutos la primera vez
   porque descarga LibGDX de internet).
3. Arriba a la derecha, elegir el runnable **Lwjgl3Launcher** y apretar **Run**.

> Si Gradle se queja del JDK: Settings > Build > Build Tools > Gradle > Gradle JVM,
> elegir el JDK empaquetado de IntelliJ (o cualquier JDK 17 o superior).

Tambien se puede ejecutar desde la terminal de IntelliJ:

```
./gradlew run        (Windows: gradlew.bat run)
```

## Controles

| Accion | Tecla |
|---|---|
| Mover | A / D |
| Saltar | W o flecha arriba |
| Atacar | ESPACIO o click izquierdo |
| Escudo / Parry | E o click derecho (los primeros 0,2 s son "parry") |
| Hablar con el mercader | F (pararse al lado) |
| Elegir zona / confirmar | Flechas o W/S + ENTER (pararse a la derecha del lobby) |
| Volver al menu | ESC |

## Como esta organizado el codigo

Todo el juego esta en `core/src/main/java/com/gladiador/juego`:

| Clase | Que hace |
|---|---|
| `MainClass` | Arranca el juego y recuerda la progresion lineal: Zona 1 niveles 1 y 2, jefe de Zona 1 y Zona 2. |
| `MenuScreen` | Pantalla de inicio con los botones Jugar / Controles / Salir. |
| `ControlesScreen` | Muestra la imagen con la explicacion de las teclas. |
| `JuegoScreen` | El lobby (la mazmorra): mercader, monedas y selector de zona. |
| `MapaScreen` | Un nivel: 3 oleadas de enemigos, plataformas y game over. |
| `ColiseoScreen` | El combate contra el jefe El Bestiario. |
| `Jugador` | Jorge: caminar, saltar, atacar, escudo, parry y recibir golpes. |
| `Enemigo` | Enemigo comun: patrulla, ve, persigue, carga, ataca y se protege (Zona 1). Tiene 4 tipos. |
| `BestiarioJefe` | El jefe: alterna latigazo e invocacion de leon. |
| `LeonInvocado` | El leon que cruza la pantalla corriendo. |
| `Comerciante` | El mercader animado del lobby. |
| `Vida` / `Monedas` | La interfaz: barra de vida con imagenes y contador de monedas. |
| `SpriteFrameUtils` | Ayudante para recortar las transparencias de los sprites. |

`lwjgl3/` solo contiene `Lwjgl3Launcher`, la ventanita que inicia el juego.
La carpeta `assets/` tiene todas las imagenes y sonidos.

## Ideas para la exposicion

- Las pantallas (`Screen`) se cambian con `game.setScreen(...)`: menu -> lobby -> nivel.
- Cada clase guarda "temporizadores" (tiempo de ataque, de enfriamiento, de invulnerabilidad)
  que se descuentan con `delta`, el tiempo transcurrido entre dos imgenes de la pantalla.
- El "parry" y el hitstop se hacen solo comparando tiempos, sin nada raro:
  mirar `Jugador.actualizarEscudo` y `MapaScreen.render`.
