.PHONY: dev dev-mobile test lint install ci-test ci-lint security-install pentest

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

## security-install : install pentester toolchain (SAST/SCA/secrets)
security-install:
	pip install bandit pip-audit safety semgrep -q
	@echo ""
	@echo "✓ Installed: bandit, pip-audit, safety, semgrep"
	@echo "⚠ gitleaks not pip-installable — install via:"
	@echo "  brew install gitleaks                       # macOS"
	@echo "  apt install gitleaks                        # Debian/Ubuntu (>= 23.04)"
	@echo "  go install github.com/zricethezav/gitleaks/v8@latest   # any platform with Go"
	@echo ""

## pentest : escape hatch — run all SAST/SCA/secrets tools manually (prefer /pentest skill)
pentest:
	@echo "→ bandit (SAST)" ; bandit -r server/ scripts/ -q || true
	@echo "→ pip-audit (SCA)" ; pip-audit || true
	@echo "→ semgrep (SAST multi-rules)" ; semgrep --config=auto server/ scripts/ --quiet || true
	@command -v gitleaks > /dev/null && (echo "→ gitleaks (secrets)" ; gitleaks detect --no-banner) || echo "⚠ gitleaks not installed — skipped"
	@echo ""
	@echo "ℹ For structured findings + POC generation, use the /pentest skill instead."
