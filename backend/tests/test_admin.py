"""Pruebas automáticas para los endpoints administrativos de Sonrisas (/api/v1/admin)."""

import pytest
from fastapi.testclient import TestClient

from app.db import Base, SessionLocal, engine
from app.main import app
from app.models import Usuario
from app.seed import seed_database, seed_modulos_heber


@pytest.fixture(scope="module")
def client():
    Base.metadata.create_all(engine)
    with SessionLocal() as db:
        seed_database(db)
        seed_modulos_heber(db)
    with TestClient(app) as c:
        yield c


@pytest.fixture(scope="module")
def admin_token(client):
    r = client.post(
        "/api/v1/auth/login",
        json={"email": "direccion@sonrisas.edu", "password": "Password123!"},
    )
    assert r.status_code == 200
    return r.json()["token"]


def test_admin_stats(client, admin_token):
    headers = {"Authorization": f"Bearer {admin_token}"}
    r = client.get("/api/v1/admin/stats", headers=headers)
    assert r.status_code == 200
    data = r.json()
    assert "totalChildren" in data
    assert "presentCount" in data
    assert "attendanceRate" in data
    assert "totalEducators" in data
    assert "totalFamilies" in data
    assert "childrenFedCount" in data
    assert "childrenNappedCount" in data
    assert data["totalChildren"] > 0


def test_admin_logs(client, admin_token):
    headers = {"Authorization": f"Bearer {admin_token}"}
    r = client.get("/api/v1/admin/logs", headers=headers)
    assert r.status_code == 200
    logs = r.json()
    assert isinstance(logs, list)
    assert len(logs) > 0
    first = logs[0]
    assert "id" in first
    assert "category" in first
    assert "title" in first
    assert "description" in first


def test_admin_user_crud(client, admin_token):
    headers = {"Authorization": f"Bearer {admin_token}"}

    # 1. List users
    r = client.get("/api/v1/admin/users", headers=headers)
    assert r.status_code == 200
    initial_count = len(r.json())
    assert initial_count >= 3

    # 2. Create user
    new_user = {
        "email": "nuevo.educador@sonrisas.edu",
        "password": "Password123!",
        "role": "EDUCADORA",
        "displayName": "Nuevo Maestro",
        "phone": "+52 555 999 888",
    }
    r = client.post("/api/v1/admin/users", json=new_user, headers=headers)
    assert r.status_code == 201
    created = r.json()
    user_id = created["id"]
    assert created["email"] == "nuevo.educador@sonrisas.edu"
    assert created["displayName"] == "Nuevo Maestro"

    # 3. Update user
    update_data = {
        "displayName": "Maestro Actualizado",
        "phone": "+52 555 111 222",
    }
    r = client.put(f"/api/v1/admin/users/{user_id}", json=update_data, headers=headers)
    assert r.status_code == 200
    assert r.json()["displayName"] == "Maestro Actualizado"

    # 4. Delete user
    r = client.delete(f"/api/v1/admin/users/{user_id}", headers=headers)
    assert r.status_code == 204

    # 5. Confirm deleted
    r = client.get("/api/v1/admin/users", headers=headers)
    assert len(r.json()) == initial_count


def test_admin_announcements_crud(client, admin_token):
    headers = {"Authorization": f"Bearer {admin_token}"}

    # Create announcement
    new_ann = {
        "title": "Aviso Urgente de Prueba",
        "summary": "Resumen urgente",
        "fullContent": "Contenido completo del aviso para todos.",
        "author": "Dirección Escolar",
    }
    r = client.post("/api/v1/announcements", json=new_ann, headers=headers)
    assert r.status_code == 201
    ann_id = r.json()["id"]

    # Update announcement
    r = client.put(
        f"/api/v1/announcements/{ann_id}",
        json={"title": "Aviso Editado de Prueba", "summary": "Resumen editado"},
        headers=headers,
    )
    assert r.status_code == 200
    assert r.json()["title"] == "Aviso Editado de Prueba"

    # Delete announcement
    r = client.delete(f"/api/v1/announcements/{ann_id}", headers=headers)
    assert r.status_code == 204


def test_admin_manage_children_for_parent(client, admin_token):
    headers = {"Authorization": f"Bearer {admin_token}"}

    # 1. Check parent users have children
    r = client.get("/api/v1/admin/users?role=FAMILIAR", headers=headers)
    assert r.status_code == 200
    fams = r.json()
    assert len(fams) > 0
    # Seed parent usr_familia_garcia should have children
    garcia = next((f for f in fams if f["id"] == "usr_familia_garcia"), fams[0])
    assert "children" in garcia
    assert len(garcia["children"]) >= 1

    # 2. List all children
    r = client.get("/api/v1/admin/children", headers=headers)
    assert r.status_code == 200
    all_children = r.json()
    assert len(all_children) >= 2

    # 3. Create child linked to parent
    new_child = {
        "fullName": "Bebé Nuevo Prueba",
        "shortName": "Bebé",
        "roomText": "Lactantes",
        "bloodType": "A+",
        "pediatrician": "Dr. Fernando",
        "emergencyPhone": "555-999-888",
    }
    r = client.post(f"/api/v1/admin/users/{garcia['id']}/children", json=new_child, headers=headers)
    assert r.status_code == 201
    created_child = r.json()
    assert created_child["fullName"] == "Bebé Nuevo Prueba"
    assert created_child["bloodType"] == "A+"
    cid = created_child["id"]

    # 4. Update child
    update_child = {
        "fullName": "Bebé Actualizado",
        "allergyAlert": "Alergia a la fresa",
    }
    r = client.put(f"/api/v1/admin/children/{cid}", json=update_child, headers=headers)
    assert r.status_code == 200
    assert r.json()["fullName"] == "Bebé Actualizado"
    assert r.json()["allergyAlert"] == "Alergia a la fresa"

    # 5. Unlink child
    r = client.delete(f"/api/v1/admin/users/{garcia['id']}/children/{cid}", headers=headers)
    assert r.status_code == 204

