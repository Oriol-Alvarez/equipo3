"""Pruebas de los módulos de Heber: grupos con familias, observaciones semanales,
planeación/menú diario y registro diario (hoja oficial)."""

import uuid

from fastapi.testclient import TestClient

from app.horario import hoy

from .conftest import Persona

API = "/api/v1"

INGRESO = {
    "time": "08:10",
    "assistantName": "Laura Gómez",
    "deliveredBy": "Mamá",
    "receivedBy": "Laura Gómez",
    "isSick": False,
    "medicationWithPrescription": "NO_APLICA",
    "arrivedClean": True,
}


# ---------------------------------------------------------------- grupos con familias

def test_familia_solo_ve_grupos_de_sus_hijos(cliente: TestClient, familiar: Persona, educadora: Persona):
    r = cliente.post(
        f"{API}/family-groups",
        headers=educadora.headers,
        json={"title": "Solo Sofía", "memberChildIds": ["sofia_lopez"]},
    )
    assert r.status_code == 201, r.text
    grupo_ajeno = r.json()["id"]

    ids = [g["id"] for g in cliente.get(f"{API}/family-groups", headers=familiar.headers).json()]
    assert "fgroup_paseo_sala_1a" in ids  # Mateo está en ese grupo
    assert grupo_ajeno not in ids

    assert cliente.get(f"{API}/family-groups/{grupo_ajeno}/messages", headers=familiar.headers).status_code == 403
    r = cliente.post(f"{API}/family-groups/{grupo_ajeno}/messages", headers=familiar.headers, json={"text": "hola"})
    assert r.status_code == 403


def test_mensajes_de_grupo_en_orden_y_sin_vacios(cliente: TestClient, familiar: Persona):
    gid = "fgroup_paseo_sala_1a"
    for texto in ("primero", "segundo"):
        r = cliente.post(f"{API}/family-groups/{gid}/messages", headers=familiar.headers, json={"text": texto})
        assert r.status_code == 201, r.text
        assert r.json()["senderId"] == familiar.id
    textos = [m["text"] for m in cliente.get(f"{API}/family-groups/{gid}/messages", headers=familiar.headers).json()]
    assert textos.index("primero") < textos.index("segundo")

    r = cliente.post(f"{API}/family-groups/{gid}/messages", headers=familiar.headers, json={"text": "   "})
    assert r.status_code == 422


def test_crear_grupo_con_alumno_inexistente_da_404(cliente: TestClient, educadora: Persona):
    r = cliente.post(
        f"{API}/family-groups",
        headers=educadora.headers,
        json={"title": "Grupo", "memberChildIds": ["no_existe"]},
    )
    assert r.status_code == 404


def test_familia_no_puede_crear_grupos(cliente: TestClient, familiar: Persona):
    r = cliente.post(
        f"{API}/family-groups", headers=familiar.headers, json={"title": "X", "memberChildIds": ["mateo_garcia"]}
    )
    assert r.status_code == 403


# ---------------------------------------------------------------- observaciones semanales

def test_familia_agrega_observacion_con_su_rol(cliente: TestClient, familiar: Persona):
    semana = f"Semana de prueba {uuid.uuid4().hex[:6]}"
    r = cliente.post(
        f"{API}/children/mateo_garcia/weekly-observations",
        headers=familiar.headers,
        json={"weekKey": semana, "text": "  Durmió muy bien  "},
    )
    assert r.status_code == 201, r.text
    obs = r.json()
    assert obs["authorRole"] == "FAMILIA"
    assert obs["text"] == "Durmió muy bien"

    lista = cliente.get(
        f"{API}/children/mateo_garcia/weekly-observations",
        headers=familiar.headers,
        params={"weekKey": semana},
    ).json()
    assert [o["id"] for o in lista] == [obs["id"]]


def test_educadora_firma_como_educadora(cliente: TestClient, educadora: Persona):
    r = cliente.post(
        f"{API}/children/sofia_lopez/weekly-observations",
        headers=educadora.headers,
        json={"weekKey": "Semana de prueba", "text": "Participó mucho"},
    )
    assert r.status_code == 201
    assert r.json()["authorRole"] == "EDUCADORA"


def test_observacion_vacia_o_larga_da_422(cliente: TestClient, familiar: Persona):
    url = f"{API}/children/mateo_garcia/weekly-observations"
    assert cliente.post(url, headers=familiar.headers, json={"weekKey": "S", "text": "  "}).status_code == 422
    assert cliente.post(url, headers=familiar.headers, json={"weekKey": "S", "text": "x" * 501}).status_code == 422


def test_familia_no_ve_observaciones_de_otros_ninos(cliente: TestClient, familiar: Persona):
    r = cliente.post(
        f"{API}/children/sofia_lopez/weekly-observations",
        headers=familiar.headers,
        json={"weekKey": "S", "text": "hola"},
    )
    assert r.status_code == 403
    todas = cliente.get(f"{API}/weekly-observations", headers=familiar.headers).json()
    assert all(o["childId"] in ("mateo_garcia", "lucia_garcia") for o in todas)


# ---------------------------------------------------------------- planeación y menú

def test_semana_actual_tiene_planeacion_sembrada(cliente: TestClient, familiar: Persona):
    planes = cliente.get(f"{API}/daily-plans", headers=familiar.headers).json()
    assert len(planes) >= 5
    assert planes[0]["menu"]["breakfast"]


