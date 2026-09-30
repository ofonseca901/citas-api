---
title: Evidencia S4
classification: HECHO
last_verified: 2026-09-24
---

# Evidencia S4

## Mejora de paneles y directorio

- Docker API y web: `200` en health y home local.
- Frontend: lint y typecheck `PASS`; build Docker `PASS`.
- Directorio ADMIN: login y consulta verificados; 11 USER independientes y 6 profesionales sintéticos activos.

- Builder: migración V3, ciclo de vida, contrato `/api/v1`, UI React y Dockerfiles preparados.
- Frontend: `npm run lint` PASS; `npm run typecheck` PASS; `npm run build` PASS mediante ejecución permitida fuera del sandbox.
- Vitest: NO VERIFICABLE. El sandbox bloqueó `spawn EPERM` y la ejecución elevada fue rechazada.
- Backend Maven/Testcontainers/Docker: NO VERIFICABLE en este entorno; Maven no está instalado y Docker Desktop no permite acceso al daemon.
- No se registran passwords, tokens ni datos personales en esta evidencia.

## Loop propio — contrato ADMIN y calendario

- **Objetivo:** reconciliar el alta administrativa por rol y el selector de fecha de citas con el contrato REST actual.
- **Builder:** implementó `POST /api/v1/admin/users` con USER, ADMIN y PROFESSIONAL, asignaciones atómicas para profesionales y `GET /api/v1/availability/dates`.
- **Verifier:** `mvn -B -ntp verify` ejecutó 9 pruebas sin fallos, incluida la creación ADMIN/PROFESSIONAL y la disponibilidad pública por rango sobre Flyway V1–V5. `npm run typecheck` validó el cliente React con el calendario mensual.
- **Resultado:** PASS para este alcance. No declara completado el MVP S4 completo; recuperación, perfil, CRUD de catálogos y las pruebas de ciclo de vida pendientes conservan su estado hasta contar con evidencia propia.
