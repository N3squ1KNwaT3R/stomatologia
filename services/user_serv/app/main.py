from fastapi import Depends, FastAPI

from app.auth import CurrentUser, get_current_user
from app.errors import install_error_handlers

app = FastAPI(title="user_serv")
install_error_handlers(app)


@app.get("/health", tags=["health"])
def health() -> dict:
    return {"status": "ok"}


@app.get("/whoami", tags=["health"])
def whoami(user: CurrentUser = Depends(get_current_user)) -> CurrentUser:
    """Проверка, что токен от auth_serv принимается этим сервисом."""
    return user
