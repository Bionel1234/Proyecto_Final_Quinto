# Jorge: El Último Gladiador

## Integrantes
* Agustín Belas
* Matias Vassallo
* Santino Laura Calle

## Descripción del Proyecto
Videojuego de acción y plataformas 2D desarrollado en Java utilizando el framework LibGDX. El jugador controla a Jorge, un gladiador que se enfrenta a desafíos y combate enemigos dentro de una arena.

Este proyecto fue generado con la herramienta LibGDX Liftoff y está estructurado mediante Gradle.

## Tecnologías Utilizadas
* **Lenguaje:** Java 17+
* **Framework:** LibGDX (v1.13.1)
* **Motor Desktop:** LWJGL3
* **Gestor de Construcción:** Gradle
* **Control de Versiones:** Git y GitHub

## Estructura del Proyecto
* `core/`: Contiene la lógica principal del videojuego y las clases agnósticas a la plataforma.
* `lwjgl3/`: Módulo de ejecución para computadoras de escritorio.
* `assets/`: Carpeta destinada a los recursos gráficos y sonoros.

## Requerimientos Previos
Para poder importar, compilar y ejecutar este proyecto localmente se necesita:
* **JDK (Java Development Kit):** Versión 17 o superior correctamente instalada.
* **IDE Recomendado:** Eclipse IDE for Java Developers, IntelliJ IDEA o VS Code.
* **Soporte de Gradle:** El IDE debe contar con el plugin de Gradle habilitado para importar el proyecto correctamente.
* **Git:** Para clonar el repositorio en tu equipo.

## Cómo Ejecutar el Juego

1. Clonar este repositorio en tu computadora usando el siguiente comando en la terminal:
   git clone https://github.com/Bionel1234/JorgeElUltimoGladiador.git

2. Abrir el entorno de desarrollo (IDE) e importar el proyecto:
   * En **Eclipse**: Ir a `File` > `Import` > `Gradle` > `Existing Gradle Project`, seleccionar la carpeta raíz del repositorio y presionar `Finish`.
   * En **IntelliJ IDEA**: Ir a `File` > `Open`, seleccionar el archivo `build.gradle` ubicado en la raíz del proyecto y elegir la opción de abrir como proyecto Gradle.

3. Esperar a que Gradle descargue todas las dependencias necesarias de LibGDX.

4. Ejecutar la aplicación:
   * Ir al módulo `lwjgl3`.
   * Desplegar el paquete del código fuente hasta ubicar la clase principal `Lwjgl3Launcher.java`.
   * Hacer clic derecho sobre el archivo y seleccionar **Run As > Java Application** (o `Run Lwjgl3Launcher.main()` en IntelliJ).

## Estado Actual del Proyecto
Para esta primera entrega, el estado del proyecto corresponde a la **Configuración inicial y estructura del proyecto**. Se ha generado la base funcional con LibGDX Liftoff, configurado los módulos `core` y `lwjgl3`, verificado la ejecución de la ventana nativa y vinculado el repositorio local con GitHub bajo el control de versiones con Git.
