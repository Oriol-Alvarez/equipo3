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
