from collections.abc import Callable

import jwt
from fastapi import Depends, Request
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy.orm import Session

from app.db import get_db
from app.errors import AppError
from app.models import User
from app.security import decode_access_token

COOKIE_NAME = "access_token"
_bearer = HTTPBearer(auto_error=False)  # даёт кнопку Authorize в Swagger


def _extract_token(request: Request, creds: HTTPAuthorizationCredentials | None) -> str | None:
    if creds is not None:
        return creds.credentials
    return request.cookies.get(COOKIE_NAME)


def get_current_user(
    request: Request,
    creds: HTTPAuthorizationCredentials | None = Depends(_bearer),
    db: Session = Depends(get_db),
) -> User:
    token = _extract_token(request, creds)
    if not token:
        raise AppError(401, "NOT_AUTHENTICATED", "Not authenticated")
    try:
        payload = decode_access_token(token)
        user_id = int(payload["sub"])
    except (jwt.PyJWTError, KeyError, ValueError):
        raise AppError(401, "INVALID_TOKEN", "Invalid or expired token")
    user = db.get(User, user_id)
    if user is None or not user.is_active:
        raise AppError(401, "INVALID_TOKEN", "Invalid or expired token")
    return user


def require_role(*roles: str) -> Callable[..., User]:
    def _dep(user: User = Depends(get_current_user)) -> User:
        if user.role not in roles:
            raise AppError(403, "FORBIDDEN", "Insufficient permissions")
        return user

    return _dep
