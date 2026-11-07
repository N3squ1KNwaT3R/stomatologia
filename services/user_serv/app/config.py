from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    database_url: str = "postgresql+psycopg://clinic:clinic@localhost:5432/user_db"
    # Должен совпадать с auth_serv — им проверяется подпись токена
    jwt_secret: str = "dev-secret-change-me-at-least-32-bytes!!"
    jwt_algorithm: str = "HS256"
    internal_token: str = "dev-internal"
    recp_url: str = "http://localhost:8002"


settings = Settings()
