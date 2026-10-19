#!/bin/bash
# Script para correr OWASP ZAP API Scan contra el backend local
# Importa el esquema OpenAPI (Swagger) y configura el Token JWT

mkdir -p zap_reports
chmod 777 zap_reports

TARGET="http://host.docker.internal:8000"
LOCAL_TARGET="http://127.0.0.1:8000"
OPENAPI_URL="${TARGET}/openapi.json"

echo "1. Obteniendo token JWT de educadora@sonrisas.edu (desde contenedor Docker)..."
TOKEN=$(docker compose exec api python -c "import urllib.request, json; print(json.loads(urllib.request.urlopen(urllib.request.Request('http://127.0.0.1:8000/api/v1/auth/login', data=json.dumps({'email': 'educadora@sonrisas.edu', 'password': 'Password123!', 'role': 'EDUCADORA', 'codigoProfesor': 'sonrisas-docente-2026'}).encode('utf-8'), headers={'Content-Type': 'application/json'})).read().decode())['token'])" | tr -d '\r' | tr -d '\n')

if [ -z "$TOKEN" ] || [[ "$TOKEN" == Error* ]]; then
    echo "Error: No se pudo obtener el token. ¿Está el backend corriendo?"
    echo "$TOKEN"
    exit 1
fi

echo "Token obtenido correctamente."
echo "2. Configurando reglas de inyección de autenticación (ZAP Replacer)..."

# Escribimos las propiedades de configuración para inyectar el Bearer Token
cat << EOF > zap_reports/auth.prop
replacer.full_list(0).description=AuthHeader
replacer.full_list(0).enabled=true
replacer.full_list(0).matchtype=req_header
replacer.full_list(0).matchstr=Authorization
replacer.full_list(0).regex=false
replacer.full_list(0).replacement=Bearer $TOKEN
EOF

echo "3. Iniciando escaneo de la API con ZAP (OpenAPI) usando autenticación JWT..."
# Corremos el escáner específico de APIs de ZAP
docker run --rm -v "$(pwd -W)/zap_reports:/zap/wrk/:rw" ghcr.io/zaproxy/zaproxy:stable zap-api-scan.py -t $OPENAPI_URL -f openapi -r zap_api_report.html -z "-configfile /zap/wrk/auth.prop"

echo "4. Iniciando escaneo Baseline con ZAP..."
docker run --rm -v "$(pwd -W)/zap_reports:/zap/wrk/:rw" ghcr.io/zaproxy/zaproxy:stable zap-baseline.py -t $TARGET -r zap_report.html

echo "Escaneo finalizado. Revisa zap_reports/zap_api_report.html y zap_reports/zap_report.html"
