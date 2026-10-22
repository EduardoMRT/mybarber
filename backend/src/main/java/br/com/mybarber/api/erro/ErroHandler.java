package br.com.mybarber.api.erro;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ErroHandler {

    private static final Logger log = LoggerFactory.getLogger(ErroHandler.class);

    public record Erro(int status, String erro, String mensagem, Map<String, String> campos) {
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Erro> api(ApiException e) {
        return ResponseEntity.status(e.getStatus())
                .body(new Erro(e.getStatus().value(), e.getCodigo(), e.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Erro> validacao(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError fe : e.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        return ResponseEntity.badRequest()
                .body(new Erro(400, "DADOS_INVALIDOS", "Verifique os campos informados.", campos));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<Erro> requisicaoInvalida(Exception e) {
        return ResponseEntity.badRequest()
                .body(new Erro(400, "REQUISICAO_INVALIDA", "Requisição inválida: verifique o formato dos dados.", null));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Erro> acesso(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new Erro(403, "SEM_PERMISSAO", "Você não tem permissão para esta ação.", null));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Erro> integridade(DataIntegrityViolationException e) {
        log.warn("Violação de integridade: {}", e.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new Erro(409, "CONFLITO", "Operação conflita com dados existentes.", null));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Erro> naoEncontrado(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new Erro(404, "NAO_ENCONTRADO", "Recurso não encontrado.", null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Erro> inesperado(Exception e) {
        log.error("Erro inesperado", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new Erro(500, "ERRO_INTERNO", "Ocorreu um erro inesperado. Tente novamente.", null));
    }
}
