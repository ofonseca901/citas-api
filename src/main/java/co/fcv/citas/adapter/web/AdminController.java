package co.fcv.citas.adapter.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import co.fcv.citas.application.AuthPorts;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
  private final JdbcTemplate jdbc; private final AuthPorts.Passwords passwords;
  public AdminController(JdbcTemplate jdbc, AuthPorts.Passwords passwords){this.jdbc=jdbc;this.passwords=passwords;}
  public record ProfessionalData(@NotBlank @Size(max=40) String professionalCode,@NotBlank @Size(max=80) String licenseNumber,@NotEmpty List<@Positive Long> specialtyIds,@Positive Long primarySpecialtyId,@NotEmpty List<@Positive Long> locationIds) {}
  public record UserRequest(@NotBlank @Size(max=100) String firstName,@NotBlank @Size(max=100) String lastName,@NotBlank @Pattern(regexp="CC|CE|TI|PA|PPT") String documentType,@NotBlank @Size(max=30) String documentNumber,@NotBlank @Email String email,@NotBlank @Size(max=25) String phone,@NotBlank @Size(min=8,max=72) String password,@Pattern(regexp="USER|PROFESSIONAL|ADMIN") String role,@Valid ProfessionalData professional){}
  public record ActiveRequest(boolean active){}
  public record EpsRequest(@NotBlank @Pattern(regexp="[A-Z0-9_]{3,40}") String code,@NotBlank @Size(max=150) String name) {}
  public record PlanRequest(@Positive long epsId,@NotBlank @Pattern(regexp="[A-Z0-9_]{3,40}") String code,@NotBlank @Size(max=150) String name) {}
  public record SpecialtyCatalogRequest(@NotBlank @Pattern(regexp="[A-Z0-9_]{3,50}") String code,@NotBlank @Size(max=150) String name,@Min(30) @Max(60) int durationMinutes,boolean general) {}
  @GetMapping("/users") public Map<String,Object> users(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){
    String like="%"+q.strip()+"%"; int offset=page*size;
    List<Map<String,Object>> items=jdbc.queryForList("select u.id,u.first_name as firstName,u.last_name as lastName,u.document_type as documentType,u.document_number as documentNumber,u.email,u.phone,u.active,u.created_at as createdAt, group_concat(r.role_code order by r.role_code) as roles from app_users u join user_roles r on r.user_id=u.id where (u.first_name like ? or u.last_name like ? or u.email like ? or u.document_number like ?) group by u.id order by u.last_name,u.first_name limit ? offset ?",like,like,like,like,size,offset);
    Integer total=jdbc.queryForObject("select count(*) from app_users where first_name like ? or last_name like ? or email like ? or document_number like ?",Integer.class,like,like,like,like);
    return Map.of("items",items,"total",Objects.requireNonNullElse(total,0),"page",page,"size",size);
  }
  @PostMapping("/users") @ResponseStatus(HttpStatus.CREATED) @Transactional public Map<String,Long> create(@Valid @RequestBody UserRequest r){
    String role = r.role() == null ? "USER" : r.role();
    if ("PROFESSIONAL".equals(role) && r.professional() == null) throw new ApiErrors.RequestFailure(400,"INVALID","El profesional requiere código, matrícula, especialidades y sedes.");
    if (!"PROFESSIONAL".equals(role) && r.professional() != null) throw new ApiErrors.RequestFailure(400,"INVALID","Los datos profesionales solo aplican a PROFESSIONAL.");
    try {
      jdbc.update("insert into app_users(first_name,last_name,document_type,document_number,email,phone,password_hash,active,created_at) values(?,?,?,?,?,?,?,?,?)",r.firstName().strip(),r.lastName().strip(),r.documentType(),r.documentNumber().strip(),r.email().strip().toLowerCase(Locale.ROOT),r.phone().strip(),passwords.hash(r.password()),true,Timestamp.from(Instant.now()));
      Long id=jdbc.queryForObject("select id from app_users where email=?",Long.class,r.email().strip().toLowerCase(Locale.ROOT));
      jdbc.update("insert into user_roles(user_id,role_code) values(?, ?)",id,role);
      if (r.professional() != null) createProfessional(id,r.professional());
      return Map.of("id",id);
    } catch(DataIntegrityViolationException e){ throw new ApiErrors.RequestFailure(409,"DUPLICATE","El correo, documento, código o matrícula ya existe."); }
  }
  private void createProfessional(long userId, ProfessionalData data) {
    if (!data.specialtyIds().contains(data.primarySpecialtyId()) || new HashSet<>(data.specialtyIds()).size() != data.specialtyIds().size() || new HashSet<>(data.locationIds()).size() != data.locationIds().size()) throw new ApiErrors.RequestFailure(400,"INVALID","Las asignaciones profesionales deben ser únicas e incluir una especialidad primaria.");
    if (jdbc.queryForObject("select count(*) from specialties where active=true and id in (" + String.join(",", Collections.nCopies(data.specialtyIds().size(), "?")) + ")",Integer.class,data.specialtyIds().toArray()) != data.specialtyIds().size()) throw new ApiErrors.RequestFailure(400,"INVALID","Todas las especialidades deben estar activas.");
    if (jdbc.queryForObject("select count(*) from locations where active=true and id in (" + String.join(",", Collections.nCopies(data.locationIds().size(), "?")) + ")",Integer.class,data.locationIds().toArray()) != data.locationIds().size()) throw new ApiErrors.RequestFailure(400,"INVALID","Todas las sedes deben estar activas.");
    jdbc.update("insert into professionals(user_id,professional_code,license_number,active) values(?,?,?,true)",userId,data.professionalCode().strip(),data.licenseNumber().strip());
    Long professionalId=jdbc.queryForObject("select id from professionals where user_id=?",Long.class,userId);
    for (Long specialtyId : data.specialtyIds()) jdbc.update("insert into professional_specialties(professional_id,specialty_id,primary_specialty) values(?,?,?)",professionalId,specialtyId,specialtyId.equals(data.primarySpecialtyId()));
    for (Long locationId : data.locationIds()) jdbc.update("insert into professional_locations(professional_id,location_id) values(?,?)",professionalId,locationId);
  }
  @PatchMapping("/users/{id}/active") @ResponseStatus(HttpStatus.NO_CONTENT) public void userActive(@PathVariable long id,@RequestBody ActiveRequest r){if(jdbc.update("update app_users set active=? where id=?",r.active(),id)==0)throw new ApiErrors.RequestFailure(404,"NOT_FOUND","Usuario inexistente.");}
  @GetMapping("/professionals") public List<Map<String,Object>> professionals(){return jdbc.queryForList("select p.id,p.professional_code as professionalCode,p.license_number as licenseNumber,p.active,u.id as userId,u.first_name as firstName,u.last_name as lastName,u.email,group_concat(distinct s.name order by s.name) as specialties,group_concat(distinct l.name order by l.name) as locations from professionals p join app_users u on u.id=p.user_id left join professional_specialties ps on ps.professional_id=p.id left join specialties s on s.id=ps.specialty_id left join professional_locations pl on pl.professional_id=p.id left join locations l on l.id=pl.location_id group by p.id order by u.last_name,u.first_name");}
  @PatchMapping("/professionals/{id}/active") @ResponseStatus(HttpStatus.NO_CONTENT) public void professionalActive(@PathVariable long id,@RequestBody ActiveRequest r){if(jdbc.update("update professionals set active=? where id=?",r.active(),id)==0)throw new ApiErrors.RequestFailure(404,"NOT_FOUND","Profesional inexistente.");}
  @GetMapping("/dashboard") public Map<String,Object> dashboard(){
    Integer users=jdbc.queryForObject("select count(*) from app_users where active=true and id in (select user_id from user_roles where role_code='USER')",Integer.class); Integer professionals=jdbc.queryForObject("select count(*) from professionals where active=true",Integer.class); Integer pending=jdbc.queryForObject("select count(*) from appointments where status='REQUESTED'",Integer.class);
    return Map.of("activeUsers",Objects.requireNonNullElse(users,0),"activeProfessionals",Objects.requireNonNullElse(professionals,0),"pendingRequests",Objects.requireNonNullElse(pending,0),"appointmentsByStatus",jdbc.queryForList("select status,count(*) as total from appointments group by status"),"occupationByLocation",jdbc.queryForList("select l.name,count(a.id) as total from locations l left join appointments a on a.location_id=l.id group by l.id,l.name order by l.name"));
  }
  @GetMapping("/catalogs/eps") public List<Map<String,Object>> eps(@RequestParam(defaultValue="false") boolean activeOnly){return jdbc.queryForList("select id,code,name,active from eps "+(activeOnly?"where active=true ":"")+"order by name");}
  @PostMapping("/catalogs/eps") @ResponseStatus(HttpStatus.CREATED) public Map<String,Long> eps(@Valid @RequestBody EpsRequest r){return Map.of("id",insert("insert into eps(code,name,active) values(?,?,true)",r.code().strip().toUpperCase(Locale.ROOT),r.name().strip()));}
  @PutMapping("/catalogs/eps/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void eps(@PathVariable long id,@Valid @RequestBody EpsRequest r){update("update eps set code=?,name=? where id=?",r.code().strip().toUpperCase(Locale.ROOT),r.name().strip(),id);}
  @PatchMapping("/catalogs/eps/{id}/active") @ResponseStatus(HttpStatus.NO_CONTENT) public void epsActive(@PathVariable long id,@RequestBody ActiveRequest r){update("update eps set active=? where id=?",r.active(),id);}
  @GetMapping("/catalogs/plans") public List<Map<String,Object>> plans(@RequestParam(required=false) Long epsId,@RequestParam(defaultValue="false") boolean activeOnly){String sql="select p.id,p.eps_id as epsId,e.name as epsName,p.code,p.name,p.active from insurance_plans p join eps e on e.id=p.eps_id where 1=1";List<Object> args=new ArrayList<>();if(epsId!=null){sql+=" and p.eps_id=?";args.add(epsId);}if(activeOnly)sql+=" and p.active=true and e.active=true";return jdbc.queryForList(sql+" order by e.name,p.name",args.toArray());}
  @PostMapping("/catalogs/plans") @ResponseStatus(HttpStatus.CREATED) public Map<String,Long> plan(@Valid @RequestBody PlanRequest r){if(!exists("select count(*) from eps where id=?",r.epsId()))throw new ApiErrors.RequestFailure(404,"NOT_FOUND","EPS inexistente.");return Map.of("id",insert("insert into insurance_plans(eps_id,code,name,active) values(?,?,?,true)",r.epsId(),r.code().strip().toUpperCase(Locale.ROOT),r.name().strip()));}
  @PutMapping("/catalogs/plans/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void plan(@PathVariable long id,@Valid @RequestBody PlanRequest r){if(!exists("select count(*) from eps where id=?",r.epsId()))throw new ApiErrors.RequestFailure(404,"NOT_FOUND","EPS inexistente.");update("update insurance_plans set eps_id=?,code=?,name=? where id=?",r.epsId(),r.code().strip().toUpperCase(Locale.ROOT),r.name().strip(),id);}
  @PatchMapping("/catalogs/plans/{id}/active") @ResponseStatus(HttpStatus.NO_CONTENT) public void planActive(@PathVariable long id,@RequestBody ActiveRequest r){update("update insurance_plans set active=? where id=?",r.active(),id);}
  @GetMapping("/catalogs/specialties") public List<Map<String,Object>> specialties(@RequestParam(defaultValue="false") boolean activeOnly){return jdbc.queryForList("select id,code,name,duration_minutes as durationMinutes,general,requires_approval as requiresApproval,active from specialties "+(activeOnly?"where active=true ":"")+"order by name");}
  @PostMapping("/catalogs/specialties") @ResponseStatus(HttpStatus.CREATED) public Map<String,Long> specialtyCatalog(@Valid @RequestBody SpecialtyCatalogRequest r){validateDuration(r.durationMinutes());return Map.of("id",insert("insert into specialties(code,name,duration_minutes,general,requires_approval,active) values(?,?,?,?,?,true)",r.code().strip().toUpperCase(Locale.ROOT),r.name().strip(),r.durationMinutes(),r.general(),!r.general()));}
  @PutMapping("/catalogs/specialties/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void specialtyCatalog(@PathVariable long id,@Valid @RequestBody SpecialtyCatalogRequest r){validateDuration(r.durationMinutes());update("update specialties set code=?,name=?,duration_minutes=?,general=?,requires_approval=? where id=?",r.code().strip().toUpperCase(Locale.ROOT),r.name().strip(),r.durationMinutes(),r.general(),!r.general(),id);}
  @PatchMapping("/catalogs/specialties/{id}/active") @ResponseStatus(HttpStatus.NO_CONTENT) public void specialtyActive(@PathVariable long id,@RequestBody ActiveRequest r){update("update specialties set active=? where id=?",r.active(),id);}
  private void validateDuration(int minutes){if(minutes!=30&&minutes!=60)throw new ApiErrors.RequestFailure(400,"INVALID","La duración debe ser 30 o 60 minutos.");}
  private boolean exists(String sql,Object...args){Integer n=jdbc.queryForObject(sql,Integer.class,args);return n!=null&&n>0;}
  private void update(String sql,Object...args){try{if(jdbc.update(sql,args)==0)throw new ApiErrors.RequestFailure(404,"NOT_FOUND","Catálogo inexistente.");}catch(DataIntegrityViolationException e){throw new ApiErrors.RequestFailure(409,"DUPLICATE","El código o nombre ya existe.");}}
  private long insert(String sql,Object...args){try{jdbc.update(sql,args);Long id=jdbc.queryForObject("select last_insert_id()",Long.class);return Objects.requireNonNull(id);}catch(DataIntegrityViolationException e){throw new ApiErrors.RequestFailure(409,"DUPLICATE","El código o nombre ya existe.");}}
}
