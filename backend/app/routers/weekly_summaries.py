"""Módulo 6: Resúmenes Semanales (/children/{id}/weekly-summaries)

El resumen de cada semana se calcula con los registros diarios reales del niño
(ver resumenes.py). La familia puede marcarlo como leído y dejar una reacción.
"""

from datetime import date

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from ..acceso import es_personal, exigir_alumno_visible
from ..db import get_db
from ..deps import Identidad, usuario_actual
from ..errors import ApiError
from ..horario import ahora
from ..models import WeeklySummaryFeedback
from ..resumenes import SEMANAS_HISTORIAL, lunes_de, resumen_semanal, semanas_recientes
from ..schemas import WeeklySummaryFeedbackRequest, WeeklySummaryOut

router = APIRouter(tags=["weekly-summaries"])


def _lunes_valido(summary_id: str) -> date:
    """El id de un resumen es el lunes de la semana (YYYY-MM-DD) dentro del historial disponible."""
    try:
        dia = date.fromisoformat(summary_id)
    except ValueError:
        raise ApiError(404, f"No existe el resumen semanal con id {summary_id}", "resumen_inexistente",
                       "El id es el lunes de la semana en formato YYYY-MM-DD.") from None
    lunes = lunes_de(dia)
    if lunes != dia or lunes not in semanas_recientes(SEMANAS_HISTORIAL):
        raise ApiError(404, f"No existe el resumen semanal con id {summary_id}", "resumen_inexistente",
                       f"Solo hay resúmenes de las últimas {SEMANAS_HISTORIAL} semanas.")
    return lunes


@router.get("/children/{childId}/weekly-summaries", response_model=list[WeeklySummaryOut])
def listar_resumenes_semanales(
    childId: str,
    quien: Identidad = Depends(usuario_actual),
    db: Session = Depends(get_db),
) -> list[WeeklySummaryOut]:
    """Resúmenes de la semana actual y las anteriores, del más reciente al más antiguo."""
    alumno = exigir_alumno_visible(quien, childId, db)
    familia = not es_personal(quien)
    return [
        WeeklySummaryOut(**resumen_semanal(db, alumno, lunes, quien.id, familia))
        for lunes in semanas_recientes()
    ]


@router.get("/children/{childId}/weekly-summaries/{summaryId}", response_model=WeeklySummaryOut)
def obtener_resumen_semanal(
    childId: str,
    summaryId: str,
    quien: Identidad = Depends(usuario_actual),
    db: Session = Depends(get_db),
) -> WeeklySummaryOut:
    alumno = exigir_alumno_visible(quien, childId, db)
    lunes = _lunes_valido(summaryId)
    return WeeklySummaryOut(**resumen_semanal(db, alumno, lunes, quien.id, not es_personal(quien)))


@router.put("/children/{childId}/weekly-summaries/{summaryId}/feedback", response_model=WeeklySummaryOut)
def responder_resumen_semanal(
    childId: str,
    summaryId: str,
    body: WeeklySummaryFeedbackRequest,
    quien: Identidad = Depends(usuario_actual),
    db: Session = Depends(get_db),
) -> WeeklySummaryOut:
    """La familia marca el resumen como leído/no leído y deja o quita su reacción."""
    if es_personal(quien):
        raise ApiError(403, "Solo la familia puede confirmar la lectura del resumen semanal", "rol_insuficiente")
    alumno = exigir_alumno_visible(quien, childId, db)
    lunes = _lunes_valido(summaryId)

    clave = (alumno.id, lunes.isoformat(), quien.id)
    registro = db.get(WeeklySummaryFeedback, clave)
    if registro is None:
        registro = WeeklySummaryFeedback(child_id=alumno.id, week_start=lunes.isoformat(), user_id=quien.id)
        db.add(registro)

    momento = ahora().replace(tzinfo=None)
    if body.isRead is not None:
        registro.is_read = body.isRead
        registro.read_at = momento if body.isRead else None
    if body.reaction is not None:
        registro.reaction = body.reaction.strip() or None
    registro.updated_at = momento
    db.commit()

    return WeeklySummaryOut(**resumen_semanal(db, alumno, lunes, quien.id, True))
