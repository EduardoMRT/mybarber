package br.com.mybarber.api.dominio;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "servico")
public class Servico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "barbearia_id")
    private Barbearia barbearia;

    private String nome;
    private String descricao;
    private BigDecimal preco;

    @Column(name = "duracao_min")
    private int duracaoMin;

    private boolean ativo = true;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;

    /** Duracao especifica por tipo de cabelo (tipoCabeloId -> minutos). */
    @ElementCollection
    @CollectionTable(name = "servico_duracao", joinColumns = @JoinColumn(name = "servico_id"))
    @MapKeyColumn(name = "tipo_cabelo_id")
    @Column(name = "duracao_min")
    private Map<Long, Integer> duracoes = new HashMap<>();

    /** RN01: duracao depende do tipo de cabelo do cliente. */
    public int duracaoPara(Long tipoCabeloId) {
        if (tipoCabeloId != null && duracoes.containsKey(tipoCabeloId)) {
            return duracoes.get(tipoCabeloId);
        }
        return duracaoMin;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Barbearia getBarbearia() {
        return barbearia;
    }

    public void setBarbearia(Barbearia barbearia) {
        this.barbearia = barbearia;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public void setPreco(BigDecimal preco) {
        this.preco = preco;
    }

    public int getDuracaoMin() {
        return duracaoMin;
    }

    public void setDuracaoMin(int duracaoMin) {
        this.duracaoMin = duracaoMin;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public Map<Long, Integer> getDuracoes() {
        return duracoes;
    }

    public void setDuracoes(Map<Long, Integer> duracoes) {
        this.duracoes = duracoes;
    }
}
