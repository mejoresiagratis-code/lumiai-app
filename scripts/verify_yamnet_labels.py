#!/usr/bin/env python3
"""
Verifica que las etiquetas declaradas en SoundCategory.kt existan, carácter a carácter, en el
mapa de clases de YAMNet (AudioSet).

Uso:
  scripts/verify_yamnet_labels.py                 # descarga el mapa oficial y compara
  scripts/verify_yamnet_labels.py ruta/al/map.csv # compara contra un CSV local (offline)

El test de CI (SoundCategoryLabelsTest) usa una copia congelada en
app/src/test/resources/yamnet_labels.txt. Este script sirve para re-validar contra el upstream
o un modelo nuevo y, opcionalmente, regenerar ese recurso (--write-resource).
"""
import csv
import io
import re
import sys
import urllib.request

OFFICIAL_CSV = (
    "https://raw.githubusercontent.com/tensorflow/models/master/research/"
    "audioset/yamnet/yamnet_class_map.csv"
)
KT = "app/src/main/kotlin/com/mejoresiagratis/lumiai/domain/sound/SoundCategory.kt"
RES = "app/src/test/resources/yamnet_labels.txt"


def load_map_names(arg):
    if arg and not arg.startswith("--"):
        data = open(arg, newline="", encoding="utf-8").read()
    else:
        with urllib.request.urlopen(OFFICIAL_CSV, timeout=30) as r:
            data = r.read().decode("utf-8")
    names, rows = [], csv.DictReader(io.StringIO(data))
    for row in rows:
        names.append(row["display_name"])
    return names


def declared_labels():
    """Extrae las etiquetas de cada constante del enum: NOMBRE(setOf("a","b"), ...)."""
    src = open(KT, encoding="utf-8").read()
    out = {}
    for m in re.finditer(r"^\s*([A-Z_]+)\s*\(\s*setOf\(([^)]*)\)", src, re.MULTILINE):
        cat, body = m.group(1), m.group(2)
        labels = re.findall(r'"((?:[^"\\]|\\.)*)"', body)
        if labels:
            out[cat] = [s.encode().decode("unicode_escape") for s in labels]
    return out


def main():
    arg = sys.argv[1] if len(sys.argv) > 1 else None
    names = load_map_names(arg)
    name_set = set(names)
    print(f"mapa: {len(names)} clases ({'local' if arg and not arg.startswith('--') else 'upstream'})")

    cats = declared_labels()
    if not cats:
        print("ERROR: no pude extraer etiquetas de", KT)
        return 2

    missing = {}
    for cat, labels in cats.items():
        for l in labels:
            ok = l in name_set
            print(f"{'OK ' if ok else 'FALTA'} {cat:14} | {l!r}")
            if not ok:
                missing.setdefault(cat, []).append(l)

    print("\n=== RESUMEN ===")
    if not missing:
        print("Todas las etiquetas existen char-a-char en el mapa.")
        if "--write-resource" in sys.argv:
            with open(RES, "w", encoding="utf-8", newline="\n") as f:
                f.write("\n".join(names) + "\n")
            print("Recurso regenerado:", RES)
        return 0

    import difflib
    for cat, labels in missing.items():
        for l in labels:
            cand = difflib.get_close_matches(l, names, n=4, cutoff=0.5)
            print(f"REVISAR {cat}: {l!r} -> cercanas: {cand}")
    return 1


if __name__ == "__main__":
    sys.exit(main())
