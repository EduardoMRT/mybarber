package br.com.mybarber.api.erro;

import org.springframework.http.HttpStatus;

/** Erro de regra de negocio com mensagem pronta para o usuario. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;

    public ApiException(HttpStatus status, String codigo, String mensagem) {
        super(mensagem);
        this.status = status;
        this.codigo = codigo;
    }

    public static ApiException naoEncontrado(String oque) {
        return new ApiException(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO", oque + ": registro não encontrado.");
    }

    public static ApiException regra(String mensagem) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "REGRA_NEGOCIO", mensagem);
    }

    public static ApiException conflito(String mensagem) {
        return new ApiException(HttpStatus.CONFLICT, "CONFLITO", mensagem);
    }

    public static ApiException proibido(String mensagem) {
        return new ApiException(HttpStatus.FORBIDDEN, "SEM_PERMISSAO", mensagem);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCodigo() {
        return codigo;
    }
}
