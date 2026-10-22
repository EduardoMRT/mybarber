package br.com.mybarber.api.seguranca;

import br.com.mybarber.api.dominio.Papel;

/** Dados do usuario autenticado, extraidos do JWT. */
public record UsuarioLogado(Long id, Papel papel, Long barbeariaId, Long barbeiroId) {

    public boolean donoBarbearia() {
        return papel == Papel.ADMIN_BARBEARIA;
    }

    public boolean barbeiro() {
        return papel == Papel.BARBEIRO;
    }
}
