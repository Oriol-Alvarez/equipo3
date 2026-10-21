"""Módulo opcional de síntesis con Inteligencia Artificial para el Resumen Semanal.

Sintetiza todos los registros de la semana de un alumno (asistencia, alimentación,
descanso, ánimo, incidentes de salud, actividades pedagógicas y observaciones de la
educadora o la familia) en un resumen ejecutivo humano, empático y profesional.

Soporta Google Gemini y OpenAI. Si no hay API key configurada o ocurre un fallo de red,
devuelve None y el sistema continúa con el resumen determinista tradicional.
"""

from __future__ import annotations

import hashlib
import json
import logging
from typing import Any

import httpx

from .config import settings

logger = logging.getLogger("sonrisas.ia")

# Caché en memoria para no repetir llamadas a la API de IA si los datos no cambiaron:
# clave: (child_id, semana_id, hash_datos) -> texto_generado
_CACHE_RESUMEN_IA: dict[tuple[str, str, str], str] = {}


def _generar_fingerprint(contexto: dict[str, Any]) -> str:
    """Calcula un hash de los datos de entrada para validar la caché."""
    serializado = json.dumps(contexto, sort_keys=True, ensure_ascii=False, default=str)
    return hashlib.sha256(serializado.encode("utf-8")).hexdigest()[:16]


def construir_prompt(contexto: dict[str, Any]) -> str:
    """Arma el prompt estructurado con todas las evidencias de la semana del niño."""
    nombre = contexto.get("nombre", "el alumno")
    rango = contexto.get("rango_fechas", "")
    asistencia = contexto.get("asistencia", "")
    llegada = contexto.get("llegada_promedio", "")
    alimentacion = contexto.get("alimentacion", "")
    notas_comida = contexto.get("notas_comida", [])
    siesta = contexto.get("siesta", "")
    animo = contexto.get("animo", "")
    salud = contexto.get("salud", "")
    actividades_plan = contexto.get("actividades_plan", [])
    actividades_bitacora = contexto.get("actividades_bitacora", [])
    observaciones = contexto.get("observaciones", [])

    secciones: list[str] = [
        f"DATOS DE LA SEMANA ({rango}) PARA: {nombre}",
        f"- Asistencia: {asistencia} (hora promedio de llegada: {llegada})",
        f"- Alimentación: {alimentacion}",
    ]
    if notas_comida:
        secciones.append(f"- Notas de comida: {'; '.join(notas_comida)}")
    secciones.append(f"- Siesta y descanso: {siesta}")
    secciones.append(f"- Estado de ánimo: {animo}")
    secciones.append(f"- Salud y seguridad: {salud}")

    if actividades_plan:
        secciones.append(f"- Planeación pedagógica: {', '.join(actividades_plan)}")
    if actividades_bitacora:
        items_act = "\n  * ".join(actividades_bitacora)
        secciones.append(f"- Comentarios y actividades registradas en bitácora por educadoras:\n  * {items_act}")
    if observaciones:
        items_obs = "\n  * ".join(observaciones)
        secciones.append(f"- Observaciones semanales (educadores/familia):\n  * {items_obs}")

    datos_str = "\n".join(secciones)

    return f"""Eres un pedagogo infantil cálido y profesional en la guardería 'Sonrisas'.
Redacta el 'Resumen Ejecutivo' semanal para la ficha móvil de la familia de {nombre}.

{datos_str}

REGLAS ESTRICTAS DE FORMATO Y EXTENSIÓN:
1. LONGITUD MÁXIMA EN PANTALLA: Máximo de 7 a 8 líneas en la pantalla de un teléfono móvil (entre 50 y 65 palabras en total, NUNCA más de 70 palabras). Debe ser muy sintético, fluido y directo.
2. ESTRUCTURA: Un único párrafo continuo. Prohibido usar viñetas, listas, saltos de línea, títulos o comillas envolventes.
3. INICIO DIRECTO: Comienza directamente hablando de {nombre} y su semana (ej. "{nombre} ha completado una semana...", "{nombre} disfrutó de una semana..."). Prohibido saludar ("Estimada familia", "Hola", etc.).
4. COMENTARIOS DE ACTIVIDADES: Es indispensable que integres de forma natural algún comentario u observación concreta de las actividades registradas en la bitácora (qué hizo, qué disfrutó o qué destacó la educadora).
5. SÍNTESIS BÁSICA: Integra con agilidad asistencia, alimentación, descanso, ánimo y que estuvo bien de salud (o la atención requerida si hubo algún incidente).
6. SIN FRASES DE CIERRE: Termina de inmediato tras sintetizar los hechos. Prohibido añadir frases hechas, lemas ni despedidas (como "¡Sigamos cosechando sonrisas!", "Hasta la próxima semana", etc.)."""


