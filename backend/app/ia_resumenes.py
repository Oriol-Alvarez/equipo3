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
import re
from typing import Any

import httpx

from .config import settings

logger = logging.getLogger("sonrisas.ia")

# Caché en memoria para no repetir llamadas a la API de IA si los datos no cambiaron:
# clave: (child_id, semana_id, hash_datos) -> texto_generado
_CACHE_RESUMEN_IA: dict[tuple[str, str, str], str] = {}

# Minimización de datos: el proveedor de IA nunca recibe el nombre del niño ni su id.
# El nombre se cambia por este marcador antes de enviar y se vuelve a poner al recibir el texto.
MARCADOR_NOMBRE = "[NIÑO]"


def _nombres_a_ocultar(nombres: list[str | None]) -> list[str]:
    """Nombre completo, nombre corto y cada palabra del nombre (de la más larga a la más corta)."""
    salida: set[str] = set()
    for n in nombres:
        n = (n or "").strip()
        if not n:
            continue
        salida.add(n)
        salida.update(p for p in re.split(r"\s+", n) if len(p) >= 3)
    return sorted(salida, key=len, reverse=True)


def seudonimizar(valor: Any, nombres: list[str]) -> Any:
    """Reemplaza los nombres por MARCADOR_NOMBRE en cualquier texto del contexto."""
    if isinstance(valor, str):
        for n in nombres:
            valor = re.sub(rf"\b{re.escape(n)}\b", MARCADOR_NOMBRE, valor, flags=re.IGNORECASE)
        return valor
    if isinstance(valor, list):
        return [seudonimizar(v, nombres) for v in valor]
    if isinstance(valor, dict):
        return {k: seudonimizar(v, nombres) for k, v in valor.items()}
    return valor


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
6. SIN FRASES DE CIERRE: Termina de inmediato tras sintetizar los hechos. Prohibido añadir frases hechas, lemas ni despedidas (como "¡Sigamos cosechando sonrisas!", "Hasta la próxima semana", etc.).
7. NOMBRE: Escribe {MARCADOR_NOMBRE} tal cual, sin cambiarlo, donde iría el nombre del niño."""


def construir_prompt_mensual(contexto: dict[str, Any]) -> str:
    """Prompt del reporte mensual (RF-19). El contexto ya viene sin el nombre del niño."""
    def lista(clave: str) -> str:
        valores = contexto.get(clave) or []
        return "\n  * " + "\n  * ".join(valores) if valores else " ninguno"

    datos = "\n".join([
        f"DATOS DEL MES ({contexto.get('mes', '')}) PARA: {MARCADOR_NOMBRE}",
        f"- Asistencia: {contexto.get('asistencia', '')} (llegada promedio: {contexto.get('llegada_promedio', '')})",
        f"- Alimentación: promedio {contexto.get('alimentacion', '')}; días en que comió poco o nada: {contexto.get('dias_comio_poco', 0)}",
        f"- Siesta: {contexto.get('siesta', '')}",
        f"- Comportamiento y ánimo:{lista('comportamiento')}",
        f"- Salud y accidentes:{lista('salud')}",
        f"- Planeación de la sala:{lista('actividades_plan')}",
        f"- Comentarios de las educadoras en la bitácora:{lista('comentarios_bitacora')}",
        f"- Observaciones semanales:{lista('observaciones')}",
        f"- Puntos a atender:{lista('puntos_atencion')}",
    ])
    return f"""Eres un pedagogo infantil cálido y profesional de la estancia infantil 'Sonrisas de Cristal'.
Redacta el REPORTE MENSUAL para la familia de {MARCADOR_NOMBRE}, con base solo en estos registros:

{datos}

REGLAS:
1. Entre 120 y 180 palabras, en 2 o 3 párrafos cortos, sin viñetas, títulos ni saludos.
2. Primer párrafo: asistencia, alimentación y descanso. Segundo: comportamiento y lo que hizo o disfrutó (usa los comentarios de la bitácora). Tercero (si hay algo): salud y lo que conviene que la familia atienda.
3. No inventes nada que no esté en los datos. Si un dato falta, no lo menciones.
4. No des diagnósticos médicos ni consejos clínicos; si hubo algo de salud, solo descríbelo y sugiere platicarlo con la educadora o su médico.
5. Escribe {MARCADOR_NOMBRE} tal cual, sin cambiarlo, donde iría el nombre del niño.
6. Sin frases de cierre ni despedidas."""


def _llamar_gemini(prompt: str, api_key: str, timeout_s: float = 6.0, max_tokens: int = 180) -> str | None:
    """Consulta la API de Google Gemini (gemini-3.5-flash-lite / gemini-3.5-flash)."""
    modelos = ["gemini-3.5-flash-lite", "gemini-3.5-flash", "gemini-flash-latest"]
    with httpx.Client(timeout=timeout_s) as client:
        for model in modelos:
            url = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={api_key}"
            payload = {
                "contents": [{"parts": [{"text": prompt}]}],
                "generationConfig": {
                    "temperature": 0.2,
                    "maxOutputTokens": max_tokens,
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


def _llamar_openai(prompt: str, api_key: str, timeout_s: float = 4.0, max_tokens: int = 160) -> str | None:
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
        "max_tokens": max_tokens,
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
    nombre = str(contexto.get("nombre", ""))
    cache_key = (child_id, semana_id, _generar_fingerprint(contexto))

    # Al proveedor solo va el contexto sin id ni nombre del niño
    enviar = seudonimizar({k: v for k, v in contexto.items() if k != "child_id"}, _nombres_a_ocultar([nombre]))
    return _generar(construir_prompt(enviar), cache_key, nombre, gemini_key, openai_key, max_tokens=180)


def generar_reporte_mensual_ia(contexto: dict[str, Any], nombres: list[str | None]) -> str | None:
    """Reporte mensual (RF-19) redactado con IA. None si no hay llave o falla: se usa el texto determinista."""
    gemini_key = (settings.gemini_api_key or "").strip()
    openai_key = (settings.openai_api_key or "").strip()
    if not gemini_key and not openai_key:
        return None

    nombre = str(contexto.get("nombre", ""))
    cache_key = (str(contexto.get("child_id", "")), "mes_" + str(contexto.get("mes_id", "")), _generar_fingerprint(contexto))
    enviar = seudonimizar(
        {k: v for k, v in contexto.items() if k not in ("child_id", "nombre")},
        _nombres_a_ocultar([nombre, *nombres]),
    )
    return _generar(construir_prompt_mensual(enviar), cache_key, nombre, gemini_key, openai_key, max_tokens=420)


def _generar(
    prompt: str, cache_key: tuple[str, str, str], nombre: str, gemini_key: str, openai_key: str, max_tokens: int
) -> str | None:
    if cache_key in _CACHE_RESUMEN_IA:
        return _CACHE_RESUMEN_IA[cache_key]
    try:
        resultado: str | None = None
        if gemini_key:
            resultado = _llamar_gemini(prompt, gemini_key, timeout_s=8.0, max_tokens=max_tokens)
        elif openai_key:
            resultado = _llamar_openai(prompt, openai_key, timeout_s=8.0, max_tokens=max_tokens)
        if resultado:
            # El nombre se vuelve a poner aquí, en el servidor de la estancia
            resultado = resultado.replace(MARCADOR_NOMBRE, nombre or "el niño").strip().strip('"')
            _CACHE_RESUMEN_IA[cache_key] = resultado
            return resultado
    except Exception as e:
        logger.warning("Fallo al generar texto con IA: %s. Se usará el texto determinista.", e)
    return None
