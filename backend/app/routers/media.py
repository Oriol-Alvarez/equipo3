"""Módulo 13: Almacenamiento y Subida Multimedia (/media)

Entrega final:
- RF-09 / RF-07: un archivo solo lo descarga quien inició sesión y tiene relación con él
  (el personal, quien lo subió o la familia de un niño que aparece en el mensaje).
- RF-14: cada consulta de una foto o archivo queda registrada (quién y cuándo).
- RF-10: la familia puede pedir que se borre una foto donde aparece su hijo; el archivo se
  elimina del almacén, desaparece de todos los mensajes y solo queda la constancia del borrado.
"""

import re
import uuid

from fastapi import APIRouter, Depends, File, Form, Query, Request, Response, UploadFile
from fastapi.responses import StreamingResponse
from sqlalchemy import select
from sqlalchemy.orm import Session

from .. import almacen
from ..acceso import es_personal, hijos_de
from ..config import settings
from ..db import get_db
from ..deps import Identidad, requiere_direccion, usuario_actual
from ..errors import ApiError
from ..models import (
    BitacoraMessage,
    ChildProfile,
    FamilyGroupMember,
    FamilyGroupMessage,
    LostObject,
    MediaFile,
    PhotoAccess,
    PhotoDeletion,
    WorkerChatMessage,
)
from ..schemas import MediaUploadOut

router = APIRouter(prefix="/media", tags=["media"])

_ID_VALIDO = re.compile(r"^[A-Za-z0-9_\-]{1,64}$")
TEXTO_FOTO_ELIMINADA = "Foto eliminada a solicitud de la familia"


@router.post("/upload", response_model=MediaUploadOut, status_code=201)
def subir_archivo(
    file: UploadFile = File(...),
    folder: str = Form("chat"),
    quien: Identidad = Depends(usuario_actual),
    db: Session = Depends(get_db),
) -> MediaUploadOut:
    """Recibe un archivo binario (JPEG, PNG, WEBP, PDF) y lo almacena en RustFS/S3."""
    datos = file.file.read(settings.media_max_bytes + 1)
    if len(datos) > settings.media_max_bytes:
        raise ApiError(
            413,
            "El archivo excede el tamaño máximo permitido",
            "archivo_grande",
            f"El límite máximo es de {settings.media_max_bytes // (1024 * 1024)} MB.",
        )

    mime = file.content_type or "application/octet-stream"
    extension = file.filename.split(".")[-1] if file.filename and "." in file.filename else "bin"
    if not re.fullmatch(r"[A-Za-z0-9]{1,8}", extension):
        extension = "bin"
    carpeta = folder if re.fullmatch(r"[A-Za-z0-9_\-]{1,40}", folder or "") else "chat"
    file_id = f"media_{uuid.uuid4().hex[:12]}"
    clave_almacen = f"{carpeta}/{file_id}.{extension}"

    almacen.guardar(clave_almacen, datos, mime)

    url_archivo = f"/api/v1/media/{file_id}"
    media_db = MediaFile(
        id=file_id,
        file_name=clave_almacen,
        mime_type=mime,
        size_bytes=len(datos),
        url=url_archivo,
        folder=carpeta,
        user_id=quien.id,
    )
    db.add(media_db)
    db.commit()

    return MediaUploadOut(
        fileId=file_id,
        url=url_archivo,
        fileName=file.filename or f"{file_id}.{extension}",
        mimeType=mime,
        sizeBytes=len(datos),
    )


# ---------------------------------------------------------------------------
# Quién puede ver o borrar un archivo
# ---------------------------------------------------------------------------


def _patron(file_id: str) -> str:
    """Las referencias se guardan como '/api/v1/media/<id>' o como URL completa."""
    return f"%/media/{file_id}"


def _ninos_del_archivo(file_id: str, db: Session) -> set[str]:
    """Niños que aparecen junto al archivo: bitácora del niño, grupo de familias o certificado."""
    patron = _patron(file_id)
    ninos = set(db.scalars(select(BitacoraMessage.child_id).where(BitacoraMessage.file_uri.like(patron))).all())
    grupos = db.scalars(
        select(FamilyGroupMessage.group_id).where(FamilyGroupMessage.attachment_uri.like(patron))
    ).all()
    if grupos:
        ninos |= set(
            db.scalars(select(FamilyGroupMember.child_id).where(FamilyGroupMember.group_id.in_(grupos))).all()
        )
    ninos |= set(
        db.scalars(select(ChildProfile.child_id).where(ChildProfile.medical_certificate_uri.like(patron))).all()
    )
    return ninos


def _es_publico(file_id: str, db: Session) -> bool:
    """Fotos de objetos perdidos: las ve cualquier persona con sesión."""
    patron = _patron(file_id)
    return db.scalar(select(LostObject.id).where(LostObject.image_uri.like(patron))) is not None


def _media_o_404(file_id: str, db: Session) -> MediaFile:
    if not _ID_VALIDO.match(file_id):
        raise ApiError(404, "El archivo solicitado no existe", "archivo_inexistente")
    media = db.get(MediaFile, file_id)
    if media is None:
        raise ApiError(404, "El archivo solicitado no existe", "archivo_inexistente")
    return media


