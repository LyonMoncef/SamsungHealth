import pytest


@pytest.fixture(autouse=True)
def clean_db():
    yield
