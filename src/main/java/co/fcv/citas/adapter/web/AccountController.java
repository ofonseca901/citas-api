package co.fcv.citas.adapter.web;

import co.fcv.citas.application.AuthPorts;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/** Account data that is intentionally separate from authentication responses. */
@RestController
@RequestMapping("/api/v1")
public class AccountController {
  private static final SecureRandom RANDOM = new SecureRandom();
  private final JdbcTemplate jdbc; private final AuthPorts.Passwords passwords; private final Clock clock;
  private final boolean mailboxEnabled;
  // Local-only transport: raw codes are memory-only and disappear on restart; persistence contains only a hash.
  private final Map<Long, String> localMailbox = new ConcurrentHashMap<>();
  public AccountController(JdbcTemplate jdbc, AuthPorts.Passwords passwords,
      @Value("${app.password-reset.mailbox-enabled:false}") boolean mailboxEnabled) {
    this.jdbc=jdbc; this.passwords=passwords; this.mailboxEnabled=mailboxEnabled; this.clock=Clock.system(ZoneId.of("America/Bogota"));
  }
  public record RecoveryRequest(@NotBlank @Email String email) {}
  public record ResetRequest(@NotBlank @Size(min=6,max=64) String code,@NotBlank @Size(min=8,max=72) String password) {}
  public record PhoneRequest(@NotBlank @Pattern(regexp="[+0-9 ()-]{7,25}") String phone) {}
  public record AffiliationRequest(@Positive long epsId,@Positive long planId,@NotBlank @Pattern(regexp="CONTRIBUTIVO|SUBSIDIADO|PARTICULAR") String regime) {}

  @PostMapping("/auth/password-recovery") @ResponseStatus(HttpStatus.ACCEPTED) @Transactional
  public void requestRecovery(@Valid @RequestBody RecoveryRequest request) {
    var rows=jdbc.queryForList("select id from app_users where email=? and active=true", request.email().strip().toLowerCase(Locale.ROOT));
    if(rows.isEmpty()) return; // deliberately indistinguishable to callers
    long user=((Number)rows.getFirst().get("id")).longValue(); String code=code(); Instant now=clock.instant();
    jdbc.update("update password_reset_tokens set consumed_at=? where user_id=? and consumed_at is null", Timestamp.from(now), user);
    jdbc.update("insert into password_reset_tokens(user_id,token_hash,expires_at,created_at) values(?,?,?,?)",user,hash(code),Timestamp.from(now.plus(Duration.ofMinutes(15))),Timestamp.from(now));
    if (mailboxEnabled) localMailbox.put(user, code);
  }
  @PostMapping("/auth/password-reset") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional
  public void reset(@Valid @RequestBody ResetRequest request) {
    int bytes=request.password().getBytes(StandardCharsets.UTF_8).length;
    if(bytes>72) throw invalid("La contraseña debe tener máximo 72 bytes UTF-8.");
    Instant now=clock.instant(); List<Map<String,Object>> tokens=jdbc.queryForList("select id,user_id from password_reset_tokens where token_hash=? and consumed_at is null and expires_at>?",hash(request.code()),Timestamp.from(now));
    if(tokens.isEmpty()) throw invalid("El código de recuperación no es válido o expiró.");
    long id=((Number)tokens.getFirst().get("id")).longValue(), user=((Number)tokens.getFirst().get("user_id")).longValue();
    jdbc.update("update password_reset_tokens set consumed_at=? where id=? and consumed_at is null",Timestamp.from(now),id);
    jdbc.update("update app_users set password_hash=? where id=?",passwords.hash(request.password()),user);
    jdbc.update("update auth_sessions set revoked=true where user_id=?",user);
    localMailbox.remove(user);
  }
  @GetMapping("/admin/password-reset-mailbox") @PreAuthorize("hasRole('ADMIN')")
  public List<Map<String,Object>> mailbox() {
    if(!mailboxEnabled) throw new ApiErrors.RequestFailure(404,"NOT_FOUND","El buzón local no está disponible.");
    return jdbc.queryForList("select t.id,t.user_id as userId,u.email,t.created_at as createdAt,t.expires_at as expiresAt from password_reset_tokens t join app_users u on u.id=t.user_id where t.consumed_at is null and t.expires_at>? order by t.created_at desc",Timestamp.from(clock.instant())).stream().map(row -> {
      Map<String,Object> result=new LinkedHashMap<>(row); result.put("code",localMailbox.get(((Number)row.get("userId")).longValue())); result.remove("userId"); return result;
    }).toList();
  }
  @GetMapping("/profile") public Map<String,Object> profile(@AuthenticationPrincipal Jwt jwt) {
    long user=user(jwt); List<Map<String,Object>> rows=jdbc.queryForList("select u.id,u.first_name as firstName,u.last_name as lastName,u.document_type as documentType,u.document_number as documentNumber,u.email,u.phone,group_concat(r.role_code order by r.role_code) roles from app_users u join user_roles r on r.user_id=u.id where u.id=? group by u.id",user);
    if(rows.isEmpty()) throw notFound("Usuario inexistente."); return rows.getFirst();
  }
  @PatchMapping("/profile") @ResponseStatus(HttpStatus.NO_CONTENT)
  public void phone(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody PhoneRequest request) { jdbc.update("update app_users set phone=? where id=?",request.phone().strip(),user(jwt)); }
  @GetMapping("/profile/affiliation") public ResponseEntity<Map<String,Object>> affiliation(@AuthenticationPrincipal Jwt jwt) {
    var rows=jdbc.queryForList("select a.user_id as userId,a.regime,p.id as planId,p.code as planCode,p.name as planName,e.id as epsId,e.code as epsCode,e.name as epsName from user_affiliations a join insurance_plans p on p.id=a.plan_id join eps e on e.id=p.eps_id where a.user_id=?",user(jwt));
    return rows.isEmpty()?ResponseEntity.noContent().build():ResponseEntity.ok(rows.getFirst());
  }
  @PutMapping("/profile/affiliation") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional
  public void affiliation(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody AffiliationRequest request) {
    Integer count=jdbc.queryForObject("select count(*) from insurance_plans p join eps e on e.id=p.eps_id where p.id=? and p.eps_id=? and p.active=true and e.active=true",Integer.class,request.planId(),request.epsId());
    if(count==null||count!=1) throw invalid("El plan no corresponde a una EPS activa.");
    jdbc.update("insert into user_affiliations(user_id,plan_id,regime,created_at) values(?,?,?,?) on duplicate key update plan_id=values(plan_id),regime=values(regime)",user(jwt),request.planId(),request.regime(),Timestamp.from(clock.instant()));
  }
  private long user(Jwt jwt){return Long.parseLong(jwt.getSubject());}
  private String code(){ return String.format("%08d",RANDOM.nextInt(100_000_000)); }
  private String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
  private ApiErrors.RequestFailure invalid(String message){return new ApiErrors.RequestFailure(400,"INVALID",message);} private ApiErrors.RequestFailure notFound(String message){return new ApiErrors.RequestFailure(404,"NOT_FOUND",message);}
}
