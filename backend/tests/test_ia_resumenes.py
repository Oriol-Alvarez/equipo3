import pytest
from app.config import settings
from app.ia_resumenes import (
    _CACHE_RESUMEN_IA,
    construir_prompt,
    generar_resumen_ejecutivo_ia,
)


@pytest.fixture(autouse=True)
def limpiar_cache_ia():
    _CACHE_RESUMEN_IA.clear()
    yield
    _CACHE_RESUMEN_IA.clear()


def test_construir_prompt_incluye_todos_los_elementos():
    contexto = {
        "nombre": "Mateo",
        "rango_fechas": "Semana del 22 al 26 de Septiembre, 2026",
        "asistencia": "5 de 5 días",
        "llegada_promedio": "08:15 AM",
        "alimentacion": "95%",
        "notas_comida": ["Comió toda la fruta", "Pidió más sopa"],
        "siesta": "Durmió siesta 5 días, promedio 1h 20m",
        "animo": "Mayormente feliz",
        "salud": "Sin incidentes ni malestares",
        "actividades_plan": ["Taller de pintura con dedos", "Modelado con plastilina"],
        "actividades_bitacora": ["Juego en el patio: construyó torres"],
        "observaciones": ["Educadora: Estuvo muy concentrado", "Familia: El fin de semana durmió bien"],
    }
    prompt = construir_prompt(contexto)
    assert "Mateo" in prompt
    assert "Semana del 22 al 26 de Septiembre, 2026" in prompt
    assert "5 de 5 días" in prompt
    assert "Taller de pintura con dedos" in prompt
    assert "Comió toda la fruta" in prompt
    assert "Estuvo muy concentrado" in prompt
    assert "Resumen Ejecutivo" in prompt
    assert "Comentarios y actividades registradas en bitácora por educadoras" in prompt
    assert "LONGITUD MÁXIMA EN PANTALLA" in prompt
    assert "teléfono móvil" in prompt


def test_sin_api_key_devuelve_none(monkeypatch):
    monkeypatch.setattr(settings, "gemini_api_key", "")
    monkeypatch.setattr(settings, "openai_api_key", "")

    resultado = generar_resumen_ejecutivo_ia({
        "child_id": "mateo",
        "semana_id": "2026-09-22",
        "nombre": "Mateo",
    })
    assert resultado is None


def test_con_gemini_simulado_devuelve_texto_y_guarda_en_cache(monkeypatch):
    monkeypatch.setattr(settings, "gemini_api_key", "clave-falsa-de-prueba")
    monkeypatch.setattr(settings, "openai_api_key", "")

    texto_ia_esperado = "Mateo tuvo una semana excelente, asistiendo los 5 días y disfrutando mucho de las actividades sensoriales."

    def fake_llamar_gemini(prompt: str, api_key: str, timeout_s: float = 4.0) -> str:
        assert api_key == "clave-falsa-de-prueba"
        assert "Mateo" in prompt
        return texto_ia_esperado

    from app import ia_resumenes
    monkeypatch.setattr(ia_resumenes, "_llamar_gemini", fake_llamar_gemini)

    contexto = {
        "child_id": "mateo",
        "semana_id": "2026-09-22",
        "nombre": "Mateo",
        "asistencia": "5 de 5 días",
    }
    resultado = generar_resumen_ejecutivo_ia(contexto)
    assert resultado == texto_ia_esperado

    # Segunda llamada: debe responder desde la caché sin volver a llamar a fake_llamar_gemini
    llamadas = 0

    def fail_llamar(*args, **kwargs):
        nonlocal llamadas
        llamadas += 1
        return "nuevo"

    monkeypatch.setattr(ia_resumenes, "_llamar_gemini", fail_llamar)
    resultado_cache = generar_resumen_ejecutivo_ia(contexto)
    assert resultado_cache == texto_ia_esperado
    assert llamadas == 0


def test_fallo_de_red_o_error_de_ia_degrada_con_gracia_a_none(monkeypatch):
    monkeypatch.setattr(settings, "gemini_api_key", "clave-falsa")

    from app import ia_resumenes

    def raise_error(*args, **kwargs):
        raise RuntimeError("Timeout o error de conexión con la IA")

    monkeypatch.setattr(ia_resumenes, "_llamar_gemini", raise_error)

    resultado = generar_resumen_ejecutivo_ia({
        "child_id": "lucia",
        "semana_id": "2026-09-22",
        "nombre": "Lucía",
    })
    assert resultado is None
