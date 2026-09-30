package co.fcv.citas.adapter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Best-effort, post-commit delivery. Failed events remain retryable and never roll back appointment state. */
@Component
public class N8nOutboxDispatcher {
  private final JdbcTemplate jdbc; private final ObjectMapper json; private final boolean enabled; private final String url; private final String bearer;
  public N8nOutboxDispatcher(JdbcTemplate jdbc,ObjectMapper json,@Value("${app.automation.webhook-enabled:false}") boolean enabled,@Value("${app.automation.webhook-url:}") String url,@Value("${app.automation.webhook-bearer-token:}") String bearer){this.jdbc=jdbc;this.json=json;this.enabled=enabled;this.url=url;this.bearer=bearer;}
  @PostConstruct void validate(){if(enabled&&(url.isBlank()||bearer.isBlank()))throw new IllegalStateException("N8N webhook habilitado sin URL o credencial.");}
  @Scheduled(fixedDelayString="${app.automation.webhook-poll-ms:30000}") public void deliver(){if(!enabled)return;for(Map<String,Object> row:jdbc.queryForList("select id,event_id,event_type,appointment_id,previous_status,status,source,occurred_at from automation_outbox where delivery_status='PENDING' and attempts<3 order by id limit 20")){long id=((Number)row.get("id")).longValue();try{Map<String,Object> payload=new LinkedHashMap<>();payload.put("schemaVersion",1);payload.put("eventId",row.get("event_id"));payload.put("eventType",row.get("event_type"));payload.put("appointmentId",row.get("appointment_id"));payload.put("previousStatus",row.get("previous_status"));payload.put("status",row.get("status"));payload.put("source",row.get("source"));payload.put("occurredAt",((Timestamp)row.get("occurred_at")).toInstant().toString());RestClient.create().post().uri(url).header("Authorization","Bearer "+bearer).contentType(org.springframework.http.MediaType.APPLICATION_JSON).body(json.writeValueAsString(payload)).retrieve().toBodilessEntity();jdbc.update("update automation_outbox set delivery_status='DELIVERED',attempts=attempts+1,delivered_at=?,last_error=null where id=?",Timestamp.from(Instant.now()),id);}catch(Exception e){jdbc.update("update automation_outbox set attempts=attempts+1,last_error=? where id=?", "delivery_failed",id);}}
  }
}
