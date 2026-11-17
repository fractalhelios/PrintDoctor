# PrintDoctor, diagnóstico de errores en impresiones 3D con ayuda de Gemini AI.

![Android](https://img.shields.io/badge/Android-Studio-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?logo=kotlin&logoColor=white)


## ¿Qué es PrintDoctor?

PrintDoctor es una aplicación para Android que ayuda a entender varios errores que puedan surgir cuando se imprime un modelo en 3D.

Esta aplicación funciona para complementar mi guía de impresión 3D como proyecto de titulación.

Creada para impresoras FDM (app probada con una Creality Ender-3 S1 Pro).

## ¿Cómo funciona?

1. **Tomar una foto.**
2. **Completar el formulario.** Incluye el tipo de filamento, temperaturas de boquilla, cama y notas si es necesario.
3. Si es necesario, puedes dar más información para afinar el resultado.

## Características

- Vista previa de cámara en vivo con CameraX.
- Formulario con los datos de la impresión.
- Diagnóstico con un modelo de IA multimodal (Gemini) y respuestas en español.
- Preguntas de seguimiento para afinar el diagnóstico.

## Entorno de programación
Hecho en Kotlin con Android Studio, con Jetpack Compose y CameraX

## Privacidad y seguridad

- Nunca subas tu clave de API a GitHub. Si se sube por accidente, bórrala en Google AI Studio y crea otra.
- Evita fotos con personas u objetos personales.

## Créditos y licencias

- [Playfair Display](https://fonts.google.com/specimen/Playfair+Display) y [Raleway](https://fonts.google.com/specimen/Raleway) de Google Fonts, bajo licencias libres.
- [CameraX](https://developer.android.com/media/camera/camerax) de Android Jetpack.
- [Gemini API](https://ai.google.dev) de Google.

**Yarad Méndez\
Licenciatura en Informática.\
Facultad de Estudios Superiores Cuautitlán, UNAM**
