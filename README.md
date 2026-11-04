# PrintDoctor

> Diagnóstico de impresiones 3D fallidas con ayuda de Gemini AI.

![Android](https://img.shields.io/badge/Android-Studio-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?logo=kotlin&logoColor=white)
![Estado](https://img.shields.io/badge/estado-en%20desarrollo-orange)


## ¿Qué es PrintDoctor?

PrintDoctor es una aplicación para Android que ayuda a entender por qué falló una impresión 3D. Tomas una foto de la pieza, indicas el filamento y algunos datos de la impresión, y la app analiza la imagen y te explica la falla más probable junto con consejos para corregirla la próxima vez.

Nació como proyecto de titulación y está pensada para impresoras FDM (Creality Ender-3 S1 Pro fue la impresora con la que se desarrolló y probó).

## ¿Cómo funciona?

1. **Tomar una foto.** Tomas una foto de la impresión fallida con la cámara integrada.
2. **Completa el formulario.** Tipo de filamento, temperaturas de boquilla y cama, y notas.
3. **Recibe el diagnóstico.** Te va a decir cual es la falla más probable, una explicación y consejos para la próxima impresión.
4. **Responde preguntas de seguimiento.** Si es necesario, puedes dar mas información para afinar el resultado.

## Características

- Vista previa de cámara en vivo con CameraX.
- Formulario con los datos de la impresión.
- Diagnóstico con un modelo de IA multimodal (Gemini) y respuestas en español.
- Preguntas de seguimiento para afinar el diagnóstico.

## Tecnologías

| Área | Herramienta |
| --- | --- |
| Lenguaje | Kotlin |
| Interfaz | Jetpack Compose + Material 3 |
| Cámara | CameraX |
| IA | API de Gemini (REST) |
| IDE | Android Studio |
| Control de versiones | Git y GitHub |

## Requisitos

- Android Studio (versión reciente).
- Un teléfono Android con depuración USB activada.
- Una clave de API de Gemini. Se crea gratis en [Google AI Studio](https://aistudio.google.com).

## Instalación y configuración

1. **Clona el repositorio:**
   ```bash
   git clone https://github.com/fractalhelios/printdoctor.git
   ```
2. **Abre la carpeta** en Android Studio
3. **Agrega tu clave de Gemini** en el archivo `local.properties` (en la raíz del proyecto; si no existe, créalo):
   ```properties
   GEMINI_API_KEY=tu_clave_aqui
   ```
   Escríbela sin comillas.
4. **Ejecuta la app** en tu teléfono.

## Estructura del proyecto

Los archivos principales están en `app/src/main/java/com/example/printdoctor/`:

| Archivo | Para qué sirve |
| --- | --- |
| `MainActivity.kt` | Punto de entrada y navegación entre pantallas |
| `SplashScreen.kt` | Presentación animada con el logotipo |
| `HomeScreen.kt` | Pantalla principal con la cámara y los botones |
| `FormScreen.kt` | Formulario con los datos de la impresión |
| `DiagnosisScreen.kt` | Resultado del diagnóstico y preguntas de seguimiento |
| `SettingsScreen.kt` | Pantalla de configuración |
| `GeminiProvider.kt` | Conexión con la API de Gemini |
| `DiagnosisModels.kt` | Modelo del resultado e interfaz para cambiar de proveedor de IA |
| `AppComponents.kt` | Botones personalizados |
| `Transitions.kt` | Efectos de transición (iris y vuelo de la foto) |
| `ImageUtils.kt` | Carga y orientación de las fotos |
| `ui/theme/` | Colores, tipografías y formas del tema |

## Privacidad y seguridad

- Nunca subas tu clave de API a GitHub. Si se sube por accidente, revócala en Google AI Studio y crea otra.
- La clave queda dentro de la app compilada, así que no distribuyas el APK generado con tu clave personal. Para una versión pública haría falta un servidor intermedio.
- La foto y los datos del formulario se envían a la API de Gemini para generar el diagnóstico. En el nivel gratuito, Google puede usar el contenido enviado para mejorar sus productos, así que evita fotos con personas u objetos personales.
- Los modelos disponibles y los límites gratuitos de Gemini cambian con frecuencia; revisa tu cuenta en Google AI Studio.

## Estado del proyecto

- [x] Cámara con vista previa en vivo
- [x] Formulario de datos de la impresión
- [x] Diagnóstico con Gemini y preguntas de seguimiento
- [x] Rediseño visual y animaciones
- [ ] Pantalla de configuración
- [ ] Pruebas con fallas reales y evaluación de aciertos
- [ ] Historial de diagnósticos

## Proyecto académico

Proyecto de titulación de la Licenciatura en Informática, Universidad Nacional Autónoma de México (UNAM).

## Uso de herramientas de IA

Dentro de la app, el diagnóstico lo genera la API de Gemini.

## Créditos y licencias

- Tipografías: [Playfair Display](https://fonts.google.com/specimen/Playfair+Display) y [Raleway](https://fonts.google.com/specimen/Raleway), de Google Fonts, bajo licencias libres.
- [CameraX](https://developer.android.com/media/camera/camerax), de Android Jetpack.
- [Gemini API](https://ai.google.dev), de Google.

## Licencia

Todos los derechos reservados.

## Autor

**Yarad Méndez** 
