# Modelo YAMNet para el modo Alerta Sonora

El modo Alerta Sonora (F2+) necesita el modelo **YAMNet** en formato TFLite colocado aquí:

```
app/src/main/assets/yamnet.tflite
```

Sin este fichero, el clasificador no inicializa y el modo **no detecta nada** (no se finge
detección: `MediaPipeSoundClassifier` notifica el error y el servicio sigue vivo sin avisar).

## Cómo obtenerlo

Modelo recomendado por Google AI Edge para MediaPipe Audio Classifier (Apache-2.0). Descárgalo
del repositorio de modelos de MediaPipe / Kaggle Models (Google · yamnet) y renómbralo a
`yamnet.tflite`.

No se versiona el binario en el repositorio (es un asset externo de varios MB); colócalo en
local antes de compilar para QA en dispositivo. En F5 se decidirá el empaquetado definitivo.

## Validación pendiente (F2/F3)

Las etiquetas declaradas en `SoundCategory.labels` deben coincidir **carácter a carácter** con
el mapa de etiquetas que trae este modelo (`yamnet_class_map.csv`, p. ej. `Doorbell`, `Bark`,
`Smoke detector, smoke alarm`). Si un nombre no coincide exactamente, esa clase nunca disparará.
