"""Pruebas de la entrega final.

- RF-11 / RNF-05: un reintento con la misma Idempotency-Key no duplica el registro.
- RF-03: la hora de asistencia se guarda bien sin importar el formato que mande la app.
- RF-07: la familia solo recibe la asistencia de sus hijos.
- RF-09 / RF-14: las fotos solo las ve quien tiene relación con el niño y cada consulta queda registrada.
- RF-10: la familia borra una foto de su hijo y el archivo desaparece del servidor.
"""

import uuid
from datetime import datetime

from fastapi.testclient import TestClient

from app.db import SessionLocal
from app.horario import fecha_local, minutos_del_dia
from app.models import Activity, AttendanceRecord, BitacoraMessage, MediaFile, PhotoAccess, PhotoDeletion

from .conftest import PNG_CHICO, Persona


def _login(cliente: TestClient, email: str, role: str) -> dict[str, str]:
    r = cliente.post("/api/v1/auth/login", json={"email": email, "password": "Password123!", "role": role})
    assert r.status_code == 200, r.text
    return {"Authorization": f"Bearer {r.json()['token']}"}


def _actividad(descripcion: str) -> dict:
    return {
        "time": "10:30 AM",
        "date": "22/10/2026",
        "category": "ALIMENTACION",
        "title": "Colación",
        "description": descripcion,
        "portionOption": "Todo",
    }


# ---------------------------------------------------------------- RF-11: sin duplicados


def test_reintento_con_la_misma_llave_no_duplica_la_actividad(cliente: TestClient, educadora: Persona):
    llave = f"pend_{uuid.uuid4().hex}"
    descripcion = f"Prueba de reintento {llave}"
    cabeceras = {**educadora.headers, "Idempotency-Key": llave}

    primera = cliente.post("/api/v1/children/mateo_garcia/activities", headers=cabeceras, json=_actividad(descripcion))
    segunda = cliente.post("/api/v1/children/mateo_garcia/activities", headers=cabeceras, json=_actividad(descripcion))

    assert primera.status_code == 201, primera.text
    assert segunda.status_code == 201
    assert segunda.json()["id"] == primera.json()["id"]
    assert segunda.headers.get("Idempotent-Replayed") == "true"
    with SessionLocal() as db:
        guardadas = db.query(Activity).filter(Activity.description == descripcion).count()
    assert guardadas == 1


def test_llaves_distintas_si_crean_dos_registros(cliente: TestClient, educadora: Persona):
    descripcion = f"Dos registros {uuid.uuid4().hex}"
    for _ in range(2):
        r = cliente.post(
            "/api/v1/children/mateo_garcia/activities",
            headers={**educadora.headers, "Idempotency-Key": uuid.uuid4().hex},
            json=_actividad(descripcion),
        )
        assert r.status_code == 201
    with SessionLocal() as db:
        assert db.query(Activity).filter(Activity.description == descripcion).count() == 2


def test_la_misma_llave_en_otra_operacion_se_rechaza(cliente: TestClient, educadora: Persona):
    llave = uuid.uuid4().hex
    cabeceras = {**educadora.headers, "Idempotency-Key": llave}
    r1 = cliente.post("/api/v1/children/mateo_garcia/activities", headers=cabeceras, json=_actividad("a"))
    assert r1.status_code == 201
    r2 = cliente.post(
        "/api/v1/children/mateo_garcia/timeline/events",
        headers=cabeceras,
        json={"time": "11:00 AM", "text": "Siesta", "iconType": "nap"},
    )
    assert r2.status_code == 422
    assert r2.json()["code"] == "llave_reutilizada"


def test_la_llave_de_otro_usuario_no_devuelve_su_respuesta(cliente: TestClient, educadora: Persona, direccion: Persona):
    llave = uuid.uuid4().hex
    r1 = cliente.post(
        "/api/v1/children/mateo_garcia/activities",
        headers={**educadora.headers, "Idempotency-Key": llave},
        json=_actividad("de la educadora"),
    )
    r2 = cliente.post(
        "/api/v1/children/mateo_garcia/activities",
        headers={**direccion.headers, "Idempotency-Key": llave},
        json=_actividad("de la dirección"),
    )
    assert r1.status_code == r2.status_code == 201
    assert r1.json()["id"] != r2.json()["id"]


def test_actividad_guarda_quien_la_capturo(cliente: TestClient, educadora: Persona):
    descripcion = f"Autor {uuid.uuid4().hex}"
    r = cliente.post("/api/v1/children/mateo_garcia/activities", headers=educadora.headers, json=_actividad(descripcion))
    assert r.status_code == 201
    with SessionLocal() as db:
        act = db.query(Activity).filter(Activity.description == descripcion).one()
    assert act.created_by_id == educadora.id
    assert act.created_by_name


# ---------------------------------------------------------------- RF-03: hora de asistencia


