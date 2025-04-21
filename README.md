# Asistente 

Este es una prueba de un "asistente con personalidad".

Está implementado como un bot de Telegram y utiliza claude-3.5-sonnet
para la IA.

Si quieres saber más sobre el proyecto, incluyendo un vídeo demo y detalles de cómo está montado,
[ve a este artículo en mi blog](https://www.juanjonavarro.com/2025/04/21/asistente-ia-con-personalidad)

La forma más sencilla de probarlo es:

1. Descarga o clona el proyecto
2. Copia el fichero `env.ejemplo` a `.env` y completa todas 
las variables necesarias.
3. Utiliza docker para arrancarlo:

```sh
$ docker compose build
$ docker compose up -d
```
