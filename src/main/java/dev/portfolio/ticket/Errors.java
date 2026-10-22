package dev.portfolio.ticket;
import java.util.Map;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;
@RestControllerAdvice
public class Errors {
    @ExceptionHandler({MethodArgumentNotValidException.class,ConstraintViolationException.class,HttpMessageNotReadableException.class})
    public ResponseEntity<?> bad(Exception e) { return ResponseEntity.badRequest().body(Map.of("error","Invalid request fields")); }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> status(ResponseStatusException e) { return ResponseEntity.status(e.getStatusCode()).body(Map.of("error",e.getReason()==null?"Request failed":e.getReason())); }
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> model(IllegalStateException e) { return ResponseEntity.status(502).body(Map.of("error","Model unavailable; check configuration")); }
}
