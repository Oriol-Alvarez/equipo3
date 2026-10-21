"""Pruebas de los datos calculados a partir de los registros reales (sin datos de ejemplo).

Cubre: resumen semanal y su lectura/reacción, historial diario, estado del día de cada
alumno, alumnos por sala, permisos de familia, mensajes sin leer y fechas reales.
"""

import tempfile
from datetime import timedelta
from pathlib import Path

import pytest

from fastapi.testclient import TestClient

from app.db import SessionLocal
from app.horario import hoy
from app.models import DailyLog
from app.resumenes import lunes_de

from .conftest import Persona

API = "/api/v1"


@pytest.fixture
def carpeta_temporal():
    """Carpeta temporal propia (tmp_path de pytest falla en algunos Windows por permisos)."""
    with tempfile.TemporaryDirectory() as carpeta:
        yield Path(carpeta)


# ---------------------------------------------------------------------------
# Resumen semanal
# ---------------------------------------------------------------------------

def test_resumen_semanal_se_calcula_con_los_registros(cliente: TestClient, familiar: Persona):
    r = cliente.get(f"{API}/children/mateo_garcia/weekly-summaries", headers=familiar.headers)
    assert r.status_code == 200, r.text
    semanas = r.json()
    assert len(semanas) == 4
    lunes_actual = lunes_de(hoy())
    assert [s["id"] for s in semanas] == [(lunes_actual - timedelta(weeks=i)).isoformat() for i in range(4)]
    assert semanas[0]["weekLabel"] == "Semana actual"

    # Los datos iniciales tienen registro en los 10 días hábiles anteriores a hoy:
    # la semana pasada está completa.
    pasada = semanas[1]
    assert pasada["weekLabel"] == "Semana pasada"
    assert pasada["attendanceDaysCount"] == 5
    assert pasada["totalSchoolDays"] == 5
    assert pasada["attendancePercentageText"] == "100%"
    assert pasada["averageArrivalTime"].endswith("AM")
    assert pasada["foodIntakePercentage"].endswith("%")
    assert "asistió 5 de 5 días" in pasada["executiveSummary"]
    # Ya no aparece el texto fijo que se mostraba para cualquier niño
    assert "Semana sumamente positiva" not in pasada["executiveSummary"]


def test_semana_sin_registros_lo_dice(cliente: TestClient, educadora: Persona):
    # Emilia (Sala 2B) no tiene registros diarios
    r = cliente.get(f"{API}/children/emilia_ramos/weekly-summaries", headers=educadora.headers)
    assert r.status_code == 200, r.text
    for semana in r.json():
        assert semana["attendanceDaysCount"] == 0
        assert semana["foodIntakePercentage"] == "—"
        assert "Emilia" in semana["executiveSummary"]


def test_familia_confirma_lectura_y_reacciona(cliente: TestClient, familiar: Persona, educadora: Persona):
    semana = lunes_de(hoy()).isoformat()
    url = f"{API}/children/lucia_garcia/weekly-summaries/{semana}/feedback"

    r = cliente.put(url, json={"isRead": True, "reaction": "❤️"}, headers=familiar.headers)
    assert r.status_code == 200, r.text
    assert r.json()["isReadByParent"] is True
    assert r.json()["readTimestampText"]
    assert r.json()["parentReaction"] == "❤️"

    # La educadora ve que la familia ya lo leyó
    r = cliente.get(f"{API}/children/lucia_garcia/weekly-summaries/{semana}", headers=educadora.headers)
    assert r.json()["isReadByParent"] is True
    assert r.json()["parentReaction"] == "❤️"

    # Quitar reacción y marcar como no leído
    r = cliente.put(url, json={"isRead": False, "reaction": ""}, headers=familiar.headers)
    assert r.json()["isReadByParent"] is False
    assert r.json()["parentReaction"] is None


def test_personal_no_confirma_lectura(cliente: TestClient, educadora: Persona):
    semana = lunes_de(hoy()).isoformat()
    r = cliente.put(
        f"{API}/children/mateo_garcia/weekly-summaries/{semana}/feedback",
        json={"isRead": True},
        headers=educadora.headers,
    )
    assert r.status_code == 403


