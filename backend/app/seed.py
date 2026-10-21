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
    BitacoraMessage,
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
            id="usr_dir_mendez",
            email="direccion@sonrisas.edu",
            password_hash=pwd,
            role="DIRECCION",
            display_name="Laura Méndez (Dirección)",
            phone="+52 55 1234 0001",
            avatar_url="https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=150",
        ),
        Usuario(
            id="usr_educadora_laura",
            email="educadora@sonrisas.edu",
            password_hash=pwd,
            role="EDUCADORA",
            display_name="Laura Gómez (Educadora)",
            phone="+52 55 1234 0002",
            avatar_url="https://images.unsplash.com/photo-1580894732454-d6771d479d2a?w=150",
        ),
        Usuario(
            id="usr_educador_carlos",
            email="carlos@sonrisas.edu",
            password_hash=pwd,
            role="EDUCADORA",
            display_name="Carlos Ruiz (Educador)",
            phone="+52 55 1234 0003",
            avatar_url="https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
        ),
        Usuario(
            id="usr_familia_garcia",
            email="familiar@sonrisas.edu",
            password_hash=pwd,
            role="FAMILIAR",
            display_name="María López Fernández",
            phone="+52 55 1234 5678",
            avatar_url="https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150",
        ),
        Usuario(
            id="usr_familia_martinez",
            email="martinez@sonrisas.edu",
            password_hash=pwd,
            role="FAMILIAR",
            display_name="Roberto Martínez Ruiz",
            phone="+52 55 8765 4321",
            avatar_url="https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150",
        ),
        Usuario(
            id="usr_familia_lopez",
            email="lopez@sonrisas.edu",
            password_hash=pwd,
            role="FAMILIAR",
            display_name="Ana López Navarro",
            phone="+52 55 5678 1234",
            avatar_url="https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
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

    # Asignación de educadores a salas
    db.add(RoomEducator(room_id="sala_1a", educator_id="usr_educadora_laura", role_title="Educadora Titular"))
    db.add(RoomEducator(room_id="sala_2b", educator_id="usr_educador_carlos", role_title="Educador Titular"))

    # 3. Alumnos (Todos vinculados a una cuenta familiar)
    ninos = [
        Child(
            id="mateo_garcia",
            full_name="Mateo García López",
            short_name="Mateo",
            age_text="2 años",
            room_id="sala_1a",
            group_text="Lactantes",
            status_text="08:15 AM • Presente",
            is_present=True,
            arrival_time="08:15 AM",
            allergy_alert=None,
            avatar_initials="MG",
            avatar_bg_color=4294948685,
        ),
        Child(
            id="lucia_garcia",
            full_name="Lucía García López",
            short_name="Lucía",
            age_text="1 año y 2 meses",
            room_id="sala_1a",
            group_text="Lactantes",
            status_text="08:45 AM • Presente",
            is_present=True,
            arrival_time="08:45 AM",
            allergy_alert=None,
            avatar_initials="LG",
            avatar_bg_color=4293944082,
        ),
        Child(
            id="lucas_martinez",
            full_name="Lucas Martínez Sánchez",
            short_name="Lucas",
            age_text="2 años",
            room_id="sala_2b",
            group_text="Maternales A",
            status_text="No ha ingresado",
            is_present=False,
            arrival_time=None,
            allergy_alert=None,
            avatar_initials="LM",
            avatar_bg_color=4287665326,
        ),
        Child(
            id="sofia_lopez",
            full_name="Sofía López Gómez",
            short_name="Sofía",
            age_text="1 año y 8 meses",
            room_id="sala_1a",
            group_text="Lactantes",
            status_text="08:30 AM • Presente",
            is_present=True,
            arrival_time="08:30 AM",
            allergy_alert=None,
            avatar_initials="SL",
            avatar_bg_color=4286695300,
        ),
    ]
    db.add_all(ninos)
    db.flush()

    # Relación padres <-> hijos (100% de los niños vinculados a una cuenta familiar)
    db.add(ChildParent(child_id="mateo_garcia", parent_user_id="usr_familia_garcia"))
    db.add(ChildParent(child_id="lucia_garcia", parent_user_id="usr_familia_garcia"))
    db.add(ChildParent(child_id="lucas_martinez", parent_user_id="usr_familia_martinez"))
    db.add(ChildParent(child_id="sofia_lopez", parent_user_id="usr_familia_lopez"))

    # Expedientes completos de todos los alumnos (formato y datos México)
    mateo_profile = ChildProfile(
        child_id="mateo_garcia",
        birth_date="14/04/2024",
        enrollment_date="01/09/2025",
        student_id="CURP-GARM240414HDFRLA01",
        blood_type="O+",
        pediatrician="Dra. Sofía Morales (Hospital Infantil)",
        pediatrician_phone="+52 55 1234 5678",
        emergency_phone="+52 55 9876 5432",
        emergency_phone_relation="Abuela materna (Rosa)",
        mother_name="María López Fernández",
        mother_phone="+52 55 1234 5678",
        father_name="Carlos García Ramos",
        father_phone="+52 55 3333 4444",
        authorized_pickups=json.dumps([
            "Rosa Fernández (Abuela materna) - CURP: FERA750312MDFRNS01 • Tel: +52 55 4444 5555",
            "Carlos García Ramos (Padre) - CURP: GARC820715HDFRRL02 • Tel: +52 55 3333 4444",
        ]),
        medical_notes="Esquema de vacunación completo conforme a Cartilla Nacional de Salud. Sin alergias registradas.",
        general_notes="Muy sociable y participativo. Se adapta muy bien a las rutinas.",
        habits_notes="Duerme con su mantita de apego.",
        medical_certificate_uri="https://cdn.sonrisas.edu/certificates/cert_mateo_2026.pdf",
        medical_certificate_name="Certificado_Medico_Mateo.pdf",
        medical_certificate_date="15/08/2026",
    )
    db.add(mateo_profile)

    lucia_profile = ChildProfile(
        child_id="lucia_garcia",
        birth_date="10/08/2025",
        enrollment_date="01/09/2026",
        student_id="CURP-GARL250810MDFRLA02",
        blood_type="O+",
        pediatrician="Dra. Sofía Morales (Hospital Infantil)",
        pediatrician_phone="+52 55 1234 5678",
        emergency_phone="+52 55 9876 5432",
        emergency_phone_relation="Abuela materna (Rosa)",
        mother_name="María López Fernández",
        mother_phone="+52 55 1234 5678",
        father_name="Carlos García Ramos",
        father_phone="+52 55 3333 4444",
        authorized_pickups=json.dumps([
            "Rosa Fernández (Abuela materna) - CURP: FERA750312MDFRNS01 • Tel: +52 55 4444 5555",
            "Carlos García Ramos (Padre) - CURP: GARC820715HDFRRL02 • Tel: +52 55 3333 4444",
        ]),
        medical_notes="Esquema de vacunación completo al día. Sin alergias conocidas.",
        general_notes="Comenzando a dar sus primeros pasos de forma alegre.",
        habits_notes="Toma biberón de 180ml a media mañana.",
    )
    db.add(lucia_profile)

    lucas_profile = ChildProfile(
        child_id="lucas_martinez",
        birth_date="22/02/2024",
        enrollment_date="01/09/2025",
        student_id="CURP-MASL240222HDFRSN03",
        blood_type="A+",
        pediatrician="Dr. Manuel Benítez (Clínica Pedregal)",
        pediatrician_phone="+52 55 2222 3333",
        emergency_phone="+52 55 8888 9999",
        emergency_phone_relation="Tío (Javier)",
        mother_name="Elena Sánchez Gil",
        mother_phone="+52 55 7777 8888",
        father_name="Roberto Martínez Ruiz",
        father_phone="+52 55 6666 7777",
        authorized_pickups=json.dumps([
            "Elena Sánchez Gil (Madre) - CURP: SAGE850918MDFLNN03 • Tel: +52 55 7777 8888",
            "Roberto Martínez Ruiz (Padre) - CURP: MARR830422HDFRRB04 • Tel: +52 55 6666 7777",
        ]),
        medical_notes="Intolerancia leve a la lactosa (sustituir lácteos enteros por leche deslactosada o vegetal). Cartilla Nacional al día.",
        general_notes="Le gusta la música y construir torres de bloques.",
        habits_notes="Usa chupete únicamente para conciliar el sueño.",
    )
    db.add(lucas_profile)

    sofia_profile = ChildProfile(
        child_id="sofia_lopez",
        birth_date="15/05/2024",
        enrollment_date="01/09/2025",
        student_id="CURP-LOGS240515MDFPZ04",
        blood_type="B+",
        pediatrician="Dra. Elena Castro (Hospital Ángeles)",
        pediatrician_phone="+52 55 3333 4444",
        emergency_phone="+52 55 1212 3434",
        emergency_phone_relation="Abuelo (Fernando)",
        mother_name="Ana López Navarro",
        mother_phone="+52 55 5656 7878",
        father_name="David Gómez Prieto",
        father_phone="+52 55 4343 2121",
        authorized_pickups=json.dumps([
            "Ana López Navarro (Madre) - CURP: LONA861105MDFPRD05 • Tel: +52 55 5656 7878",
            "Fernando López (Abuelo) - CURP: LOPF580214HDFRRN06 • Tel: +52 55 1212 3434",
        ]),
        medical_notes="Salud general óptima. Esquema de vacunación nacional al día. Sin alergias registradas.",
        general_notes="Muy sociable y curiosa, disfruta de las actividades de dibujo.",
        habits_notes="Duerme siesta regular de 13:30 a 15:00.",
    )
    db.add(sofia_profile)

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
    db.add(
        BitacoraMessage(
            id="bm_mateo_1",
            child_id="mateo_garcia",
            sender_id="usr_educadora_laura",
            sender_name="Laura Gómez",
            message="Mateo comió muy bien hoy todo su puré.",
            time="12:30 PM",
            date="12/10/2026",
        )
    )
    db.add(
        BitacoraMessage(
            id="bm_lucia_1",
            child_id="lucia_garcia",
            sender_id="usr_educador_carlos",
            sender_name="Carlos Ruiz",
            message="Lucía tomó su biberón y durmió una siesta tranquila.",
            time="11:45 AM",
            date="12/10/2026",
        )
    )

    # 4. Personal Escolar
    personal = [
        StaffMember(id="staff_1", user_id="usr_dir_mendez", name="Laura Méndez", role="Dirección General", avatar_initials="LM", avatar_bg_color=4287505578, is_online=True),
        StaffMember(id="staff_2", user_id="usr_educadora_laura", name="Laura Gómez", role="Educadora Titular", avatar_initials="LG", avatar_bg_color=4278364369, is_online=True),
        StaffMember(id="staff_3", user_id="usr_educador_carlos", name="Carlos Ruiz", role="Educador Lactantes", avatar_initials="CR", avatar_bg_color=4283215696, is_online=True),
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
        unread_count=0,
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

PERFILES_OTRAS_SALAS = {
    "emilia_ramos": {
        "birth_date": "15/06/2024",
        "enrollment_date": "01/09/2025",
        "student_id": "CURP-RACE240615MDFMX01",
        "blood_type": "A+",
        "pediatrician": "Dra. Sofía Morales (Hospital Infantil)",
        "pediatrician_phone": "+52 55 1234 5678",
        "emergency_phone": "+52 55 5555 1111",
        "emergency_phone_relation": "Abuela (Gabriela)",
        "mother_name": "Gabriela Cruz Mendoza",
        "mother_phone": "+52 55 5555 1234",
        "father_name": "Fernando Ramos Silva",
        "father_phone": "+52 55 5555 5678",
        "authorized_pickups": json.dumps([
            "Gabriela Cruz Mendoza (Madre) - CURP: CRMG880420MDFRND01 • Tel: +52 55 5555 1234",
            "Fernando Ramos Silva (Padre) - CURP: RASF851012HDFMSN02 • Tel: +52 55 5555 5678",
        ]),
        "medical_notes": "Esquema de vacunación completo conforme a Cartilla Nacional de Salud. Sin alergias registradas.",
        "general_notes": "Le encanta pintar con los dedos y jugar en el patio.",
        "habits_notes": "Toma siesta regular de 13:00 a 14:30.",
    },
    "diego_herrera": {
        "birth_date": "12/03/2024",
        "enrollment_date": "01/09/2025",
        "student_id": "CURP-HESD240312HDFRT02",
        "blood_type": "O+",
        "pediatrician": "Dr. Manuel Benítez (Clínica Pedregal)",
        "pediatrician_phone": "+52 55 2222 3333",
        "emergency_phone": "+52 55 5555 2222",
        "emergency_phone_relation": "Tía (Patricia)",
        "mother_name": "Patricia Soto Vargas",
        "mother_phone": "+52 55 5555 2345",
        "father_name": "Javier Herrera Beltrán",
        "father_phone": "+52 55 5555 6789",
        "authorized_pickups": json.dumps([
            "Patricia Soto Vargas (Madre) - CURP: SOVP870914MDFTT03 • Tel: +52 55 5555 2345",
            "Javier Herrera Beltrán (Padre) - CURP: HEBJ840523HDFRL04 • Tel: +52 55 5555 6789",
        ]),
        "medical_notes": "Salud general óptima. Esquema de vacunación nacional al día. Sin alergias.",
        "general_notes": "Muy curioso, disfruta de los juegos de construcción.",
        "habits_notes": "Le gusta escuchar cuentos antes de dormir.",
    },
    "valentina_ruiz": {
        "birth_date": "20/09/2024",
        "enrollment_date": "01/09/2025",
        "student_id": "CURP-RUMV240920MDFZ03",
        "blood_type": "O+",
        "pediatrician": "Dra. Elena Castro (Hospital Ángeles)",
        "pediatrician_phone": "+52 55 3333 4444",
        "emergency_phone": "+52 55 5555 3333",
        "emergency_phone_relation": "Abuelo (Arturo)",
        "mother_name": "Mónica Mora Delgado",
        "mother_phone": "+52 55 5555 3456",
        "father_name": "Alejandro Ruiz Fuentes",
        "father_phone": "+52 55 5555 7890",
        "authorized_pickups": json.dumps([
            "Mónica Mora Delgado (Madre) - CURP: MODM891102MDFRL05 • Tel: +52 55 5555 3456",
            "Alejandro Ruiz Fuentes (Padre) - CURP: RUFA860818HDFNT06 • Tel: +52 55 5555 7890",
        ]),
        "medical_notes": "Sin alergias. Cartilla Nacional de Vacunación al día.",
        "general_notes": "Muy alegre, participa activamente en rondas musicales.",
        "habits_notes": "Usa vaso entrenador para agua.",
    },
    "santiago_flores": {
        "birth_date": "05/10/2023",
        "enrollment_date": "01/09/2025",
        "student_id": "CURP-FOVS231005HDFLN04",
        "blood_type": "B+",
        "pediatrician": "Dr. Manuel Benítez (Clínica Pedregal)",
        "pediatrician_phone": "+52 55 2222 3333",
        "emergency_phone": "+52 55 5555 4444",
        "emergency_phone_relation": "Tío (Rodrigo)",
        "mother_name": "Claudia Vega Pineda",
        "mother_phone": "+52 55 5555 4567",
        "father_name": "Mauricio Flores Garza",
        "father_phone": "+52 55 5555 8901",
        "authorized_pickups": json.dumps([
            "Claudia Vega Pineda (Madre) - CURP: VEPC850325MDFGN07 • Tel: +52 55 5555 4567",
            "Mauricio Flores Garza (Padre) - CURP: FOGM830712HDFR08 • Tel: +52 55 5555 8901",
        ]),
        "medical_notes": "Esquema de vacunación completo. Sin alergias registradas.",
        "general_notes": "Le gustan las actividades al aire libre y armar rompecabezas.",
        "habits_notes": "Duerme siesta de 13:30 a 15:00.",
    },
    "regina_navarro": {
        "birth_date": "18/11/2023",
        "enrollment_date": "01/09/2025",
        "student_id": "CURP-NADR231118MDFVT05",
        "blood_type": "A+",
        "pediatrician": "Dra. Sofía Morales (Hospital Infantil)",
        "pediatrician_phone": "+52 55 1234 5678",
        "emergency_phone": "+52 55 5555 5555",
        "emergency_phone_relation": "Abuela (Silvia)",
        "mother_name": "Silvia Díaz Romo",
        "mother_phone": "+52 55 5555 5678",
        "father_name": "Jorge Navarro Campos",
        "father_phone": "+52 55 5555 9012",
        "authorized_pickups": json.dumps([
            "Silvia Díaz Romo (Madre) - CURP: DIRS861208MDFMZ09 • Tel: +52 55 5555 5678",
            "Jorge Navarro Campos (Padre) - CURP: NACJ840619HDFRP10 • Tel: +52 55 5555 9012",
        ]),
        "medical_notes": "Salud excelente, Cartilla Nacional al día. Sin alergias conocidas.",
        "general_notes": "Sociable, le encantan los títeres y el baile.",
        "habits_notes": "Se alimenta sola con cubiertos de aprendizaje.",
    },
    "leonardo_castro": {
        "birth_date": "08/08/2023",
        "enrollment_date": "01/09/2025",
        "student_id": "CURP-CAPL230808HDFZZ06",
        "blood_type": "O+",
        "pediatrician": "Dra. Elena Castro (Hospital Ángeles)",
        "pediatrician_phone": "+52 55 3333 4444",
        "emergency_phone": "+52 55 5555 6666",
        "emergency_phone_relation": "Tía (Lorena)",
        "mother_name": "Lorena Paz Rangel",
        "mother_phone": "+52 55 5555 6789",
        "father_name": "Eduardo Castro Meza",
        "father_phone": "+52 55 5555 0123",
        "authorized_pickups": json.dumps([
            "Lorena Paz Rangel (Madre) - CURP: PARL870115MDFTZ11 • Tel: +52 55 5555 6789",
            "Eduardo Castro Meza (Padre) - CURP: CAME850530HDFRC12 • Tel: +52 55 5555 0123",
        ]),
        "medical_notes": "Cartilla Nacional completa. Sin restricciones médicas ni alergias.",
        "general_notes": "Muy colaborativo y ordenado con sus juguetes.",
        "habits_notes": "Duerme con peluche favorito durante la siesta.",
    },
}


def seed_salas_adicionales(db: Session) -> None:
    """Agrega los alumnos de las salas 2B y 3C si aún no existen (también en bases ya creadas)."""
    if db.get(Room, "sala_2b") is None or db.get(Room, "sala_3c") is None:
        return
    agregados = False
    for child_id, nombre, corto, edad, sala, iniciales, color in ALUMNOS_OTRAS_SALAS:
        if db.get(Child, child_id) is None:
            db.add(
                Child(
                    id=child_id,
                    full_name=nombre,
                    short_name=corto,
                    age_text=edad,
                    room_id=sala,
                    group_text="Maternales A" if sala == "sala_2b" else "Maternales B",
                    status_text="No ha ingresado",
                    is_present=False,
                    arrival_time=None,
                    allergy_alert=None,
                    avatar_initials=iniciales,
                    avatar_bg_color=color,
                )
            )
            agregados = True

        profile = db.get(ChildProfile, child_id)
        p_data = PERFILES_OTRAS_SALAS.get(child_id, {})
        if profile is None:
            db.add(ChildProfile(child_id=child_id, **p_data))
            agregados = True
        elif not profile.student_id:
            for k, v in p_data.items():
                setattr(profile, k, v)
            agregados = True
    if agregados:
        db.commit()
