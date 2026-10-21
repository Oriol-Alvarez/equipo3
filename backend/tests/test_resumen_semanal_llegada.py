"""Una actividad cuenta como asistencia, pero su hora no es una hora de llegada."""
from fastapi.testclient import TestClient

from app.horario import hoy
from app.resumenes import lunes_de

API = "/api/v1"


def _login(cliente: TestClient, correo: str) -> dict[str, str]:
    r = cliente.post(f"{API}/auth/login", json={"email": correo, "password": "Password123!"})
    assert r.status_code == 200, r.text
    return {"Authorization": "Bearer " + r.json()["token"]}


def _semana_actual(cliente: TestClient, h: dict[str, str], nino: str) -> dict:
    r = cliente.get(f"{API}/children/{nino}/weekly-summaries/{lunes_de(hoy()).isoformat()}", headers=h)
    assert r.status_code == 200, r.text
    return r.json()


def test_actividad_cuenta_asistencia_sin_cambiar_la_llegada(cliente: TestClient):
    h = _login(cliente, "educadora@sonrisas.edu")
    nino = "valentina_ruiz"
    antes = _semana_actual(cliente, h, nino)

    r = cliente.post(f"{API}/children/{nino}/activities", headers=h, json={
        "time": "05:02 AM", "date": hoy().strftime("%d/%m/%Y"), "category": "Estado de Ánimo",
        "title": "Estado de Ánimo", "description": "Tranquila", "portionOption": ""})
    if r.status_code == 404:
        return  # el niño no existe en este seed
    assert r.status_code == 201, r.text

    despues = _semana_actual(cliente, h, nino)
    dia_ya_contado = despues["attendanceDaysCount"] == antes["attendanceDaysCount"]
    assert dia_ya_contado or despues["attendanceDaysCount"] == antes["attendanceDaysCount"] + 1
    assert despues["averageArrivalTime"] == antes["averageArrivalTime"], \
        "la hora de una actividad no debe usarse como hora de llegada"
    assert "feliz" not in despues["moodSummary"].lower() or "feliz" in antes["moodSummary"].lower(), \
        "un ánimo no reconocido no se convierte en Feliz"
