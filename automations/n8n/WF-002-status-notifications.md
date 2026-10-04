# WF-002 - Notificacion por cambio de estado OscarF

**Trigger:** Webhook recibido desde `citas-api`.

**Instancia objetivo:** `https://impulso-n8n.aiacademy.com.co/`.

**Nombre del workflow en n8n:** `WF-002 Status notifications OscarF`.

Eventos minimos:

- cita especializada `APPROVED`/`REJECTED`;
- reprogramacion `APPROVED`/`REJECTED`;
- cancelacion.

## Requisitos

- validar payload minimo;
- ramificar por tipo/estado;
- Gmail con mensaje coherente;
- respuesta webhook deterministica;
- error/reintento/trazabilidad;
- secretos/credenciales fuera del JSON;
- configurar Gmail OAuth en n8n usando el callback autorizado `https://impulso-n8n.aiacademy.com.co/rest/oauth2-credential/callback`.

## Entregable

`WF-002-status-notifications.json`.
