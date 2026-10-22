package br.com.mybarber.api.dominio;

import java.util.EnumSet;
import java.util.Set;

public enum StatusAgendamento {
    CONFIRMADO, AGUARDANDO_CLIENTE, CONCLUIDO, CANCELADO, FALTA;

    /** Status que ocupam a agenda do barbeiro. */
    public static final Set<StatusAgendamento> OCUPAM = EnumSet.of(CONFIRMADO, AGUARDANDO_CLIENTE, CONCLUIDO);

    public boolean ativo() {
        return this == CONFIRMADO || this == AGUARDANDO_CLIENTE;
    }
}
