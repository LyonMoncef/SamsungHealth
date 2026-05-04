import os
from functools import lru_cache

from sqlalchemy import Engine, create_engine
from sqlalchemy.orm import Session, sessionmaker

from server.logging_config import get_logger


_log = get_logger(__name__)

_DEFAULT_PG_URL = "postgresql+psycopg://samsung:samsung@localhost:5432/samsunghealth"


@lru_cache(maxsize=1)
def get_engine() -> Engine:
    url = os.environ.get("DATABASE_URL", _DEFAULT_PG_URL)
    return create_engine(url, future=True, pool_pre_ping=True)


@lru_cache(maxsize=1)
def _session_factory() -> sessionmaker:
    return sessionmaker(bind=get_engine(), expire_on_commit=False, future=True)


SessionLocal = _session_factory


def get_session() -> Session:
    return _session_factory()()
