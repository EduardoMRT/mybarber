package br.com.mybarber.api.publico;

import br.com.mybarber.api.dominio.Barbearia;
import br.com.mybarber.api.dominio.Servico;
import br.com.mybarber.api.dominio.StatusBarbearia;
import br.com.mybarber.api.erro.ApiException;
import br.com.mybarber.api.publico.PublicoDtos.*;
import br.com.mybarber.api.repositorio.BarbeariaRepository;
import br.com.mybarber.api.repositorio.BarbeiroRepository;
import br.com.mybarber.api.repositorio.ServicoRepository;
import br.com.mybarber.api.repositorio.TipoCabeloRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

/** Consultas abertas usadas pelo app antes e durante o agendamento. */
@Service
public class PublicoService {

    private static final int LIMITE = 50;

    private final BarbeariaRepository barbearias;
    private final BarbeiroRepository barbeiros;
    private final ServicoRepository servicos;
    private final TipoCabeloRepository tiposCabelo;

    public PublicoService(BarbeariaRepository barbearias, BarbeiroRepository barbeiros, ServicoRepository servicos,
                          TipoCabeloRepository tiposCabelo) {
        this.barbearias = barbearias;
        this.barbeiros = barbeiros;
        this.servicos = servicos;
        this.tiposCabelo = tiposCabelo;
    }

    @Transactional(readOnly = true)
    public List<TipoCabeloDto> tiposCabelo() {
        return tiposCabelo.findByAtivoTrueOrderByOrdemAscNomeAsc().stream()
                .map(t -> new TipoCabeloDto(t.getId(), t.getNome(), t.getDescricao())).toList();
    }

    /**
     * Sem texto: barbearias ate o raio informado (precisam ter local marcado no mapa).
     * Com texto (nome, bairro ou cidade): todas as que combinam, com ou sem local marcado
     * e sem limite de raio; com a posicao do cliente, as mais proximas vem primeiro e as
     * sem local no fim.
     */
    @Transactional(readOnly = true)
    public List<BarbeariaResumoDto> buscar(Double lat, Double lng, Double raioKm, String texto) {
        List<BarbeariaResumoDto> lista = new ArrayList<>();
        String termo = texto == null || texto.isBlank() ? null : "%" + texto.trim().toLowerCase() + "%";
        boolean temPosicao = lat != null && lng != null;

        if (termo != null) {
            for (Barbearia b : barbearias.buscarPorTexto(StatusBarbearia.ATIVA, termo)) {
                Double d = temPosicao && temLocal(b)
                        ? distanciaKm(lat, lng, b.getLatitude().doubleValue(), b.getLongitude().doubleValue())
                        : null;
                lista.add(resumo(b, d));
            }
            if (temPosicao) {
                lista.sort(Comparator.comparing(BarbeariaResumoDto::distanciaKm,
                        Comparator.nullsLast(Comparator.naturalOrder())));
            }
        } else if (temPosicao) {
            double raio = raioKm == null ? 30 : Math.max(1, Math.min(raioKm, 200));
            double dLat = raio / 111.0;
            double dLng = raio / (111.0 * Math.max(0.1, Math.cos(Math.toRadians(lat))));
            for (Barbearia b : barbearias.buscarNaArea(StatusBarbearia.ATIVA,
                    bd(lat - dLat), bd(lat + dLat), bd(lng - dLng), bd(lng + dLng))) {
                double d = distanciaKm(lat, lng, b.getLatitude().doubleValue(), b.getLongitude().doubleValue());
                if (d <= raio) lista.add(resumo(b, d));
            }
            lista.sort(Comparator.comparing(BarbeariaResumoDto::distanciaKm));
        } else {
            for (Barbearia b : barbearias.buscarPorTexto(StatusBarbearia.ATIVA, null)) {
                lista.add(resumo(b, null));
            }
        }
        return lista.size() > LIMITE ? lista.subList(0, LIMITE) : lista;
    }

    private static boolean temLocal(Barbearia b) {
        return b.getLatitude() != null && b.getLongitude() != null;
    }

    @Transactional(readOnly = true)
    public BarbeariaDetalheDto detalhe(Long id) {
        Barbearia b = barbearias.findById(id)
                .filter(x -> x.getStatus() == StatusBarbearia.ATIVA)
                .orElseThrow(() -> ApiException.naoEncontrado("Barbearia"));
        List<ServicoPublicoDto> svs = servicos.findByBarbeariaIdAndAtivoTrueOrderByNomeAsc(id).stream()
                .map(s -> new ServicoPublicoDto(s.getId(), s.getNome(), s.getDescricao(), s.getPreco(), s.getDuracaoMin(),
                        s.getDuracoes().entrySet().stream().map(e -> new DuracaoPublicaDto(e.getKey(), e.getValue())).toList()))
                .toList();
        List<BarbeiroPublicoDto> bbs = barbeiros.findByBarbeariaIdAndAtivoTrueOrderByNomeAsc(id).stream()
                .map(x -> new BarbeiroPublicoDto(x.getId(), x.getNome(), x.getBio(),
                        new HashSet<>(x.getTiposCabeloIds()), new HashSet<>(x.getServicosIds())))
                .toList();
        return new BarbeariaDetalheDto(b.getId(), b.getNome(), b.getDescricao(), b.getTelefone(), b.enderecoFormatado(),
                b.getCep(), b.getBairro(), b.getCidade(), b.getUf(), b.getLatitude(), b.getLongitude(),
                b.getPrazoCancelamentoMin(), svs, bbs);
    }

    private BarbeariaResumoDto resumo(Barbearia b, Double distancia) {
        BigDecimal aPartir = servicos.findByBarbeariaIdAndAtivoTrueOrderByNomeAsc(b.getId()).stream()
                .map(Servico::getPreco).min(Comparator.naturalOrder()).orElse(null);
        return new BarbeariaResumoDto(b.getId(), b.getNome(), b.enderecoFormatado(), b.getBairro(), b.getCidade(), b.getUf(),
                b.getLatitude(), b.getLongitude(),
                distancia == null ? null : Math.round(distancia * 10) / 10.0,
                b.getTelefone(), barbeiros.countByBarbeariaIdAndAtivoTrue(b.getId()), aPartir);
    }

    private static BigDecimal bd(double v) {
        return BigDecimal.valueOf(v);
    }

    /** Distancia entre dois pontos (formula de Haversine). */
    static double distanciaKm(double lat1, double lng1, double lat2, double lng2) {
        double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * r * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
