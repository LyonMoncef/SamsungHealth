.PHONY: dev test lint install ci-test ci-lint

## install : install Python dependencies
install:
	pip install -r requirements.txt -q
	pip install pytest pytest-asyncio httpx ruff -q

## dev : start the FastAPI server (reload + accessible from phone on LAN)
dev:
	uvicorn server.main:app --reload --host 0.0.0.0 --port 8000

## test : run the test suite
test:
	python3 -m pytest tests/ -v

## lint : run ruff
lint:
	ruff check server/ scripts/

## ci-test : CI entry point (install + test)
ci-test:
	pip install -r requirements.txt -q
	pip install pytest pytest-asyncio httpx ruff -q
	pytest tests/ -v

## ci-lint : CI entry point (install + lint)
ci-lint:
	pip install ruff -q
	ruff check server/ scripts/