def _exigir_puede_ver(quien: Identidad, media: MediaFile, db: Session) -> None:
    if es_personal(quien) or media.user_id == quien.id:
        return
    if _es_publico(media.id, db):
        return
    if _ninos_del_archivo(media.id, db) & set(hijos_de(quien, db)):
        return
    raise ApiError(
        403,
        "No tienes acceso a este archivo",
        "sin_acceso_archivo",
        "Una familia solo puede ver las fotos donde aparecen sus hijos.",
    )


def _exigir_puede_borrar(quien: Identidad, media: MediaFile, db: Session) -> None:
    if es_personal(quien) or media.user_id == quien.id:
        return
    if _ninos_del_archivo(media.id, db) & set(hijos_de(quien, db)):
        return
    raise ApiError(403, "Solo puedes pedir que se borren fotos donde aparece tu hijo", "sin_permiso_borrar")


@router.get("/{fileId}")
def descargar_archivo(
    fileId: str,
    request: Request,
    quien: Identidad = Depends(usuario_actual),
    db: Session = Depends(get_db),
) -> Response:
    """Descarga el archivo si la persona tiene acceso y deja constancia de la consulta (RF-14)."""
    media = _media_o_404(fileId, db)
    _exigir_puede_ver(quien, media, db)

    objeto = almacen.abrir(media.file_name)
    if objeto is None:
        raise ApiError(404, "El objeto no se encuentra en el almacén de datos", "objeto_inexistente")

    db.add(
        PhotoAccess(
            file_id=media.id,
            user_id=quien.id,
            user_name=quien.display_name or quien.email,
            user_role=quien.role,
        )
    )
    db.commit()

    etag = objeto["ETag"]
    # Contenido privado: el teléfono lo puede guardar en su caché, ningún intermediario.
    headers_cache = {"ETag": etag, "Cache-Control": "private, max-age=86400"}

    if request.headers.get("If-None-Match") == etag:
        objeto["Body"].close()
        return Response(status_code=304, headers=headers_cache)

    return StreamingResponse(
        objeto["Body"].iter_chunks(64 * 1024),
        media_type=media.mime_type,
        headers={**headers_cache, "Content-Length": str(objeto["ContentLength"])},
    )


@router.delete("/{fileId}")
def eliminar_archivo(
    fileId: str,
    reason: str = Query(default="", max_length=300),
    quien: Identidad = Depends(usuario_actual),
    db: Session = Depends(get_db),
) -> dict:
    """RF-10: borra la foto del almacén y de todos los mensajes donde aparecía."""
    media = _media_o_404(fileId, db)
    _exigir_puede_borrar(quien, media, db)
    patron = _patron(media.id)
    limpiadas = 0

    for m in db.scalars(select(BitacoraMessage).where(BitacoraMessage.file_uri.like(patron))).all():
        m.file_uri, m.file_name, m.file_mime_type = None, None, None
        m.message = f"{m.message} ({TEXTO_FOTO_ELIMINADA.lower()})" if m.message.strip() else TEXTO_FOTO_ELIMINADA
        limpiadas += 1
    for m in db.scalars(select(WorkerChatMessage).where(WorkerChatMessage.file_uri.like(patron))).all():
        m.file_uri, m.file_name, m.file_mime_type = None, None, None
        m.message = m.message if m.message.strip() else TEXTO_FOTO_ELIMINADA
        limpiadas += 1
    for m in db.scalars(select(FamilyGroupMessage).where(FamilyGroupMessage.attachment_uri.like(patron))).all():
        m.attachment_uri, m.attachment_name, m.attachment_mime_type = None, None, None
        m.text = f"{m.text} ({TEXTO_FOTO_ELIMINADA.lower()})" if m.text.strip() else TEXTO_FOTO_ELIMINADA
        limpiadas += 1
    for o in db.scalars(select(LostObject).where(LostObject.image_uri.like(patron))).all():
        o.image_uri = None
        limpiadas += 1
    for p in db.scalars(select(ChildProfile).where(ChildProfile.medical_certificate_uri.like(patron))).all():
        p.medical_certificate_uri, p.medical_certificate_name, p.medical_certificate_date = None, None, None
        limpiadas += 1

    almacen.borrar(media.file_name)
    db.add(
        PhotoDeletion(
            file_id=media.id,
            requested_by_id=quien.id,
            requested_by_name=quien.display_name or quien.email,
            requested_by_role=quien.role,
            reason=reason.strip(),
            references_cleared=limpiadas,
        )
    )
    db.delete(media)
    db.commit()

    return {"deleted": True, "fileId": fileId, "referencesCleared": limpiadas}


@router.get("/{fileId}/accesses")
def consultar_accesos(
    fileId: str,
    _: Identidad = Depends(requiere_direccion),
    db: Session = Depends(get_db),
) -> list[dict]:
    """RF-14: la dirección consulta quién vio un archivo y cuándo."""
    accesos = db.scalars(
        select(PhotoAccess).where(PhotoAccess.file_id == fileId).order_by(PhotoAccess.accessed_at.desc())
    ).all()
    return [
        {
            "userId": a.user_id,
            "userName": a.user_name,
            "userRole": a.user_role,
            "accessedAt": a.accessed_at.isoformat() if a.accessed_at else None,
        }
        for a in accesos
    ]
