---
title: Contrato S5 - recordatorios y documentacion API
status: vigente
classification: HECHO
last_verified: 2026-09-30
---

# Contrato S5

## Documentacion

`GET /v3/api-docs` y `/swagger-ui/index.html` son publicos como documentacion. No otorgan acceso a operaciones protegidas.

## Recordatorios para WF-001

`GET /api/v1/automation/appointment-reminders?from=YYYY-MM-DD&to=YYYY-MM-DD`

- Requiere encabezado `X-Reminder-Token` con credencial de servicio externa.
- Solo devuelve citas `APPROVED` en un rango futuro de hasta siete dias.
- Cada elemento contiene `appointmentId`, `scheduledStartAt`, `location`, `specialty` y `recipientEmail` sintetico.
- `401` para credencial invalida, `400` para rango invalido y `404` si el servicio no se habilito por entorno.
- La operacion no altera citas, estados, slots ni historial.

La URL de despliegue y la credencial no se versionan. n8n Cloud se mantiene sin ejecutar hasta disponer de acceso HTTPS autorizado.
