"""Resumen semanal e historial diario calculados con los registros reales.

Las fuentes son lo que captura el personal en la app:
- DailyLog: hoja oficial por día (ingreso, estadía y egreso).
- Activity: tarjetas de actividad publicadas en la bitácora.
- DailyPlan: planeación de la sala (actividades pedagógicas del día).

No hay datos inventados: si una semana o un día no tiene registros, el texto lo dice.
"""

import json
from datetime import date, datetime, timedelta
from typing import Any

from sqlalchemy import select
from sqlalchemy.orm import Session

from .horario import hoy
from .ia_resumenes import generar_resumen_ejecutivo_ia
from .models import Activity, Child, DailyLog, DailyPlan, WeeklyObservation, WeeklySummaryFeedback

SEMANAS_HISTORIAL = 4
DIAS_HISTORIAL = 14

MESES = [
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
]
MESES_CORTOS = ["Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"]
DIAS_CORTOS = ["Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"]

# Porcentaje aproximado de lo que comió según la opción de la hoja oficial
PORCENTAJE_COMIDA = {"NADA": 0, "POCO": 33, "TODO": 100, "PIDIO_MAS": 100}
TEXTO_COMIDA = {"NADA": "no comió", "POCO": "comió poco", "TODO": "comió todo", "PIDIO_MAS": "pidió más"}
COMIDAS = (("breakfast", "Desayuno"), ("snack", "Colación"), ("lunch", "Comida"))
TEXTO_ANIMO = {"FELIZ": "feliz", "TRISTE": "triste", "ENOJADO": "enojado"}

# Categorías de actividad tal como las guarda la app (etiqueta) -> nombre del enum en la app
CATEGORIA_POR_ETIQUETA = {
    "alimentacion": "ALIMENTACION",
    "descanso": "DESCANSO",
    "siesta": "DESCANSO",
    "funciones excretoras": "FUNCIONES_EXCRETORAS",
    "estado de animo": "ESTADO_ANIMO",
    "accidentes": "ACCIDENTES",
    "salud": "SALUD",
}
PORCION_POR_ETIQUETA = {"poco": "POCO", "medio": "MEDIO", "todo": "TODO"}


# ---------------------------------------------------------------------------
# Utilidades de fechas y textos
# ---------------------------------------------------------------------------

def a_fecha(texto: str | None) -> date | None:
    """Acepta 'YYYY-MM-DD' (registro diario) y 'dd/MM/yyyy' (bitácora de la app)."""
    if not texto:
        return None
    for formato in ("%Y-%m-%d", "%d/%m/%Y", "%d-%m-%Y"):
        try:
            return datetime.strptime(texto.strip(), formato).date()
        except ValueError:
            pass
    return None


def lunes_de(dia: date) -> date:
    return dia - timedelta(days=dia.weekday())


def texto_rango(lunes: date) -> str:
    viernes = lunes + timedelta(days=4)
    if lunes.month == viernes.month:
        return f"Semana del {lunes.day} al {viernes.day} de {MESES[lunes.month - 1]}, {viernes.year}"
    return (
        f"Semana del {lunes.day} de {MESES[lunes.month - 1]} "
        f"al {viernes.day} de {MESES[viernes.month - 1]}, {viernes.year}"
    )


def etiqueta_semana(lunes: date, lunes_actual: date) -> str:
    atras = (lunes_actual - lunes).days // 7
    if atras == 0:
        return "Semana actual"
    if atras == 1:
        return "Semana pasada"
    return f"Hace {atras} semanas"


def minutos_de(hora: str | None) -> int | None:
    """'08:15' o '08:15 AM' -> minutos desde medianoche."""
    if not hora:
        return None
    limpio = hora.strip().upper().replace(".", "")
    for formato in ("%H:%M", "%I:%M %p", "%I:%M%p"):
        try:
            t = datetime.strptime(limpio, formato)
            return t.hour * 60 + t.minute
        except ValueError:
            pass
    return None


