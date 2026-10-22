package br.com.mybarber.api.publico;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public final class PublicoDtos {

    private PublicoDtos() {
    }

    public record TipoCabeloDto(Long id, String nome, String descricao) {
    }

    public record BarbeariaResumoDto(Long id, String nome, String endereco, String bairro, String cidade, String uf,
                                     BigDecimal latitude, BigDecimal longitude, Double distanciaKm, String telefone,
                                     long barbeirosAtivos, BigDecimal precoAPartirDe) {
    }

    public record DuracaoPublicaDto(Long tipoCabeloId, int duracaoMin) {
    }

    public record ServicoPublicoDto(Long id, String nome, String descricao, BigDecimal preco, int duracaoMin,
                                    List<DuracaoPublicaDto> duracoes) {
    }

    public record BarbeiroPublicoDto(Long id, String nome, String bio, Set<Long> tiposCabeloIds, Set<Long> servicosIds) {
    }

    public record BarbeariaDetalheDto(Long id, String nome, String descricao, String telefone, String endereco,
                                      String cep, String bairro, String cidade, String uf,
                                      BigDecimal latitude, BigDecimal longitude,
                                      int prazoCancelamentoMin,
                                      List<ServicoPublicoDto> servicos, List<BarbeiroPublicoDto> barbeiros) {
    }

    public record DocumentoLegalDto(String slug, String titulo, String versao, String conteudo) {
    }
}
