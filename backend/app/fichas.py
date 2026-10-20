"""Ficha básica de un alumno tal como la recibe la app (ChildOut).

El estado del día (presente, retirado o sin ingresar) se calcula con el registro diario
de HOY, para que cada mañana todos empiecen como "No ha ingresado" sin depender de un
indicador guardado que nunca se reiniciaba.
"""

import json
from collections.abc import Iterable
from dataclasses import dataclass

from sqlalchemy import select
from sqlalchemy.orm import Session

from .horario import hoy
from .models import Child, ChildProfile, DailyLog, Room, RoomEducator, StaffMember, Usuario
from .resumenes import hora_12h, minutos_de
from .schemas import ChildOut


def _hora(registro_json: str | None) -> str | None:
    if not registro_json:
        return None
    try:
        datos = json.loads(registro_json)
    except ValueError:
        return None
    minutos = minutos_de(datos.get("time")) if isinstance(datos, dict) else None
    return hora_12h(minutos) if minutos is not None else None


@dataclass
class Educadora:
    id: str
    name: str
    role: str
    initials: str
    color: int


def educadora_de_sala(db: Session, room_id: str) -> Educadora | None:
    """Primera educadora asignada a la sala (None si la sala no tiene ninguna)."""
    asignacion = db.scalar(
        select(RoomEducator).where(RoomEducator.room_id == room_id).order_by(RoomEducator.id).limit(1)
    )
    if asignacion is None:
        return None
    personal = db.scalar(select(StaffMember).where(StaffMember.user_id == asignacion.educator_id).limit(1))
    if personal is not None:
        return Educadora(
            id=asignacion.educator_id,
            name=personal.name,
            role=asignacion.role_title or personal.role,
            initials=personal.avatar_initials,
            color=personal.avatar_bg_color,
        )
    usuario = db.get(Usuario, asignacion.educator_id)
    if usuario is None:
        return None
    nombre = usuario.display_name.split(" (")[0].strip()
    return Educadora(
        id=usuario.id,
        name=nombre,
        role=asignacion.role_title,
        initials="".join(parte[0] for parte in nombre.split()[:2]).upper() or "ED",
        color=4278364369,
    )


def fichas(alumnos: Iterable[Child], db: Session) -> list[ChildOut]:
    alumnos = list(alumnos)
    if not alumnos:
        return []
    hoy_iso = hoy().isoformat()
    registros = {
        r.child_id: r
        for r in db.scalars(
            select(DailyLog).where(DailyLog.date == hoy_iso, DailyLog.child_id.in_([a.id for a in alumnos]))
        ).all()
    }
    salas = {s.id: s.name for s in db.scalars(select(Room)).all()}
    nacimientos = {
        p.child_id: p.birth_date
        for p in db.scalars(select(ChildProfile).where(ChildProfile.child_id.in_([a.id for a in alumnos]))).all()
    }

    educadoras = {sala: educadora_de_sala(db, sala) for sala in {a.room_id for a in alumnos}}

    salida = []
    for a in alumnos:
        educadora = educadoras.get(a.room_id)
        r = registros.get(a.id)
        llegada = _hora(r.ingreso) if r else None
        salida_hora = _hora(r.egreso) if r else None
        if salida_hora:
            presente, estado = False, f"{salida_hora} • Retirado(a)"
        elif llegada:
            presente, estado = True, f"{llegada} • Presente"
        else:
            presente, estado = False, "No ha ingresado"
        salida.append(
            ChildOut(
                id=a.id,
                fullName=a.full_name,
                shortName=a.short_name,
                ageText=a.age_text,
                roomText=salas.get(a.room_id, a.room_id),
                groupText=a.group_text,
                statusText=estado,
                isPresent=presente,
                arrivalTime=llegada,
                allergyAlert=a.allergy_alert,
                avatarInitials=a.avatar_initials,
                avatarBgColor=a.avatar_bg_color,
                birthDate=nacimientos.get(a.id) or "",
                roomId=a.room_id,
                educatorName=educadora.name if educadora else None,
                educatorRole=educadora.role if educadora else None,
                educatorAvatarInitials=educadora.initials if educadora else None,
                educatorAvatarBgColor=educadora.color if educadora else None,
            )
        )
    return salida


def ficha(alumno: Child, db: Session) -> ChildOut:
    return fichas([alumno], db)[0]
