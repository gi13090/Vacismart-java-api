package br.com.fiap.vacismart.web.advice;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.dao.*;
import java.util.*;

@RestControllerAdvice
public class ErroApiAdvice extends ResponseEntityExceptionHandler {
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail erro = ProblemDetail.forStatusAndDetail(status, "Campos inválidos");
        erro.setProperty("campos", ex.getBindingResult().getFieldErrors().stream()
                .map(e -> Map.of("campo", e.getField(), "mensagem", Objects.toString(e.getDefaultMessage(), "Inválido"))).toList());
        return handleExceptionInternal(ex, erro, headers, status, request);
    }
    @ExceptionHandler(NoSuchElementException.class)
    public ProblemDetail ausente(NoSuchElementException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Recurso não encontrado");
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail invalido(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail conflito(IllegalStateException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }
    @ExceptionHandler({DataIntegrityViolationException.class, ConcurrencyFailureException.class})
    public ProblemDetail integridade(Exception ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Operação em conflito com vínculos, dados existentes ou operação concorrente");
    }
    @ExceptionHandler(Exception.class)
    public ProblemDetail inesperado(Exception ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno ao processar a requisição");
    }
}
