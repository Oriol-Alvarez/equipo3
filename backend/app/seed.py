"""Población inicial de la base de datos (seeding) con las entidades estándar de Sonrisas.
Permite probar la aplicación de inmediato con usuarios y datos coincidentes con la app Android.
"""

import json
from sqlalchemy import select
from sqlalchemy.orm import Session

from .models import (
    Activity,
    Announcement,
    AttendanceRecord,
    Child,
    ChildParent,
    ChildProfile,
    FamilyGroup,
    FamilyGroupMember,
    FamilyGroupMessage,
    LostObject,
    Room,
    RoomEducator,
    StaffMember,
    Suggestion,
    Usuario,
    WorkerChat,
    WorkerChatMessage,
)
from .security import hash_password


def seed_database(db: Session) -> None:
    """Inserta datos de prueba si la base de datos se encuentra vacía."""
    if db.scalar(select(Usuario).limit(1)) is not None:
        return  # Ya existen datos

    # 1. Usuarios del sistema
    pwd = hash_password("Password123!")
    usuarios = [
        Usuario(
            id="usr_familia_garcia",
            email="familiar@sonrisas.edu",
            password_hash=pwd,
            role="FAMILIAR",
            display_name="María López Fernández",
            phone="+52 555 123 456",
            avatar_url="https://cdn.sonrisas.edu/avatars/maria.png",
        ),
        Usuario(
            id="usr_educadora_laura",
            email="educadora@sonrisas.edu",
            password_hash=pwd,
            role="EDUCADORA",
            display_name="Laura Gómez",
            phone="+52 555 987 654",
            avatar_url=None,
        ),
        Usuario(
            id="usr_dir_mendez",
            email="direccion@sonrisas.edu",
            password_hash=pwd,
            role="DIRECCION",
            display_name="Laura Méndez",
            phone="+52 555 333 444",
            avatar_url=None,
        ),
    ]
    db.add_all(usuarios)
    db.flush()

    # 2. Salas
    salas = [
        Room(id="sala_1a", name="Sala 1A", level="Lactantes", capacity=15),
        Room(id="sala_2b", name="Sala 2B", level="Maternales A", capacity=12),
        Room(id="sala_3c", name="Sala 3C", level="Maternales B", capacity=14),
    ]
    db.add_all(salas)
    db.flush()

    # Asignación de educadora a sala
    db.add(RoomEducator(room_id="sala_1a", educator_id="usr_educadora_laura", role_title="Educadora Titular"))

    # 3. Alumnos
    ninos = [
        Child(
            id="mateo_garcia",
            full_name="Mateo García",
            short_name="Mateo G.",
            age_text="2 años",
            room_id="sala_1a",
            group_text="Grupo Sala 1A",
            status_text="08:15 AM • Presente",
            is_present=True,
            arrival_time="08:15 AM",
            allergy_alert="Alergia a Nuez (No Certificado)",
            avatar_initials="MG",
            avatar_bg_color=4294948685,
        ),
        Child(
            id="lucia_garcia",
            full_name="Lucía García López",
            short_name="Lucía G.",
            age_text="1 año y 2 meses",
            room_id="sala_1a",
            group_text="Lactantes A",
            status_text="08:45 AM • Presente",
            is_present=True,
            arrival_time="08:45 AM",
            allergy_alert=None,
            avatar_initials="LG",
            avatar_bg_color=4293944082,
        ),
        Child(
            id="sofia_lopez",
            full_name="Sofía López",
            short_name="Sofía L.",
            age_text="1 año y 8 meses",
            room_id="sala_1a",
            group_text="Grupo Sala 1A",
            status_text="08:30 AM • Presente",
            is_present=True,
            arrival_time="08:30 AM",
            allergy_alert=None,
            avatar_initials="SL",
            avatar_bg_color=4286695300,
        ),
        Child(
            id="lucas_martinez",
            full_name="Lucas Martínez",
            short_name="Lucas M.",
            age_text="2 años",
            room_id="sala_1a",
            group_text="Grupo Sala 1A",
            status_text="No ha ingresado",
            is_present=False,
            arrival_time=None,
            allergy_alert=None,
            avatar_initials="LM",
            avatar_bg_color=4287665326,
        ),
    ]
    db.add_all(ninos)
    db.flush()

    # Relación padres <-> hijos
    db.add(ChildParent(child_id="mateo_garcia", parent_user_id="usr_familia_garcia"))
    db.add(ChildParent(child_id="lucia_garcia", parent_user_id="usr_familia_garcia"))

    # Expediente completo de Mateo
    mateo_profile = ChildProfile(
        child_id="mateo_garcia",
        birth_date="14/04/2024",
        enrollment_date="01/09/2025",
        student_id="EST-MATE-2026",
        blood_type="O+",
        pediatrician="Dra. Sofía Morales",
        pediatrician_phone="+52 555 111 222",
        emergency_phone="+52 555 333 444",
        emergency_phone_relation="Abuela materna",
        mother_name="María López Fernández",
        mother_phone="+52 555 123 456",
        father_name="Carlos García Ramos",
        father_phone="+52 555 987 654",
        authorized_pickups=json.dumps([
            "Rosa Fernández (Abuela materna) - DNI ***4521",
            "Elena García (Tía paterna) - DNI ***9812",
        ]),
        medical_notes="Alergia a Nuez (No Certificado). Se requiere extremo cuidado en meriendas.",
        general_notes="Muy sociable y participativo. Se adapta muy bien a las rutinas.",
        habits_notes="Duerme con su mantita de apego.",
        medical_certificate_uri="https://cdn.sonrisas.edu/certificates/cert_mateo_2026.pdf",
        medical_certificate_name="Certificado_Medico_Mateo.pdf",
        medical_certificate_date="15/08/2026",
    )
    db.add(mateo_profile)

    # Asistencia y actividad inicial para la línea de tiempo de Mateo
    db.add(
        AttendanceRecord(
            id="att_seed_mateo_1",
            child_id="mateo_garcia",
            is_ingreso=True,
            date="12/10/2026",
            day_of_week="L",
            time="08:15 AM",
            status="REGISTRADO",
            assistant_name="María Rodríguez Hernández",
            observations="Llega alegre con ropa limpia de recambio.",
        )
    )
    db.add(
        Activity(
            id="act_seed_mateo_1",
            child_id="mateo_garcia",
            date="12/10/2026",
            time="12:20 PM",
            category="Alimentación",
            portion_option="Medio plato",
            title="Almuerzo - Menú del día",
            description="Comió sopa de verduras con pollo.",
        )
    )

    # 4. Personal Escolar
    personal = [
        StaffMember(id="staff_1", user_id="usr_dir_mendez", name="Laura Méndez", role="Dirección General", avatar_initials="LM", avatar_bg_color=4287505578, is_online=True),
        StaffMember(id="staff_2", user_id="usr_educadora_laura", name="Laura Gómez", role="Educadora Titular", avatar_initials="LG", avatar_bg_color=4278364369, is_online=True),
    ]
    db.add_all(personal)

    # 5. Anuncios y Circulares
    anuncios = [
        Announcement(
            id="ann_1",
            title="Reunión de Padres y Madres - Inicio de Curso",
            date="Hoy, 10:00 AM",
            summary="Recordatorio sobre la reunión de este viernes en el salón principal.",
            full_content="Estimadas familias,\n\nLes recordamos que este viernes 2 de Octubre a las 18:00h tendrá lugar la reunión general informativa.\n\nAtentamente,\nDirección.",
            author="Dirección General",
        ),
        Announcement(
            id="ann_2",
            title="Campaña de Vacunación contra la Gripe",
            date="Ayer, 09:00 AM",
            summary="Información sobre la próxima campaña de salud.",
            full_content="Estimados padres, en colaboración con el centro de salud local, se llevará a cabo la jornada preventiva.",
            author="Servicio Médico",
        ),
    ]
    db.add_all(anuncios)

    # 6. Catálogo de Objetos Perdidos
    objetos = [
        LostObject(
            id="lost_1",
            title="Chaqueta Azul",
            description="Chaqueta de invierno infantil marca geoxrosa.",
            category="Ropa",
            location="Zona de recreo",
            image_uri="https://cdn.sonrisas.edu/lost/chaqueta_azul.jpg",
            room="Sala 1A",
            is_claimed=False,
        ),
        LostObject(
            id="lost_2",
            title="Botella de Agua Roja",
            description="Botella de plástico con tapa deportiva.",
            category="Otros",
            location="Comedor",
            image_uri="https://cdn.sonrisas.edu/lost/botella_roja.jpg",
            room="Sala 1A",
            is_claimed=False,
        ),
    ]
    db.add_all(objetos)

    # 7. Sugerencias
    db.add(
        Suggestion(
            id="sug_1",
            parent_user_id="usr_familia_garcia",
            parent_name="Elena Ramos",
            child_id="mateo_garcia",
            child_name="Mateo García",
            room_name="Sala 1A",
            date="12/10/2026",
            time="09:30 AM",
            category="ALIMENTACION",
            subject="Variedad de frutas frescas en merienda matutina",
            content="Nos gustaría proponer si es posible incluir mayor variedad de frutas de temporada en la media mañana.",
            status="PENDIENTE",
        )
    )
    db.add(
        Suggestion(
            id="sug_2",
            parent_user_id="usr_familia_garcia",
            parent_name="Marcos López",
            child_id="mateo_garcia",
            child_name="Mateo García",
            room_name="Sala 1A",
            date="11/10/2026",
            time="17:15 PM",
            category="INSTALACIONES",
            subject="Toldo de sombra para el arenero del patio",
            content="Notamos que en los días soleados la zona del arenero exterior recibe sol directo al mediodía.",
            status="EN_REVISION",
            response="Muchas gracias por la observación, Marcos. El área de mantenimiento ya está evaluando presupuesto.",
            response_date="11/10/2026 06:00 PM",
            responder_name="Laura Méndez (Dirección)",
        )
    )

    # 8. Grupo Familiar de Ejemplo
    fg = FamilyGroup(
        id="fgroup_paseo_sala_1a",
        title="Paseo al parque",
        created_by_id="usr_educadora_laura",
        avatar_bg_color=4278225275,
        last_message="¡Hola, familias! Creé este grupo para organizar el paseo.",
        last_message_time="09:00 AM",
    )
    db.add(fg)
    db.flush()
    db.add(FamilyGroupMember(group_id=fg.id, child_id="mateo_garcia"))
    db.add(FamilyGroupMember(group_id=fg.id, child_id="sofia_lopez"))
    db.add(FamilyGroupMember(group_id=fg.id, child_id="lucas_martinez"))
    db.add(
        FamilyGroupMessage(
            id="fgm_1",
            group_id=fg.id,
            sender_id="usr_educadora_laura",
            sender_name="Educadora Sala 1A",
            text="¡Hola, familias! Creé este grupo para organizar el paseo del viernes.",
            time="09:00 AM",
        )
    )

    # 9. Chats de Personal
    global_chat = WorkerChat(
        id="global_staff_chat",
        title="Chat Global de Trabajadores",
        subtitle="18 miembros",
        is_global=True,
        is_group=True,
        avatar_initials="CG",
        avatar_bg_color=4287505578,
        last_message="Buenos días equipo. Hoy reunión rápida a las 16:30.",
        last_message_time="09:00 AM",
    )
    db.add(global_chat)
    db.add(
        WorkerChatMessage(
            id="gm_1",
            chat_id="global_staff_chat",
            sender_id="usr_dir_mendez",
            sender_name="Laura Méndez",
            message="Buenos días equipo. Hoy reunión rápida a las 16:30.",
            time="09:00 AM",
        )
    )

    wchat1 = WorkerChat(
        id="wchat_1",
        title="Laura Méndez",
        subtitle="Dirección General • En línea",
        is_global=False,
        is_group=False,
        avatar_initials="LM",
        avatar_bg_color=4287505578,
        last_message="¿Cómo va la adaptación de Mateo García?",
        last_message_time="11:30 AM",
        unread_count=1,
    )
    db.add(wchat1)
    db.add(
        WorkerChatMessage(
            id="wm_1",
            chat_id="wchat_1",
            sender_id="usr_dir_mendez",
            sender_name="Laura Méndez",
            message="¿Cómo va la adaptación de Mateo García?",
            time="11:30 AM",
        )
    )

    db.commit()


