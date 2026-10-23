"""RF-19: reporte mensual del niño (/children/{id}/monthly-reports).

Se calcula con los mismos registros reales que el resumen semanal. Si hay llave de Gemini u
OpenAI en el .env, el texto lo redacta la IA (sin mandarle el nombre del niño); si no, se usa un
texto armado con los mismos datos. Lo ve quien puede ver al niño: su familia y el personal.
"""

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from ..acceso import exigir_alumno_visible
from ..db import get_db
from ..deps import Identidad, usuario_actual
from ..errors import ApiError
from ..resumenes import MESES_REPORTE, mes_de_texto, meses_recientes, reporte_mensual
from ..schemas import MonthlyReportOut

router = APIRouter(tags=["monthly-reports"])


@router.get("/children/{childId}/monthly-reports", response_model=list[str])
def meses_disponibles(
    childId: str,
    quien: Identidad = Depends(usuario_actual),
    db: Session = Depends(get_db),
) -> list[str]:
    """Meses que se pueden consultar (YYYY-MM), del más reciente al más antiguo."""
    exigir_alumno_visible(quien, childId, db)
    return meses_recientes(3)


@router.get("/children/{childId}/monthly-reports/{month}", response_model=MonthlyReportOut)
def obtener_reporte_mensual(
    childId: str,
    month: str,
    quien: Identidad = Depends(usuario_actual),
    db: Session = Depends(get_db),
) -> MonthlyReportOut:
    alumno = exigir_alumno_visible(quien, childId, db)
    mes = mes_de_texto(month)
    if mes is None:
        raise ApiError(
            404,
            f"No hay reporte mensual para {month}",
            "reporte_inexistente",
            f"El mes va en formato YYYY-MM y puede ser el actual o uno de los últimos {MESES_REPORTE}.",
        )
    return MonthlyReportOut(**reporte_mensual(db, alumno, *mes))
