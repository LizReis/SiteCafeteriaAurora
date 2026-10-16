package br.com.cafeaurora.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {
    public record ErroApi(Instant momento, int status, String mensagem, List<String> detalhes) {}

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroApi> naoEncontrado(RecursoNaoEncontradoException ex) {
        return erro(HttpStatus.NOT_FOUND, ex.getMessage(), List.of());
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroApi> conflito(RegraNegocioException ex) {
        return erro(HttpStatus.CONFLICT, ex.getMessage(), List.of());
    }

    @ExceptionHandler(EntradaInvalidaException.class)
    public ResponseEntity<ErroApi> entradaInvalida(EntradaInvalidaException ex) {
        return erro(HttpStatus.BAD_REQUEST, ex.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroApi> falhaValidacao(MethodArgumentNotValidException ex) {
        List<String> detalhes = ex.getBindingResult().getFieldErrors().stream()
                .map(campo -> campo.getField() + ": " + campo.getDefaultMessage())
                .distinct().toList();
        return erro(HttpStatus.BAD_REQUEST, "Dados de entrada inválidos", detalhes);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroApi> restricaoBanco(DataIntegrityViolationException ex) {
        return erro(HttpStatus.CONFLICT, "O registro viola uma restrição de integridade do banco", List.of());
    }

    private ResponseEntity<ErroApi> erro(HttpStatus status, String mensagem, List<String> detalhes) {
        return ResponseEntity.status(status)
                .body(new ErroApi(Instant.now(), status.value(), mensagem, detalhes));
    }
}
