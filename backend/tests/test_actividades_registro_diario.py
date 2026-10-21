"""Guardar actividades no debe romper el registro diario ni inventar datos."""
from fastapi.testclient import TestClient

from app.horario import hoy

API = "/api/v1"


def _login(cliente: TestClient, correo: str) -> dict[str, str]:
    r = cliente.post(f"{API}/auth/login", json={"email": correo, "password": "Password123!"})
    assert r.status_code == 200, r.text
    return {"Authorization": "Bearer " + r.json()["token"]}


def _registro_de_hoy(cliente: TestClient, h: dict[str, str], child_id: str) -> dict | None:
    r = cliente.get(f"{API}/daily-logs", headers=h)
    assert r.status_code == 200, r.text
    dia = hoy().isoformat()
    return next((d for d in r.json() if d["childId"] == child_id and d["date"] == dia), None)


def test_actividades_no_rompen_registro_diario_ni_inventan_ingreso(cliente: TestClient):
    h = _login(cliente, "educadora@sonrisas.edu")
    fecha = hoy().strftime("%d/%m/%Y")
    casos = [
        {"time": "10:00 AM", "category": "Alimentación", "title": "Alimentación",
         "description": "Desayuno: Todo", "portionOption": "Comida: Todo"},
        {"time": "11:00 AM", "category": "Descanso", "title": "Descanso",
         "description": "Durmió 40 minutos", "portionOption": "40 min siesta"},
        {"time": "12:00 PM", "category": "Estado de Ánimo", "title": "Estado de Ánimo",
         "description": "Se le vio así en la mañana", "portionOption": "🙁 Triste"},
    ]
    for body in casos:
        r = cliente.post(f"{API}/children/mateo_garcia/activities", json={**body, "date": fecha}, headers=h)
        assert r.status_code == 201, r.text
        # El registro diario debe seguir respondiendo después de cada actividad
        _registro_de_hoy(cliente, h, "mateo_garcia")

    registro = _registro_de_hoy(cliente, h, "mateo_garcia")
    assert registro is not None, "la actividad debe reflejarse en la estadía del día"
    ingreso = registro["ingreso"]
    assert ingreso is None or ingreso.get("observations") != "Registro por actividad en bitácora", \
        "una actividad no debe crear un ingreso: solo Asistencia lo registra"
    assert registro["estadia"]["mood"] == "TRISTE", "el ánimo elegido no se reemplaza por Feliz"


def test_animo_no_reconocido_no_se_inventa(cliente: TestClient):
    h = _login(cliente, "educadora@sonrisas.edu")
    body = {"time": "09:00 AM", "category": "Estado de Ánimo", "title": "Estado de Ánimo",
            "description": "Llegó con sueño", "portionOption": "", "date": hoy().strftime("%d/%m/%Y")}
    r = cliente.post(f"{API}/children/sofia_lopez/activities", json=body, headers=h)
    assert r.status_code in (201, 404), r.text
    if r.status_code == 201:
        registro = _registro_de_hoy(cliente, h, "sofia_lopez")
        if registro and registro["estadia"]:
            assert registro["estadia"].get("mood") in (None, ""), "no debe inventar Feliz"