# ---------------------------------------------------------------------------
# Datos de ejemplo de observaciones semanales, planeación/menú y registro diario.
# Se ejecuta en cada arranque y solo agrega lo que falta (no borra nada).
# ---------------------------------------------------------------------------

_MENUS_SEMANA = [
    ("Avena con plátano y leche", "Gajos de mandarina", "Crema de calabaza, arroz con pollo y agua de jamaica", "Mateo García: menú sin nuez."),
    ("Hot cakes de avena con fruta", "Yogur natural", "Sopa de fideo, tortitas de verdura y agua de pepino", ""),
    ("Huevo revuelto con jitomate y tortilla", "Pepino y jícama", "Lentejas con zanahoria, arroz y agua de limón", ""),
    ("Pan integral con frijoles y queso fresco", "Manzana picada", "Caldo de verduras, pescado empapelado y puré de papa", ""),
    ("Licuado de fresa y galletas de avena", "Papaya", "Pasta con verduras, pollo deshebrado y agua de melón", ""),
]

_ACTIVIDADES_SEMANA = [
    [("09:00", "Asamblea de bienvenida y canciones"), ("10:00", "Motricidad fina: ensartar cuentas grandes"), ("11:30", "Juego libre en el patio")],
    [("09:00", "Asamblea: los colores"), ("10:00", "Pintura con dedos sobre mural"), ("11:30", "Cuentacuentos con títeres")],
    [("09:00", "Asamblea: los animales de la granja"), ("10:00", "Circuito de psicomotricidad"), ("11:30", "Música y movimiento")],
    [("09:00", "Asamblea: el clima"), ("10:00", "Taller sensorial con texturas"), ("11:30", "Jardín: regar las plantas")],
    [("09:00", "Asamblea y repaso de la semana"), ("10:00", "Construcción con bloques"), ("11:30", "Mini fiesta de cumpleaños del mes")],
]