def hora_12h(minutos: int) -> str:
    return datetime(2000, 1, 1, minutos // 60, minutos % 60).strftime("%I:%M %p")


def duracion(minutos: int) -> str:
    return f"{minutos // 60}h {minutos % 60:02d}m"


def _json(texto: str | None) -> dict[str, Any]:
    if not texto:
        return {}
    try:
        valor = json.loads(texto)
        return valor if isinstance(valor, dict) else {}
    except ValueError:
        return {}


def _plural(n: int, singular: str, plural: str) -> str:
    return f"{n} {singular if n == 1 else plural}"


# ---------------------------------------------------------------------------
# Resumen semanal
# ---------------------------------------------------------------------------

def dias_escolares_transcurridos(lunes: date, hoy_: date) -> int:
    """Días de lunes a viernes ya transcurridos en la semana (5 si la semana ya terminó)."""
    if hoy_ >= lunes + timedelta(days=4):
        return 5
    if hoy_ < lunes:
        return 0
    return (hoy_ - lunes).days + 1


def _feedback(db: Session, child_id: str, semana: str, quien_id: str, es_familia: bool) -> dict[str, Any]:
    """Lectura y reacción. La familia ve la suya; el personal ve la más reciente de cualquier familiar."""
    consulta = select(WeeklySummaryFeedback).where(
        WeeklySummaryFeedback.child_id == child_id,
        WeeklySummaryFeedback.week_start == semana,
    )
    if es_familia:
        consulta = consulta.where(WeeklySummaryFeedback.user_id == quien_id)
    registros = list(db.scalars(consulta).all())
    leidos = [r for r in registros if r.is_read and r.read_at is not None]
    leidos.sort(key=lambda r: r.read_at, reverse=True)
    con_reaccion = [r for r in registros if r.reaction]
    con_reaccion.sort(key=lambda r: r.updated_at or datetime.min, reverse=True)
    return {
        "isReadByParent": bool(leidos),
        "readTimestampText": leidos[0].read_at.strftime("%d/%m %I:%M %p") if leidos else None,
        "parentReaction": con_reaccion[0].reaction if con_reaccion else None,
    }


def resumen_semanal(
    db: Session, alumno: Child, lunes: date, quien_id: str, es_familia: bool
) -> dict[str, Any]:
    hoy_ = hoy()
    lunes_actual = lunes_de(hoy_)
    viernes = lunes + timedelta(days=4)
    dias_semana = [(lunes + timedelta(days=i)).isoformat() for i in range(5)]
    dias_semana_mex = [(lunes + timedelta(days=i)).strftime("%d/%m/%Y") for i in range(5)]
    dias_semana_todos = set(dias_semana) | set(dias_semana_mex)

    registros = {
        r.date: r
        for r in db.scalars(
            select(DailyLog).where(DailyLog.child_id == alumno.id, DailyLog.date.in_(dias_semana))
        ).all()
    }
    ingresos = {d: _json(r.ingreso) for d, r in registros.items() if r.ingreso}
    estadias = {d: _json(r.estadia) for d, r in registros.items() if r.estadia}

    # Asistencia y hora de llegada
    transcurridos = dias_escolares_transcurridos(lunes, hoy_)
    asistio = len(ingresos)
    porcentaje_asistencia = f"{round(asistio * 100 / transcurridos)}%" if transcurridos else "—"
    llegadas = [m for m in (minutos_de(i.get("time")) for i in ingresos.values()) if m is not None]
    llegada_promedio = hora_12h(round(sum(llegadas) / len(llegadas))) if llegadas else "—"

    # Alimentación
    porcentajes: list[int] = []
    resumen_comidas: list[str] = []
    for campo, nombre in COMIDAS:
        valores = [e.get(campo) for e in estadias.values() if e.get(campo) in PORCENTAJE_COMIDA]
        porcentajes.extend(PORCENTAJE_COMIDA[v] for v in valores)
        if valores:
            completos = sum(1 for v in valores if v in ("TODO", "PIDIO_MAS"))
            resumen_comidas.append(f"{nombre}: comió completo {completos} de {_plural(len(valores), 'día', 'días')}")
    notas_comida = [e.get("foodNotes", "").strip() for e in estadias.values() if e.get("foodNotes", "").strip()]
    destacados_comida = resumen_comidas + notas_comida[:2]
    porcentaje_comida = f"{round(sum(porcentajes) / len(porcentajes))}%" if porcentajes else "—"

    # Siesta
    siestas = [
        int(e["napMinutes"])
        for e in estadias.values()
        if isinstance(e.get("napMinutes"), int) and e.get("slept") is not False and e["napMinutes"] > 0
    ]
    total_siesta = sum(siestas)
    if siestas:
        promedio_siesta = round(total_siesta / len(siestas))
        texto_siesta = (
            f"Durmió siesta {_plural(len(siestas), 'día', 'días')}, "
            f"con un promedio de {duracion(promedio_siesta)} por día."
        )
        siesta_diaria = f"{duracion(promedio_siesta)}/día"
    else:
        texto_siesta = "No hay registros de siesta esta semana."
        siesta_diaria = "—"

    # Estado de ánimo
    animos = [e.get("mood") for e in estadias.values() if e.get("mood") in TEXTO_ANIMO]
    participo = sum(1 for e in estadias.values() if e.get("participated") is True)
    if animos:
        predominante = max(TEXTO_ANIMO, key=animos.count)
        animo = f"Mayormente {TEXTO_ANIMO[predominante]}"
        if participo:
            animo += f", participó {_plural(participo, 'día', 'días')}"
    else:
        animo = "Sin registros de ánimo"

    # Salud y seguridad
    incidentes: list[str] = []
    for d in sorted(set(ingresos) | set(estadias)):
        etiqueta = DIAS_CORTOS[date.fromisoformat(d).weekday()]
        ing = ingresos.get(d, {})
        est = estadias.get(d, {})
        if ing.get("isSick"):
            incidentes.append(f"{etiqueta}: llegó con malestar")
        if ing.get("hasSymptoms") and ing.get("symptoms"):
            incidentes.append(f"{etiqueta}: síntomas al llegar ({ing['symptoms']})")
        if est.get("hadAccident"):
            detalle = est.get("accidentDescription") or "sin descripción"
            incidentes.append(f"{etiqueta}: accidente ({detalle})")
        if est.get("hadHealthIssue"):
            detalle = est.get("healthIssue") or "sin detalle"
            incidentes.append(f"{etiqueta}: atención de salud ({detalle})")
    if incidentes:
        salud = "; ".join(incidentes) + "."
    elif registros:
        salud = "Sin accidentes ni incidentes de salud registrados."
    else:
        salud = "Sin registros esta semana."

    # Actividades pedagógicas de la planeación de su sala
    planes = db.scalars(
        select(DailyPlan).where(DailyPlan.room_id == alumno.room_id, DailyPlan.date.in_(dias_semana))
    ).all()
    pedagogicas: list[str] = []
    for plan in sorted(planes, key=lambda p: p.date):
        try:
            actividades = json.loads(plan.activities or "[]")
        except ValueError:
            actividades = []
        for a in actividades:
            titulo = str(a.get("title", "")).strip() if isinstance(a, dict) else ""
            if titulo and titulo not in pedagogicas:
                pedagogicas.append(titulo)
    pedagogicas = pedagogicas[:6]

    # Resumen ejecutivo en una o dos frases
    nombre = alumno.short_name or alumno.full_name
    if not registros:
        if transcurridos == 0:
            ejecutivo = f"La semana de {nombre} aún no comienza."
        else:
            ejecutivo = f"Aún no hay registros diarios de {nombre} en esta semana."
    else:
        partes = [f"{nombre} asistió {asistio} de {_plural(transcurridos, 'día', 'días')}"]
        if porcentajes:
            partes.append(f"comió en promedio el {porcentaje_comida} de sus alimentos")
        if siestas:
            partes.append(f"durmió {duracion(total_siesta)} de siesta en total")
        ejecutivo = ", ".join(partes[:-1]) + (" y " if len(partes) > 1 else "") + partes[-1] + "."
        if animos:
            ejecutivo += f" Su estado de ánimo fue {animo.lower()}."

        # Síntesis opcional con IA integrando todos los datos disponibles
        observaciones_objs = db.scalars(
            select(WeeklyObservation).where(
                WeeklyObservation.child_id == alumno.id,
                WeeklyObservation.week_key.in_([
                    texto_rango(lunes),
                    etiqueta_semana(lunes, lunes_actual),
                    lunes.isoformat(),
                ]),
            )
        ).all()
        textos_obs = [f"{o.author_role.capitalize()}: {o.text}" for o in observaciones_objs]

        actividades_bitacora = [
            f"{a.title}: {a.description}".strip(" :")
            for a in db.scalars(
                select(Activity).where(Activity.child_id == alumno.id, Activity.date.in_(dias_semana_todos))
            ).all()
            if (a.title or a.description)
        ][:5]

        ejecutivo_ia = generar_resumen_ejecutivo_ia({
            "child_id": alumno.id,
            "semana_id": lunes.isoformat(),
            "nombre": nombre,
            "rango_fechas": texto_rango(lunes),
            "asistencia": f"{asistio} de {_plural(transcurridos, 'día', 'días')}",
            "llegada_promedio": llegada_promedio,
            "alimentacion": f"{porcentaje_comida} (destacados: {'; '.join(destacados_comida)})" if destacados_comida else porcentaje_comida,
            "notas_comida": notas_comida,
            "siesta": texto_siesta,
            "animo": animo,
            "salud": salud,
            "actividades_plan": pedagogicas,
            "actividades_bitacora": actividades_bitacora,
            "observaciones": textos_obs,
        })
        if ejecutivo_ia:
            ejecutivo = ejecutivo_ia

    return {
        "id": lunes.isoformat(),
        "weekLabel": etiqueta_semana(lunes, lunes_actual),
        "weekRangeText": texto_rango(lunes),
        "attendanceDaysCount": asistio,
        "totalSchoolDays": transcurridos,
        "attendancePercentageText": porcentaje_asistencia,
        "averageArrivalTime": llegada_promedio,
        "foodIntakePercentage": porcentaje_comida,
        "totalNapHours": duracion(total_siesta) if siestas else "—",
        "averageNapDaily": siesta_diaria,
        "moodSummary": animo,
        "executiveSummary": ejecutivo,
        "foodHighlights": destacados_comida,
        "napHighlights": texto_siesta,
        "pedagogicalHighlights": pedagogicas,
        "healthAndSafetySummary": salud,
        "weekEnd": viernes.isoformat(),
        **_feedback(db, alumno.id, lunes.isoformat(), quien_id, es_familia),
    }


def semanas_recientes(semanas: int = SEMANAS_HISTORIAL) -> list[date]:
    """Lunes de la semana actual y de las anteriores, de la más reciente a la más antigua."""
    actual = lunes_de(hoy())
    return [actual - timedelta(weeks=i) for i in range(semanas)]


# ---------------------------------------------------------------------------
# Historial diario (pestaña "Bitácoras" de la ficha)
# ---------------------------------------------------------------------------

def _categoria(etiqueta: str | None) -> str:
    clave = (etiqueta or "").strip().lower()
    for a, b in (("á", "a"), ("é", "e"), ("í", "i"), ("ó", "o"), ("ú", "u")):
        clave = clave.replace(a, b)
    return CATEGORIA_POR_ETIQUETA.get(clave, "OTROS")


def _porcion(etiqueta: str | None) -> str | None:
    return PORCION_POR_ETIQUETA.get((etiqueta or "").strip().lower())


def _estado_fisico(datos: dict[str, Any]) -> str:
    if datos.get("goodPhysicalCondition") is False:
        lesiones = (datos.get("injuries") or "").strip()
        return f"Con observaciones: {lesiones}" if lesiones else "Con observaciones en su estado físico."
    if datos.get("goodPhysicalCondition") is True:
        return "Buen estado general."
    return "Sin registrar."


def _actividades_de_estadia(fecha: str, est: dict[str, Any]) -> list[dict[str, Any]]:
    """Convierte la estadía de la hoja oficial en renglones del historial."""
    filas: list[dict[str, Any]] = []
    for campo, nombre in COMIDAS:
        valor = est.get(campo)
        if valor in TEXTO_COMIDA:
            porcion = {"NADA": None, "POCO": "POCO", "TODO": "TODO", "PIDIO_MAS": "TODO"}[valor]
            filas.append({
                "id": f"log-{fecha}-{campo}",
                "time": "",
                "title": nombre,
                "category": "ALIMENTACION",
                "portion": porcion,
                "description": TEXTO_COMIDA[valor].capitalize() + ".",
            })
    if est.get("slept") is True or (isinstance(est.get("napMinutes"), int) and est["napMinutes"] > 0):
        minutos = est.get("napMinutes")
        detalle = f"Durmió {duracion(minutos)}." if isinstance(minutos, int) and minutos > 0 else "Durmió siesta."
        if est.get("napComments"):
            detalle += f" {est['napComments']}"
        filas.append({"id": f"log-{fecha}-siesta", "time": "", "title": "Siesta",
                      "category": "DESCANSO", "portion": None, "description": detalle})
    elif est.get("slept") is False:
        filas.append({"id": f"log-{fecha}-siesta", "time": "", "title": "Siesta",
                      "category": "DESCANSO", "portion": None, "description": "No durmió siesta."})
    if est.get("mood") in TEXTO_ANIMO:
        detalle = f"Estuvo {TEXTO_ANIMO[est['mood']]}."
        if est.get("participated") is True:
            detalle += " Participó en las actividades."
        if est.get("moodNotes"):
            detalle += f" {est['moodNotes']}"
        filas.append({"id": f"log-{fecha}-animo", "time": "", "title": "Estado de ánimo",
                      "category": "ESTADO_ANIMO", "portion": None, "description": detalle})
    if est.get("hadAccident"):
        filas.append({"id": f"log-{fecha}-accidente", "time": "", "title": "Accidente",
                      "category": "ACCIDENTES", "portion": None,
                      "description": est.get("accidentDescription") or "Accidente registrado."})
    if est.get("hadHealthIssue"):
        detalle = est.get("healthIssue") or "Situación de salud registrada."
        if est.get("careProvided"):
            detalle += f" Atención: {est['careProvided']}"
        filas.append({"id": f"log-{fecha}-salud", "time": "", "title": "Salud",
                      "category": "SALUD", "portion": None, "description": detalle})
    return filas


def historial_diario(db: Session, child_id: str, dias: int = DIAS_HISTORIAL) -> list[dict[str, Any]]:
    hoy_ = hoy()
    desde = hoy_ - timedelta(days=dias - 1)
    registros = {
        r.date: r
        for r in db.scalars(
            select(DailyLog).where(
                DailyLog.child_id == child_id,
                DailyLog.date >= desde.isoformat(),
                DailyLog.date <= hoy_.isoformat(),
            )
        ).all()
    }
    actividades_por_dia: dict[str, list[Activity]] = {}
    for a in db.scalars(select(Activity).where(Activity.child_id == child_id)).all():
        dia = a_fecha(a.date)
        if dia and desde <= dia <= hoy_:
            actividades_por_dia.setdefault(dia.isoformat(), []).append(a)

    salida: list[dict[str, Any]] = []
    for fecha in sorted(set(registros) | set(actividades_por_dia), reverse=True):
        dia = date.fromisoformat(fecha)
        r = registros.get(fecha)
        ing = _json(r.ingreso) if r else {}
        est = _json(r.estadia) if r else {}
        egr = _json(r.egreso) if r else {}

        ingreso = None
        if ing:
            minutos = minutos_de(ing.get("time"))
            ingreso = {
                "time": hora_12h(minutos) if minutos is not None else ing.get("time", ""),
                "delivererName": ing.get("deliveredBy") or "Sin registrar",
                "receiverName": ing.get("receivedBy") or ing.get("assistantName") or "Sin registrar",
                "physicalCondition": _estado_fisico(ing),
                "hasSymptoms": bool(ing.get("hasSymptoms")),
                "symptomsDetail": ing.get("symptoms", ""),
                "observations": ing.get("observations", ""),
            }
        egreso = None
        if egr:
            minutos = minutos_de(egr.get("time"))
            egreso = {
                "time": hora_12h(minutos) if minutos is not None else egr.get("time", ""),
                "collectorName": egr.get("collectedBy") or "Sin registrar",
                "delivererName": egr.get("deliveredByStaff") or "Sin registrar",
                "physicalCondition": _estado_fisico(egr),
                "observations": egr.get("observations", ""),
            }

        tarjetas = sorted(
            actividades_por_dia.get(fecha, []),
            key=lambda a: minutos_de(a.time) if minutos_de(a.time) is not None else 9999,
        )
        filas = [
            {
                "id": a.id,
                "time": a.time,
                "title": a.title,
                "category": _categoria(a.category),
                "portion": _porcion(a.portion_option),
                "description": a.description,
            }
            for a in tarjetas
        ] + _actividades_de_estadia(fecha, est)

        if dia == hoy_:
            etiqueta = f"Hoy, {dia.day} {MESES_CORTOS[dia.month - 1]}"
        elif dia == hoy_ - timedelta(days=1):
            etiqueta = f"Ayer, {dia.day} {MESES_CORTOS[dia.month - 1]}"
        else:
            etiqueta = f"{DIAS_CORTOS[dia.weekday()]} {dia.day} {MESES_CORTOS[dia.month - 1]}"

        salida.append({
            "date": dia.strftime("%d/%m/%Y"),
            "dayLabel": etiqueta,
            "isToday": dia == hoy_,
            "ingreso": ingreso,
            "egreso": egreso,
            "activities": filas,
        })
    return salida