def test_semana_fuera_del_historial_da_404(cliente: TestClient, familiar: Persona):
    r = cliente.get(f"{API}/children/mateo_garcia/weekly-summaries/2020-01-06", headers=familiar.headers)
    assert r.status_code == 404


def test_actividad_en_bitacora_actualiza_resumen_semanal(cliente: TestClient, educadora: Persona, familiar: Persona):
    semana = lunes_de(hoy()).isoformat()
    # 1. Consultar resumen semanal antes
    r1 = cliente.get(f"{API}/children/mateo_garcia/weekly-summaries/{semana}", headers=familiar.headers)
    assert r1.status_code == 200

    # 2. Registrar actividad de comida en la bitácora
    r2 = cliente.post(
        f"{API}/children/mateo_garcia/activities",
        json={
            "time": "12:30 PM",
            "category": "Alimentación",
            "title": "Almuerzo",
            "description": "Comió todo su plato de verduras con pollo.",
            "mealType": "COMIDA",
            "consumptionOption": "TODO",
            "portionOption": "Todo",
            "date": hoy().strftime("%d/%m/%Y"),
        },
        headers=educadora.headers,
    )
    assert r2.status_code == 201

    # 3. Verificar que el resumen semanal refleja la actividad y el porcentaje
    r3 = cliente.get(f"{API}/children/mateo_garcia/weekly-summaries/{semana}", headers=familiar.headers)
    assert r3.status_code == 200
    resumen_actualizado = r3.json()
    assert resumen_actualizado["foodIntakePercentage"] != "—"
    assert any("comió completo" in h for h in resumen_actualizado["foodHighlights"])


def test_actividad_animo_actualiza_resumen_semanal(cliente: TestClient, educadora: Persona, familiar: Persona):
    semana = lunes_de(hoy()).isoformat()
    r = cliente.post(
        f"{API}/children/mateo_garcia/activities",
        json={
            "time": "11:00 AM",
            "category": "Estado de Ánimo",
            "title": "Estado de Ánimo",
            "description": "Estadía: Feliz. Participó alegremente.",
            "moodState": "FELIZ",
            "date": hoy().strftime("%d/%m/%Y"),
        },
        headers=educadora.headers,
    )
    assert r.status_code == 201

    r2 = cliente.get(f"{API}/children/mateo_garcia/weekly-summaries/{semana}", headers=familiar.headers)
    assert r2.status_code == 200
    resumen = r2.json()
    assert "feliz" in resumen["moodSummary"].lower()
    assert "\n" not in resumen["moodSummary"]
    assert len(resumen["moodSummary"]) <= 25


# ---------------------------------------------------------------------------
# Historial diario
# ---------------------------------------------------------------------------

def test_historial_diario_sale_de_los_registros(cliente: TestClient, familiar: Persona):
    r = cliente.get(f"{API}/children/mateo_garcia/bitacoras", headers=familiar.headers)
    assert r.status_code == 200, r.text
    dias = r.json()
    assert dias, "Debe haber días con registro"
    # Otras pruebas agregan actividades con la fecha de hoy; se revisa un día sembrado
    ayer_o_antes = next(d for d in dias if not d["isToday"])
    assert [d["date"] for d in dias] == sorted(
        (d["date"] for d in dias), key=lambda f: f[6:] + f[3:5] + f[:2], reverse=True
    ), "Del más reciente al más antiguo"
    assert ayer_o_antes["ingreso"]["time"].endswith(("AM", "PM"))
    assert ayer_o_antes["ingreso"]["delivererName"] == "Mamá"
    assert ayer_o_antes["egreso"]["collectorName"] == "Mamá"
    categorias = {a["category"] for a in ayer_o_antes["activities"]}
    assert {"ALIMENTACION", "DESCANSO", "ESTADO_ANIMO"} <= categorias
    # Ya no aparece el día inventado
    assert all(d["date"] != "12/10/2026" or d["ingreso"]["delivererName"] != "María López (Madre)" for d in dias)


def test_historial_vacio_si_no_hay_registros(cliente: TestClient, educadora: Persona):
    r = cliente.get(f"{API}/children/diego_herrera/bitacoras", headers=educadora.headers)
    assert r.status_code == 200
    assert r.json() == []


