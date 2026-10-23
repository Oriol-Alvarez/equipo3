"""Resumen semanal e historial diario calculados con los registros reales.

Las fuentes son lo que captura el personal en la app:
- DailyLog: hoja oficial por día (ingreso, estadía y egreso).
- Activity: tarjetas de actividad publicadas en la bitácora.
- DailyPlan: planeación de la sala (actividades pedagógicas del día).

No hay datos inventados: si una semana o un día no tiene registros, el texto lo dice.
"""

import calendar
import json
from datetime import date, datetime, timedelta
from typing import Any

from sqlalchemy import select
from sqlalchemy.orm import Session

from .horario import hoy
from .ia_resumenes import generar_reporte_mensual_ia, generar_resumen_ejecutivo_ia
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
PORCENTAJE_COMIDA = {
    "NADA": 0,
    "POCO": 33,
    "MEDIO": 50,
    "TODO": 100,
    "PIDIO_MAS": 100,
}
TEXTO_COMIDA = {
    "NADA": "no comió",
    "POCO": "comió poco",
    "MEDIO": "comió medio plato",
    "TODO": "comió todo",
    "PIDIO_MAS": "pidió más",
}
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


def normalizar_opcion_comida(texto: str | None) -> str | None:
    """Normaliza cualquier texto de consumo/porción a una clave de PORCENTAJE_COMIDA."""
    if not texto:
        return None
    t = texto.strip().upper().replace("Ó", "O").replace("Í", "I").replace("Á", "A").replace("É", "E")
    if "PIDIO" in t or "MAS" in t:
        return "PIDIO_MAS"
    if "TODO" in t or "COMPLET" in t:
        return "TODO"
    if "MEDIO" in t or "1/2" in t:
        return "MEDIO"
    if "POCO" in t or "1/3" in t:
        return "POCO"
    if "NADA" in t or "NO COMIO" in t:
        return "NADA"
    return t if t in PORCENTAJE_COMIDA else None


def normalizar_tipo_comida(texto: str | None) -> str:
    """Mapea a 'breakfast', 'snack' o 'lunch'."""
    if not texto:
        return "lunch"
    t = texto.strip().upper().replace("Ó", "O").replace("Í", "I").replace("Á", "A").replace("É", "E")
    if "DESAYUNO" in t or "BREAKFAST" in t:
        return "breakfast"
    if "COLACION" in t or "SNACK" in t or "MERIENDA" in t:
        return "snack"
    return "lunch"


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


