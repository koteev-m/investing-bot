.PHONY: up down logs migrate test lint format

up:
	docker build -f Dockerfile.base -t investing-bot-base .
	docker-compose up -d

down:
	docker-compose down

logs:
	docker-compose logs -f

migrate:
	./gradlew :data:flywayMigrate

test:
	./gradlew test

lint:
	./gradlew detekt ktlintCheck

format:
	./gradlew ktlintFormat
