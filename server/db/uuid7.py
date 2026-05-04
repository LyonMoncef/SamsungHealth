"""UUID v7 helper + SQLAlchemy TypeDecorator (spec V2.1 postgres-migration)."""
import uuid as _uuid

import uuid_utils
from sqlalchemy import CHAR
from sqlalchemy.dialects.postgresql import UUID as PG_UUID
from sqlalchemy.types import TypeDecorator


def uuid7() -> _uuid.UUID:
    return _uuid.UUID(bytes=uuid_utils.uuid7().bytes)


class Uuid7(TypeDecorator):
    impl = CHAR
    cache_ok = True

    def load_dialect_impl(self, dialect):
        if dialect.name == "postgresql":
            return dialect.type_descriptor(PG_UUID(as_uuid=True))
        return dialect.type_descriptor(CHAR(36))

    def process_bind_param(self, value, dialect):
        if value is None:
            return None
        if isinstance(value, _uuid.UUID):
            return value if dialect.name == "postgresql" else str(value)
        return _uuid.UUID(value) if dialect.name == "postgresql" else str(_uuid.UUID(value))

    def process_result_value(self, value, dialect):
        if value is None:
            return None
        return value if isinstance(value, _uuid.UUID) else _uuid.UUID(value)
