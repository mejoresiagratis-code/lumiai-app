# Modelo YAMNet para el modo Alerta Sonora

El modo Alerta Sonora (F2+) usa el modelo **YAMNet** en formato TFLite, situado aquí:

```
app/src/main/assets/yamnet.tflite
```

`MediaPipeSoundClassifier` lo carga con `setModelAssetPath("yamnet.tflite")`. Sin este fichero el
clasificador no inicializa y el modo **no detecta nada** (no se finge detección: se notifica el
error y el servicio sigue vivo sin avisar).

## Empaquetado

A día de hoy el binario **se versiona en el repositorio** (~3,9 MB) y viaja dentro del APK como
asset. Es la opción simple para QA y CI: no hay que colocar nada a mano. En F5 se reevaluará el
empaquetado definitivo según el tamaño final del APK (bundle vs. descarga en primer arranque);
si se cambia a descarga, habrá que destrackearlo (`git rm --cached`) y volver a ignorarlo.

## Origen y licencia

Modelo de Google AI Edge para MediaPipe Audio Classifier, **Apache-2.0**. Fuente:
`https://storage.googleapis.com/mediapipe-models/audio_classifier/yamnet/float32/1/yamnet.tflite`
(equivalente al YAMNet de la guía oficial de MediaPipe / Kaggle Models · Google · YAMNet).

## Validación de etiquetas (automatizada)

Las etiquetas declaradas en `SoundCategory.labels` deben coincidir **carácter a carácter** con el
mapa de clases del modelo (`yamnet_class_map.csv`, 521 clases de AudioSet). Esto lo verifica en CI
`SoundCategoryLabelsTest` (recurso congelado `app/src/test/resources/yamnet_labels.txt`). Para
re-validar contra el upstream o un modelo nuevo: `scripts/verify_yamnet_labels.py`
(`--write-resource` regenera el recurso congelado).