def test_educadora_publica_y_familia_consulta(cliente: TestClient, educadora: Persona, familiar: Persona):
    cuerpo = {
        "menu": {"breakfast": "Avena", "snack": "Manzana", "lunch": "Sopa", "notes": ""},
        "activities": [{"time": "11:00", "title": "Patio"}, {"time": "09:00", "title": "Asamblea"}],
    }
    r = cliente.put(f"{API}/daily-plans/2030-01-07", headers=educadora.headers, json=cuerpo)
    assert r.status_code == 200, r.text
    assert [a["time"] for a in r.json()["activities"]] == ["09:00", "11:00"]

    r = cliente.get(f"{API}/daily-plans/2030-01-07", headers=familiar.headers)
    assert r.status_code == 200
    assert r.json()["menu"]["breakfast"] == "Avena"

    rango = cliente.get(f"{API}/daily-plans", headers=familiar.headers, params={"from": "2030-01-01", "to": "2030-01-31"})
    assert [p["date"] for p in rango.json()] == ["2030-01-07"]


def test_familia_no_puede_publicar_planeacion(cliente: TestClient, familiar: Persona):
    cuerpo = {"menu": {"breakfast": "a", "snack": "b", "lunch": "c"}, "activities": [{"time": "09:00", "title": "x"}]}
    assert cliente.put(f"{API}/daily-plans/2030-01-08", headers=familiar.headers, json=cuerpo).status_code == 403


def test_planeacion_invalida_da_422(cliente: TestClient, educadora: Persona):
    malo = {"menu": {"breakfast": "", "snack": "b", "lunch": "c"}, "activities": [{"time": "9am", "title": "x"}]}
    assert cliente.put(f"{API}/daily-plans/2030-01-09", headers=educadora.headers, json=malo).status_code == 422
    bueno = {"menu": {"breakfast": "a", "snack": "b", "lunch": "c"}, "activities": [{"time": "09:00", "title": "x"}]}
    assert cliente.put(f"{API}/daily-plans/09-01-2030", headers=educadora.headers, json=bueno).status_code == 422


# ---------------------------------------------------------------- registro diario

def test_registro_completo_de_un_dia(cliente: TestClient, educadora: Persona, familiar: Persona):
    base = f"{API}/children/lucia_garcia/daily-logs/2030-02-04"
    assert cliente.put(f"{base}/ingreso", headers=educadora.headers, json=INGRESO).status_code == 200
    r = cliente.put(f"{base}/estadia", headers=educadora.headers, json={"breakfast": "TODO", "mood": "FELIZ", "napMinutes": 60})
    assert r.status_code == 200, r.text
    r = cliente.put(f"{base}/egreso", headers=educadora.headers, json={"time": "15:00", "collectedBy": "Papá"})
    assert r.status_code == 200, r.text
    dia = r.json()
    assert dia["ingreso"]["time"] == "08:10"
    assert dia["estadia"]["breakfast"] == "TODO"
    assert dia["egreso"]["collectedBy"] == "Papá"

    lista = cliente.get(
        f"{API}/children/lucia_garcia/daily-logs",
        headers=familiar.headers,
        params={"from": "2030-02-01", "to": "2030-02-28"},
    ).json()
    assert [d["date"] for d in lista] == ["2030-02-04"]


def test_salida_antes_de_entrada_da_422(cliente: TestClient, educadora: Persona):
    base = f"{API}/children/mateo_garcia/daily-logs/2030-02-05"
    cliente.put(f"{base}/ingreso", headers=educadora.headers, json=INGRESO)
    r = cliente.put(f"{base}/egreso", headers=educadora.headers, json={"time": "07:00"})
    assert r.status_code == 422
    assert r.json()["code"] == "salida_antes_de_entrada"


def test_opciones_invalidas_da_422(cliente: TestClient, educadora: Persona):
    base = f"{API}/children/mateo_garcia/daily-logs/2030-02-06"
    assert cliente.put(f"{base}/estadia", headers=educadora.headers, json={"mood": "MUY_FELIZ"}).status_code == 422
    assert cliente.put(f"{base}/estadia", headers=educadora.headers, json={"campoRaro": 1}).status_code == 422
    assert cliente.put(f"{base}/ingreso", headers=educadora.headers, json={"time": "8:10"}).status_code == 422


def test_familia_no_escribe_registro_ni_ve_otros(cliente: TestClient, familiar: Persona):
    base = f"{API}/children/mateo_garcia/daily-logs/2030-02-07"
    assert cliente.put(f"{base}/ingreso", headers=familiar.headers, json=INGRESO).status_code == 403
    assert cliente.get(f"{API}/children/sofia_lopez/daily-logs", headers=familiar.headers).status_code == 403
    todos = cliente.get(f"{API}/daily-logs", headers=familiar.headers).json()
    assert todos, "el seed debe traer registros de las últimas semanas"
    assert {d["childId"] for d in todos} <= {"mateo_garcia", "lucia_garcia"}


def test_registro_sembrado_es_anterior_a_hoy(cliente: TestClient, educadora: Persona):
    todos = cliente.get(f"{API}/daily-logs", headers=educadora.headers).json()
    assert all(d["date"] < hoy().isoformat() or d["date"] >= "2030-01-01" for d in todos)
