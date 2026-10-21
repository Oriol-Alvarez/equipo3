"""El panel de Dirección muestra el estado de HOY, igual que la sala de la educadora."""
from fastapi.testclient import TestClient

from app.horario import hoy

API = "/api/v1"


def _login(cliente: TestClient, correo: str) -> dict[str, str]:
    r = cliente.post(f"{API}/auth/login", json={"email": correo, "password": "Password123!"})
    assert r.status_code == 200, r.text
    return {"Authorization": "Bearer " + r.json()["token"]}


def test_presentes_de_direccion_coinciden_con_las_salas(cliente: TestClient):
    direccion = _login(cliente, "direccion@sonrisas.edu")
    stats = cliente.get(f"{API}/admin/stats", headers=direccion)
    assert stats.status_code == 200, stats.text
    datos = stats.json()

    salas = cliente.get(f"{API}/rooms", headers=direccion)
    assert salas.status_code == 200, salas.text
    presentes_en_salas = sum(s["presentCount"] for s in salas.json())

    assert datos["presentCount"] == presentes_en_salas
    assert datos["pendingPickupsCount"] == datos["presentCount"]
    assert datos["ingresosCount"] == datos["presentCount"] + datos["egresosCount"]


def test_solo_cuenta_actividades_de_hoy(cliente: TestClient):
    educadora = _login(cliente, "educadora@sonrisas.edu")
    direccion = _login(cliente, "direccion@sonrisas.edu")
    antes = cliente.get(f"{API}/admin/stats", headers=direccion).json()

    # Una actividad de otro día no cambia el estado de hoy
    r = cliente.post(f"{API}/children/lucia_garcia/activities", headers=educadora, json={
        "time": "10:00 AM", "date": "01/01/2020", "category": "Accidentes", "title": "Accidentes",
        "description": "Prueba de otro día", "portionOption": "Accidente"})
    assert r.status_code == 201, r.text
    otro_dia = cliente.get(f"{API}/admin/stats", headers=direccion).json()
    assert otro_dia["incidentsCount"] == antes["incidentsCount"]

    # Una actividad de hoy sí cuenta
    r = cliente.post(f"{API}/children/lucia_garcia/activities", headers=educadora, json={
        "time": "10:30 AM", "date": hoy().strftime("%d/%m/%Y"), "category": "Accidentes", "title": "Accidentes",
        "description": "Prueba de hoy", "portionOption": "Accidente"})
    assert r.status_code == 201, r.text
    hoy_stats = cliente.get(f"{API}/admin/stats", headers=direccion).json()
    assert hoy_stats["incidentsCount"] == antes["incidentsCount"] + 1
