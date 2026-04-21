.PHONY: dev dev-mobile test lint install ci-test ci-lint

## install : install Python dependencies
install:
	pip install -r requirements.txt -q
	pip install pytest pytest-asyncio httpx ruff -q

## dev : start the FastAPI server (reload + accessible from phone on LAN)
dev:
	uvicorn server.main:app --reload --host 0.0.0.0 --port 8001

## dev-mobile : instructions pour tester depuis le téléphone (WSL2 port forwarding)
dev-mobile:
	@echo "" ;\
	echo "=== Test depuis téléphone (WSL2) ===" ;\
	echo "" ;\
	echo "1. Sur Windows (PowerShell Admin) :" ;\
	echo "   PowerShell -ExecutionPolicy Bypass -File '\\\\wsl.localhost\\Ubuntu\\home\\tats\\MyPersonalProjects\\SamsungHealth\\scripts\\dev-mobile.ps1'" ;\
	echo "" ;\
	echo "2. Dans WSL2, lancer le serveur :" ;\
	echo "   make dev" ;\
	echo "" ;\
	echo "3. Le script affiche l'URL à entrer dans l'app Android -> Settings -> Backend URL" ;\
	echo ""

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
