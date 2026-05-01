import os
import tempfile
import pytest
from fastapi.testclient import TestClient

# Point DB to a temp file before importing the app
_tmp = tempfile.NamedTemporaryFile(suffix=".db", delete=False)
_tmp.close()
os.environ["HEALTH_TEST_DB"] = _tmp.name

import server.database as db_module
db_module.DB_PATH = type(db_module.DB_PATH)(_tmp.name)

from server.main import app


@pytest.fixture(scope="session")
def client():
    with TestClient(app) as c:
        yield c


@pytest.fixture(autouse=True)
def clean_db():
    db_module.init_db()
    yield
    conn = db_module.get_connection()
    conn.execute("DELETE FROM sleep_stages")
    conn.execute("DELETE FROM sleep_sessions")
    conn.execute("DELETE FROM steps_hourly")
    conn.execute("DELETE FROM heart_rate_hourly")
    conn.execute("DELETE FROM exercise_sessions")
    conn.commit()
    conn.close()
