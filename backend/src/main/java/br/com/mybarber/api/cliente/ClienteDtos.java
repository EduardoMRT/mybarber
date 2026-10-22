package br.com.mybarber.api.cliente;

import br.com.mybarber.api.agenda.AgendamentoDtos.AgendamentoDto;
import br.com.mybarber.api.auth.AuthDtos.UsuarioDto;
import br.com.mybarber.api.dominio.ComprimentoCabelo;
import br.com.mybarber.api.dominio.Sexo;
import br.com.mybarber.api.notificacao.NotificacaoService.NotificacaoDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class ClienteDtos {

    private ClienteDtos() {
    }

    public record PerfilRequest(@NotBlank @Size(max = 120) String nome,
                                @Size(max = 20) String telefone,
                                Sexo sexo,
                                Long tipoCabeloId,
                                ComprimentoCabelo comprimentoCabelo,
                                @Size(max = 500) String observacoes) {
    }

    public record ExcluirContaRequest(@NotBlank String senha) {
    }

    public record ConsentimentoDto(String documento, String versao, LocalDateTime aceitoEm) {
    }

    /** LGPD art. 18, II e V: acesso e portabilidade dos dados do titular. */
    public record MeusDadosDto(LocalDateTime geradoEm, UsuarioDto conta, List<ConsentimentoDto> consentimentos,
                               List<AgendamentoDto> agendamentos, List<NotificacaoDto> notificacoes) {
    }
}