# ---------------------------------------------------------------------------
# Estado del día, salas y nombres
# ---------------------------------------------------------------------------

def test_estado_del_dia_sale_del_registro_de_hoy(cliente: TestClient, educadora: Persona):
    hoy_iso = hoy().isoformat()
    with SessionLocal() as db:
        if db.get(DailyLog, ("santiago_flores", hoy_iso)) is None:
            db.add(DailyLog(child_id="santiago_flores", date=hoy_iso, ingreso='{"time": "08:40"}'))
            db.commit()

    r = cliente.get(f"{API}/rooms/sala_3c/children", headers=educadora.headers)
    assert r.status_code == 200, r.text
    por_id = {c["id"]: c for c in r.json()}
    assert por_id["santiago_flores"]["isPresent"] is True
    assert por_id["santiago_flores"]["arrivalTime"] == "08:40 AM"
    assert por_id["santiago_flores"]["roomText"] == "Sala 3C"
    # Quien no tiene registro hoy aparece sin ingresar
    assert por_id["regina_navarro"]["isPresent"] is False
    assert por_id["regina_navarro"]["statusText"] == "No ha ingresado"


def test_cada_sala_tiene_sus_propios_alumnos(cliente: TestClient, educadora: Persona):
    ids = {}
    for sala in ("sala_1a", "sala_2b", "sala_3c"):
        r = cliente.get(f"{API}/rooms/{sala}/children", headers=educadora.headers)
        assert r.status_code == 200
        ids[sala] = {c["id"] for c in r.json()}
        assert ids[sala], f"{sala} debe tener alumnos"
    assert not (ids["sala_1a"] & ids["sala_2b"])
    assert not (ids["sala_2b"] & ids["sala_3c"])


def test_sala_inexistente_da_404(cliente: TestClient, educadora: Persona):
    assert cliente.get(f"{API}/rooms/sala_9z/children", headers=educadora.headers).status_code == 404


# ---------------------------------------------------------------------------
# Permisos de la familia
# ---------------------------------------------------------------------------

def test_familia_no_ve_datos_de_otros_ninos(cliente: TestClient, familiar: Persona):
    # Sofía no es hija del familiar de prueba
    for ruta in (
        "children/sofia_lopez/timeline",
        "children/sofia_lopez/bitacoras",
        "children/sofia_lopez/weekly-summaries",
        "children/sofia_lopez/full-profile",
        "chats/bitacora/sofia_lopez/messages",
    ):
        r = cliente.get(f"{API}/{ruta}", headers=familiar.headers)
        assert r.status_code == 403, f"{ruta}: {r.status_code}"

    r = cliente.post(
        f"{API}/chats/bitacora/sofia_lopez/messages", json={"message": "Hola"}, headers=familiar.headers
    )
    assert r.status_code == 403


# ---------------------------------------------------------------------------
# Expediente
# ---------------------------------------------------------------------------

def test_expediente_guarda_notas_de_habitos(cliente: TestClient, familiar: Persona):
    r = cliente.put(
        f"{API}/children/mateo_garcia/full-profile",
        json={"habitsAndPedagogicalNotes": "Le gusta dormir con su peluche."},
        headers=familiar.headers,
    )
    assert r.status_code == 200, r.text
    r = cliente.get(f"{API}/children/mateo_garcia/full-profile", headers=familiar.headers)
    assert r.json()["generalNotes"] == "Le gusta dormir con su peluche."


def test_expediente_vacio_para_alumno_sin_datos(cliente: TestClient, educadora: Persona):
    r = cliente.get(f"{API}/children/valentina_ruiz/full-profile", headers=educadora.headers)
    assert r.status_code == 200
    assert r.json()["childId"] == "valentina_ruiz"


# ---------------------------------------------------------------------------
# Mensajes sin leer y fechas reales
# ---------------------------------------------------------------------------

