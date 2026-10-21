"""Módulo para reiniciar la base de datos y dejar únicamente los datos de demostración para la presentación.

Funciona tanto en el contenedor Docker (PostgreSQL) como en SQLite local.
"""

from pathlib import Path
from sqlalchemy import text

from .db import Base, SessionLocal, engine
from .main import _aplicar_migraciones_compatibilidad
from .seed import seed_database, seed_modulos_heber, seed_salas_adicionales
from . import almacen


def reset_database() -> None:
    print(f"Reiniciando base de datos ({engine.url.render_as_string(hide_password=True)})...")

    # 1. Eliminar todas las tablas
    if engine.dialect.name == "postgresql":
        with engine.connect() as conn:
            conn.execute(text("DROP SCHEMA public CASCADE; CREATE SCHEMA public;"))
            conn.commit()
    else:
        Base.metadata.drop_all(bind=engine)
    print("✓ Tablas y esquemas antiguos eliminados.")

    # 2. Recrear tablas y aplicar compatibilidad
    Base.metadata.create_all(bind=engine)
    _aplicar_migraciones_compatibilidad()
    print("✓ Tablas creadas con esquema limpio.")

    # 3. Poblar datos demo
    with SessionLocal() as db:
        seed_database(db)
        seed_modulos_heber(db)
        seed_salas_adicionales(db)
    print("✓ Datos de demostración insertados con éxito.")

    # 4. Limpiar objetos multimedia en S3 / RustFS
    try:
        borrados = 0
        for clave, _ in list(almacen.listar()):
            almacen.borrar(clave)
            borrados += 1
        if borrados > 0:
            print(f"✓ {borrados} archivos multimedia de prueba eliminados del almacén de objetos.")
        else:
            print("✓ Almacén de objetos limpio.")
    except Exception as e:
        print(f"Aviso al limpiar almacén de objetos: {e}")

    # 5. Limpiar uploads temporales locales si existen
    backend_dir = Path(__file__).resolve().parent.parent
    uploads_dir = backend_dir / "uploads" / "chat"
    if uploads_dir.exists():
        eliminados = 0
        for f in uploads_dir.glob("*"):
            if f.is_file():
                f.unlink()
                eliminados += 1
        if eliminados > 0:
            print(f"✓ {eliminados} archivos temporales locales eliminados de uploads/chat.")


if __name__ == "__main__":
    reset_database()
