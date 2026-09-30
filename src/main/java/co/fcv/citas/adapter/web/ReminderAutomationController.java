package co.fcv.citas.adapter.web;

import jakarta.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

/** Narrow service-to-service read model for WF-001. It is not a user authentication endpoint. */
@RestController
@RequestMapping("/api/v1/automation")
public class ReminderAutomationController {
  private final JdbcTemplate jdbc; private final String token; private final Clock clock=Clock.system(ZoneId.of("America/Bogota"));
  public ReminderAutomationController(JdbcTemplate jdbc,@Value("${app.automation.reminder-token:}") String token){this.jdbc=jdbc;this.token=token;}
  public record Reminder(long appointmentId,String scheduledStartAt,String location,String specialty,String recipientEmail) {}
  @GetMapping("/appointment-reminders")
  public List<Reminder> reminders(@RequestHeader(value="X-Reminder-Token",required=false) String presented,@RequestParam @NotNull LocalDate from,@RequestParam @NotNull LocalDate to){
    if(token.isBlank()) throw new ApiErrors.RequestFailure(404,"NOT_FOUND","La automatización de recordatorios no está habilitada.");
    if(presented==null||!MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),presented.getBytes(StandardCharsets.UTF_8))) throw new ApiErrors.RequestFailure(401,"UNAUTHORIZED","Credencial de automatización inválida.");
    if(to.isBefore(from)||Duration.between(from.atStartOfDay(),to.plusDays(1).atStartOfDay()).toDays()>7||from.isBefore(LocalDate.now(clock))) throw new ApiErrors.RequestFailure(400,"INVALID","El rango debe ser futuro y de máximo siete días.");
    return jdbc.query("select a.id,a.scheduled_start_at,l.name,s.name,u.email from appointments a join locations l on l.id=a.location_id join specialties s on s.id=a.specialty_id join app_users u on u.id=a.patient_user_id where a.status='APPROVED' and a.scheduled_start_at>=? and a.scheduled_start_at<? order by a.scheduled_start_at",(rs,row)->new Reminder(rs.getLong(1),rs.getTimestamp(2).toLocalDateTime().toString(),rs.getString(3),rs.getString(4),rs.getString(5)),java.sql.Timestamp.valueOf(from.atStartOfDay()),java.sql.Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
  }
}
