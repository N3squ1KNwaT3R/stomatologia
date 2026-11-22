"""Вызовы auth_serv -> recp_serv (см. docs/contracts.md, «Внутренние вызовы»)."""
import logging

import httpx

from app.config import settings
from app.models import User

log = logging.getLogger(__name__)


class RecpClient:
    def __init__(self, base_url: str, internal_token: str, timeout: float = 5.0):
        self.base_url = base_url.rstrip("/")
        self.internal_token = internal_token
        self.timeout = timeout

    def create_patient_card(self, user: User) -> bool:
        """Создать пустую карту пациента для нового аккаунта. Идемпотентно по user_id.

        Регистрацию не ломаем, если recp_serv недоступен: аккаунт уже создан,
        карту рецепция сможет завести вручную (или повторный вызов создаст её).
        Возвращает True, если карта создана / уже была.
        """
        body = {
            # Java-DTO (InternalCreatePatientRequest) без snake_case-настройки Jackson
            # читает camelCase; snake_case — по контракту. Лишние поля Spring игнорирует.
            "user_id": user.id,
            "userId": user.id,
            "first_name": user.first_name,
            "firstName": user.first_name,
            "last_name": user.last_name,
            "lastName": user.last_name,
            "email": user.email,
            "phone": user.phone,
        }
        try:
            r = httpx.post(
                f"{self.base_url}/internal/patients",
                json=body,
                headers={"X-Internal-Token": self.internal_token},
                timeout=self.timeout,
            )
            r.raise_for_status()
            return True
        except httpx.HTTPError as exc:
            log.error("recp_serv: не удалось создать карту пациента для user_id=%s: %s", user.id, exc)
            return False


def get_recp_client() -> RecpClient:
    return RecpClient(settings.recp_url, settings.internal_token)
