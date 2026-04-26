"""
Tests RED — V2.0.5 structlog observability foundation : logging config.

Mappé sur frontmatter `tested_by:` du spec
docs/vault/specs/2026-04-26-v2-structlog-observability.md.

Classes:
- TestStructlogConfig
- TestLogFields

Cible:
- `from server.logging_config import configure_logging, get_logger`

Note RED: `server.logging_config` n'existe pas encore. `structlog` n'est pas
encore dans `requirements.txt` (V2.0.5 l'ajoute). L'import au top du module
doit faire échouer la collection / les tests jusqu'à l'implémentation : c'est
exactement le contrat RED de TDD.
"""
import json
import re

import pytest


class TestStructlogConfig:
    def test_logger_emits_json(self, capsys, monkeypatch):
        """
        given APP_ENV=prod (renderer JSON),
        when get_logger("test").info("hello", foo=42),
        then la ligne capturée stdout est du JSON valide avec
             clés timestamp/level/logger/event/foo.
        """
        # spec §Tests d'acceptation #1
        from server.logging_config import configure_logging, get_logger

        monkeypatch.setenv("APP_ENV", "prod")
        monkeypatch.setenv("LOG_LEVEL", "INFO")
        configure_logging()

        log = get_logger("test")
        log.info("hello", foo=42)

        captured = capsys.readouterr()
        line = (captured.out + captured.err).strip().splitlines()[-1]
        payload = json.loads(line)
        assert payload["event"] == "hello"
        assert payload["foo"] == 42
        assert "timestamp" in payload
        assert "level" in payload
        assert "logger" in payload

    def test_log_level_from_env(self, capsys, monkeypatch):
        """
        given LOG_LEVEL=WARNING,
        when on émet un info() puis un warning(),
        then 0 ligne info capturée, 1 ligne warning capturée.
        """
        # spec §Tests d'acceptation #5
        from server.logging_config import configure_logging, get_logger

        monkeypatch.setenv("APP_ENV", "prod")
        monkeypatch.setenv("LOG_LEVEL", "WARNING")
        configure_logging()

        log = get_logger("test.level")
        log.info("should_be_filtered")
        log.warning("should_pass")

        captured = capsys.readouterr()
        out = (captured.out + captured.err)
        assert "should_be_filtered" not in out
        assert "should_pass" in out

    def test_pretty_renderer_in_dev_mode(self, capsys, monkeypatch):
        """
        given APP_ENV=dev (ConsoleRenderer),
        when get_logger("test").info("hello"),
        then output n'est pas du JSON (pas d'objet `{...}` brut parsable)
             et contient le mot "hello".
        """
        # spec §Tests d'acceptation #6
        from server.logging_config import configure_logging, get_logger

        monkeypatch.setenv("APP_ENV", "dev")
        monkeypatch.setenv("LOG_LEVEL", "INFO")
        configure_logging()

        log = get_logger("test.dev")
        log.info("hello")

        captured = capsys.readouterr()
        out = (captured.out + captured.err)
        assert "hello" in out
        # ConsoleRenderer n'émet pas une ligne JSON parsable
        last_line = out.strip().splitlines()[-1]
        with pytest.raises((json.JSONDecodeError, ValueError)):
            json.loads(last_line)


class TestLogFields:
    def test_logger_includes_timestamp_iso8601(self, capsys, monkeypatch):
        """
        given APP_ENV=prod,
        when get_logger("t").info("e"),
        then la clé `timestamp` matche ^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d+Z$ (ISO8601 UTC).
        """
        # spec §Tests d'acceptation #2
        from server.logging_config import configure_logging, get_logger

        monkeypatch.setenv("APP_ENV", "prod")
        monkeypatch.setenv("LOG_LEVEL", "INFO")
        configure_logging()

        get_logger("t").info("e")

        captured = capsys.readouterr()
        line = (captured.out + captured.err).strip().splitlines()[-1]
        payload = json.loads(line)
        assert "timestamp" in payload
        assert re.match(
            r"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d+Z$",
            payload["timestamp"],
        ), f"timestamp ne matche pas ISO8601 UTC: {payload['timestamp']!r}"

    def test_logger_includes_level(self, capsys, monkeypatch):
        """
        given APP_ENV=prod,
        when get_logger("t").info("e"),
        then payload["level"] == "info" (lowercase string).
        """
        # spec §Tests d'acceptation #3
        from server.logging_config import configure_logging, get_logger

        monkeypatch.setenv("APP_ENV", "prod")
        monkeypatch.setenv("LOG_LEVEL", "INFO")
        configure_logging()

        get_logger("t.lvl").info("e")

        captured = capsys.readouterr()
        line = (captured.out + captured.err).strip().splitlines()[-1]
        payload = json.loads(line)
        assert payload["level"] == "info"

    def test_logger_includes_scope(self, capsys, monkeypatch):
        """
        given APP_ENV=prod,
        when get_logger("server.routers.sleep").info("e"),
        then payload["logger"] == "server.routers.sleep" (scope = nom du logger).
        """
        # spec §Tests d'acceptation #4
        from server.logging_config import configure_logging, get_logger

        monkeypatch.setenv("APP_ENV", "prod")
        monkeypatch.setenv("LOG_LEVEL", "INFO")
        configure_logging()

        get_logger("server.routers.sleep").info("e")

        captured = capsys.readouterr()
        line = (captured.out + captured.err).strip().splitlines()[-1]
        payload = json.loads(line)
        assert payload["logger"] == "server.routers.sleep"
