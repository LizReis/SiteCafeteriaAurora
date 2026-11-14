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
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ErroApi> uploadExcessivo(Exception ex) {
        return erro(HttpStatus.PAYLOAD_TOO_LARGE,"A foto deve ter no máximo 5 MB e a requisição até 6 MB",List.of());
    }
    @ExceptionHandler(AvaliacaoPersistenciaException.class)
    public ResponseEntity<ErroApi> falhaAvaliacao(AvaliacaoPersistenciaException ex) {
        return erro(HttpStatus.INTERNAL_SERVER_ERROR,ex.getMessage(),List.of());
    }
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

    @ExceptionHandler(PedidoPersistenciaException.class)
    public ResponseEntity<ErroApi> falhaPedido(PedidoPersistenciaException ex) {
        return erro(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroApi> restricaoBanco(DataIntegrityViolationException ex) {
        return erro(HttpStatus.CONFLICT, "O registro viola uma restrição de integridade do banco", List.of());
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ErroApi> jsonInvalido(Exception ex) {
        return erro(HttpStatus.BAD_REQUEST, "JSON inválido ou campos não permitidos", List.of());
    }

    private ResponseEntity<ErroApi> erro(HttpStatus status, String mensagem, List<String> detalhes) {
        return ResponseEntity.status(status)
                .body(new ErroApi(Instant.now(), status.value(), mensagem, detalhes));
    }
}
