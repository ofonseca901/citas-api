# WF-001 - Recordatorio de citas proximas OscarF

**Trigger:** Schedule.

**Objetivo:** consultar citas `APPROVED` dentro de una ventana configurable, enviar Gmail al usuario ficticio/de laboratorio y registrar resultado.

**Instancia objetivo:** `https://impulso-n8n.aiacademy.com.co/`.

**Nombre del workflow en n8n:** `WF-001 Appointment Reminders OscarF`.

## Requisitos

- no enviar recordatorio a `CANCELLED`/`REJECTED`;
- evitar duplicado para la misma cita/ventana segun estrategia del workflow;
- manejar API no disponible;
- credenciales fuera del JSON;
- ejecucion de prueba controlada antes de activar;
- configurar Gmail OAuth en n8n usando el callback autorizado `https://impulso-n8n.aiacademy.com.co/rest/oauth2-credential/callback`.

## Entregable

`WF-001-appointment-reminders.json`.
