from fastapi import FastAPI

from app.errors import install_error_handlers
from app.router import router

app = FastAPI(title="auth_serv")
install_error_handlers(app)
app.include_router(router)


@app.get("/health", tags=["health"])
def health() -> dict:
    return {"status": "ok"}
