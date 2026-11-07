from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    database_url: str = "postgresql+psycopg://clinic:clinic@localhost:5432/auth_db"
    jwt_secret: str = "dev-secret-change-me-at-least-32-bytes!!"
    jwt_algorithm: str = "HS256"
    jwt_ttl_minutes: int = 60 * 24
    cookie_secure: bool = False  # True на проде (HTTPS)
    recp_url: str = "http://localhost:8002"
    internal_token: str = "dev-internal"


settings = Settings()
