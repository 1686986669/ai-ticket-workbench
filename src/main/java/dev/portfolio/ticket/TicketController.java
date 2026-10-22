package dev.portfolio.ticket;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Validated
public class TicketController {
    private final TicketService service;
    private final AiService ai;
    public TicketController(TicketService service,AiService ai) { this.service=service; this.ai=ai; }
    public record Create(@NotBlank @Size(max=120) String title,@NotBlank @Size(max=4000) String description) {}
    public record Change(@NotNull @Pattern(regexp="IN_PROGRESS|RESOLVED") String status,@Min(0) int version) {}
    @GetMapping("/health") public Map<String,String> health() { return Map.of("status","UP"); }
    @PostMapping("/tickets") @ResponseStatus(HttpStatus.CREATED)
    public Ticket create(@Valid @RequestBody Create body) { return service.create(body.title(),body.description()); }
    @GetMapping("/tickets")
    public Map<String,Object> list(@RequestParam(defaultValue="1") @Min(1) @Max(10000) int page,
                                  @RequestParam(defaultValue="10") @Min(1) @Max(100) int size) { return service.list(page,size); }
    @GetMapping("/tickets/{id}") public Ticket get(@PathVariable long id) { return service.get(id); }
    @PatchMapping("/tickets/{id}/status")
    public Ticket status(@PathVariable long id,@Valid @RequestBody Change body) { return service.transition(id,body.status(),body.version()); }
    @PostMapping("/tickets/{id}/suggestion")
    public Map<String,String> suggestion(@PathVariable long id) { return ai.suggest(service.get(id)); }
}