def seed_modulos_heber(db: Session) -> None:
    import json
    from datetime import timedelta

    from .horario import hoy
    from .models import Child, DailyLog, DailyPlan, Room, WeeklyObservation

    if db.get(Room, "sala_1a") is None:
        return  # La base todavía no tiene los datos base

    # 1. Observación semanal de ejemplo
    if db.get(WeeklyObservation, "wobs_1") is None and db.get(Usuario, "usr_familia_garcia") is not None:
        db.add(
            WeeklyObservation(
                id="wobs_1",
                child_id="mateo_garcia",
                week_key="Semana del 15 al 19 de Septiembre, 2026",
                author_role="FAMILIA",
                author_user_id="usr_familia_garcia",
                author_name="Familia García",
                text="En casa también lo notamos más tranquilo para dormir. El fin de semana comió brócoli en puré sin problema.",
                time="19/09 07:40 PM",
            )
        )

    # 2. Planeación de la semana escolar actual (lunes a viernes), si falta algún día
    lunes = hoy() - timedelta(days=hoy().weekday())
    for i in range(5):
        dia = (lunes + timedelta(days=i)).isoformat()
        if db.get(DailyPlan, ("sala_1a", dia)) is None:
            desayuno, colacion, comida, notas = _MENUS_SEMANA[i]
            db.add(
                DailyPlan(
                    room_id="sala_1a",
                    date=dia,
                    breakfast=desayuno,
                    snack=colacion,
                    lunch=comida,
                    notes=notas,
                    activities=json.dumps([{"time": t, "title": a} for t, a in _ACTIVIDADES_SEMANA[i]], ensure_ascii=False),
                    updated_by="Educadora Sala 1A",
                )
            )

    # 3. Registro diario de los últimos 10 días hábiles (antes de hoy) para la sala 1A
    if db.scalar(select(DailyLog).limit(1)) is None:
        alumnos = db.scalars(select(Child).where(Child.room_id == "sala_1a").order_by(Child.id)).all()
        dias = []
        d = hoy() - timedelta(days=1)
        while len(dias) < 10:
            if d.weekday() < 5:
                dias.append(d)
            d -= timedelta(days=1)
        comidas = ["TODO", "POCO", "TODO", "PIDIO_MAS", "TODO"]
        animos = ["FELIZ", "FELIZ", "TRISTE", "FELIZ", "ENOJADO"]
        for n, alumno in enumerate(alumnos):
            for k, dia in enumerate(sorted(dias)):
                idx = (n + k) % 5
                entrada = f"08:{(5 * idx):02d}"
                salida = f"{14 + idx % 2}:{(10 * idx):02d}"
                db.add(
                    DailyLog(
                        child_id=alumno.id,
                        date=dia.isoformat(),
                        ingreso=json.dumps({
                            "time": entrada, "assistantName": "Laura Gómez", "deliveredBy": "Mamá",
                            "receivedBy": "Laura Gómez", "isSick": False, "medicationWithPrescription": "NO_APLICA",
                            "hasSymptoms": False, "symptoms": "", "arrivedClean": True, "backpackComplete": True,
                            "goodPhysicalCondition": True, "injuries": "", "observations": "",
                        }),
                        estadia=json.dumps({
                            "breakfast": comidas[idx], "snack": "TODO", "lunch": comidas[(idx + 1) % 5],
                            "foodNotes": "", "slept": True, "napMinutes": 45 + 15 * (idx % 3), "napComments": "",
                            "sphincterControl": "NO_APLICA", "sphincterPeeCount": None, "sphincterPoopCount": None,
                            "peeCount": 3, "poopCount": 1, "excretionNotes": "", "mood": animos[idx],
                            "cried": idx == 2, "fought": False, "participated": True, "moodNotes": "",
                            "hadAccident": False, "accidentDescription": "", "accidentFolio": "",
                            "hadHealthIssue": False, "healthIssue": "", "careProvided": "", "generalNotes": "",
                        }),
                        egreso=json.dumps({
                            "time": salida, "goodPhysicalCondition": True, "injuries": "", "collectedBy": "Mamá",
                            "deliveredClean": True, "belongingsComplete": True, "observations": "",
                            "deliveredByStaff": "Laura Gómez",
                        }),
                    )
                )

    db.commit()


