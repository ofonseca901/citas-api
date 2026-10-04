# WF-003 - Caso adicional: resumen operativo diario OscarF

**Uso:** actividad extra si el grupo avanza rapido.

**Trigger:** Schedule.

**Instancia objetivo:** `https://impulso-n8n.aiacademy.com.co/`.

**Nombre del workflow en n8n:** `WF-003 Daily operational summary OscarF`.

**Flujo:** API -> citas del dia -> agrupar por sede/estado -> construir resumen -> Gmail.

## Resultado esperado

Un correo de laboratorio con metricas simples:

- total por sede;
- `APPROVED`/`COMPLETED`/`NO_SHOW`/`CANCELLED`;
- incidencias de API si existen.

No requiere informacion privada real. La credencial Gmail OAuth se configura en n8n usando el callback autorizado `https://impulso-n8n.aiacademy.com.co/rest/oauth2-credential/callback`.
