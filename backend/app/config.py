from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Configuración del sistema leída desde variables de entorno o archivo .env."""

    model_config = SettingsConfigDict(
        env_file=(".env", "backend/.env"),
        env_file_encoding="utf-8",
        extra="ignore",
    )

    database_url: str = "postgresql+psycopg://postgres:postgres@localhost:5432/sonrisas"
    jwt_secret: str = "super-secret-jwt-key-for-sonrisas-app-tc2007b"
    codigo_profesor: str = "sonrisas-docente-2026"

    # Tiempos de vida del token (en segundos)
    access_ttl_s: int = 24 * 60 * 60  # 24 horas por defecto
    refresh_ttl_s: int = 7 * 24 * 60 * 60  # 7 días

    # Almacén de objetos compatible con S3 (RustFS / MinIO / AWS S3)
    s3_endpoint: str = "http://localhost:9000"
    s3_access_key: str = "sonrisas-admin"
    s3_secret_key: str = "sonrisas-secret-key-12345678"
    s3_bucket: str = "sonrisas"

    # Límite de tamaño para subidas multimedia (10 MB)
    media_max_bytes: int = 10 * 1024 * 1024

    # Notificaciones push: ruta al JSON de la cuenta de servicio de Firebase.
    # Vacío = no se mandan notificaciones (todo lo demás funciona igual).
    firebase_credentials: str = ""

    # Generación opcional de resúmenes con IA (Google Gemini u OpenAI)
    # Si están vacías, se utiliza el motor determinista tradicional sin costo ni llamadas externas.
    gemini_api_key: str = ""
    openai_api_key: str = ""

    # Implantación (ver docs/entrega-final.md, plan de implantación)
    # true  = desarrollo y demostración: se cargan las cuentas y niños de ejemplo (Password123!).
    # false = estancia real: NO se carga nada de ejemplo; solo la cuenta de Dirección y las salas de abajo.
    datos_demo: bool = True
    direccion_email: str = ""
    direccion_password: str = ""
    direccion_nombre: str = "Dirección"
    # Formato: "Sala 1A:Lactantes,Sala 2B:Maternales A"
    salas_iniciales: str = ""


settings = Settings()
