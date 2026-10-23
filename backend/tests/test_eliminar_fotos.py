"""Pruebas para eliminación de fotos en chats con educadores y grupos por parte de los padres."""

import io
from fastapi.testclient import TestClient
from .conftest import Persona

API = "/api/v1"


def test_padre_elimina_foto_en_chat_con_educadora(cliente: TestClient, familiar: Persona, educadora: Persona):
    # 1. Enviar un mensaje con foto y texto en el chat de bitácora de su hijo Mateo García
    r_msg = cliente.post(
        f"{API}/chats/bitacora/mateo_garcia/messages",
        headers=familiar.headers,
        json={
            "message": "Mira la foto de hoy",
            "fileUri": "https://cdn.sonrisas.edu/media/foto_mateo.jpg",
            "fileName": "foto_mateo.jpg",
            "fileMimeType": "image/jpeg",
        },
    )
    assert r_msg.status_code == 201
    msg_id = r_msg.json()["id"]

    # 2. El padre elimina la foto del mensaje
    r_del = cliente.delete(
        f"{API}/chats/bitacora/mateo_garcia/messages/{msg_id}/photo",
        headers=familiar.headers,
    )
    assert r_del.status_code == 204

    # 3. Al listar los mensajes, el mensaje sigue existiendo pero su fileUri es None
    msgs = cliente.get(f"{API}/chats/bitacora/mateo_garcia/messages", headers=familiar.headers).json()
    msg = next(m for m in msgs if m["id"] == msg_id)
    assert msg["message"] == "Mira la foto de hoy"
    assert msg["fileUri"] is None


def test_padre_elimina_foto_sin_texto_en_chat_educadora(cliente: TestClient, familiar: Persona):
    # 1. Enviar un mensaje que solo contiene foto
    r_msg = cliente.post(
        f"{API}/chats/bitacora/mateo_garcia/messages",
        headers=familiar.headers,
        json={
            "message": "",
            "fileUri": "https://cdn.sonrisas.edu/media/solo_foto.jpg",
            "fileName": "solo_foto.jpg",
            "fileMimeType": "image/jpeg",
        },
    )
    assert r_msg.status_code == 201
    msg_id = r_msg.json()["id"]

    # 2. El padre elimina la foto
    r_del = cliente.delete(
        f"{API}/chats/bitacora/mateo_garcia/messages/{msg_id}/photo",
        headers=familiar.headers,
    )
    assert r_del.status_code == 204

    # 3. Como solo era foto, el mensaje completo se elimina de la lista
    msgs = cliente.get(f"{API}/chats/bitacora/mateo_garcia/messages", headers=familiar.headers).json()
    assert not any(m["id"] == msg_id for m in msgs)


def test_padre_elimina_foto_en_grupo_familiar(cliente: TestClient, familiar: Persona):
    gid = "fgroup_paseo_sala_1a"
    # 1. Enviar mensaje con foto al grupo
    r_msg = cliente.post(
        f"{API}/family-groups/{gid}/messages",
        headers=familiar.headers,
        json={
            "text": "Aquí está la foto de la autorización",
            "attachment": {
                "uri": "https://cdn.sonrisas.edu/media/autorizacion.jpg",
                "name": "autorizacion.jpg",
                "mimeType": "image/jpeg",
            },
        },
    )
    assert r_msg.status_code == 201
    msg_id = r_msg.json()["id"]

    # 2. El padre elimina la foto
    r_del = cliente.delete(
        f"{API}/family-groups/{gid}/messages/{msg_id}/photo",
        headers=familiar.headers,
    )
    assert r_del.status_code == 204

    # 3. Al consultar los mensajes del grupo, el mensaje no tiene attachment
    msgs = cliente.get(f"{API}/family-groups/{gid}/messages", headers=familiar.headers).json()
    msg = next(m for m in msgs if m["id"] == msg_id)
    assert msg["text"] == "Aquí está la foto de la autorización"
    assert msg["attachment"] is None


def test_padre_no_puede_eliminar_en_grupo_ajeno(cliente: TestClient, familiar: Persona, educadora: Persona):
    # Crear un grupo donde solo está Sofía López (no hijo de este familiar)
    r_grupo = cliente.post(
        f"{API}/family-groups",
        headers=educadora.headers,
        json={"title": "Grupo Privado Sofía", "memberChildIds": ["sofia_lopez"]},
    )
    assert r_grupo.status_code == 201
    gid = r_grupo.json()["id"]

    r_msg = cliente.post(
        f"{API}/family-groups/{gid}/messages",
        headers=educadora.headers,
        json={
            "text": "Foto privada",
            "attachment": {
                "uri": "https://cdn.sonrisas.edu/media/privada.jpg",
                "name": "privada.jpg",
                "mimeType": "image/jpeg",
            },
        },
    )
    assert r_msg.status_code == 201
    msg_id = r_msg.json()["id"]

    # El familiar intenta eliminar en un grupo donde no pertenece
    r_del = cliente.delete(
        f"{API}/family-groups/{gid}/messages/{msg_id}/photo",
        headers=familiar.headers,
    )
    assert r_del.status_code == 403


def test_foto_se_borra_del_almacen_rustfs_s3(cliente: TestClient, familiar: Persona):
    from app import almacen
    from .conftest import PNG_CHICO

    # 1. Subir una fotografía al almacén
    r_subida = cliente.post(
        f"{API}/media/upload",
        headers=familiar.headers,
        files={"file": ("foto_prueba.png", io.BytesIO(PNG_CHICO), "image/png")},
        data={"folder": "chat"},
    )
    assert r_subida.status_code == 201
    datos_media = r_subida.json()
    file_id = datos_media["fileId"]
    media_url = datos_media["url"]

    # Comprobar que el archivo se puede descargar
    assert cliente.get(media_url).status_code == 200

    # 2. Enviar la foto en el chat de bitácora
    r_msg = cliente.post(
        f"{API}/chats/bitacora/mateo_garcia/messages",
        headers=familiar.headers,
        json={
            "message": "Foto que se borrará de RustFS",
            "fileUri": media_url,
            "fileName": "foto_prueba.png",
            "fileMimeType": "image/png",
        },
    )
    assert r_msg.status_code == 201
    msg_id = r_msg.json()["id"]

    # 3. Eliminar la foto a través del endpoint
    r_del = cliente.delete(
        f"{API}/chats/bitacora/mateo_garcia/messages/{msg_id}/photo",
        headers=familiar.headers,
    )
    assert r_del.status_code == 204

    # 4. Verificar que se borró de RustFS/almacén (al intentar descargar da 404)
    r_get_media = cliente.get(media_url)
    assert r_get_media.status_code == 404

