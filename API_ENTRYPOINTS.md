# Documentación Técnica de Entrypoints de la API - Sonrisas App

**Proyecto:** Sonrisas - Aplicación Móvil de Control Escolar y Comunicación en Estancias Infantiles  
**Versión de API:** `v1`  
**Base URL:** `https://api.sonrisas.edu/api/v1`  
**Protocolos:** HTTPS (REST) y WSS (WebSockets)  
**Formato de intercambio:** `application/json` (excepto subida de archivos: `multipart/form-data`)  
**Autenticación:** JSON Web Tokens (JWT) vía cabecera `Authorization: Bearer <token>`  

---

## 1. Convenciones y Arquitectura General

### 1.1 Roles de Usuario (RBAC)
* `EDUCADORA`: Personal docente y de atención directa a los menores en las salas.
* `FAMILIAR`: Padres, madres o tutores legales de los menores inscritos.
* `DIRECCION`: Administradores del centro infantil (gestión de personal, circulares y auditoría).

### 1.2 Códigos de Estado HTTP Estándar
* `200 OK`: Petición procesada exitosamente.
* `201 Created`: Recurso creado exitosamente.
* `204 No Content`: Petición procesada exitosamente sin contenido de retorno.
* `400 Bad Request`: Parámetros inválidos, datos faltantes o error de validación de negocio.
* `401 Unauthorized`: Token ausente, inválido o expirado.
* `403 Forbidden`: El rol o usuario no tiene permisos sobre el recurso solicitado.
* `404 Not Found`: Recurso no encontrado.
* `500 Internal Server Error`: Error inesperado en el servidor.

---

## 2. Índice de Módulos de la API

