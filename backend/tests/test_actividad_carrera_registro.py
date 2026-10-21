"""La primera actividad del día no se pierde si la app crea el registro diario al mismo tiempo."""
from fastapi.testclient import TestClient

from app.horario import hoy
from app.routers import bitacora

API = "/api/v1"


def _login(cliente: TestClient, correo: str) -> dict[str, str]:
    r = cliente.post(f"{API}/auth/login", json={"email": correo, "password": "Password123!"})
    assert r.status_code == 200, r.text
    return {"Authorization": "Bearer " + r.json()["token"]}


def test_actividad_se_guarda_aunque_el_registro_del_dia_aparezca_a_la_vez(cliente: TestClient, monkeypatch):
    h = _login(cliente, "educadora@sonrisas.edu")
    dia = hoy()
    nino = "lucas_martinez"

    # La app crea el registro del día (PUT de la estadía) ...
    r = cliente.put(f"{API}/children/{nino}/daily-logs/{dia.isoformat()}/estadia", headers=h,
                    json={"breakfast": "POCO"})
    assert r.status_code == 200, r.text

    # ... pero el POST de la actividad no lo vio en su primera consulta (carrera real)
    real = bitacora._registro_del_dia
    llamadas = {"n": 0}

    def primera_vez_no_lo_ve(db, child_id, fecha_iso):
        llamadas["n"] += 1
        return None if llamadas["n"] == 1 else real(db, child_id, fecha_iso)

    monkeypatch.setattr(bitacora, "_registro_del_dia", primera_vez_no_lo_ve)

    r = cliente.post(f"{API}/children/{nino}/activities", headers=h, json={
        "time": "10:00 AM", "date": dia.strftime("%d/%m/%Y"), "category": "Alimentación",
        "title": "Alimentación", "description": "Colación: Comió todo", "portionOption": "Colación: Todo",
        "mealType": "COLACION"})
    assert r.status_code == 201, r.text
    assert llamadas["n"] == 2, "debe reintentar sobre el registro existente"

    # La actividad está en la bitácora y la estadía conserva lo de la app y lo nuevo
    linea = cliente.get(f"{API}/children/{nino}/timeline", headers=h)
    assert linea.status_code == 200, linea.text
    assert any(i.get("description") == "Colación: Comió todo" for i in linea.json())

    logs = cliente.get(f"{API}/daily-logs", headers=h).json()
    registro = next(d for d in logs if d["childId"] == nino and d["date"] == dia.isoformat())
    assert registro["estadia"]["breakfast"] == "POCO"
    assert registro["estadia"]["snack"] == "TODO"
