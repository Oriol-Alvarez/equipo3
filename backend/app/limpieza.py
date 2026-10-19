"""Limpieza periódica de archivos multimedia huérfanos (no asociados a ningún registro)."""

from datetime import datetime, timedelta, timezone

from sqlalchemy import select

from . import almacen
from .db import SessionLocal
from .models import LostObject, MediaFile

MARGEN = timedelta(hours=1)


def limpiar_huerfanas(margen: timedelta = MARGEN, ahora: datetime | None = None) -> list[str]:
    """Borra del almacén aquellos archivos que no están en la base de datos y superan el margen de cortesía."""
    ahora = ahora or datetime.now(timezone.utc)
    with SessionLocal() as db:
        usadas_media = set(db.scalars(select(MediaFile.file_name)))
        usadas_objetos = set(db.scalars(select(LostObject.image_uri).where(LostObject.image_uri.is_not(None))))

    usadas = usadas_media.union(usadas_objetos)
    borradas = []
    for clave, guardada in almacen.listar():
        if clave not in usadas and ahora - guardada > margen:
            almacen.borrar(clave)
            borradas.append(clave)
    return borradas


if __name__ == "__main__":
    borradas = limpiar_huerfanas()
    print(f"{len(borradas)} archivos huérfanos eliminados.")
