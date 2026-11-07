"""Проверка JWT, выданного auth_serv. В БД auth_serv не ходим — всё нужное лежит в токене.

Минус для MVP: деактивированный пользователь работает до истечения токена (~1 день).
"""
from collections.abc import Callable
from dataclasses import dataclass
from hmac import compare_digest

import jwt
from fastapi import Depends, Header, Request
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from app.config import settings
from app.errors import AppError

COOKIE_NAME = "access_token"
_bearer = HTTPBearer(auto_error=False)


@dataclass(frozen=True)
class CurrentUser:
    id: int
    role: str


def get_current_user(
    request: Request,
    creds: HTTPAuthorizationCredentials | None = Depends(_bearer),
) -> CurrentUser:
    token = creds.credentials if creds else request.cookies.get(COOKIE_NAME)
    if not token:
        raise AppError(401, "NOT_AUTHENTICATED", "Not authenticated")
    try:
        payload = jwt.decode(token, settings.jwt_secret, algorithms=[settings.jwt_algorithm])
        return CurrentUser(id=int(payload["sub"]), role=str(payload["role"]))
    except (jwt.PyJWTError, KeyError, ValueError):
        raise AppError(401, "INVALID_TOKEN", "Invalid or expired token")


def require_role(*roles: str) -> Callable[..., CurrentUser]:
    def _dep(user: CurrentUser = Depends(get_current_user)) -> CurrentUser:
        if user.role not in roles:
            raise AppError(403, "FORBIDDEN", "Insufficient permissions")
        return user

    return _dep


def require_internal(x_internal_token: str | None = Header(default=None)) -> None:
    """Для эндпоинтов /internal/* — вызовы только от других сервисов."""
    if not x_internal_token or not compare_digest(x_internal_token, settings.internal_token):
        raise AppError(403, "FORBIDDEN", "Internal endpoint")
