"""Pruebas de cumplimiento de contrato según la especificación de API_ENTRYPOINTS.md."""

from fastapi.testclient import TestClient

from .conftest import PNG_CHICO, Persona


def test_salas_endpoint_contrato(cliente: TestClient, educadora: Persona):
    r = cliente.get("/api/v1/rooms", headers=educadora.headers)
    assert r.status_code == 200
    salas = r.json()
    assert isinstance(salas, list)
    assert len(salas) >= 1
    primera = salas[0]
    for campo in ("id", "name", "level", "presentCount", "totalCount"):
        assert campo in primera


def test_alumnos_sala_contrato(cliente: TestClient, educadora: Persona):
    r = cliente.get("/api/v1/rooms/sala_1a/children", headers=educadora.headers)
    assert r.status_code == 200
    alumnos = r.json()
    assert isinstance(alumnos, list)
    assert len(alumnos) >= 1
    alumno = alumnos[0]
    for campo in ("id", "fullName", "shortName", "ageText", "roomText", "statusText", "isPresent"):
        assert campo in alumno


def test_expediente_completo_contrato(cliente: TestClient, familiar: Persona):
    r = cliente.get("/api/v1/children/mateo_garcia/full-profile", headers=familiar.headers)
    assert r.status_code == 200
    expediente = r.json()
    for campo in (
        "childId",
        "birthDate",
        "enrollmentDate",
        "studentId",
        "bloodType",
        "pediatrician",
        "pediatricianPhone",
        "emergencyPhone",
        "emergencyPhoneRelation",
        "motherName",
        "fatherName",
        "authorizedPickups",
        "medicalNotes",
    ):
        assert campo in expediente


def test_linea_de_tiempo_contrato(cliente: TestClient, familiar: Persona):
    r = cliente.get("/api/v1/children/mateo_garcia/timeline", headers=familiar.headers)
    assert r.status_code == 200
    items = r.json()
    assert isinstance(items, list)
    assert len(items) >= 1
    primer_item = items[0]
    assert "type" in primer_item
    assert "time" in primer_item
    assert "id" in primer_item


def test_grupos_familiares_contrato(cliente: TestClient, educadora: Persona):
    r = cliente.get("/api/v1/family-groups", headers=educadora.headers)
    assert r.status_code == 200
    grupos = r.json()
    assert isinstance(grupos, list)
    assert len(grupos) >= 1
    g = grupos[0]
    for campo in ("id", "title", "memberChildIds", "createdById", "lastMessage"):
        assert campo in g


def test_catalogo_objetos_perdidos_contrato(cliente: TestClient, familiar: Persona):
    r = cliente.get("/api/v1/lost-objects", headers=familiar.headers)
    assert r.status_code == 200
    objetos = r.json()
    assert isinstance(objetos, list)
    assert len(objetos) >= 1
    obj = objetos[0]
    for campo in ("id", "title", "description", "category", "room", "isClaimed"):
        assert campo in obj


def test_subida_media_y_descarga_contrato(cliente: TestClient, educadora: Persona):
    # Subida de imagen
    r_subida = cliente.post(
        "/api/v1/media/upload",
        headers=educadora.headers,
        files={"file": ("prueba.png", PNG_CHICO, "image/png")},
        data={"folder": "chat"},
    )
    assert r_subida.status_code == 201
    media_data = r_subida.json()
    assert "fileId" in media_data
    assert "url" in media_data
    assert media_data["mimeType"] == "image/png"

    # Descarga de imagen
    file_id = media_data["fileId"]
    r_bajada = cliente.get(f"/api/v1/media/{file_id}")
    assert r_bajada.status_code == 200
    assert r_bajada.content == PNG_CHICO
    assert "ETag" in r_bajada.headers


def test_registrar_actividad_y_persistencia_timeline(
    cliente: TestClient, educadora: Persona, familiar: Persona
):
    # 1. Educadora registra una actividad pedagógica/alimentación para Mateo
    payload = {
        "time": "10:30 AM",
        "category": "Alimentación",
        "title": "Alimentación: Completo",
        "description": "Mateo comió toda su fruta y merienda.",
        "portionOption": "Completo",
    }
    r_post = cliente.post(
        "/api/v1/children/mateo_garcia/activities",
        headers=educadora.headers,
        json=payload,
    )
    assert r_post.status_code == 201
    datos = r_post.json()
    assert datos["status"] == "CREADA"
    act_id = datos["id"]
    assert datos["timelineItem"]["type"] == "ACTIVITY_CARD"
    assert datos["timelineItem"]["description"] == "Mateo comió toda su fruta y merienda."

    # 2. Familiar consulta la línea de tiempo del día y comprueba que la actividad está guardada
    r_get = cliente.get("/api/v1/children/mateo_garcia/timeline", headers=familiar.headers)
    assert r_get.status_code == 200
    timeline = r_get.json()
    actividad_encontrada = next((item for item in timeline if item["id"] == act_id), None)
    assert actividad_encontrada is not None
    assert actividad_encontrada["type"] == "ACTIVITY_CARD"
    assert actividad_encontrada["category"] == "Alimentación"
    assert actividad_encontrada["portionLabel"] == "Completo"
    assert actividad_encontrada["description"] == "Mateo comió toda su fruta y merienda."


