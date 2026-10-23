"""RF-19: reporte mensual del niño."""

from fastapi.testclient import TestClient

from app.horario import hoy

from .conftest import Persona


def _mes_actual() -> str:
    d = hoy()
    return f"{d.year:04d}-{d.month:02d}"


def test_la_familia_ve_el_reporte_mensual_de_su_hijo(cliente: TestClient, familiar: Persona, educadora: Persona):
    # Una actividad de hoy para que el mes tenga registros
    r = cliente.post(
        "/api/v1/children/mateo_garcia/activities",
        headers=educadora.headers,
        json={
            "time": "11:00 AM",
            "title": "Alimentación",
            "description": "Comió toda la sopa",
            "category": "Alimentación",
            "consumptionOption": "TODO",
        },
    )
    assert r.status_code in (200, 201), r.text

    meses = cliente.get("/api/v1/children/mateo_garcia/monthly-reports", headers=familiar.headers)
    assert meses.status_code == 200
    assert meses.json()[0] == _mes_actual()

    rep = cliente.get(f"/api/v1/children/mateo_garcia/monthly-reports/{_mes_actual()}", headers=familiar.headers)
    assert rep.status_code == 200, rep.text
    datos = rep.json()
    assert datos["id"] == _mes_actual()
    assert datos["isCurrentMonth"] is True
    assert datos["attendanceDays"] >= 1
    assert datos["activitiesRecorded"] >= 1
    assert datos["schoolDays"] >= datos["attendanceDays"] or datos["schoolDays"] == 0
    assert "Mateo" in datos["reportText"]
    assert datos["generatedWithAi"] is False  # sin llave de IA se usa el texto armado con los datos
    assert datos["pointsForFamily"]


def test_la_familia_no_ve_el_reporte_de_otro_nino(cliente: TestClient, familiar: Persona):
    r = cliente.get(f"/api/v1/children/sofia_lopez/monthly-reports/{_mes_actual()}", headers=familiar.headers)
    assert r.status_code in (403, 404)


def test_mes_invalido_o_futuro_da_404(cliente: TestClient, educadora: Persona):
    d = hoy()
    futuro = f"{d.year + 1:04d}-{d.month:02d}"
    for mes in ("2026-13", "hola", futuro, "2000-01"):
        r = cliente.get(f"/api/v1/children/mateo_garcia/monthly-reports/{mes}", headers=educadora.headers)
        assert r.status_code == 404, mes


def test_sin_sesion_no_hay_reporte(cliente: TestClient):
    r = cliente.get(f"/api/v1/children/mateo_garcia/monthly-reports/{_mes_actual()}")
    assert r.status_code == 401
