---
title: Evidencia S6 - automatizaciones locales
status: parcial
classification: HECHO
last_verified: 2026-10-04
---

# Evidencia S6 - automatizaciones locales

## Ejecutado localmente

- Migración V7 para outbox de automatización.
- Endpoints de recordatorios y resumen con credenciales de servicio separadas.
- Plantillas sanitizadas e inactivas para WF-002 y WF-003, además de WF-001.
- `mvn test` en el contenedor de API: 11 pruebas, 0 fallos, 0 errores.

## Pendiente de evidencia externa

- Autenticación MCP contra la instancia n8n del laboratorio.
- Inspección de credenciales existentes antes de crear una credencial `MCP Citas`.
- OAuth Gmail con privilegio mínimo de envío.
- Túnel HTTPS temporal y pruebas controladas de los tres workflows.
- Evidencia de deduplicación y recepción en el buzón de laboratorio.

No se considera que un workflow esté funcionando en Cloud hasta que estas acciones se ejecuten y documenten sin secretos.

## Verificacion 2026-10-04

- Los JSON `WF-001`, `WF-002` y `WF-003` son JSON valido y sus nombres incluyen `OscarF`.
- El outbox de `citas-api` registra `previousStatus` antes de publicar decisiones administrativas, reprogramaciones y cancelaciones.
- No hay herramienta MCP/n8n conectada en esta sesion para validar la instancia Cloud.
- El host de ejecucion no tiene `node`, `npm`, `java` ni `mvn` en PATH. Docker Desktop respondio `500 Internal Server Error` contra el pipe `dockerDesktopLinuxEngine`, por lo que no fue posible levantar ni verificar la pila desde esta sesion.
