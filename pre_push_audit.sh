#!/usr/bin/env bash
# pre_push_audit.sh — Gate estatico antes de push.
#
# No compila (el sandbox no tiene Gradle/SDK); complementa al CI con chequeos baratos
# que atrapan los errores que mas nos han mordido: llaves/parentesis desbalanceados,
# ficheros sin package y trazas de depuracion olvidadas.
#
# Heuristica conocida: cuenta { } ( ) en todo el fichero, incluidos comentarios y strings,
# asi que un literal con un parentesis suelto puede dar falso positivo. Es un gate, no un
# compilador: ante un aviso, revisa a mano.
#
# Uso:
#   bash pre_push_audit.sh           # audita los .kt cambiados vs origin/main (+ staged/unstaged)
#   bash pre_push_audit.sh --all     # audita todos los .kt de app/src
set -u

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT" || exit 2

mode="${1:-changed}"
files=()
if [ "$mode" = "--all" ]; then
  while IFS= read -r f; do files+=("$f"); done < <(find app/src -name '*.kt' 2>/dev/null)
else
  tmp=()
  if git rev-parse --verify -q origin/main >/dev/null 2>&1; then
    while IFS= read -r f; do tmp+=("$f"); done < <(git diff --name-only origin/main...HEAD -- '*.kt' 2>/dev/null)
  fi
  while IFS= read -r f; do tmp+=("$f"); done < <(git diff --name-only -- '*.kt' 2>/dev/null)
  while IFS= read -r f; do tmp+=("$f"); done < <(git diff --cached --name-only -- '*.kt' 2>/dev/null)
  # dedup + solo existentes
  declare -A seen=()
  for f in "${tmp[@]}"; do
    [ -n "$f" ] && [ -f "$f" ] && [ -z "${seen[$f]:-}" ] && { seen[$f]=1; files+=("$f"); }
  done
fi

if [ "${#files[@]}" -eq 0 ]; then
  echo "pre-push: sin .kt que auditar."
  exit 0
fi

echo "pre-push: auditando ${#files[@]} fichero(s) .kt..."
fail=0
for f in "${files[@]}"; do
  ob=$(grep -o '{' "$f" | wc -l); cb=$(grep -o '}' "$f" | wc -l)
  op=$(grep -o '(' "$f" | wc -l); cp=$(grep -o ')' "$f" | wc -l)
  msg=""
  [ "$ob" -ne "$cb" ] && msg="$msg llaves($ob/$cb)"
  [ "$op" -ne "$cp" ] && msg="$msg parens($op/$cp)"
  grep -q '^package ' "$f" || msg="$msg sin-package"
  if [ -n "$msg" ]; then echo "  ✗ $f:$msg"; fail=1; else echo "  ✓ $f"; fi
done

# Avisos no bloqueantes
todo=$(grep -rnE 'TODO|FIXME|XXX' "${files[@]}" 2>/dev/null | wc -l)
[ "$todo" -gt 0 ] && echo "  aviso: $todo marca(s) TODO/FIXME/XXX"
dbg=$(grep -rnE 'println\(|Log\.(d|v)\(' "${files[@]}" 2>/dev/null | wc -l)
[ "$dbg" -gt 0 ] && echo "  aviso: $dbg posible(s) traza(s) de depuracion (println/Log.d/Log.v)"

# Recordatorio: si cambiaste una firma de constructor, revisa los tests.
if git diff origin/main...HEAD -- '*.kt' 2>/dev/null | grep -qE '^\+.*(constructor|class .*\()'; then
  echo "  recordatorio: cambiaste constructores/clases; revisa app/src/test por construcciones posicionales."
fi

if [ "$fail" -ne 0 ]; then
  echo "pre-push: FALLO. Corrige antes de hacer push."
  exit 1
fi
echo "pre-push: OK."
