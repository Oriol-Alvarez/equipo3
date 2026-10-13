"""Módulo 6: Resúmenes Semanales Pedagógicos (/children/{id}/weekly-summaries)"""

import json

from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.orm import Session

from ..db import get_db
from ..deps import Identidad, usuario_actual
from ..errors import ApiError
from ..models import WeeklySummary
from ..schemas import WeeklySummaryOut

router = APIRouter(tags=["weekly-summaries"])


@router.get("/children/{childId}/weekly-summaries", response_model=list[WeeklySummaryOut])
def listar_resumenes_semanales(
    childId: str,
    _: Identidad = Depends(usuario_actual),
    db: Session = Depends(get_db),
) -> list[WeeklySummaryOut]:
    """Lista todos los resúmenes semanales registrados para el menor."""
    resumenes = db.scalars(select(WeeklySummary).where(WeeklySummary.child_id == childId)).all()
    if not resumenes:
        # Devolver el resumen pedagógico estándar de muestra si aún no hay registros
        return [
            WeeklySummaryOut(
                id="week_2026_w39",
                weekLabel="Semana actual",
                weekRangeText="Semana del 22 al 26 de Septiembre, 2026",
                attendanceDaysCount=5,
                totalSchoolDays=5,
                attendancePercentageText="100%",
                averageArrivalTime="08:18 AM",
                foodIntakePercentage="92%",
                totalNapHours="6h 15m",
                averageNapDaily="1h 15m/día",
                moodSummary="Muy participativo y sociable",
                executiveSummary="Semana sumamente positiva para Mateo...",
                foodHighlights=["Gran aceptación en cremas de verduras", "Excelente autonomía con los cubiertos"],
                napHighlights="Ritmo biológico de descanso muy estable.",
                pedagogicalHighlights=["Motricidad fina: Modelado con plastilina", "Sensorial: Taller de pintura con dedos"],
                healthAndSafetySummary="Protocolo estricto de alergias cumplido al 100%.",
            )
        ]

    resultado = []
    for r in resumenes:
        food = json.loads(r.food_highlights) if r.food_highlights.startswith("[") else []
        pedag = json.loads(r.pedagogical_highlights) if r.pedagogical_highlights.startswith("[") else []
        resultado.append(
            WeeklySummaryOut(
                id=r.id,
                weekLabel=r.week_label,
                weekRangeText=r.week_range_text,
                attendanceDaysCount=r.attendance_days_count,
                totalSchoolDays=r.total_school_days,
                attendancePercentageText=r.attendance_percentage_text,
                averageArrivalTime=r.average_arrival_time,
                foodIntakePercentage=r.food_intake_percentage,
                totalNapHours=r.total_nap_hours,
                averageNapDaily=r.average_nap_daily,
                moodSummary=r.mood_summary,
                executiveSummary=r.executive_summary,
                foodHighlights=food,
                napHighlights=r.nap_highlights,
                pedagogicalHighlights=pedag,
                healthAndSafetySummary=r.health_and_safety_summary,
            )
        )
    return resultado


@router.get("/children/{childId}/weekly-summaries/{summaryId}", response_model=WeeklySummaryOut)
def obtener_resumen_semanal(
    childId: str,
    summaryId: str,
    _: Identidad = Depends(usuario_actual),
    db: Session = Depends(get_db),
) -> WeeklySummaryOut:
    """Obtiene el detalle completo de un informe semanal pedagógico."""
    resumen = db.get(WeeklySummary, summaryId)
    if resumen is None:
        raise ApiError(404, f"No existe el resumen semanal con id {summaryId}")

    food = json.loads(resumen.food_highlights) if resumen.food_highlights.startswith("[") else []
    pedag = json.loads(resumen.pedagogical_highlights) if resumen.pedagogical_highlights.startswith("[") else []
    return WeeklySummaryOut(
        id=resumen.id,
        weekLabel=resumen.week_label,
        weekRangeText=resumen.week_range_text,
        attendanceDaysCount=resumen.attendance_days_count,
        totalSchoolDays=resumen.total_school_days,
        attendancePercentageText=resumen.attendance_percentage_text,
        averageArrivalTime=resumen.average_arrival_time,
        foodIntakePercentage=resumen.food_intake_percentage,
        totalNapHours=resumen.total_nap_hours,
        averageNapDaily=resumen.average_nap_daily,
        moodSummary=resumen.mood_summary,
        executiveSummary=resumen.executive_summary,
        foodHighlights=food,
        napHighlights=resumen.nap_highlights,
        pedagogicalHighlights=pedag,
        healthAndSafetySummary=resumen.health_and_safety_summary,
    )