1. [Autenticación y Sesión](#módulo-1-autenticación-y-sesión-auth-users)
2. [Salas y Aulas Escolares](#módulo-2-salas-y-aulas-escolares-rooms)
3. [Alumnos y Expedientes Médicos](#módulo-3-alumnos-y-expedientes-médicos-children)
4. [Control de Asistencia: Ingreso y Egreso](#módulo-4-control-de-asistencia-ingreso-y-egreso-attendance)
5. [Bitácora Diaria, Línea de Tiempo y Actividades](#módulo-5-bitácora-diaria-línea-de-tiempo-y-actividades-bitacora-activities)
6. [Resúmenes Semanales Pedagógicos](#módulo-6-resúmenes-semanales-pedagógicos-weekly-summaries)
7. [Mensajería de Personal y Chat Global](#módulo-7-mensajería-de-personal-y-chat-global-staff-chats)
8. [Chat Bitácora Individual (Educadora ↔ Familia)](#módulo-8-chat-bitácora-individual-chatsbitacora)
9. [Grupos Familiares](#módulo-9-grupos-familiares-family-groups)
10. [Avisos y Circulares del Centro](#módulo-10-avisos-y-circulares-generales-announcements)
11. [Buzón de Sugerencias y Quejas](#módulo-11-buzón-de-sugerencias-y-quejas-suggestions)
12. [Catálogo de Objetos Perdidos](#módulo-12-catálogo-de-objetos-perdidos-lost-objects)
13. [Almacenamiento Multimedia](#módulo-13-almacenamiento-y-subida-multimedia-media)
14. [Notificaciones Push y WebSockets](#módulo-14-notificaciones-push-y-tiempo-real-ws-notifications)
15. [Observaciones Semanales](#módulo-15-observaciones-semanales-weekly-observations)
16. [Planeación y Menú Diario](#módulo-16-planeación-y-menú-diario-daily-plans)
17. [Registro Diario / Bitácora de Filtro Diario](#módulo-17-registro-diario--bitácora-de-filtro-diario-daily-logs)

---

## Módulo 1: Autenticación y Sesión (`/auth`, `/users`)

### 1.1 Iniciar Sesión
* **Método:** `POST`
* **Ruta:** `/auth/login`
* **Rol requerido:** Público
* **Descripción:** Valida credenciales según el rol seleccionado (`EDUCADORA` o `FAMILIAR`) y devuelve el token de sesión JWT y el perfil básico.
* **Request Body:**
  ```json
  {
    "email": "familiar@sonrisas.edu",
    "password": "Password123!",
    "role": "FAMILIAR"
  }
  ```
* **Response (200 OK):**
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiIsIn...",
    "refreshToken": "d8e8fca2-...",
    "expiresIn": 86400,
    "user": {
      "id": "usr_familia_garcia",
      "email": "familiar@sonrisas.edu",
      "role": "FAMILIAR",
      "displayName": "María López Fernández",
      "avatarUrl": "https://cdn.sonrisas.edu/avatars/maria.png"
    }
  }
  ```

### 1.2 Refrescar Token
* **Método:** `POST`
* **Ruta:** `/auth/refresh`
* **Rol requerido:** Autenticado
* **Request Body:**
  ```json
  {
    "refreshToken": "d8e8fca2-..."
  }
  ```
* **Response (200 OK):**
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiIsIn...",
    "expiresIn": 86400
  }
  ```

### 1.3 Cerrar Sesión
* **Método:** `POST`
* **Ruta:** `/auth/logout`
* **Rol requerido:** Autenticado
* **Response (204 No Content)**

### 1.4 Obtener Perfil del Usuario Actual
* **Método:** `GET`
* **Ruta:** `/auth/me`
* **Rol requerido:** Autenticado
* **Response (200 OK):**
  ```json
  {
    "id": "usr_familia_garcia",
    "email": "familiar@sonrisas.edu",
    "role": "FAMILIAR",
    "displayName": "María López Fernández",
    "phone": "+52 555 123 456",
    "avatarUrl": null
  }
  ```

---

## Módulo 2: Salas y Aulas Escolares (`/rooms`)

### 2.1 Listar Salas Asignadas a la Educadora
* **Método:** `GET`
* **Ruta:** `/rooms`
* **Rol requerido:** `EDUCADORA`, `DIRECCION`
* **Descripción:** Lista las salas que la educadora tiene a su cargo con los contadores de asistencia actualizados.
* **Response (200 OK):**
  ```json
  [
    {
      "id": "sala_1a",
      "name": "Sala 1A",
      "level": "Lactantes",
      "presentCount": 12,
      "totalCount": 15
    },
    {
      "id": "sala_2b",
      "name": "Sala 2B",
      "level": "Maternales A",
      "presentCount": 8,
      "totalCount": 12
    }
  ]
  ```

### 2.2 Obtener Detalle de una Sala
* **Método:** `GET`
* **Ruta:** `/rooms/{roomId}`
* **Rol requerido:** `EDUCADORA`, `DIRECCION`
* **Response (200 OK):**
  ```json
  {
    "id": "sala_1a",
    "name": "Sala 1A",
    "level": "Lactantes",
    "capacity": 15,
    "presentCount": 12,
    "totalCount": 15,
    "educators": [
      {
        "id": "staff_1",
        "name": "Laura Gómez",
        "role": "Educadora Titular"
      }
    ]
  }
  ```

### 2.3 Listar Alumnos de una Sala
* **Método:** `GET`
* **Ruta:** `/rooms/{roomId}/children`
* **Rol requerido:** `EDUCADORA`, `DIRECCION`
* **Query Params:** `isPresent` (opcional: boolean)
* **Response (200 OK):**
  ```json
  [
    {
      "id": "mateo_garcia",
      "fullName": "Mateo García",
      "shortName": "Mateo G.",
      "ageText": "2 años",
      "roomText": "Sala 1A",
      "groupText": "Grupo Sala 1A",
      "statusText": "08:15 AM • Presente",
      "isPresent": true,
      "arrivalTime": "08:15 AM",
      "allergyAlert": "Alergia a Nuez (No Certificado)",
      "avatarInitials": "MG",
      "avatarBgColor": 4294948685
    }
  ]
  ```

---

## Módulo 3: Alumnos y Expedientes Médicos (`/children`)

### 3.1 Listar Hijos del Familiar Autenticado
* **Método:** `GET`
* **Ruta:** `/parents/me/children`
* **Rol requerido:** `FAMILIAR`
* **Descripción:** Devuelve todos los niños asociados a la cuenta del familiar para la pantalla de selección y el inicio familiar.
* **Response (200 OK):**
  ```json
  [
    {
      "id": "mateo_garcia",
      "fullName": "Mateo García",
      "shortName": "Mateo G.",
      "ageText": "2 años",
      "roomText": "Sala 1A",
      "groupText": "Grupo Sala 1A",
      "statusText": "08:15 AM • Presente",
      "isPresent": true,
      "arrivalTime": "08:15 AM",
      "allergyAlert": "Alergia a Nuez (No Certificado)",
      "avatarInitials": "MG",
      "avatarBgColor": 4294948685
    },
    {
      "id": "lucia_garcia",
      "fullName": "Lucía García López",
      "shortName": "Lucía G.",
      "ageText": "1 año y 2 meses",
      "roomText": "Sala Bebés",
      "groupText": "Lactantes A",
      "statusText": "08:45 AM • Presente",
      "isPresent": true,
      "arrivalTime": "08:45 AM",
      "allergyAlert": null,
      "avatarInitials": "LG",
      "avatarBgColor": 4293944082
    }
  ]
  ```

### 3.2 Obtener Ficha de un Alumno
* **Método:** `GET`
* **Ruta:** `/children/{childId}`
* **Rol requerido:** Ambos (con autorización sobre el menor)
* **Response (200 OK):** Mismo objeto `Child`.

### 3.3 Obtener Expediente Completo del Alumno
* **Método:** `GET`
* **Ruta:** `/children/{childId}/full-profile`
* **Rol requerido:** Ambos
* **Response (200 OK):**
  ```json
  {
    "childId": "mateo_garcia",
    "birthDate": "14/04/2024",
    "enrollmentDate": "01/09/2025",
    "studentId": "CURP-GARM240414HDFRLA01",
    "bloodType": "O+",
    "pediatrician": "Dra. Sofía Morales",
    "pediatricianPhone": "+52 55 1234 5678",
    "emergencyPhone": "+52 55 9876 5432",
    "motherName": "María López Fernández",
    "motherPhone": "+52 55 1234 5678",
    "fatherName": "Carlos García Ramos",
    "fatherPhone": "+52 55 3333 4444",
    "authorizedPickups": [
      "Rosa Fernández (Abuela materna) - CURP: FERA750312MDFRNS01 • Tel: +52 55 4444 5555",
      "Carlos García Ramos (Padre) - CURP: GARC820715HDFRRL02 • Tel: +52 55 3333 4444"
    ],
    "medicalNotes": "Alergia a Nuez (No Certificado). Se requiere extremo cuidado en meriendas.",
    "generalNotes": "Muy sociable y participativo. Se adapta muy bien a las rutinas.",
    "medicalCertificateUri": "https://cdn.sonrisas.edu/certificates/cert_mateo_2026.pdf",
    "medicalCertificateName": "Certificado_Medico_Mateo.pdf",
    "medicalCertificateDate": "15/08/2026"
  }
  ```

### 3.4 Actualizar Expediente del Alumno
* **Método:** `PUT`
* **Ruta:** `/children/{childId}/full-profile`
* **Rol requerido:** Ambos
* **Request Body:**
  ```json
  {
    "pediatrician": "Dra. Sofía Morales",
    "pediatricianPhone": "+52 55 1234 5678",
    "medicalNotes": "Alergia a Nuez certificada.",
    "habitsAndPedagogicalNotes": "Duerme con su mantita de apego.",
    "emergencyPhone": "+52 55 9876 5432",
    "authorizedPickups": [
      "Rosa Fernández (Abuela materna) - CURP: FERA750312MDFRNS01 • Tel: +52 55 4444 5555",
      "Carlos García Ramos (Padre) - CURP: GARC820715HDFRRL02 • Tel: +52 55 3333 4444"
    ],
    "medicalCertificateUri": "https://cdn.sonrisas.edu/certificates/cert_mateo_2026.pdf",
    "medicalCertificateName": "Certificado_Medico_Mateo.pdf",
    "medicalCertificateDate": "15/08/2026"
  }
  ```
* **Response (200 OK):** Objeto de expediente actualizado.

### 3.5 Subir Certificado Médico
* **Método:** `POST`
* **Ruta:** `/children/{childId}/medical-certificate`
* **Rol requerido:** Ambos
* **Content-Type:** `multipart/form-data`
* **Form-data:**
  - `file`: Archivo binario (PDF, PNG, JPG).
  - `certificateDate`: Fecha de emisión (ej. `15/08/2026`).
* **Response (200 OK):**
  ```json
  {
    "certificateUri": "https://cdn.sonrisas.edu/certificates/cert_mateo_2026.pdf",
    "certificateName": "Certificado_Medico_Mateo.pdf",
    "certificateDate": "15/08/2026"
  }
  ```

---

## Módulo 4: Control de Asistencia: Ingreso y Egreso (`/attendance`)

### 4.1 Registrar Ingreso (Paso a Paso - 10 preguntas)
* **Método:** `POST`
* **Ruta:** `/attendance/ingreso`
* **Rol requerido:** `EDUCADORA`
* **Descripción:** Registra el formulario secuencial de entrada.
* **Request Body:**
  ```json
  {
    "childId": "mateo_garcia",
    "date": "12/10/2026",
    "dayOfWeek": "L",
    "entryTime": "08:15",
    "assistantName": "María Rodríguez Hernández",
    "isChildSick": false,
    "medicationOption": "NO_APLICA",
    "hasSymptoms": false,
    "symptomsDetail": "",
    "isClean": true,
    "backpackComplete": true,
    "goodPhysicalCondition": true,
    "injuryDetail": "",
    "observations": "Llega alegre con ropa limpia de recambio.",
    "delivererName": "Carlos García Pérez",
    "delivererSignatureData": "data:image/png;base64,iVBORw0KGgoAAA...",
    "receiverStaffName": "María Rodríguez H.",
    "receiverStaffSignatureData": "data:image/png;base64,iVBORw0KGgoAAA..."
  }
  ```
* **Response (201 Created):**
  ```json
  {
    "id": "att_1728723600000",
    "childId": "mateo_garcia",
    "isIngreso": true,
    "status": "REGISTRADO",
    "recordedAt": "2026-10-12T08:15:00Z"
  }
  ```

### 4.2 Registrar Egreso (Paso a Paso - 7 preguntas)
* **Método:** `POST`
* **Ruta:** `/attendance/egreso`
* **Rol requerido:** `EDUCADORA`
* **Descripción:** Registra el formulario secuencial de salida.
* **Request Body:**
  ```json
  {
    "childId": "mateo_garcia",
    "date": "12/10/2026",
    "exitTime": "16:30",
    "goodPhysicalCondition": true,
    "injuryDetail": "",
    "isClean": true,
    "backpackComplete": true,
    "observations": "Jornada tranquila, merendó completa su compota.",
    "receiverTutorName": "María López Fernández",
    "receiverTutorSignatureData": "data:image/png;base64,iVBORw0KGgoAAA...",
    "delivererStaffName": "María Rodríguez H.",
    "delivererStaffSignatureData": "data:image/png;base64,iVBORw0KGgoAAA..."
  }
  ```
* **Response (201 Created):**
  ```json
  {
    "id": "att_1728753000000",
    "childId": "mateo_garcia",
    "isIngreso": false,
    "status": "REGISTRADO",
    "recordedAt": "2026-10-12T16:30:00Z"
  }
  ```

### 4.3 Consultar Registros de Asistencia
* **Método:** `GET`
* **Ruta:** `/attendance/records`
* **Rol requerido:** Ambos
* **Query Params:**
  - `childId`: ID del alumno (requerido para familiares).
  - `date`: `dd/MM/yyyy` (opcional).
  - `startDate`, `endDate`: Rango de fechas (opcional).
* **Response (200 OK):**
  ```json
  [
    {
      "id": "att_1",
      "childId": "mateo_garcia",
      "isIngreso": true,
      "date": "12/10/2026",
      "dayOfWeek": "L",
      "time": "08:15 AM",
      "observations": "Ingreso normal"
    }
  ]
  ```

---

## Módulo 5: Bitácora Diaria, Línea de Tiempo y Actividades (`/bitacora`, `/activities`)

### 5.1 Obtener Línea de Tiempo del Día
* **Método:** `GET`
* **Ruta:** `/children/{childId}/timeline`
* **Rol requerido:** Ambos
* **Query Params:** `date` (opcional, por defecto fecha de hoy).
* **Response (200 OK):**
  ```json
  [
    {
      "type": "EVENT_CHIP",
      "id": "chip_1",
      "time": "08:15 AM",
      "text": "Ingreso: 08:15 AM",
      "iconType": "entry"
    },
    {
      "type": "EVENT_CHIP",
      "id": "chip_2",
      "time": "10:15 AM",
      "text": "Estado: Feliz",
      "iconType": "mood"
    },
    {
      "type": "CHAT_MESSAGE",
      "id": "msg_1",
      "time": "11:30 AM",
      "message": "¡Buenos días! Muchas gracias por el aviso.",
      "isOutgoing": false,
      "fileUri": null,
      "fileName": null,
      "fileMimeType": null
    },
    {
      "type": "ACTIVITY_CARD",
      "id": "act_1",
      "time": "12:20 PM",
      "category": "Alimentación",
      "portionLabel": "Medio plato",
      "description": "Comió sopa de verduras con pollo."
    }
  ]
  ```

### 5.2 Obtener Historial de Bitácoras Diarias
* **Método:** `GET`
* **Ruta:** `/children/{childId}/bitacoras`
* **Rol requerido:** Ambos
* **Response (200 OK):**
  ```json
  [
    {
      "date": "29/09/2026",
      "dayLabel": "Hoy, 29 Sep",
      "isToday": true,
      "ingreso": {
        "time": "08:15 AM",
        "delivererName": "María López (Madre)",
        "receiverName": "Laura Gómez (Educadora)",
        "physicalCondition": "Buen estado general.",
        "hasSymptoms": false,
        "symptomsDetail": "",
        "observations": "Durmió 9 horas continuas."
      },
      "egreso": null,
      "activities": [
        {
          "id": "act_1",
          "time": "12:15 PM",
          "title": "Almuerzo - Menú del día",
          "category": "ALIMENTACION",
          "portion": "MEDIO",
          "description": "Tomó la crema de calabaza completa."
        }
      ]
    }
  ]
  ```

### 5.3 Registrar Nueva Actividad
* **Método:** `POST`
* **Ruta:** `/children/{childId}/activities`
* **Rol requerido:** `EDUCADORA`
* **Request Body:**
  ```json
  {
    "time": "12:30 PM",
    "category": "ALIMENTACION",
    "mealType": "COMIDA",
    "consumptionOption": "TODO",
    "portionOption": "TODO",
    "title": "Almuerzo - Lentejas con arroz",
    "description": "Comió todo el plato con gran apetito.",
    "napMinutes": null,
    "moodState": null,
    "excretoryFunction": null
  }
  ```
* **Response (201 Created):**
  ```json
  {
    "id": "act_101",
    "status": "CREADA",
    "timelineItem": {
      "type": "ACTIVITY_CARD",
      "id": "act_101",
      "time": "12:30 PM",
      "category": "Alimentación",
      "portionLabel": "Todo (1/1)",
      "description": "Comió todo el plato con gran apetito."
    }
  }
  ```

### 5.4 Registrar Evento Rápido en Línea de Tiempo
* **Método:** `POST`
* **Ruta:** `/children/{childId}/timeline/events`
* **Rol requerido:** `EDUCADORA`
* **Request Body:**
  ```json
  {
    "time": "01:15 PM",
    "text": "Siesta: 45 min",
    "iconType": "nap"
  }
  ```
* **Response (201 Created):** Objeto `EventChip`.

---

## Módulo 6: Resúmenes Semanales Pedagógicos (`/weekly-summaries`)

### 6.1 Listar Resúmenes Semanales del Alumno
* **Método:** `GET`
* **Ruta:** `/children/{childId}/weekly-summaries`
* **Rol requerido:** Ambos
* **Response (200 OK):**
  ```json
  [
    {
      "id": "week_2026_w39",
      "weekLabel": "Semana actual",
      "weekRangeText": "Semana del 22 al 26 de Septiembre, 2026",
      "attendanceDaysCount": 5,
      "totalSchoolDays": 5,
      "attendancePercentageText": "100%",
      "averageArrivalTime": "08:18 AM",
      "foodIntakePercentage": "92%",
      "totalNapHours": "6h 15m",
      "averageNapDaily": "1h 15m/día",
      "moodSummary": "Muy participativo y sociable",
      "executiveSummary": "Semana sumamente positiva para Mateo...",
      "foodHighlights": [
        "Gran aceptación en cremas de verduras",
        "Excelente autonomía con los cubiertos"
      ],
      "napHighlights": "Ritmo biológico de descanso muy estable.",
      "pedagogicalHighlights": [
        "Motricidad fina: Modelado con plastilina",
        "Sensorial: Taller de pintura con dedos"
      ],
      "healthAndSafetySummary": "Protocolo estricto de alergias cumplido al 100%."
    }
  ]
  ```

### 6.2 Obtener Resumen Semanal por ID
* **Método:** `GET`
* **Ruta:** `/children/{childId}/weekly-summaries/{summaryId}`
* **Rol requerido:** Ambos
* **Response (200 OK):** Objeto `WeeklySummary`.

---

## Módulo 7: Mensajería de Personal y Chat Global (`/staff`, `/chats`)

### 7.1 Listar Miembros del Personal Escolar
* **Método:** `GET`
* **Ruta:** `/staff/members`
* **Rol requerido:** `EDUCADORA`, `DIRECCION`
* **Response (200 OK):**
  ```json
  [
    {
      "id": "staff_1",
      "name": "Laura Méndez",
      "role": "Dirección General",
      "avatarInitials": "LM",
      "avatarBgColor": 4287505578,
      "isOnline": true
    },
    {
      "id": "staff_2",
      "name": "Carlos Ruiz",
      "role": "Educador Lactantes",
      "avatarInitials": "CR",
      "avatarBgColor": 4278364369,
      "isOnline": true
    }
  ]
  ```

### 7.2 Listar Chats entre Trabajadores
* **Método:** `GET`
* **Ruta:** `/chats/workers`
* **Rol requerido:** `EDUCADORA`, `DIRECCION`
* **Response (200 OK):**
  ```json
  [
    {
      "id": "wchat_1",
      "title": "Laura Méndez",
      "subtitle": "Dirección General • En línea",
      "lastMessage": "¿Cómo va la adaptación de Mateo García?",
      "lastMessageTime": "11:30 AM",
      "unreadCount": 1,
      "isGlobal": false,
      "isGroup": false,
      "avatarInitials": "LM",
      "avatarBgColor": 4287505578
    }
  ]
  ```

### 7.3 Obtener Mensajes de un Chat de Trabajadores
* **Método:** `GET`
* **Ruta:** `/chats/workers/{chatId}/messages`
* **Rol requerido:** `EDUCADORA`, `DIRECCION`
* **Response (200 OK):**
  ```json
  [
    {
      "id": "wm_1",
      "senderId": "staff_1",
      "senderName": "Laura Méndez",
      "message": "Hola, ¿llegó la autorización médica?",
      "time": "10:15 AM",
      "isOutgoing": false
    }
  ]
  ```

### 7.4 Enviar Mensaje en Chat de Trabajadores
* **Método:** `POST`
* **Ruta:** `/chats/workers/{chatId}/messages`
* **Rol requerido:** `EDUCADORA`, `DIRECCION`
* **Request Body:**
  ```json
  {
    "message": "Hola Laura, sí, la mamá la entregó a primera hora.",
    "fileUri": null,
    "fileName": null,
    "fileMimeType": null
  }
  ```
* **Response (201 Created):** Objeto `WorkerChatMessage`.

### 7.5 Obtener Mensajes del Chat Global del Personal
* **Método:** `GET`
* **Ruta:** `/chats/staff-global/messages`
* **Rol requerido:** `EDUCADORA`, `DIRECCION`
* **Response (200 OK):**
  ```json
  {
    "id": "global_staff_chat",
    "title": "Chat Global de Trabajadores",
    "membersCount": 18,
    "messages": [
      {
        "id": "gm_1",
        "senderId": "staff_1",
        "senderName": "Laura Méndez",
        "message": "Buenos días equipo. Hoy reunión rápida a las 16:30.",
        "time": "09:00 AM",
        "isOutgoing": false
      }
    ]
  }
  ```

### 7.6 Enviar Mensaje al Chat Global del Personal
* **Método:** `POST`
* **Ruta:** `/chats/staff-global/messages`
* **Rol requerido:** `EDUCADORA`, `DIRECCION`
* **Request Body:**
  ```json
  {
    "message": "Entendido Laura, Sala 1A tendrá listos los informes."
  }
  ```
* **Response (201 Created):** Objeto `WorkerChatMessage`.

---

## Módulo 8: Chat Bitácora Individual (`/chats/bitacora`)

### 8.1 Listar Chats con Familias de una Sala
* **Método:** `GET`
* **Ruta:** `/rooms/{roomId}/parent-chats`
* **Rol requerido:** `EDUCADORA`
* **Descripción:** Bandeja de conversaciones directas para la educadora.
* **Response (200 OK):**
  ```json
  [
    {
      "childId": "mateo_garcia",
      "childName": "Mateo García",
      "parentName": "María López (Mamá)",
      "lastMessage": "Mateo disfrutó mucho su comida de hoy.",
      "lastMessageTime": "01:50 PM",
      "unreadCount": 0
    }
  ]
  ```

### 8.2 Obtener Mensajes de Chat de Bitácora
* **Método:** `GET`
* **Ruta:** `/chats/bitacora/{childId}/messages`
* **Rol requerido:** Ambos (autorizados)
* **Response (200 OK):**
  ```json
  [
    {
      "id": "msg_1",
      "senderId": "usr_familia_garcia",
      "senderName": "María López",
      "time": "11:30 AM",
      "message": "¡Buenos días! ¿Tomó su leche?",
      "isOutgoing": false,
      "fileUri": null
    }
  ]
  ```

### 8.3 Enviar Mensaje en Chat de Bitácora
* **Método:** `POST`
* **Ruta:** `/chats/bitacora/{childId}/messages`
* **Rol requerido:** Ambos (autorizados)
* **Request Body:**
  ```json
  {
    "message": "Sí, tomó toda su leche y acaba de despertar tranquilo.",
    "fileUri": "https://cdn.sonrisas.edu/media/foto_siesta.jpg",
    "fileName": "foto_siesta.jpg",
    "fileMimeType": "image/jpeg"
  }
  ```
* **Response (201 Created):** Objeto `ChatMessage`.

### 8.4 Eliminar Foto de Mensaje en Chat de Bitácora
* **Método:** `DELETE`
* **Ruta:** `/chats/bitacora/{childId}/messages/{messageId}/photo` (también disponible como `/chats/bitacora/{childId}/messages/{messageId}`)
* **Rol requerido:** Ambos (familias y personal autorizados)
* **Descripción:** Permite a las familias y educadoras eliminar una fotografía del chat. Si el mensaje tenía texto además de la foto, la foto se elimina pero se conserva el texto; si el mensaje solo contenía la foto, el mensaje completo se elimina.
* **Response (204 No Content)**

---

## Módulo 9: Grupos Familiares (`/family-groups`)

### 9.1 Listar Candidatos para Grupos Familiares
* **Método:** `GET`
* **Ruta:** `/family-groups/candidates`
* **Rol requerido:** `EDUCADORA`
* **Response (200 OK):**
  ```json
  [
    {
      "childId": "mateo_garcia",
      "childName": "Mateo García",
      "parentName": "María López (Mamá)",
      "avatarInitials": "MG",
      "avatarBgColor": 4294948685
    },
    {
      "childId": "sofia_lopez",
      "childName": "Sofía López",
      "parentName": "Marcos Rossi (Papá)",
      "avatarInitials": "SL",
      "avatarBgColor": 4286695300
    }
  ]
  ```

### 9.2 Listar Grupos Familiares del Usuario
* **Método:** `GET`
* **Ruta:** `/family-groups`
* **Rol requerido:** Ambos
* **Descripción:** Filtra automáticamente: la educadora ve los grupos que gestiona; la familia ve los grupos donde sus hijos son miembros.
* **Response (200 OK):**
  ```json
  [
    {
      "id": "fgroup_paseo_sala_1a",
      "title": "Paseo al parque",
      "memberChildIds": ["mateo_garcia", "sofia_lopez", "lucas_martinez"],
      "createdById": "staff_1",
      "avatarBgColor": 4278225275,
      "lastMessage": "¡Hola, familias! Creé este grupo para organizar el paseo.",
      "lastMessageTime": "09:00 AM"
    }
  ]
  ```

### 9.3 Crear Grupo Familiar
* **Método:** `POST`
* **Ruta:** `/family-groups`
* **Rol requerido:** `EDUCADORA`
* **Request Body:**
  ```json
  {
    "title": "Paseo al parque",
    "memberChildIds": ["mateo_garcia", "sofia_lopez", "lucas_martinez"]
  }
  ```
* **Response (201 Created):** Objeto `FamilyGroupChat`.

### 9.4 Obtener Mensajes de un Grupo Familiar
* **Método:** `GET`
* **Ruta:** `/family-groups/{groupId}/messages`
* **Rol requerido:** Ambos (miembros del grupo)
* **Response (200 OK):**
  ```json
  [
    {
      "id": "fgm_1",
      "senderId": "staff_1",
      "senderName": "Educadora Sala 1A",
      "text": "¡Hola, familias! Creé este grupo para organizar el paseo del viernes.",
      "time": "09:00 AM",
      "attachment": null
    }
  ]
  ```

### 9.5 Enviar Mensaje en un Grupo Familiar
* **Método:** `POST`
* **Ruta:** `/family-groups/{groupId}/messages`
* **Rol requerido:** Ambos (miembros del grupo)
* **Request Body:**
  ```json
  {
    "text": "Perfecto, ¿a qué hora salen?",
    "attachment": {
      "uri": "https://cdn.sonrisas.edu/media/itinerario.pdf",
      "name": "itinerario.pdf",
      "mimeType": "application/pdf"
    }
  }
  ```
* **Response (201 Created):** Objeto `FamilyGroupMessage`.
* **Errores:** `403 sin_acceso_grupo` si una familia escribe o lee un grupo donde no está ninguno de sus hijos;
  `422 mensaje_vacio` si no hay texto ni adjunto. Para adjuntar una foto o archivo, primero se sube con
  `POST /media/upload` y se manda la `url` que regresa (`/api/v1/media/{fileId}`).
* **Orden:** `GET /family-groups` regresa los grupos del más reciente al más antiguo según su último mensaje;
  `GET /family-groups/{groupId}/messages` regresa los mensajes del más viejo al más nuevo.

### 9.6 Eliminar Foto o Adjunto en un Grupo Familiar
* **Método:** `DELETE`
* **Ruta:** `/family-groups/{groupId}/messages/{messageId}/photo` (también disponible como `/family-groups/{groupId}/messages/{messageId}`)
* **Rol requerido:** Ambos (miembros del grupo)
* **Descripción:** Permite a las familias y educadoras miembros del grupo eliminar una fotografía o adjunto de un mensaje del grupo. Si el mensaje tenía texto además del adjunto, se elimina el adjunto y se conserva el texto; si el mensaje solo contenía el adjunto, se elimina el mensaje completo.
* **Response (204 No Content)**

---

## Módulo 10: Avisos y Circulares Generales (`/announcements`)

### 10.1 Listar Avisos y Circulares
* **Método:** `GET`
* **Ruta:** `/announcements`
* **Rol requerido:** Ambos
* **Response (200 OK):**
  ```json
  [
    {
      "id": "ann_1",
      "title": "Reunión de Padres y Madres - Inicio de Curso",
      "date": "Hoy, 10:00 AM",
      "summary": "Recordatorio sobre la reunión de este viernes en el salón principal.",
      "fullContent": "Estimadas familias,\n\nLes recordamos que este viernes 2 de Octubre a las 18:00h...",
      "isUnread": true,
      "author": "Dirección General"
    }
  ]
  ```

### 10.2 Obtener Contador de Avisos No Leídos
* **Método:** `GET`
* **Ruta:** `/announcements/unread-count`
* **Rol requerido:** Ambos
* **Response (200 OK):**
  ```json
  {
    "unreadCount": 2
  }
  ```

### 10.3 Marcar Aviso como Leído
* **Método:** `PATCH`
* **Ruta:** `/announcements/{id}/read`
* **Rol requerido:** Ambos
* **Response (200 OK):**
  ```json
  {
    "id": "ann_1",
    "isUnread": false
  }
  ```

### 10.4 Marcar Todos los Avisos como Leídos
* **Método:** `POST` o `PATCH`
* **Ruta:** `/announcements/mark-all-read` (o `/announcements/read-all`)
* **Rol requerido:** Ambos
* **Response (200 OK):**
  ```json
  {
    "unreadCount": 0
  }
  ```

### 10.5 Publicar Nuevo Aviso
* **Método:** `POST`
* **Ruta:** `/announcements`
* **Rol requerido:** `DIRECCION`
* **Request Body:**
  ```json
  {
    "title": "Campaña de Vacunación contra la Gripe",
    "summary": "Información sobre la próxima campaña de salud.",
    "fullContent": "Estimados padres, en colaboración con el centro de salud...",
    "author": "Servicio Médico"
  }
  ```
* **Response (201 Created):** Objeto `Announcement`.

---

## Módulo 11: Buzón de Sugerencias y Quejas (`/suggestions`)

### 11.1 Listar Sugerencias
* **Método:** `GET`
* **Ruta:** `/suggestions`
* **Rol requerido:** Ambos
* **Query Params:**
  - `status`: `PENDIENTE`, `EN_REVISION`, `ATENDIDA` (opcional).
  - `category`: `ALIMENTACION`, `INSTALACIONES`, `ACTIVIDADES`, `HORARIOS`, `GENERAL` (opcional).
  - `search`: Texto de búsqueda (opcional).
* **Response (200 OK):**
  ```json
  [
    {
      "id": "sug_1",
      "parentName": "Elena Ramos",
      "childName": "Mateo García",
      "roomName": "Sala 1A",
      "date": "12/10/2026",
      "time": "09:30 AM",
      "category": "ALIMENTACION",
      "subject": "Variedad de frutas frescas en merienda matutina",
      "content": "Nos gustaría proponer mayor variedad de frutas de temporada...",
      "status": "PENDIENTE",
      "response": null,
      "responseDate": null,
      "responderName": null
    }
  ]
  ```

### 11.2 Crear Nueva Sugerencia
* **Método:** `POST`
* **Ruta:** `/suggestions`
* **Rol requerido:** `FAMILIAR`
* **Request Body:**
  ```json
  {
    "childId": "mateo_garcia",
    "roomName": "Sala 1A",
    "category": "ALIMENTACION",
    "subject": "Variedad de frutas frescas en merienda matutina",
    "content": "Nos gustaría proponer si es posible incluir mayor variedad de frutas de temporada..."
  }
  ```
* **Response (201 Created):** Objeto `ParentSuggestion`.

### 11.3 Responder y Cambiar Estado de Sugerencia
* **Método:** `PATCH`
* **Ruta:** `/suggestions/{id}/status`
* **Rol requerido:** `EDUCADORA`, `DIRECCION`
* **Request Body:**
  ```json
  {
    "status": "ATENDIDA",
    "response": "Muchas gracias por la observación. Ya coordinamos con el equipo de nutrición para incluirlas.",
    "responderName": "Laura Méndez (Dirección)"
  }
  ```
* **Response (200 OK):** Objeto `ParentSuggestion` actualizado.

---

## Módulo 12: Catálogo de Objetos Perdidos (`/lost-objects`)

### 12.1 Listar Objetos Perdidos
* **Método:** `GET`
* **Ruta:** `/lost-objects`
* **Rol requerido:** Ambos
* **Query Params:**
  - `category`: `Todos`, `Ropa`, `Juguetes`, `Útiles`, `Otros`.
  - `isClaimed`: `true` o `false` (opcional).
  - `search`: Texto de búsqueda.
* **Response (200 OK):**
  ```json
  [
    {
      "id": "lost_1",
      "title": "Chaqueta Azul",
      "description": "Chaqueta de invierno infantil marca geoxrosa.",
      "category": "Ropa",
      "location": "Zona de recreo",
      "imageUri": "https://cdn.sonrisas.edu/lost/chaqueta_azul.jpg",
      "room": "Sala 1A",
      "isClaimed": false
    }
  ]
  ```

### 12.2 Publicar Objeto Perdido
* **Método:** `POST`
* **Ruta:** `/lost-objects`
* **Rol requerido:** `EDUCADORA`, `DIRECCION`
* **Content-Type:** `multipart/form-data` o `application/json` (con URL de media previa):
* **Request Body:**
  ```json
  {
    "title": "Botella de Agua Roja",
    "description": "Botella de plástico con tapa deportiva.",
    "category": "Otros",
    "location": "Comedor",
    "room": "Sala 1A",
    "imageUri": "https://cdn.sonrisas.edu/lost/botella_roja.jpg"
  }
  ```
* **Response (201 Created):** Objeto `LostItem`.

### 12.3 Reclamar Objeto Perdido
* **Método:** `PATCH`
* **Ruta:** `/lost-objects/{id}/claim`
* **Rol requerido:** `FAMILIAR`, `EDUCADORA`
* **Request Body (Opcional):**
  ```json
  {
    "childName": "Mateo García"
  }
  ```
* **Response (200 OK):**
  ```json
  {
    "id": "lost_1",
    "isClaimed": true,
    "claimedByName": "Mateo García"
  }
  ```

### 12.4 Eliminar Objeto Perdido (Entrega física completada)
* **Método:** `DELETE`
* **Ruta:** `/lost-objects/{id}`
* **Rol requerido:** `EDUCADORA`, `DIRECCION`
* **Descripción:** Se ejecuta desde el rol de trabajador cuando el objeto ha sido reclamado y entregado en persona en la estancia.
* **Response (204 No Content)**

---

## Módulo 13: Almacenamiento y Subida Multimedia (`/media`)

### 13.1 Subir Archivo Binario
* **Método:** `POST`
* **Ruta:** `/media/upload`
* **Rol requerido:** Autenticado
* **Content-Type:** `multipart/form-data`
* **Form-data:**
  - `file`: Archivo binario (JPEG, PNG, WEBP, PDF).
  - `folder`: Subcarpeta de destino (`chat`, `certificates`, `lost_objects`, `signatures`).
* **Response (201 Created):**
  ```json
  {
    "fileId": "media_982341",
    "url": "https://cdn.sonrisas.edu/media/chat/img_20261012.jpg",
    "fileName": "img_20261012.jpg",
    "mimeType": "image/jpeg",
    "sizeBytes": 204850
  }
  ```

### 13.2 Descargar o Visualizar Archivo
* **Método:** `GET`
* **Ruta:** `/media/{fileId}`
* **Rol requerido:** Autenticado
* **Response (200 OK):** Flujo de bytes con cabecera `Content-Type` correspondiente.

---

## Módulo 14: Notificaciones Push y Tiempo Real (`/ws`, `/notifications`)

### 14.1 Registrar Token de Notificaciones Push (FCM)
* **Método:** `POST`
* **Ruta:** `/notifications/device-token`
* **Rol requerido:** Autenticado
* **Request Body:**
  ```json
  {
    "deviceToken": "fcm_token_device_abc123...",
    "platform": "ANDROID"
  }
  ```
* **Response (200 OK)**

### 14.2 Conexión WebSocket en Tiempo Real
* **Protocolo:** `WSS`
* **Ruta:** `/ws/v1/connect?token={jwt}`
* **Rol requerido:** Autenticado
* **Eventos Emitidos por el Servidor:**
  - `chat:message:new`: Notificación inmediata de mensaje en chat bitácora o chat trabajador.
  - `group:message:new`: Nuevo mensaje en grupo familiar.
  - `attendance:updated`: Actualización en vivo de asistencia de un menor.
  - `announcement:new`: Nuevo aviso escolar publicado.
* **Payload de Ejemplo (`group:message:new`):**
  ```json
  {
    "event": "group:message:new",
    "data": {
      "groupId": "fgroup_paseo_sala_1a",
      "message": {
        "id": "fgm_10",
        "senderId": "usr_familia_garcia",
        "senderName": "María López",
        "text": "¡Perfecto, muchas gracias!",
        "time": "09:35 AM"
      }
    }
  }
  ```

---

## Módulo 15: Observaciones Semanales (`/weekly-observations`)

Comentarios manuales de la familia o de la educadora en el resumen semanal de un niño.
El rol del autor (`FAMILIA` o `EDUCADORA`) lo decide el servidor según el token.
Una familia solo puede leer y escribir observaciones de sus propios hijos (`403 sin_acceso_alumno`).

### 15.1 Listar Observaciones Visibles
* **Método:** `GET`
* **Ruta:** `/weekly-observations?weekKey={opcional}`
* **Rol requerido:** Autenticado (personal: todas; familia: las de sus hijos)
* **Response (200 OK):**
  ```json
  [
    {
      "id": "wobs_1",
      "childId": "mateo_garcia",
      "weekKey": "Semana del 15 al 19 de Septiembre, 2026",
      "authorRole": "FAMILIA",
      "authorName": "Familia García",
      "text": "En casa también lo notamos más tranquilo para dormir.",
      "time": "19/09 07:40 PM"
    }
  ]
  ```

### 15.2 Listar Observaciones de un Alumno
* **Método:** `GET`
* **Ruta:** `/children/{childId}/weekly-observations?weekKey={opcional}`

### 15.3 Agregar Observación
* **Método:** `POST`
* **Ruta:** `/children/{childId}/weekly-observations`
* **Request Body:** `{ "weekKey": "Semana del 15 al 19 de Septiembre, 2026", "text": "..." }`
* **Response (201 Created):** Objeto `WeeklyObservation`.
* **Errores:** `422 observacion_vacia`, `422 observacion_larga` (máximo 500 caracteres).

---

## Módulo 16: Planeación y Menú Diario (`/daily-plans`)

La educadora publica el menú y las actividades del día; las familias lo consultan. Fechas en formato `YYYY-MM-DD`.

### 16.1 Listar Planeaciones
* **Método:** `GET`
* **Ruta:** `/daily-plans?from=2026-10-12&to=2026-10-16&roomId=sala_1a`
* **Rol requerido:** Autenticado
* **Response (200 OK):**
  ```json
  [
    {
      "date": "2026-10-12",
      "roomId": "sala_1a",
      "menu": {
        "breakfast": "Avena con plátano y leche",
        "snack": "Gajos de mandarina",
        "lunch": "Crema de calabaza, arroz con pollo y agua de jamaica",
        "notes": "Mateo García: menú sin nuez."
      },
      "activities": [{ "time": "09:00", "title": "Asamblea de bienvenida y canciones" }],
      "updatedBy": "Educadora Sala 1A"
    }
  ]
  ```

### 16.2 Obtener Planeación de un Día
* **Método:** `GET`
* **Ruta:** `/daily-plans/{fecha}` — `404 sin_planeacion` si no se ha publicado.

### 16.3 Publicar o Reemplazar Planeación
* **Método:** `PUT`
* **Ruta:** `/daily-plans/{fecha}`
* **Rol requerido:** `EDUCADORA` / `DIRECCION`
* **Request Body:** `{ "menu": {...}, "activities": [{ "time": "09:00", "title": "..." }], "roomId": "sala_1a" }`
* **Validación:** desayuno, colación y comida obligatorios; de 1 a 20 actividades con hora `HH:mm` (24 h).
  Las actividades se guardan ordenadas por hora.

---

## Módulo 17: Registro Diario / Bitácora de Filtro Diario (`/daily-logs`)

Fuente de la hoja oficial que se exporta en PDF. Cada día de cada niño tiene tres apartados que se guardan
por separado: **II. Ingreso**, **III. Estadía** y **IV. Egreso**. Los nombres de los campos son los mismos
que en la app (`model/DailyLog.kt`) y las opciones van con el nombre del enum:
`FoodConsumptionOption` (`NADA`, `POCO`, `TODO`, `PIDIO_MAS`), `MoodState` (`FELIZ`, `TRISTE`, `ENOJADO`),
`SphincterControl` (`NO_APLICA`, `AVISO_SI`, `AVISO_NO`), `MedicationPrescriptionOption` (`SI`, `NO`, `NO_APLICA`).

### 17.1 Listar Registros Visibles
* **Método:** `GET`
* **Ruta:** `/daily-logs?from=YYYY-MM-DD&to=YYYY-MM-DD` (por defecto, los últimos 28 días)
* **Rol requerido:** Autenticado (personal: todos; familia: sus hijos)
* **Response (200 OK):**
  ```json
  [
    {
      "childId": "mateo_garcia",
      "date": "2026-10-12",
      "ingreso": { "time": "08:10", "assistantName": "Laura Gómez", "deliveredBy": "Mamá", "isSick": false },
      "estadia": { "breakfast": "TODO", "slept": true, "napMinutes": 60, "mood": "FELIZ" },
      "egreso": { "time": "15:00", "collectedBy": "Mamá", "deliveredByStaff": "Laura Gómez" }
    }
  ]
  ```

### 17.2 Listar Registros de un Alumno
* **Método:** `GET`
* **Ruta:** `/children/{childId}/daily-logs?from=&to=`

### 17.3 Guardar un Apartado del Día
* **Métodos:** `PUT`
* **Rutas:**
  - `/children/{childId}/daily-logs/{fecha}/ingreso`
  - `/children/{childId}/daily-logs/{fecha}/estadia`
  - `/children/{childId}/daily-logs/{fecha}/egreso`
* **Rol requerido:** `EDUCADORA` / `DIRECCION`
* **Request Body:** el objeto `IngresoLog`, `EstadiaLog` o `EgresoLog` completo (reemplaza al anterior de ese día).
* **Response (200 OK):** el registro completo del día.
* **Errores:** `422 opcion_invalida`, `422 salida_antes_de_entrada`, `422` si la hora no es `HH:mm` o si viene un campo desconocido.

