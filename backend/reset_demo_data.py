"""Script ejecutable para reiniciar la base de datos a los datos de demo."""

import sys
from pathlib import Path

backend_dir = Path(__file__).resolve().parent
if str(backend_dir) not in sys.path:
    sys.path.insert(0, str(backend_dir))

from app.reset_demo import reset_database

if __name__ == "__main__":
    reset_database()
