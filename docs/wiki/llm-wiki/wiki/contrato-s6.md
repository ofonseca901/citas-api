---
title: Contrato S6 - automatizaciones de citas
status: vigente
classification: HECHO
last_verified: 2026-09-30
---

# Contrato S6 - automatizaciones de citas

## Principios

Las rutas de automatización no sustituyen JWT de usuarios. Cada una requiere una credencial de servicio distinta configurada exclusivamente por variable de entorno. Ninguna respuesta incluye historia clínica, contraseñas, hashes, tokens ni datos reales del laboratorio.

## WF-001 - recordatorios

`GET /api/v1/automation/appointment-reminders?from=YYYY-MM-DD&to=YYYY-MM-DD`

- Cabecera: `X-Reminder-Token`.
- Solo devuelve citas `APPROVED` en una ventana futura de hasta siete días.
- Devuelve identificador de cita, inicio, sede, especialidad y destinatario sintético mínimo.
- `401` indica token ausente o inválido; `400`, rango no permitido; `404`, automatización deshabilitada.

## WF-002 - cambios de estado

Las transiciones elegibles crean un registro inmutable en `automation_outbox` dentro de la transacción de la cita. El despachador posterior al commit solo se habilita mediante `N8N_WEBHOOK_ENABLED=true` junto con URL y Bearer.

Payload mínimo: `schemaVersion`, `eventId`, `eventType`, `appointmentId`, `previousStatus`, `status`, `source` y `occurredAt`.

Se entregan decisiones administrativas y cancelaciones. La aprobación automática de una cita general no genera webhook. Los intentos se acotan a tres, no revierten la cita y dejan trazabilidad técnica sin secretos.

## WF-003 - resumen operativo

`GET /api/v1/automation/daily-summary?date=YYYY-MM-DD`

- Cabecera: `X-Summary-Token`.
- Devuelve exclusivamente agregados `location`, `status` y `total`.
- La fecha queda limitada a los últimos 31 días y el siguiente día.
- No muta citas ni expone identidad de pacientes.

## Estado de integración externa

Los JSON `WF-001`, `WF-002` y `WF-003` son plantillas sanitizadas e inactivas. La autenticación MCP, la credencial Gmail OAuth, la creación de credenciales en n8n Cloud, el túnel HTTPS y las ejecuciones Cloud no están verificadas en este repositorio. No deben marcarse como completadas hasta disponer de una conexión autorizada y evidencia de ejecución sintética.
