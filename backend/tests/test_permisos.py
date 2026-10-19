"""Pruebas de Control de Acceso Basado en Roles (RBAC):
Verifica que las acciones protegidas sean denegadas si el rol no es el adecuado.
"""

from fastapi.testclient import TestClient

from .conftest import Persona


def test_familiar_no_puede_registrar_ingreso_da_403(cliente: TestClient, familiar: Persona):
    r = cliente.post(
        "/api/v1/attendance/ingreso",
        headers=familiar.headers,
        json={
            "childId": "mateo_garcia",
            "date": "12/10/2026",
            "entryTime": "08:15",
            "assistantName": "Intento No Autorizado",
        },
    )
    assert r.status_code == 403
    assert r.json()["code"] == "rol_insuficiente"


def test_educadora_puede_registrar_ingreso(cliente: TestClient, educadora: Persona):
    r = cliente.post(
        "/api/v1/attendance/ingreso",
        headers=educadora.headers,
        json={
            "childId": "mateo_garcia",
            "date": "12/10/2026",
            "entryTime": "08:15",
            "assistantName": "Laura Gómez",
            "isChildSick": False,
            "observations": "Ingreso regular",
        },
    )
    assert r.status_code == 201
    assert r.json()["status"] == "REGISTRADO"
    assert r.json()["isIngreso"] is True


def test_educadora_no_puede_publicar_anuncios_da_403(cliente: TestClient, educadora: Persona):
    # La publicación de avisos generales es potestad exclusiva de DIRECCION
    r = cliente.post(
        "/api/v1/announcements",
        headers=educadora.headers,
        json={
            "title": "Aviso no permitido",
            "summary": "Resumen",
            "fullContent": "Contenido completo",
        },
    )
    assert r.status_code == 403
    assert r.json()["code"] == "rol_insuficiente"


def test_direccion_puede_publicar_anuncios(cliente: TestClient, direccion: Persona):
    r = cliente.post(
        "/api/v1/announcements",
        headers=direccion.headers,
        json={
            "title": "Circular General Oficial",
            "summary": "Información relevante",
            "fullContent": "Se convoca a reunión este viernes.",
            "author": "Dirección General",
        },
    )
    assert r.status_code == 201
    assert r.json()["title"] == "Circular General Oficial"


def test_familiar_puede_enviar_sugerencia(cliente: TestClient, familiar: Persona):
    r = cliente.post(
        "/api/v1/suggestions",
        headers=familiar.headers,
        json={
            "childId": "mateo_garcia",
            "roomName": "Sala 1A",
            "category": "ALIMENTACION",
            "subject": "Propuesta de fruta",
            "content": "¿Podrían incluir más plátano en las mañanas?",
        },
    )
    assert r.status_code == 201
    data = r.json()
    assert data["status"] == "PENDIENTE"
    assert data["parentName"] == "María López Fernández"


def test_familiar_no_puede_cambiar_estado_de_sugerencia_da_403(cliente: TestClient, familiar: Persona):
    r = cliente.patch(
        "/api/v1/suggestions/sug_1/status",
        headers=familiar.headers,
        json={
            "status": "ATENDIDA",
            "response": "Auto-aprobado",
            "responderName": "Familiar",
        },
    )
    assert r.status_code == 403
    assert r.json()["code"] == "rol_insuficiente"


def test_educadora_puede_responder_sugerencia(cliente: TestClient, educadora: Persona):
    r = cliente.patch(
        "/api/v1/suggestions/sug_1/status",
        headers=educadora.headers,
        json={
            "status": "ATENDIDA",
            "response": "Coordinado con el equipo docente.",
            "responderName": "Laura Gómez (Educadora)",
        },
    )
    assert r.status_code == 200
    assert r.json()["status"] == "ATENDIDA"
    assert r.json()["response"] == "Coordinado con el equipo docente."


def test_educadora_puede_marcar_sugerencia_en_progreso_como_atendida(cliente: TestClient, educadora: Persona):
    # sug_2 inicia en estado EN_REVISION en seed
    r = cliente.patch(
        "/api/v1/suggestions/sug_2/status",
        headers=educadora.headers,
        json={
            "status": "ATENDIDA",
            "response": "Instalación completada y verificada.",
            "responderName": "Mantenimiento / Dirección",
        },
    )
    assert r.status_code == 200
    datos = r.json()
    assert datos["status"] == "ATENDIDA"
    assert "Instalación completada" in datos["response"]