def test_mensaje_con_hora_personalizada_y_orden_cronologico_timeline(
    cliente: TestClient, educadora: Persona, familiar: Persona
):
    # 1. Enviar mensaje con hora 09:15 AM
    r_msg1 = cliente.post(
        "/api/v1/chats/bitacora/mateo_garcia/messages",
        headers=familiar.headers,
        json={"message": "Mensaje de las 9:15", "time": "09:15 AM"},
    )
    assert r_msg1.status_code == 201
    assert r_msg1.json()["time"] == "09:15 AM"

    # 2. Registrar actividad con hora 10:00 AM
    r_act = cliente.post(
        "/api/v1/children/mateo_garcia/activities",
        headers=educadora.headers,
        json={
            "time": "10:00 AM",
            "category": "Juego",
            "title": "Juego: Patio",
            "description": "Jugó en el patio",
        },
    )
    assert r_act.status_code == 201

    # 3. Enviar mensaje posterior con hora 11:30 AM
    r_msg2 = cliente.post(
        "/api/v1/chats/bitacora/mateo_garcia/messages",
        headers=educadora.headers,
        json={"message": "Mensaje de las 11:30", "time": "11:30 AM"},
    )
    assert r_msg2.status_code == 201
    assert r_msg2.json()["time"] == "11:30 AM"

    # 4. Obtener timeline y verificar que los elementos están ordenados cronológicamente
    r_timeline = cliente.get("/api/v1/children/mateo_garcia/timeline", headers=familiar.headers)
    assert r_timeline.status_code == 200
    timeline = r_timeline.json()

    id_msg1 = r_msg1.json()["id"]
    id_act = r_act.json()["id"]
    id_msg2 = r_msg2.json()["id"]

    idx_msg1 = next(i for i, item in enumerate(timeline) if item["id"] == id_msg1)
    idx_act = next(i for i, item in enumerate(timeline) if item["id"] == id_act)
    idx_msg2 = next(i for i, item in enumerate(timeline) if item["id"] == id_msg2)

    # 09:15 AM (mensaje) < 10:00 AM (actividad) < 11:30 AM (mensaje)
    assert idx_msg1 < idx_act < idx_msg2


def test_elementos_diferentes_dias_ordenados_por_dia_y_hora(
    cliente: TestClient, educadora: Persona, familiar: Persona
):
    # Actividad de un día previo a mediodía (12:20 PM el 12/10/2026)
    r_act = cliente.post(
        "/api/v1/children/mateo_garcia/activities",
        headers=educadora.headers,
        json={
            "time": "12:20 PM",
            "date": "12/10/2026",
            "category": "Alimentación",
            "title": "Almuerzo",
            "description": "Comió su plato principal",
        },
    )
    assert r_act.status_code == 201
    id_act = r_act.json()["id"]

    # Mensaje enviado un día posterior por la mañana temprana (08:30 AM el 19/10/2026)
    r_msg = cliente.post(
        "/api/v1/chats/bitacora/mateo_garcia/messages",
        headers=familiar.headers,
        json={
            "message": "Mensaje de días después a primera hora",
            "time": "08:30 AM",
            "date": "19/10/2026",
        },
    )
    assert r_msg.status_code == 201
    id_msg = r_msg.json()["id"]

    r_timeline = cliente.get("/api/v1/children/mateo_garcia/timeline", headers=familiar.headers)
    assert r_timeline.status_code == 200
    timeline = r_timeline.json()

    idx_act = next(i for i, item in enumerate(timeline) if item["id"] == id_act)
    idx_msg = next(i for i, item in enumerate(timeline) if item["id"] == id_msg)

    # 12/10/2026 12:20 PM debe ir ANTES de 19/10/2026 08:30 AM
    assert idx_act < idx_msg