def test_minutos_del_dia_acepta_los_formatos_de_la_app():
    assert minutos_del_dia("08:15") == 8 * 60 + 15
    assert minutos_del_dia("08:15 AM") == 8 * 60 + 15
    assert minutos_del_dia("02:30 PM") == 14 * 60 + 30
    assert minutos_del_dia("12:10 PM") == 12 * 60 + 10
    assert minutos_del_dia("12:10 AM") == 10
    # Versiones anteriores de la app mandaban la hora de 24 h con AM/PM pegado
    assert minutos_del_dia("14:30 AM") == 14 * 60 + 30
    assert minutos_del_dia("15:20 PM") == 15 * 60 + 20
    assert minutos_del_dia("25:00") is None
    assert minutos_del_dia("hola") is None


def test_ingreso_y_egreso_guardan_la_hora_correcta(cliente: TestClient, educadora: Persona):
    r = cliente.post(
        "/api/v1/attendance/ingreso",
        headers=educadora.headers,
        json={"childId": "mateo_garcia", "date": "2026-10-22", "entryTime": "14:30",
              "assistantName": "Laura Gómez", "delivererName": "María López", "receiverStaffName": "Laura Gómez"},
    )
    assert r.status_code == 201, r.text
    r2 = cliente.post(
        "/api/v1/attendance/egreso",
        headers=educadora.headers,
        json={"childId": "mateo_garcia", "date": "2026-10-22", "exitTime": "17:05",
              "receiverTutorName": "Carlos García", "delivererStaffName": "Laura Gómez"},
    )
    assert r2.status_code == 201, r2.text
    with SessionLocal() as db:
        ingreso = db.get(AttendanceRecord, r.json()["id"])
        egreso = db.get(AttendanceRecord, r2.json()["id"])
    assert ingreso.time == "02:30 PM"
    assert ingreso.deliverer_name == "María López"
    assert ingreso.recorded_by_id == educadora.id
    assert egreso.time == "05:05 PM"
    assert egreso.receiver_tutor_name == "Carlos García"

    hoja = cliente.get("/api/v1/children/mateo_garcia/daily-logs?from=2026-10-22&to=2026-10-22", headers=educadora.headers)
    assert hoja.status_code == 200
    dia = hoja.json()[0]
    assert dia["ingreso"]["time"] == "14:30"
    assert dia["egreso"]["time"] == "17:05"
    assert dia["egreso"]["collectedBy"] == "Carlos García"


def test_hora_invalida_en_asistencia_da_422(cliente: TestClient, educadora: Persona):
    r = cliente.post(
        "/api/v1/attendance/ingreso",
        headers=educadora.headers,
        json={"childId": "mateo_garcia", "date": "2026-10-22", "entryTime": "a las ocho"},
    )
    assert r.status_code == 422
    assert r.json()["code"] == "hora_invalida"


def test_familia_solo_recibe_asistencia_de_sus_hijos(cliente: TestClient, educadora: Persona, familiar: Persona):
    cliente.post(
        "/api/v1/attendance/ingreso",
        headers=educadora.headers,
        json={"childId": "lucas_martinez", "date": "2026-10-22", "entryTime": "08:00"},
    )
    r = cliente.get("/api/v1/attendance/records", headers=familiar.headers)
    assert r.status_code == 200
    assert {x["childId"] for x in r.json()} <= {"mateo_garcia", "lucia_garcia"}
    otro = cliente.get("/api/v1/attendance/records?childId=lucas_martinez", headers=familiar.headers)
    assert otro.status_code == 403


def test_fecha_local_convierte_utc_a_hora_de_la_estancia():
    # 01:00 UTC del 22 de octubre son las 7:00 p. m. del 21 en Monterrey
    assert fecha_local(datetime(2026, 10, 22, 1, 0)) == "21/10/2026"
    assert fecha_local(datetime(2026, 10, 22, 18, 0)) == "22/10/2026"


# ---------------------------------------------------------------- RF-09 / RF-10 / RF-14: fotos


def _foto_en_bitacora(cliente: TestClient, educadora: Persona, child_id: str) -> tuple[str, str]:
    """Sube una foto y la manda en la bitácora del niño. Devuelve (fileId, id del mensaje)."""
    subida = cliente.post(
        "/api/v1/media/upload",
        headers=educadora.headers,
        files={"file": ("actividad.png", PNG_CHICO, "image/png")},
        data={"folder": "bitacora"},
    )
    assert subida.status_code == 201, subida.text
    datos = subida.json()
    msg = cliente.post(
        f"/api/v1/chats/bitacora/{child_id}/messages",
        headers=educadora.headers,
        json={"message": "Pintando con acuarelas", "fileUri": datos["url"], "fileName": "actividad.png",
              "fileMimeType": "image/png", "time": "11:00 AM", "date": "22/10/2026"},
    )
    assert msg.status_code == 201, msg.text
    return datos["fileId"], msg.json()["id"]


def test_foto_sin_sesion_da_401(cliente: TestClient, educadora: Persona):
    file_id, _ = _foto_en_bitacora(cliente, educadora, "mateo_garcia")
    assert cliente.get(f"/api/v1/media/{file_id}").status_code == 401


