from fastapi import APIRouter, Depends, Response, status
from sqlalchemy import select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.config import settings
from app.db import get_db
from app.deps import COOKIE_NAME, get_current_user, require_role
from app.errors import AppError
from app.models import Role, User
from app.recp_client import RecpClient, get_recp_client
from app.schemas import ChangePasswordIn, LoginIn, RegisterIn, StaffCreateIn, TokenOut, UserOut
from app.security import create_access_token, hash_password, verify_password

router = APIRouter(prefix="/auth", tags=["auth"])


def _create_user(db: Session, data: RegisterIn, role: str) -> User:
    user = User(
        email=data.email,
        password_hash=hash_password(data.password),
        first_name=data.first_name,
        last_name=data.last_name,
        phone=data.phone,
        role=role,
    )
    db.add(user)
    try:
        db.commit()
    except IntegrityError:
        db.rollback()
        raise AppError(409, "EMAIL_TAKEN", "Email already registered")
    return user


@router.post("/register", response_model=UserOut, status_code=status.HTTP_201_CREATED)
def register(
    data: RegisterIn,
    db: Session = Depends(get_db),
    recp: RecpClient = Depends(get_recp_client),
) -> User:
    user = _create_user(db, data, Role.PATIENT)
    # Пустая карта пациента в recp_serv — docs/contracts.md, «Внутренние вызовы»
    recp.create_patient_card(user)
    return user


@router.post("/staff", response_model=UserOut, status_code=status.HTTP_201_CREATED)
def create_staff(
    data: StaffCreateIn,
    _: User = Depends(require_role(Role.ADMIN)),
    db: Session = Depends(get_db),
) -> User:
    """ADMIN создаёт аккаунт врача или рецепции. Профиль врача (кабинет, услуги) — в recp_serv: POST /doctors."""
    return _create_user(db, data, data.role)


@router.post("/change-password", status_code=status.HTTP_204_NO_CONTENT)
def change_password(
    data: ChangePasswordIn,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> None:
    if not verify_password(data.old_password, user.password_hash):
        raise AppError(400, "WRONG_PASSWORD", "Current password is incorrect")
    if data.old_password == data.new_password:
        raise AppError(400, "SAME_PASSWORD", "New password must differ from the current one")
    user.password_hash = hash_password(data.new_password)
    db.commit()


@router.post("/login", response_model=TokenOut)
def login(data: LoginIn, response: Response, db: Session = Depends(get_db)) -> TokenOut:
    user = db.scalar(select(User).where(User.email == data.email))
    ok = verify_password(data.password, user.password_hash if user else None)
    if not ok or user is None or not user.is_active:
        # Один и тот же ответ на «нет email» и «не тот пароль»
        raise AppError(401, "INVALID_CREDENTIALS", "Invalid email or password")
    token = create_access_token(user.id, user.role)
    response.set_cookie(
        COOKIE_NAME,
        token,
        httponly=True,
        samesite="lax",
        secure=settings.cookie_secure,
        max_age=settings.jwt_ttl_minutes * 60,
    )
    return TokenOut(access_token=token, user=UserOut.model_validate(user))


@router.post("/logout", status_code=status.HTTP_204_NO_CONTENT)
def logout(response: Response) -> None:
    response.delete_cookie(COOKIE_NAME)


@router.get("/me", response_model=UserOut)
def me(user: User = Depends(get_current_user)) -> User:
    return user


@router.get("/users", response_model=list[UserOut])
def list_users(
    _: User = Depends(require_role(Role.ADMIN)),
    db: Session = Depends(get_db),
) -> list[User]:
    return list(db.scalars(select(User).order_by(User.id)))