def test_no_leidos_por_usuario_en_chat_de_bitacora(cliente: TestClient, familiar: Persona, educadora: Persona):
    def no_leidos() -> int:
        r = cliente.get(f"{API}/rooms/sala_1a/parent-chats", headers=educadora.headers)
        return next(c["unreadCount"] for c in r.json() if c["childId"] == "lucia_garcia")

    cliente.put(f"{API}/chats/bitacora/lucia_garcia/read", headers=educadora.headers)
    assert no_leidos() == 0

    r = cliente.post(
        f"{API}/chats/bitacora/lucia_garcia/messages", json={"message": "¿Comió bien?"}, headers=familiar.headers
    )
    assert r.status_code == 201, r.text
    assert r.json()["date"] == hoy().strftime("%d/%m/%Y")
    assert no_leidos() == 1

    assert cliente.put(f"{API}/chats/bitacora/lucia_garcia/read", headers=educadora.headers).status_code == 204
    assert no_leidos() == 0


def test_no_leidos_en_chats_del_personal(cliente: TestClient, educadora: Persona, direccion: Persona):
    def no_leidos() -> int:
        r = cliente.get(f"{API}/chats/workers", headers=educadora.headers)
        return next(c["unreadCount"] for c in r.json() if c["id"] == "wchat_1")

    cliente.put(f"{API}/chats/workers/wchat_1/read", headers=educadora.headers)
    assert no_leidos() == 0
    cliente.post(f"{API}/chats/workers/wchat_1/messages", json={"message": "Junta a las 3"}, headers=direccion.headers)
    assert no_leidos() == 1
    cliente.put(f"{API}/chats/workers/wchat_1/read", headers=educadora.headers)
    assert no_leidos() == 0


def test_evento_rapido_usa_la_fecha_de_hoy(cliente: TestClient, educadora: Persona):
    r = cliente.post(
        f"{API}/children/mateo_garcia/timeline/events",
        json={"time": "10:00 AM", "text": "Tomó agua", "iconType": "mood"},
        headers=educadora.headers,
    )
    assert r.status_code == 201, r.text
    assert r.json()["date"] == hoy().strftime("%d/%m/%Y")


def test_asistencia_acepta_hora_en_12_horas(cliente: TestClient, educadora: Persona):
    r = cliente.post(
        f"{API}/attendance/ingreso",
        json={"childId": "leonardo_castro", "date": hoy().isoformat(), "entryTime": "8:05 AM"},
        headers=educadora.headers,
    )
    assert r.status_code == 201, r.text
    r = cliente.get(f"{API}/children/leonardo_castro", headers=educadora.headers)
    assert r.json()["isPresent"] is True
    assert r.json()["arrivalTime"] == "08:05 AM"


# ---------------------------------------------------------------------------
# Objetos perdidos
# ---------------------------------------------------------------------------

def test_familia_reporta_objeto_y_solo_el_personal_lo_elimina(
    cliente: TestClient, familiar: Persona, educadora: Persona
):
    r = cliente.post(
        f"{API}/lost-objects",
        json={"title": "Chamarra azul", "description": "Talla 3, con nombre", "category": "Ropa", "room": "Sala 1A"},
        headers=familiar.headers,
    )
    assert r.status_code == 201, r.text
    objeto = r.json()["id"]
    assert cliente.delete(f"{API}/lost-objects/{objeto}", headers=familiar.headers).status_code == 403
    assert cliente.delete(f"{API}/lost-objects/{objeto}", headers=educadora.headers).status_code == 204
    ids = {o["id"] for o in cliente.get(f"{API}/lost-objects", headers=educadora.headers).json()}
    assert objeto not in ids


# ---------------------------------------------------------------------------
# Sugerencias
# ---------------------------------------------------------------------------

def test_familia_solo_ve_sus_sugerencias(cliente: TestClient, familiar: Persona, registrar):
    otra_familia = registrar("otrafamilia")
    r = cliente.get(f"{API}/suggestions", headers=otra_familia.headers)
    assert r.status_code == 200
    assert r.json() == [], "Una familia nueva no debe ver sugerencias de otras familias"