# Alumnos de las salas 2B y 3C. Antes esas salas no tenían niños y la app mostraba
# los de la Sala 1A al entrar a cualquiera de ellas.
ALUMNOS_OTRAS_SALAS = [
    ("emilia_ramos", "Emilia Ramos Cruz", "Emilia R.", "2 años y 4 meses", "sala_2b", "ER", 4294940672),
    ("diego_herrera", "Diego Herrera Soto", "Diego H.", "2 años y 7 meses", "sala_2b", "DH", 4280391411),
    ("valentina_ruiz", "Valentina Ruiz Mora", "Valentina R.", "2 años y 1 mes", "sala_2b", "VR", 4293467747),
    ("santiago_flores", "Santiago Flores Vega", "Santiago F.", "3 años", "sala_3c", "SF", 4283215696),
    ("regina_navarro", "Regina Navarro Díaz", "Regina N.", "2 años y 11 meses", "sala_3c", "RN", 4294198070),
    ("leonardo_castro", "Leonardo Castro Paz", "Leonardo C.", "3 años y 2 meses", "sala_3c", "LC", 4284513675),
]


def seed_salas_adicionales(db: Session) -> None:
    """Agrega los alumnos de las salas 2B y 3C si aún no existen (también en bases ya creadas)."""
    if db.get(Room, "sala_2b") is None or db.get(Room, "sala_3c") is None:
        return
    agregados = False
    for child_id, nombre, corto, edad, sala, iniciales, color in ALUMNOS_OTRAS_SALAS:
        if db.get(Child, child_id) is not None:
            continue
        db.add(
            Child(
                id=child_id,
                full_name=nombre,
                short_name=corto,
                age_text=edad,
                room_id=sala,
                group_text=f"Grupo {'Sala 2B' if sala == 'sala_2b' else 'Sala 3C'}",
                status_text="No ha ingresado",
                is_present=False,
                arrival_time=None,
                allergy_alert=None,
                avatar_initials=iniciales,
                avatar_bg_color=color,
            )
        )
        db.add(ChildProfile(child_id=child_id))
        agregados = True
    if agregados:
        db.commit()
