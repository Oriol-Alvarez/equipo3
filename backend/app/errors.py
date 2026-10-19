"""Manejador centralizado de errores con el formato estándar del curso:
    { "error": "mensaje amigable", "code": "codigo_error", "hint": "pista técnica" }
"""

import logging

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

logger = logging.getLogger(__name__)


class ApiError(Exception):
    def __init__(
        self,
        status: int,
        error: str,
        code: str | None = None,
        hint: str | None = None,
    ) -> None:
        super().__init__(error)
        self.status = status
        self.error = error
        self.code = code
        self.hint = hint


def instalar(app: FastAPI) -> None:
    @app.exception_handler(ApiError)
    async def api_error_handler(_: Request, e: ApiError) -> JSONResponse:
        cuerpo: dict[str, str] = {"error": e.error}
        if e.code:
            cuerpo["code"] = e.code
        if e.hint:
            cuerpo["hint"] = e.hint

        headers = {}
        if e.status == 401:
            headers["WWW-Authenticate"] = 'Bearer realm="sonrisas"'

        return JSONResponse(cuerpo, status_code=e.status, headers=headers)

    @app.exception_handler(RequestValidationError)
    async def validation_error_handler(_: Request, e: RequestValidationError) -> JSONResponse:
        errors = e.errors()
        if errors:
            primero = errors[0]
            campo = str(primero["loc"][-1]) if primero.get("loc") else "body"
            mensaje = str(primero.get("msg", "dato inválido")).removeprefix("Value error, ")
        else:
            campo = "body"
            mensaje = "Datos inválidos en la petición"

        return JSONResponse({"field": campo, "error": mensaje}, status_code=422)

    @app.exception_handler(ValueError)
    async def value_error_handler(_: Request, e: ValueError) -> JSONResponse:
        logger.warning(f"Validation error (ValueError): {e}")
        return JSONResponse(
            {"error": "Dato inválido proporcionado en la solicitud.", "code": "bad_request"},
            status_code=400
        )

    from sqlalchemy.exc import SQLAlchemyError
    @app.exception_handler(SQLAlchemyError)
    async def sqlalchemy_error_handler(_: Request, e: SQLAlchemyError) -> JSONResponse:
        logger.warning(f"Database error (SQLAlchemyError): {e}")
        return JSONResponse(
            {"error": "Conflicto o dato inválido para la base de datos.", "code": "db_error"},
            status_code=400
        )

    @app.exception_handler(Exception)
    async def global_exception_handler(_: Request, e: Exception) -> JSONResponse:
        logger.exception(f"Unhandled exception: {e}")
        return JSONResponse(
            {"message": "No se pudo procesar la solicitud debido a datos malformados o estado no válido.", "code": "request_error"},
            status_code=400
        )