def test_sugerencia_usa_el_nino_real_y_quien_responde(
    cliente: TestClient, familiar: Persona, educadora: Persona
):
    r = cliente.post(
        f"{API}/suggestions",
        json={"childId": "lucia_garcia", "roomName": "x", "category": "GENERAL", "subject": "Horario", "content": "¿A qué hora es la junta?"},
        headers=familiar.headers,
    )
    assert r.status_code == 201, r.text
    sug = r.json()
    assert sug["childName"] == "Lucía García López"
    assert sug["roomName"] == "Sala 1A"

    r = cliente.patch(
        f"{API}/suggestions/{sug['id']}/status",
        json={"status": "ATENDIDA", "response": "A las 5 pm", "responderName": "Otro nombre"},
        headers=educadora.headers,
    )
    assert r.status_code == 200, r.text
    assert r.json()["responderName"] == "Laura Gómez (Educadora)"  # nombre de la cuenta en la base de datos


def test_familia_no_envia_sugerencia_de_nino_ajeno(cliente: TestClient, familiar: Persona):
    r = cliente.post(
        f"{API}/suggestions",
        json={"childId": "sofia_lopez", "roomName": "Sala 1A", "category": "GENERAL", "subject": "x", "content": "y"},
        headers=familiar.headers,
    )
    assert r.status_code == 403


def test_personal_crea_grupo_y_todos_lo_ven(cliente: TestClient, educadora: Persona, direccion: Persona, familiar: Persona):
    r = cliente.post(f"{API}/chats/workers", json={"title": "Junta de salas", "memberIds": ["usr_dir_mendez"]}, headers=educadora.headers)
    assert r.status_code == 201, r.text
    grupo = r.json()
    assert grupo["isGroup"] is True and grupo["title"] == "Junta de salas"
    ids = {c["id"] for c in cliente.get(f"{API}/chats/workers", headers=direccion.headers).json()}
    assert grupo["id"] in ids
    assert cliente.post(f"{API}/chats/workers", json={"title": "x"}, headers=familiar.headers).status_code == 403


# ---------------------------------------------------------------------------
# Notificaciones push
# ---------------------------------------------------------------------------

def test_push_llega_a_la_familia_y_se_desvincula(cliente: TestClient, familiar: Persona, educadora: Persona,
                                                   carpeta_temporal, monkeypatch):
    from app import notificaciones_push
    from app.config import settings

    enviados: list[tuple[list[str], str]] = []
    credenciales = carpeta_temporal / "firebase.json"
    credenciales.write_text("{}")
    monkeypatch.setattr(settings, "firebase_credentials", str(credenciales))
    monkeypatch.setattr(notificaciones_push, "ENVIAR_EN_HILO", False)
    monkeypatch.setattr(notificaciones_push, "_despachar", lambda tokens, titulo, cuerpo, datos: enviados.append((tokens, titulo)))

    r = cliente.post(f"{API}/notifications/device-token", json={"deviceToken": "tel-familia-1"}, headers=familiar.headers)
    assert r.status_code == 200, r.text

    # La educadora escribe en la bitácora de Mateo: le llega a la familia
    cliente.post(f"{API}/chats/bitacora/mateo_garcia/messages", json={"message": "Comió muy bien"}, headers=educadora.headers)
    assert enviados and enviados[-1][0] == ["tel-familia-1"]
    assert "Mateo" in enviados[-1][1]

    # Quien escribe no se notifica a sí mismo
    enviados.clear()
    cliente.post(f"{API}/chats/bitacora/mateo_garcia/messages", json={"message": "Gracias"}, headers=familiar.headers)
    assert all("tel-familia-1" not in tokens for tokens, _ in enviados)

    # Al cerrar sesión se desvincula y ya no le llega nada
    r = cliente.post(f"{API}/notifications/device-token/unregister", json={"deviceToken": "tel-familia-1"},
                     headers=familiar.headers)
    assert r.status_code == 204
    enviados.clear()
    cliente.post(f"{API}/chats/bitacora/mateo_garcia/messages", json={"message": "Otra"}, headers=educadora.headers)
    assert enviados == []


def test_sin_credenciales_no_se_envia_nada(cliente: TestClient, familiar: Persona, educadora: Persona, monkeypatch):
    from app import notificaciones_push
    from app.config import settings

    monkeypatch.setattr(settings, "firebase_credentials", "")
    llamado = []
    monkeypatch.setattr(notificaciones_push, "_despachar", lambda *a: llamado.append(a))
    cliente.post(f"{API}/notifications/device-token", json={"deviceToken": "tel-x"}, headers=familiar.headers)
    r = cliente.post(f"{API}/chats/bitacora/mateo_garcia/messages", json={"message": "Hola"}, headers=educadora.headers)
    assert r.status_code == 201
    assert llamado == []


