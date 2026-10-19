"""Almacén de objetos multimedia compatible con Amazon S3 / RustFS / MinIO.
Soporta fallback a almacenamiento local si el almacén S3 no está accesible durante pruebas locales.
"""

import os
from collections.abc import Iterator
from datetime import datetime, timezone
from io import BytesIO
from pathlib import Path
from typing import Any

import boto3
from botocore.config import Config
from botocore.exceptions import BotoCoreError, ClientError

from .config import settings

LOCAL_STORAGE_DIR = Path(__file__).resolve().parent.parent / "uploads"

_s3 = boto3.client(
    "s3",
    endpoint_url=settings.s3_endpoint,
    aws_access_key_id=settings.s3_access_key,
    aws_secret_access_key=settings.s3_secret_key,
    region_name="us-east-1",
    config=Config(
        s3={"addressing_style": "path"},
        connect_timeout=2,
        retries={"max_attempts": 2},
    ),
)

_usar_local = False


class LocalBodyWrapper:
    def __init__(self, data: bytes):
        self._bio = BytesIO(data)

    def read(self, amt: int | None = None) -> bytes:
        return self._bio.read(amt)

    def iter_chunks(self, chunk_size: int = 64 * 1024):
        while chunk := self._bio.read(chunk_size):
            yield chunk

    def close(self):
        self._bio.close()


def preparar() -> None:
    """Verifica o crea el bucket S3 al arrancar la aplicación. Activa modo local si no hay S3."""
    global _usar_local
    try:
        _s3.head_bucket(Bucket=settings.s3_bucket)
        _usar_local = False
    except ClientError as e:
        error_code = e.response.get("Error", {}).get("Code")
        if error_code in ("404", "NoSuchBucket"):
            try:
                _s3.create_bucket(Bucket=settings.s3_bucket)
                _usar_local = False
                return
            except Exception:
                pass
        _usar_local = True
    except Exception:
        _usar_local = True

    if _usar_local:
        LOCAL_STORAGE_DIR.mkdir(parents=True, exist_ok=True)


def guardar(clave: str, datos: bytes, tipo: str) -> None:
    """Guarda un objeto binario con su tipo MIME."""
    global _usar_local
    if not _usar_local:
        try:
            _s3.put_object(
                Bucket=settings.s3_bucket,
                Key=clave,
                Body=datos,
                ContentType=tipo,
            )
            return
        except (BotoCoreError, ClientError):
            _usar_local = True

    LOCAL_STORAGE_DIR.mkdir(parents=True, exist_ok=True)
    archivo = LOCAL_STORAGE_DIR / clave
    archivo.parent.mkdir(parents=True, exist_ok=True)
    archivo.write_bytes(datos)
    meta = LOCAL_STORAGE_DIR / f"{clave}.meta"
    meta.parent.mkdir(parents=True, exist_ok=True)
    meta.write_text(tipo, encoding="utf-8")


def abrir(clave: str) -> dict[str, Any] | None:
    """Obtiene el objeto S3 o local listo para streaming."""
    global _usar_local
    if not _usar_local:
        try:
            return _s3.get_object(Bucket=settings.s3_bucket, Key=clave)
        except ClientError as e:
            if e.response.get("Error", {}).get("Code") in ("NoSuchKey", "404"):
                return None
            _usar_local = True
        except (BotoCoreError, Exception):
            _usar_local = True

    archivo = LOCAL_STORAGE_DIR / clave
    if not archivo.exists():
        return None
    datos = archivo.read_bytes()
    meta = LOCAL_STORAGE_DIR / f"{clave}.meta"
    tipo = meta.read_text(encoding="utf-8") if meta.exists() else "application/octet-stream"

    return {
        "Body": LocalBodyWrapper(datos),
        "ContentType": tipo,
        "ContentLength": len(datos),
        "ETag": f'"{len(datos)}-{hash(datos)}"',
    }


def existe(clave: str) -> bool:
    """Comprueba si un objeto existe en el almacén."""
    global _usar_local
    if not _usar_local:
        try:
            _s3.head_object(Bucket=settings.s3_bucket, Key=clave)
            return True
        except ClientError as e:
            if e.response.get("Error", {}).get("Code") in ("NoSuchKey", "404"):
                return False
            _usar_local = True
        except (BotoCoreError, Exception):
            _usar_local = True

    return (LOCAL_STORAGE_DIR / clave).exists()


def borrar(clave: str) -> None:
    """Elimina un objeto del almacén."""
    global _usar_local
    if not _usar_local:
        try:
            _s3.delete_object(Bucket=settings.s3_bucket, Key=clave)
            return
        except (BotoCoreError, ClientError):
            _usar_local = True

    archivo = LOCAL_STORAGE_DIR / clave
    if archivo.exists():
        archivo.unlink()
    meta = LOCAL_STORAGE_DIR / f"{clave}.meta"
    if meta.exists():
        meta.unlink()


def listar() -> Iterator[tuple[str, datetime]]:
    """Itera sobre todos los objetos y sus fechas de modificación."""
    global _usar_local
    if not _usar_local:
        try:
            for pagina in _s3.get_paginator("list_objects_v2").paginate(Bucket=settings.s3_bucket):
                for objeto in pagina.get("Contents", []):
                    yield objeto["Key"], objeto["LastModified"]
            return
        except Exception:
            _usar_local = True

    if LOCAL_STORAGE_DIR.exists():
        for f in LOCAL_STORAGE_DIR.iterdir():
            if not f.name.endswith(".meta") and f.is_file():
                mtime = datetime.fromtimestamp(f.stat().st_mtime, tz=timezone.utc)
                yield f.name, mtime