def _registros_del_periodo(db: Session, alumno: Child, dias: list[date]):
    """Hoja oficial y tarjetas de la bitácora de los días indicados, ya combinadas por día.

    Devuelve (registros, ingresos, estadias, dias_con_actividad, actividades).
    """
    dias_iso = [d.isoformat() for d in dias]
    validos = set(dias_iso)
    dias_todos = validos | {d.strftime("%d/%m/%Y") for d in dias}

    registros = {
        r.date: r
        for r in db.scalars(
            select(DailyLog).where(DailyLog.child_id == alumno.id, DailyLog.date.in_(dias_iso))
        ).all()
    }
    ingresos = {d: _json(r.ingreso) for d, r in registros.items() if r.ingreso}
    estadias = {d: _json(r.estadia) for d, r in registros.items() if r.estadia}

    # Tarjetas de actividades de la bitácora para el mismo periodo
    actividades_periodo = db.scalars(
        select(Activity).where(Activity.child_id == alumno.id, Activity.date.in_(dias_todos))
    ).all()

    # Días con actividades registradas: el niño estuvo presente aunque no se haya pasado asistencia
    dias_con_actividad: set[str] = set()

    # Integrar actividades de la bitácora en la asistencia y estadías
    for a in actividades_periodo:
        dia_obj = a_fecha(a.date)
        if not dia_obj:
            continue
        dia_iso = dia_obj.isoformat()
        if dia_iso not in validos:
            continue

        # Si el día no tenía ingreso registrado pero hubo actividades, se considera asistido
        # Cuenta como asistido, pero la hora de la actividad no es una hora de llegada
        dias_con_actividad.add(dia_iso)

        cat_upper = (a.category or "").upper().replace("Á", "A").replace("É", "E").replace("Í", "I").replace("Ó", "O").replace("Ú", "U")
        if dia_iso not in estadias:
            estadias[dia_iso] = {}

        if "ALIMENTACION" in cat_upper:
            opt = normalizar_opcion_comida(a.consumption_option or a.portion_option or a.description or a.title)
            tipo = normalizar_tipo_comida(a.meal_type or a.title or a.description)
            if opt:
                estadias[dia_iso][tipo] = opt
            if a.description:
                prev_notas = estadias[dia_iso].get("foodNotes", "")
                if a.description not in prev_notas:
                    estadias[dia_iso]["foodNotes"] = f"{prev_notas}; {a.description}".strip("; ")

        elif "DESCANSO" in cat_upper or "SIESTA" in cat_upper:
            mins = a.nap_minutes
            if not mins and a.description:
                import re
                match = re.search(r"(\d+)\s*minuto", a.description)
                if match:
                    mins = int(match.group(1))
            if mins and mins > 0:
                estadias[dia_iso]["slept"] = True
                estadias[dia_iso]["napMinutes"] = mins
            if a.description:
                prev_comm = estadias[dia_iso].get("napComments", "")
                if a.description not in prev_comm:
                    estadias[dia_iso]["napComments"] = f"{prev_comm}; {a.description}".strip("; ")

        elif "ANIMO" in cat_upper:
            mood = (a.mood_state or "").upper()
            if not mood and a.description:
                for m in ("FELIZ", "TRISTE", "ENOJADO"):
                    if m in a.description.upper():
                        mood = m
                        break
            if not mood:
                texto = f"{a.portion_option or ''} {a.title or ''}".upper()
                for m in ("FELIZ", "TRISTE", "ENOJADO"):
                    if m in texto:
                        mood = m
                        break
            if mood:  # si no se reconoce, no se inventa
                estadias[dia_iso]["mood"] = mood
            estadias[dia_iso]["participated"] = True

        elif "ACCIDENTE" in cat_upper:
            estadias[dia_iso]["hadAccident"] = True
            if a.description:
                estadias[dia_iso]["accidentDescription"] = a.description

        elif "SALUD" in cat_upper:
            estadias[dia_iso]["hadHealthIssue"] = True
            if a.description:
                estadias[dia_iso]["healthIssue"] = a.description

    return registros, ingresos, estadias, dias_con_actividad, actividades_periodo


def resumen_semanal(
    db: Session, alumno: Child, lunes: date, quien_id: str, es_familia: bool
) -> dict[str, Any]:
    hoy_ = hoy()
    lunes_actual = lunes_de(hoy_)
    viernes = lunes + timedelta(days=4)
    dias_semana = [(lunes + timedelta(days=i)).isoformat() for i in range(5)]
    dias_semana_mex = [(lunes + timedelta(days=i)).strftime("%d/%m/%Y") for i in range(5)]
    dias_semana_todos = set(dias_semana) | set(dias_semana_mex)

    registros, ingresos, estadias, dias_con_actividad, actividades_semana = _registros_del_periodo(
        db, alumno, [lunes + timedelta(days=i) for i in range(5)]
    )

    # Asistencia y hora de llegada
    transcurridos = dias_escolares_transcurridos(lunes, hoy_)
    asistio = len(set(ingresos) | dias_con_actividad)
    porcentaje_asistencia = f"{round(asistio * 100 / transcurridos)}%" if transcurridos else "—"
    llegadas = [m for m in (minutos_de(i.get("time")) for i in ingresos.values()) if m is not None]
    llegada_promedio = hora_12h(round(sum(llegadas) / len(llegadas))) if llegadas else "—"

    # Alimentación
    porcentajes: list[int] = []
    resumen_comidas: list[str] = []
    for campo, nombre in COMIDAS:
        valores = [
            normalizar_opcion_comida(e.get(campo))
            for e in estadias.values()
            if normalizar_opcion_comida(e.get(campo)) in PORCENTAJE_COMIDA
        ]
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

    # Estado de ánimo (conciso para que quepa en 1 sola línea de la tarjeta)
    animos = [e.get("mood") for e in estadias.values() if e.get("mood") in TEXTO_ANIMO]
    participo = sum(1 for e in estadias.values() if e.get("participated") is True)
    if animos:
        predominante = max(TEXTO_ANIMO, key=animos.count)
        animo = f"Mayormente {TEXTO_ANIMO[predominante]}"
    else:
        animo = "Sin registros"

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
    tiene_registros = bool(registros or estadias or ingresos or actividades_semana)
    if not tiene_registros:
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
            ejecutivo += f" Su estado de ánimo fue {animo.lower()}" + (f", participando {_plural(participo, 'día', 'días')}." if participo else ".")

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

        actividades_bitacora: list[str] = []
        for a in db.scalars(
            select(Activity).where(Activity.child_id == alumno.id, Activity.date.in_(dias_semana_todos))
        ).all():
            partes_a = []
            if a.title:
                partes_a.append(a.title)
            if a.description:
                desc = a.description.strip()
                if desc and desc.lower() != (a.title or "").strip().lower():
                    partes_a.append(f'- Comentario: "{desc}"')
            if partes_a:
                actividades_bitacora.append(" ".join(partes_a))
        actividades_bitacora = actividades_bitacora[:8]

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