def test_familia_del_nino_ve_la_foto_y_queda_registrado(cliente: TestClient, educadora: Persona, familiar: Persona):
    file_id, _ = _foto_en_bitacora(cliente, educadora, "mateo_garcia")
    r = cliente.get(f"/api/v1/media/{file_id}", headers=familiar.headers)
    assert r.status_code == 200
    assert r.content == PNG_CHICO
    assert "private" in r.headers["Cache-Control"]
    with SessionLocal() as db:
        accesos = db.query(PhotoAccess).filter(PhotoAccess.file_id == file_id, PhotoAccess.user_id == familiar.id).count()
    assert accesos == 1


def test_otra_familia_no_ve_la_foto(cliente: TestClient, educadora: Persona):
    file_id, _ = _foto_en_bitacora(cliente, educadora, "mateo_garcia")
    otra_familia = _login(cliente, "martinez@sonrisas.edu", "FAMILIAR")
    r = cliente.get(f"/api/v1/media/{file_id}", headers=otra_familia)
    assert r.status_code == 403
    assert r.json()["code"] == "sin_acceso_archivo"


def test_otra_familia_no_puede_borrar_la_foto(cliente: TestClient, educadora: Persona):
    file_id, _ = _foto_en_bitacora(cliente, educadora, "mateo_garcia")
    otra_familia = _login(cliente, "martinez@sonrisas.edu", "FAMILIAR")
    r = cliente.delete(f"/api/v1/media/{file_id}", headers=otra_familia)
    assert r.status_code == 403
    with SessionLocal() as db:
        assert db.get(MediaFile, file_id) is not None


def test_familia_borra_la_foto_de_su_hijo_y_desaparece(
    cliente: TestClient, educadora: Persona, familiar: Persona, direccion: Persona
):
    file_id, msg_id = _foto_en_bitacora(cliente, educadora, "mateo_garcia")

    r = cliente.delete(f"/api/v1/media/{file_id}?reason=No autorizo esta foto", headers=familiar.headers)
    assert r.status_code == 200, r.text
    assert r.json()["referencesCleared"] == 1

    # Ya no existe para nadie, ni siquiera para el personal
    assert cliente.get(f"/api/v1/media/{file_id}", headers=educadora.headers).status_code == 404
    with SessionLocal() as db:
        assert db.get(MediaFile, file_id) is None
        mensaje = db.get(BitacoraMessage, msg_id)
        assert mensaje.file_uri is None
        assert "eliminada" in mensaje.message.lower()
        borrado = db.query(PhotoDeletion).filter(PhotoDeletion.file_id == file_id).one()
        assert borrado.requested_by_id == familiar.id

    # La línea de tiempo ya no trae el archivo
    linea = cliente.get("/api/v1/children/mateo_garcia/timeline", headers=familiar.headers).json()
    assert all(file_id not in (item.get("fileUri") or "") for item in linea)

    # La dirección ve la constancia del borrado en la bitácora de auditoría
    logs = cliente.get("/api/v1/admin/logs?category=FOTO&limit=200", headers=direccion.headers)
    assert logs.status_code == 200
    assert any(file_id in l["description"] and l["title"] == "Foto eliminada" for l in logs.json())


def test_direccion_consulta_quien_vio_una_foto(
    cliente: TestClient, educadora: Persona, familiar: Persona, direccion: Persona
):
    file_id, _ = _foto_en_bitacora(cliente, educadora, "mateo_garcia")
    cliente.get(f"/api/v1/media/{file_id}", headers=familiar.headers)
    r = cliente.get(f"/api/v1/media/{file_id}/accesses", headers=direccion.headers)
    assert r.status_code == 200
    assert any(a["userId"] == familiar.id for a in r.json())
    assert cliente.get(f"/api/v1/media/{file_id}/accesses", headers=familiar.headers).status_code == 403


def test_baja_del_alumno_borra_sus_fotos(
    cliente: TestClient, educadora: Persona, direccion: Persona, registrar
):
    """Aviso de privacidad: al dar de baja a un niño no quedan sus fotos en el servidor."""
    tutor = registrar("tutorbaja")
    nuevo = cliente.post(
        f"/api/v1/admin/users/{tutor.id}/children",
        headers=direccion.headers,
        json={"fullName": "Niño de Prueba Baja", "roomText": "Sala 1A"},
    )
    assert nuevo.status_code == 201, nuevo.text
    child_id = nuevo.json()["id"]
    file_id, _ = _foto_en_bitacora(cliente, educadora, child_id)

    r = cliente.delete(f"/api/v1/admin/users/{tutor.id}/children/{child_id}", headers=direccion.headers)
    assert r.status_code == 204
    with SessionLocal() as db:
        assert db.get(MediaFile, file_id) is None
        assert db.query(PhotoDeletion).filter(PhotoDeletion.file_id == file_id).count() == 1
    assert cliente.get(f"/api/v1/media/{file_id}", headers=direccion.headers).status_code == 404
