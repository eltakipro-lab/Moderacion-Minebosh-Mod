# Moderación Minebosh Tabs — proyecto Fabric listo para compilar

Este .zip contiene un proyecto completo de mod de **Fabric** (Minecraft 1.20.1)
armado a partir de tus dos archivos originales (`Config.java` y
`ModeracionScreen.java`), a los que añadí:

- `ModeracionModClient.java`: registra la tecla **`]`** (corchete derecho,
  remapeable en Opciones → Controles) para abrir el panel en el juego.
- `fabric.mod.json`: el manifiesto que hace que Fabric Loader reconozca
  esto como un mod instalable.
- `build.gradle`, `settings.gradle`, `gradle.properties`: el proyecto de
  compilación (Fabric Loom).
- Traducciones (`en_us.json`, `es_es.json`) para el nombre de la tecla.

## Por qué no te entrego ya el .jar compilado

Compilar un mod de Fabric requiere descargar Minecraft, las mappings de
Yarn, Fabric Loader y Fabric API desde internet (vía Gradle). Este entorno
donde te respondo **no tiene acceso a internet**, así que no puedo
completar esa compilación aquí. Sí puedes hacerlo tú en tu propio PC en
un par de minutos.

## Cómo compilarlo (necesitas JDK 17 e internet)

1. Instala **Java 17** (o superior) si no lo tienes.
2. Descomprime este proyecto en una carpeta.
3. Abre una terminal en esa carpeta y ejecuta:
   - Windows: `gradlew.bat build`
   - Linux/Mac: `./gradlew build`
   - Si no tienes el wrapper de Gradle (no viene incluido, es un binario),
     descarga el "Fabric example mod" desde
     https://fabricmc.net/develop/template/ (que ya trae `gradlew`), y
     copia dentro de él las carpetas `src/` y los archivos `build.gradle`,
     `gradle.properties` de este .zip, sobrescribiendo los suyos.
4. Al terminar, el `.jar` final estará en `build/libs/`
   (el que **no** dice `-sources` ni `-dev`).
5. Copia ese `.jar` a la carpeta `mods` de tu instalación con
   **Fabric Loader 0.15+** y **Fabric API** para Minecraft 1.20.1.

## Cosas que quizá quieras revisar/ajustar

- **Versión de Minecraft**: asumí 1.20.1 porque el código usa clases
  (`DrawContext`, `ButtonWidget.builder(...).dimensions(...)`) propias de
  esa mappings de Yarn. Si tu servidor/cliente usa otra versión, cambia
  `minecraft_version`, `yarn_mappings`, `loader_version` y
  `fabric_version` en `gradle.properties` (puedes ver combinaciones
  válidas en https://fabricmc.net/develop/).
- **Tecla por defecto**: `]`. Cámbiala en `ModeracionModClient.java`
  (constante `GLFW.GLFW_KEY_RIGHT_BRACKET`) si prefieres otra.
- **Comandos enviados** (`mute`, `ipban`, `hist`): siguen igual que en tu
  `Config.java` original, no los toqué.
- Requiere **Fabric API** instalado además del mod (por el keybinding y
  el evento de tick).