# ---------------------------------------------------------------------------
# Reporte mensual (RF-19)
# ---------------------------------------------------------------------------

MESES_REPORTE = 12  # cuántos meses hacia atrás se pueden consultar


def mes_de_texto(texto: str) -> tuple[int, int] | None:
    """'2026-10' -> (2026, 10) si es el mes actual o uno de los últimos MESES_REPORTE."""
    try:
        anio_txt, mes_txt = texto.strip().split("-")
        anio, mes = int(anio_txt), int(mes_txt)
        date(anio, mes, 1)
    except (ValueError, AttributeError):
        return None
    hoy_ = hoy()
    atras = (hoy_.year - anio) * 12 + (hoy_.month - mes)
    if atras < 0 or atras >= MESES_REPORTE:
        return None
    return anio, mes


def meses_recientes(cuantos: int = 3) -> list[str]:
    """Mes actual y anteriores en formato YYYY-MM, del más reciente al más antiguo."""
    hoy_ = hoy()
    anio, mes = hoy_.year, hoy_.month
    salida: list[str] = []
    for _ in range(cuantos):
        salida.append(f"{anio:04d}-{mes:02d}")
        mes -= 1
        if mes == 0:
            anio, mes = anio - 1, 12
    return salida


def reporte_mensual(db: Session, alumno: Child, anio: int, mes: int) -> dict[str, Any]:
    """Reporte del mes con los registros reales: asistencia, comida, siesta, comportamiento,
    salud, actividades y observaciones, más los puntos que conviene que la familia atienda."""
    hoy_ = hoy()
    primero = date(anio, mes, 1)
    ultimo = date(anio, mes, calendar.monthrange(anio, mes)[1])
    es_actual = primero <= hoy_ <= ultimo
    habiles = [
        primero + timedelta(days=i)
        for i in range((ultimo - primero).days + 1)
        if (primero + timedelta(days=i)).weekday() < 5
    ]
    transcurridos = [d for d in habiles if d <= hoy_]
    registros, ingresos, estadias, dias_con_actividad, actividades = _registros_del_periodo(db, alumno, habiles)
    nombre = alumno.short_name or alumno.full_name
    nombre_mes = MESES[mes - 1]

    # Asistencia
    asistio = len(set(ingresos) | dias_con_actividad)
    n_habiles = len(transcurridos)
    porcentaje_asistencia = f"{round(asistio * 100 / n_habiles)}%" if n_habiles else "—"
    llegadas = [m for m in (minutos_de(i.get("time")) for i in ingresos.values()) if m is not None]
    llegada_promedio = hora_12h(round(sum(llegadas) / len(llegadas))) if llegadas else "—"

    # Alimentación
    porcentajes: list[int] = []
    dias_comio_poco: list[str] = []
    for d, e in sorted(estadias.items()):
        valores = [normalizar_opcion_comida(e.get(campo)) for campo, _ in COMIDAS]
        valores = [v for v in valores if v in PORCENTAJE_COMIDA]
        porcentajes.extend(PORCENTAJE_COMIDA[v] for v in valores)
        if valores and all(v in ("NADA", "POCO") for v in valores):
            dias_comio_poco.append(d)
    porcentaje_comida = f"{round(sum(porcentajes) / len(porcentajes))}%" if porcentajes else "—"

    # Siesta
    siestas = [
        int(e["napMinutes"])
        for e in estadias.values()
        if isinstance(e.get("napMinutes"), int) and e["napMinutes"] > 0 and e.get("slept") is not False
    ]
    siesta_diaria = f"{duracion(round(sum(siestas) / len(siestas)))}/día" if siestas else "—"

    # Comportamiento (estado de ánimo y participación)
    animos = [e.get("mood") for e in estadias.values() if e.get("mood") in TEXTO_ANIMO]
    conteo = {m: animos.count(m) for m in TEXTO_ANIMO}
    participo = sum(1 for e in estadias.values() if e.get("participated") is True)
    comportamiento: list[str] = []
    if animos:
        predominante = max(TEXTO_ANIMO, key=lambda m: conteo[m])
        animo = f"Mayormente {TEXTO_ANIMO[predominante]}"
        comportamiento = [
            f"{TEXTO_ANIMO[m].capitalize()}: {_plural(c, 'día', 'días')}" for m, c in conteo.items() if c
        ]
    else:
        animo = "Sin registros"
    if participo:
        comportamiento.append(f"Participó en las actividades {_plural(participo, 'día', 'días')}")
    for e in estadias.values():
        nota = e.get("moodNotes")
        if isinstance(nota, str) and nota.strip() and len(comportamiento) < 8:
            comportamiento.append(nota.strip())

    # Salud y seguridad, con la fecha de cada evento
    salud: list[str] = []
    for d in sorted(set(ingresos) | set(estadias)):
        etiqueta = date.fromisoformat(d).strftime("%d/%m")
        ing = ingresos.get(d, {})
        est = estadias.get(d, {})
        if ing.get("isSick"):
            salud.append(f"{etiqueta}: llegó con malestar")
        if ing.get("hasSymptoms") and ing.get("symptoms"):
            salud.append(f"{etiqueta}: síntomas al llegar ({ing['symptoms']})")
        if est.get("hadAccident"):
            salud.append(f"{etiqueta}: accidente ({est.get('accidentDescription') or 'sin descripción'})")
        if est.get("hadHealthIssue"):
            salud.append(f"{etiqueta}: atención de salud ({est.get('healthIssue') or 'sin detalle'})")

    # Actividades pedagógicas de la planeación de su sala
    planes = db.scalars(
        select(DailyPlan).where(
            DailyPlan.room_id == alumno.room_id, DailyPlan.date.in_([d.isoformat() for d in habiles])
        )
    ).all()
    pedagogicas: list[str] = []
    for plan in sorted(planes, key=lambda p: p.date):
        try:
            lista = json.loads(plan.activities or "[]")
        except ValueError:
            lista = []
        for a in lista:
            titulo = str(a.get("title", "")).strip() if isinstance(a, dict) else ""
            if titulo and titulo not in pedagogicas:
                pedagogicas.append(titulo)
    pedagogicas = pedagogicas[:8]

    # Lo que escribió la educadora en las tarjetas de la bitácora
    comentarios: list[str] = []
    for a in sorted(actividades, key=lambda x: (a_fecha(x.date) or primero, minutos_de(x.time) or 0)):
        desc = (a.description or "").strip()
        if desc and desc.lower() != (a.title or "").strip().lower():
            comentarios.append(f"{a.title}: {desc}" if a.title else desc)
    comentarios = comentarios[:15]

    # Observaciones semanales de las semanas del mes
    lunes_actual = lunes_de(hoy_)
    claves: set[str] = set()
    for lunes in sorted({lunes_de(d) for d in habiles}):
        claves |= {texto_rango(lunes), lunes.isoformat(), etiqueta_semana(lunes, lunes_actual)}
    observaciones = [
        f"{'Familia' if (o.author_role or '').upper().startswith('FAMILI') else 'Educadora'}: {o.text.strip()}"
        for o in db.scalars(
            select(WeeklyObservation).where(
                WeeklyObservation.child_id == alumno.id, WeeklyObservation.week_key.in_(claves)
            )
        ).all()
        if (o.text or "").strip()
    ][:6]

    # Puntos que conviene que la familia atienda
    tiene_registros = bool(registros or estadias or ingresos or actividades)
    puntos: list[str] = []
    if salud:
        puntos.append(f"Situaciones de salud o accidentes registrados: {len(salud)} (ver detalle).")
    if dias_comio_poco:
        puntos.append(f"Comió poco o nada {_plural(len(dias_comio_poco), 'día', 'días')}.")
    dificiles = conteo["TRISTE"] + conteo["ENOJADO"]
    if dificiles:
        puntos.append(f"Estuvo triste o enojado {_plural(dificiles, 'día', 'días')}; vale la pena platicarlo con su educadora.")
    faltas = n_habiles - asistio
    if tiene_registros and faltas > 0:
        puntos.append(f"Faltó {_plural(faltas, 'día hábil', 'días hábiles')}.")
    if tiene_registros and not puntos:
        puntos.append("Nada que requiera atención especial este mes.")

    # Texto del reporte
    if not tiene_registros:
        texto = f"Aún no hay registros de {nombre} en {nombre_mes}."
    else:
        oraciones = [
            f"En {nombre_mes}, {nombre} asistió {asistio} de {_plural(n_habiles, 'día hábil', 'días hábiles')}"
            + (f" ({porcentaje_asistencia})" if n_habiles else "")
            + (f", con llegada promedio a las {llegada_promedio}" if llegadas else "")
            + "."
        ]
        if porcentajes:
            oraciones.append(f"Comió en promedio el {porcentaje_comida} de sus alimentos.")
        if siestas:
            oraciones.append(
                f"Durmió siesta {_plural(len(siestas), 'día', 'días')}, con un promedio de {siesta_diaria.replace('/día', '')} por día."
            )
        if animos:
            oraciones.append(
                f"Su estado de ánimo fue {animo.lower()}"
                + (f" y participó en las actividades {_plural(participo, 'día', 'días')}." if participo else ".")
            )
        if pedagogicas:
            oraciones.append("En su sala se trabajó: " + ", ".join(pedagogicas[:4]) + ".")
        oraciones.append(
            f"Se registraron {_plural(len(salud), 'situación de salud o accidente', 'situaciones de salud o accidentes')}."
            if salud
            else "No se registraron accidentes ni situaciones de salud."
        )
        texto = " ".join(oraciones)

    con_ia = False
    if tiene_registros:
        texto_ia = generar_reporte_mensual_ia(
            {
                "child_id": alumno.id,
                "mes_id": f"{anio:04d}-{mes:02d}",
                "nombre": nombre,
                "mes": f"{nombre_mes} de {anio}",
                "asistencia": f"{asistio} de {_plural(n_habiles, 'día hábil', 'días hábiles')} ({porcentaje_asistencia})",
                "llegada_promedio": llegada_promedio,
                "alimentacion": porcentaje_comida,
                "dias_comio_poco": len(dias_comio_poco),
                "siesta": f"{_plural(len(siestas), 'día', 'días')} con siesta, promedio {siesta_diaria}",
                "comportamiento": comportamiento,
                "salud": salud,
                "actividades_plan": pedagogicas,
                "comentarios_bitacora": comentarios,
                "observaciones": observaciones,
                "puntos_atencion": puntos,
            },
            nombres=[alumno.full_name, alumno.short_name],
        )
        if texto_ia:
            texto, con_ia = texto_ia, True

    if es_actual:
        periodo = f"Del 1 al {hoy_.day} de {nombre_mes} de {anio} (mes en curso)"
    else:
        periodo = f"Del 1 al {ultimo.day} de {nombre_mes} de {anio}"

    return {
        "id": f"{anio:04d}-{mes:02d}",
        "monthLabel": f"{nombre_mes.capitalize()} {anio}",
        "periodText": periodo,
        "isCurrentMonth": es_actual,
        "schoolDays": n_habiles,
        "attendanceDays": asistio,
        "attendancePercentageText": porcentaje_asistencia,
        "averageArrivalTime": llegada_promedio,
        "foodIntakePercentage": porcentaje_comida,
        "napDays": len(siestas),
        "averageNapDaily": siesta_diaria,
        "moodSummary": animo,
        "activitiesRecorded": len(actividades),
        "behaviorHighlights": comportamiento,
        "healthEvents": salud,
        "pedagogicalHighlights": pedagogicas,
        "observations": observaciones,
        "pointsForFamily": puntos,
        "reportText": texto,
        "generatedWithAi": con_ia,
        "generatedAt": hoy_.strftime("%d/%m/%Y"),
    }
