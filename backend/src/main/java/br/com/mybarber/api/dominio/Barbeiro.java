package br.com.mybarber.api.dominio;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "barbeiro")
public class Barbeiro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "barbearia_id")
    private Barbearia barbearia;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    private String nome;
    private String telefone;
    private String bio;

    @Column(name = "intervalo_min")
    private int intervaloMin;

    private boolean ativo = true;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;

    /** Tipos de cabelo que o barbeiro atende. Vazio = atende todos. */
    @ElementCollection
    @CollectionTable(name = "barbeiro_tipo_cabelo", joinColumns = @JoinColumn(name = "barbeiro_id"))
    @Column(name = "tipo_cabelo_id")
    private Set<Long> tiposCabeloIds = new HashSet<>();

    /** Servicos que o barbeiro realiza. Vazio = realiza todos os servicos da barbearia. */
    @ElementCollection
    @CollectionTable(name = "barbeiro_servico", joinColumns = @JoinColumn(name = "barbeiro_id"))
    @Column(name = "servico_id")
    private Set<Long> servicosIds = new HashSet<>();

    public boolean atendeTipo(Long tipoCabeloId) {
        return tipoCabeloId == null || tiposCabeloIds.isEmpty() || tiposCabeloIds.contains(tipoCabeloId);
    }

    public boolean realizaServico(Long servicoId) {
        return servicosIds.isEmpty() || servicosIds.contains(servicoId);
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

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public int getIntervaloMin() {
        return intervaloMin;
    }

    public void setIntervaloMin(int intervaloMin) {
        this.intervaloMin = intervaloMin;
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

    public Set<Long> getTiposCabeloIds() {
        return tiposCabeloIds;
    }

    public void setTiposCabeloIds(Set<Long> tiposCabeloIds) {
        this.tiposCabeloIds = tiposCabeloIds;
    }

    public Set<Long> getServicosIds() {
        return servicosIds;
    }

    public void setServicosIds(Set<Long> servicosIds) {
        this.servicosIds = servicosIds;
    }
}
