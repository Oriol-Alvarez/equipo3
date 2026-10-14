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
