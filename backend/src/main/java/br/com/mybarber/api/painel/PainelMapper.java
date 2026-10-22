package br.com.mybarber.api.painel;

import br.com.mybarber.api.dominio.Barbearia;
import br.com.mybarber.api.dominio.Barbeiro;
import br.com.mybarber.api.dominio.Papel;
import br.com.mybarber.api.dominio.Servico;
import br.com.mybarber.api.dominio.Usuario;
import br.com.mybarber.api.painel.PainelDtos.*;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

public final class PainelMapper {

    private PainelMapper() {
    }

    public static BarbeariaDto barbearia(Barbearia b, Usuario dono, long barbeirosAtivos) {
        return new BarbeariaDto(b.getId(), b.getNome(), b.getDocumento(), b.getTelefone(), b.getEmail(), b.getDescricao(),
                b.getCep(), b.getLogradouro(), b.getNumero(), b.getComplemento(), b.getBairro(), b.getCidade(), b.getUf(),
                b.getLatitude(), b.getLongitude(),
                b.getGradeMinutos(), b.getAntecedenciaMinimaMin(), b.getPrazoCancelamentoMin(),
                b.getPrazoRespostaRemarcacaoHoras(),
                b.getStatus(), b.getMotivoBloqueio(),
                b.getValorMensalidade(), b.getDiaVencimento(), b.getDiasTolerancia(),
                b.getCriadoEm(), dono != null ? dono.getNome() : null, dono != null ? dono.getEmail() : null,
                barbeirosAtivos);
    }

    public static BarbeiroDto barbeiro(Barbeiro b) {
        Usuario u = b.getUsuario();
        return new BarbeiroDto(b.getId(), b.getNome(), b.getTelefone(), b.getBio(), b.getIntervaloMin(), b.isAtivo(),
                u != null, u != null ? u.getEmail() : null, u != null && u.getPapel() == Papel.ADMIN_BARBEARIA,
                new HashSet<>(b.getTiposCabeloIds()), new HashSet<>(b.getServicosIds()));
    }

    public static ServicoDto servico(Servico s) {
        List<DuracaoTipoDto> duracoes = s.getDuracoes().entrySet().stream()
                .map(e -> new DuracaoTipoDto(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(DuracaoTipoDto::tipoCabeloId))
                .toList();
        return new ServicoDto(s.getId(), s.getNome(), s.getDescricao(), s.getPreco(), s.getDuracaoMin(), s.isAtivo(), duracoes);
    }

    public static void aplicar(Barbearia b, BarbeariaRequest r) {
        b.setNome(r.nome().trim());
        b.setDocumento(r.documento());
        b.setTelefone(r.telefone());
        b.setEmail(r.email());
        b.setDescricao(r.descricao());
        b.setCep(r.cep());
        b.setLogradouro(r.logradouro());
        b.setNumero(r.numero());
        b.setComplemento(r.complemento());
        b.setBairro(r.bairro());
        b.setCidade(r.cidade());
        b.setUf(r.uf() != null ? r.uf().toUpperCase() : null);
        b.setLatitude(r.latitude());
        b.setLongitude(r.longitude());
        if (r.gradeMinutos() != null) b.setGradeMinutos(r.gradeMinutos());
        if (r.antecedenciaMinimaMin() != null) b.setAntecedenciaMinimaMin(r.antecedenciaMinimaMin());
        if (r.prazoCancelamentoMin() != null) b.setPrazoCancelamentoMin(r.prazoCancelamentoMin());
        if (r.prazoRespostaRemarcacaoHoras() != null) b.setPrazoRespostaRemarcacaoHoras(r.prazoRespostaRemarcacaoHoras());
    }
}
