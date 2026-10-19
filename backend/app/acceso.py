"""Reglas de acceso compartidas: qué niños puede ver cada usuario."""

from sqlalchemy import select
from sqlalchemy.orm import Session

from .deps import Identidad
from .errors import ApiError
from .models import Child, ChildParent

ROLES_PERSONAL = ("EDUCADORA", "DIRECCION")


def es_personal(quien: Identidad) -> bool:
    return quien.role in ROLES_PERSONAL


def hijos_de(quien: Identidad, db: Session) -> list[str]:
    """Ids de los niños asociados a un familiar."""
    return list(db.scalars(select(ChildParent.child_id).where(ChildParent.parent_user_id == quien.id)).all())


def ids_visibles(quien: Identidad, db: Session) -> list[str] | None:
    """None = el personal ve a todos los niños; si no, la lista de hijos del familiar."""
    return None if es_personal(quien) else hijos_de(quien, db)


def exigir_alumno_visible(quien: Identidad, child_id: str, db: Session) -> Child:
    """Devuelve el alumno o lanza 404/403 si no existe o el usuario no tiene acceso a él."""
    alumno = db.get(Child, child_id)
    if alumno is None:
        raise ApiError(404, f"No existe el alumno con id {child_id}", "alumno_inexistente")
    if not es_personal(quien) and child_id not in hijos_de(quien, db):
        raise ApiError(
            403,
            "No tienes acceso a la información de este alumno",
            "sin_acceso_alumno",
            "Una familia solo puede consultar a sus propios hijos.",
        )
    return alumno
