.PHONY: setup infra backend dev test backend-test web-test reset-db

setup:
	powershell -NoProfile -ExecutionPolicy Bypass -File scripts/setup.ps1

infra:
	docker compose up -d postgres redis minio minio-init mailpit

backend:
	cd backend && ./mvnw spring-boot:run

dev:
	powershell -NoProfile -ExecutionPolicy Bypass -File scripts/dev.ps1

test:
	powershell -NoProfile -ExecutionPolicy Bypass -File scripts/test.ps1

backend-test:
	cd backend && ./mvnw test

web-test:
	pnpm --recursive --if-present test

reset-db:
	powershell -NoProfile -ExecutionPolicy Bypass -File scripts/reset-db.ps1