def test_envio_real_firma_y_arma_el_mensaje_de_fcm(cliente: TestClient, familiar: Persona, educadora: Persona,
                                                   carpeta_temporal, monkeypatch):
    """Recorre el envío completo con la red simulada: token OAuth2 firmado con RS256 y mensaje a FCM."""
    import io
    import json as _json

    import jwt
    import pytest

    rsa = pytest.importorskip("cryptography.hazmat.primitives.asymmetric.rsa")
    serialization = pytest.importorskip("cryptography.hazmat.primitives.serialization")
    from app import notificaciones_push
    from app.config import settings

    llave = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    pem = llave.private_bytes(
        serialization.Encoding.PEM, serialization.PrivateFormat.PKCS8, serialization.NoEncryption()
    ).decode()
    credenciales = carpeta_temporal / "firebase.json"
    credenciales.write_text(_json.dumps({
        "project_id": "sonrisas-prueba",
        "client_email": "push@sonrisas-prueba.iam.gserviceaccount.com",
        "private_key": pem,
        "token_uri": "https://oauth2.googleapis.com/token",
    }))
    monkeypatch.setattr(settings, "firebase_credentials", str(credenciales))
    monkeypatch.setattr(notificaciones_push, "ENVIAR_EN_HILO", False)
    monkeypatch.setitem(notificaciones_push._cache_token, "valor", None)

    peticiones = []

    class Respuesta(io.BytesIO):
        def __enter__(self):
            return self

        def __exit__(self, *a):
            return False

    def urlopen_falso(peticion, timeout=None):
        peticiones.append(peticion)
        if "oauth2" in peticion.full_url:
            return Respuesta(b'{"access_token": "token-falso", "expires_in": 3600}')
        return Respuesta(b"{}")

    monkeypatch.setattr(notificaciones_push.urllib.request, "urlopen", urlopen_falso)

    cliente.post(f"{API}/notifications/device-token", json={"deviceToken": "tel-real"}, headers=familiar.headers)
    cliente.post(f"{API}/chats/bitacora/mateo_garcia/messages", json={"message": "Llegó bien"}, headers=educadora.headers)

    oauth = [p for p in peticiones if "oauth2" in p.full_url]
    fcm = [p for p in peticiones if "fcm.googleapis.com" in p.full_url]
    assert oauth and fcm
    asercion = dict(x.split("=", 1) for x in oauth[0].data.decode().split("&"))["assertion"]
    datos_jwt = jwt.decode(asercion, llave.public_key(), algorithms=["RS256"], audience="https://oauth2.googleapis.com/token")
    assert datos_jwt["scope"] == notificaciones_push.ALCANCE
    assert all(p.full_url.endswith("/projects/sonrisas-prueba/messages:send") for p in fcm)
    assert all(p.headers["Authorization"] == "Bearer token-falso" for p in fcm)
    # La familia puede tener varios teléfonos registrados: a todos les llega
    cuerpos = {(_json.loads(p.data)["message"]["token"]): _json.loads(p.data) for p in fcm}
    assert "tel-real" in cuerpos
    assert "Llegó bien" in cuerpos["tel-real"]["message"]["notification"]["body"]


def test_familia_ve_la_educadora_de_su_hijo(cliente: TestClient, familiar: Persona):
    r = cliente.get(f"{API}/children/mateo_garcia/educator", headers=familiar.headers)
    assert r.status_code == 200, r.text
    educadora = r.json()
    assert educadora["name"] == "Laura Gómez"
    assert educadora["roomId"] == "sala_1a"

    # La ficha del niño trae el mismo nombre
    r = cliente.get(f"{API}/children/mateo_garcia", headers=familiar.headers)
    assert r.json()["educatorName"] == "Laura Gómez"


def test_familia_no_ve_la_educadora_de_nino_ajeno(cliente: TestClient, familiar: Persona):
    r = cliente.get(f"{API}/children/sofia_lopez/educator", headers=familiar.headers)
    assert r.status_code == 403
