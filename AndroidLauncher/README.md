# Mi Launcher

Launcher (pantalla de inicio) personalizado y ligero para Android 8.0+ escrito en Kotlin, sin dependencias externas.

## Funciones

- **Reloj y fecha** grandes en la pantalla de inicio, sobre tu fondo de pantalla.
- **Apps favoritas en inicio**: mantén pulsada una app → *Añadir a inicio* / *Quitar de inicio* (hasta 24 elementos entre apps y carpetas).
- **Carpetas**: mantén pulsada una app → *Mover a carpeta…* → elige una o crea *+ Nueva carpeta*.
  Toca la carpeta para abrirla; mantenla pulsada para renombrarla o eliminarla (sus apps vuelven a inicio).
  Dentro de una carpeta, mantén pulsada una app → *Sacar de la carpeta*. Las carpetas vacías se borran solas.
- **Cajón de apps** con todas las apps en orden alfabético y **buscador** (pulsa "Ir" en el teclado para abrir el primer resultado).
- **Gestos**:
  - Deslizar hacia arriba → abre el cajón.
  - Deslizar hacia abajo en el cajón (arriba del todo) → lo cierra.
  - Deslizar hacia abajo en inicio → baja la barra de notificaciones.
  - Botón Atrás o Inicio → cierra el cajón.
- **Menú de app**: información de la app y desinstalar.
- **Menú de inicio** (botón ⋮ o mantener pulsado el fondo): cambiar fondo, número de columnas (3–6), elegir launcher predeterminado y ajustes del sistema.
- Se actualiza solo al instalar o desinstalar apps.

## Cómo instalarlo

### Opción A: descargar el APK de GitHub Actions
1. En GitHub, ve a la pestaña **Actions** → workflow **Android Launcher APK** → la última ejecución correcta.
2. Descarga el artefacto **MiLauncher-apk** (es un `.zip` con `app-debug.apk` dentro).
3. Pásalo al móvil y ábrelo (permite "instalar apps de fuentes desconocidas" si te lo pide).

### Opción B: compilar con Android Studio
1. Abre la carpeta `AndroidLauncher/` en Android Studio.
2. Conecta el móvil con la depuración USB activada y pulsa **Run**.

O desde la terminal (con el Android SDK instalado):

```bash
cd AndroidLauncher
./gradlew assembleDebug
# APK en app/build/outputs/apk/debug/app-debug.apk
```

### Ponerlo como launcher predeterminado
Pulsa el botón **Inicio** del móvil y elige **Mi Launcher** → *Siempre*. O ve a
*Ajustes → Aplicaciones → Aplicaciones predeterminadas → Aplicación de inicio*
(también desde el menú ⋮ → *Elegir launcher predeterminado*).

## Estructura

```
app/src/main/
├── AndroidManifest.xml          # Declara la app como HOME
├── java/com/danielmoly/launcher/
│   ├── MainActivity.kt          # Inicio, cajón, gestos y menús
│   ├── AppInfo.kt               # Modelo de app + carga de apps/iconos
│   ├── AppAdapter.kt            # Adaptador del cajón y carpetas
│   ├── HomeAdapter.kt           # Adaptador de inicio (apps + carpetas)
│   ├── HomeStore.kt             # Lógica de inicio y carpetas
│   ├── HomeItem.kt              # App o carpeta en inicio
│   ├── FolderIcon.kt            # Icono de carpeta con miniaturas
│   ├── ExpandedGridView.kt      # Cuadrícula de inicio sin scroll
│   └── Prefs.kt                 # Inicio, carpetas y columnas guardados
└── res/                         # Layouts, textos, tema e icono
```

## Personalizarlo

- Colores: `app/src/main/res/values/colors.xml`
- Textos: `app/src/main/res/values/strings.xml`
- Formato del reloj/fecha: `TextClock` en `res/layout/activity_main.xml`
- Icono del launcher: `res/drawable/ic_launcher_foreground.xml` y `colors.xml` (`icon_bg`)
