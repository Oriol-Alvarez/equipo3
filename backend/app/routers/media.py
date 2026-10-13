"""Módulo 13: Almacenamiento y Subida Multimedia (/media)"""

import uuid

from fastapi import APIRouter, Depends, File, Form, Request, Response, UploadFile
from fastapi.responses import StreamingResponse
from sqlalchemy.orm import Session

from .. import almacen
from ..config import settings
from ..db import get_db
from ..deps import Identidad, usuario_actual
from ..errors import ApiError
from ..models import MediaFile
from ..schemas import MediaUploadOut

router = APIRouter(prefix="/media", tags=["media"])


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
    file_id = f"media_{uuid.uuid4().hex[:12]}"
    clave_almacen = f"{folder}/{file_id}.{extension}"

    almacen.guardar(clave_almacen, datos, mime)

    url_archivo = f"/api/v1/media/{file_id}"
    media_db = MediaFile(
        id=file_id,
        file_name=clave_almacen,
        mime_type=mime,
        size_bytes=len(datos),
        url=url_archivo,
        folder=folder,
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


@router.get("/{fileId}")
def descargar_archivo(
    fileId: str,
    request: Request,
    db: Session = Depends(get_db),
) -> Response:
    """Descarga o visualiza en streaming el archivo almacenado con soporte de caché ETag."""
    media = db.get(MediaFile, fileId)
    if media is None:
        raise ApiError(404, "El archivo solicitado no existe", "archivo_inexistente")

    objeto = almacen.abrir(media.file_name)
    if objeto is None:
        raise ApiError(404, "El objeto no se encuentra en el almacén de datos", "objeto_inexistente")

    etag = objeto["ETag"]
    headers_cache = {
        "ETag": etag,
        "Cache-Control": "public, max-age=31536000, immutable",
    }

    if request.headers.get("If-None-Match") == etag:
        objeto["Body"].close()
        return Response(status_code=304, headers=headers_cache)

    return StreamingResponse(
        objeto["Body"].iter_chunks(64 * 1024),
        media_type=media.mime_type,
        headers={
            **headers_cache,
            "Content-Length": str(objeto["ContentLength"]),
        },
    )