def _llamar_gemini(prompt: str, api_key: str, timeout_s: float = 6.0) -> str | None:
    """Consulta la API de Google Gemini (gemini-3.5-flash-lite / gemini-3.5-flash)."""
    modelos = ["gemini-3.5-flash-lite", "gemini-3.5-flash", "gemini-flash-latest"]
    with httpx.Client(timeout=timeout_s) as client:
        for model in modelos:
            url = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={api_key}"
            payload = {
                "contents": [{"parts": [{"text": prompt}]}],
                "generationConfig": {
                    "temperature": 0.2,
                    "maxOutputTokens": 180,
                },
            }
            try:
                resp = client.post(url, json=payload)
                if resp.status_code == 200:
                    data = resp.json()
                    candidatos = data.get("candidates", [])
                    if candidatos:
                        partes = candidatos[0].get("content", {}).get("parts", [])
                        if partes and "text" in partes[0]:
                            return partes[0]["text"].strip()
                else:
                    logger.warning("Modelo %s devolvió status %s: %s", model, resp.status_code, resp.text[:120])
            except Exception as e:
                logger.warning("Fallo al consultar modelo %s: %s", model, e)
    return None


def _llamar_openai(prompt: str, api_key: str, timeout_s: float = 4.0) -> str | None:
    """Consulta la API de OpenAI (gpt-4o-mini)."""
    url = "https://api.openai.com/v1/chat/completions"
    headers = {"Authorization": f"Bearer {api_key}"}
    payload = {
        "model": "gpt-4o-mini",
        "messages": [
            {"role": "system", "content": "Eres un pedagogo infantil que redacta resúmenes ejecutivos breves y cálidos para familias."},
            {"role": "user", "content": prompt},
        ],
        "temperature": 0.2,
        "max_tokens": 160,
    }
    with httpx.Client(timeout=timeout_s) as client:
        resp = client.post(url, headers=headers, json=payload)
        if resp.status_code != 200:
            logger.warning("Error de OpenAI API (%s): %s", resp.status_code, resp.text)
            return None
        data = resp.json()
        elecciones = data.get("choices", [])
        if elecciones:
            return elecciones[0].get("message", {}).get("content", "").strip()
    return None


def generar_resumen_ejecutivo_ia(contexto: dict[str, Any]) -> str | None:
    """Genera el resumen ejecutivo usando IA si hay API key disponible.

    Si no hay API key configurada o ocurre cualquier error, devuelve None
    para que se use la plantilla determinista predeterminada.
    """
    gemini_key = (settings.gemini_api_key or "").strip()
    openai_key = (settings.openai_api_key or "").strip()

    if not gemini_key and not openai_key:
        return None

    child_id = str(contexto.get("child_id", ""))
    semana_id = str(contexto.get("semana_id", ""))
    fingerprint = _generar_fingerprint(contexto)
    cache_key = (child_id, semana_id, fingerprint)

    if cache_key in _CACHE_RESUMEN_IA:
        return _CACHE_RESUMEN_IA[cache_key]

    prompt = construir_prompt(contexto)

    try:
        resultado: str | None = None
        if gemini_key:
            resultado = _llamar_gemini(prompt, gemini_key)
        elif openai_key:
            resultado = _llamar_openai(prompt, openai_key)

        if resultado:
            _CACHE_RESUMEN_IA[cache_key] = resultado
            return resultado
    except Exception as e:
        logger.warning("Fallo al generar resumen con IA: %s. Se usará el resumen determinista.", e)

    return None
