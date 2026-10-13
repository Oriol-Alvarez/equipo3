#!/bin/bash
# Script de pruebas de humo (smoke test) para la API de Sonrisas.
# Uso:
#   bash smoke.sh                      -> http://127.0.0.1:8000/api/v1
#   bash smoke.sh https://otro-host/api/v1
set -e

B=${1:-http://127.0.0.1:8000/api/v1}
echo "Probando API en: $B"

j() { curl -s -w ' [%{http_code}]\n' "$@"; }
tok() { grep -oE "\"$1\": ?\"[^\"]+" | cut -d'"' -f4; }

echo "== 1 Health check (200)"
curl -fsS $B/health > /dev/null && echo "  [200 OK]"

echo "== 2 Login Familiar (200)"
curl -s -X POST $B/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"familiar@sonrisas.edu","password":"Password123!","role":"FAMILIAR"}' > l_fam.json
FAM_TOKEN=$(tok token < l_fam.json)
FAM_REFRESH=$(tok refreshToken < l_fam.json)
echo "  Token obtenido para familiar: ${FAM_TOKEN:0:15}..."

echo "== 3 Login Educadora (200)"
curl -s -X POST $B/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"educadora@sonrisas.edu","password":"Password123!","role":"EDUCADORA"}' > l_edu.json
EDU_TOKEN=$(tok token < l_edu.json)
echo "  Token obtenido para educadora: ${EDU_TOKEN:0:15}..."

echo "== 4 Login Erróneo (401)"
COD=$(curl -s -o /dev/null -w "%{http_code}" -X POST $B/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"familiar@sonrisas.edu","password":"ClaveInvalida","role":"FAMILIAR"}')
[ "$COD" = "401" ] && echo "  Rechazado correctamente [401]" || (echo "  Fallo: esperaba 401 y dio $COD"; exit 1)

echo "== 5 Familiar intentando registrar ingreso (403)"
COD=$(curl -s -o /dev/null -w "%{http_code}" -X POST $B/attendance/ingreso \
  -H "Authorization: Bearer $FAM_TOKEN" -H 'Content-Type: application/json' \
  -d '{"childId":"mateo_garcia","date":"12/10/2026","entryTime":"08:15"}')
[ "$COD" = "403" ] && echo "  Protegido correctamente [403]" || (echo "  Fallo: esperaba 403 y dio $COD"; exit 1)

echo "== 6 Educadora registrando ingreso (201)"
curl -s -X POST $B/attendance/ingreso \
  -H "Authorization: Bearer $EDU_TOKEN" -H 'Content-Type: application/json' \
  -d '{"childId":"mateo_garcia","date":"12/10/2026","entryTime":"08:15","assistantName":"Laura Gómez","observations":"Llega bien"}' > att.json
grep -q '"status": ?"REGISTRADO"' att.json && echo "  Ingreso registrado [201]"

echo "== 7 Listar salas (200)"
curl -s -H "Authorization: Bearer $EDU_TOKEN" $B/rooms | grep -q "sala_1a" && echo "  Salas listadas [200]"

echo "== 8 Consultar línea de tiempo de Mateo (200)"
curl -s -H "Authorization: Bearer $FAM_TOKEN" "$B/children/mateo_garcia/timeline" | grep -q "Ingreso" && echo "  Línea de tiempo obtenida [200]"

echo "== 9 Subir imagen de prueba (201)"
echo "iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAIAAAD91JpzAAAAEElEQVR4nGPgi/ECIgYIBQASzgLRQRXuCAAAAABJRU5ErkJggg==" | base64 -d > prueba.png
curl -s -X POST $B/media/upload -H "Authorization: Bearer $EDU_TOKEN" -F file=@prueba.png -F folder=chat > m.json
FILE_ID=$(tok fileId < m.json)
[ -n "$FILE_ID" ] && echo "  Imagen subida con id $FILE_ID [201]"

echo "== 10 Descargar imagen con ETag (200 y 304)"
curl -s -D h.txt -o bajada.png $B/media/$FILE_ID
cmp -s prueba.png bajada.png && echo "  Imagen idéntica [200]"
ETAG=$(grep -i '^etag:' h.txt | tr -d '\r' | awk '{print $2}')
if [ -n "$ETAG" ]; then
  COD=$(curl -s -o /dev/null -w "%{http_code}" -H "If-None-Match: $ETAG" $B/media/$FILE_ID)
  [ "$COD" = "304" ] && echo "  Caché validado con ETag [304]"
fi

echo "== 11 Refrescar token (200)"
curl -s -X POST $B/auth/refresh -H 'Content-Type: application/json' -d "{\"refreshToken\":\"$FAM_REFRESH\"}" > ref.json
grep -q "token" ref.json && echo "  Token renovado [200]"

echo "== 12 Logout (204)"
COD=$(curl -s -o /dev/null -w "%{http_code}" -X POST $B/auth/logout -H 'Content-Type: application/json' -d "{\"refreshToken\":\"$FAM_REFRESH\"}")
[ "$COD" = "204" ] && echo "  Sesión cerrada [204]"

rm -f l_fam.json l_edu.json att.json m.json ref.json prueba.png bajada.png h.txt
echo "============================================="
echo "¡TODAS LAS PRUEBAS DE HUMO PASARON CON ÉXITO!"
echo "============================================="
