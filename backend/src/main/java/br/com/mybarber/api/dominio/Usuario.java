package br.com.mybarber.api.dominio;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String email;

    @Column(name = "senha_hash")
    private String senhaHash;

    private String telefone;

    @Enumerated(EnumType.STRING)
    private Papel papel;

    private boolean ativo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barbearia_id")
    private Barbearia barbearia;

    @Enumerated(EnumType.STRING)
    private Sexo sexo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_cabelo_id")
    private TipoCabelo tipoCabelo;

    @Enumerated(EnumType.STRING)
    @Column(name = "comprimento_cabelo")
    private ComprimentoCabelo comprimentoCabelo;

    private String observacoes;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;

    @Column(name = "excluido_em")
    private LocalDateTime excluidoEm;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public Papel getPapel() {
        return papel;
    }

    public void setPapel(Papel papel) {
        this.papel = papel;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public Barbearia getBarbearia() {
        return barbearia;
    }

    public void setBarbearia(Barbearia barbearia) {
        this.barbearia = barbearia;
    }

    public Sexo getSexo() {
        return sexo;
    }

    public void setSexo(Sexo sexo) {
        this.sexo = sexo;
    }

    public TipoCabelo getTipoCabelo() {
        return tipoCabelo;
    }

    public void setTipoCabelo(TipoCabelo tipoCabelo) {
        this.tipoCabelo = tipoCabelo;
    }

    public ComprimentoCabelo getComprimentoCabelo() {
        return comprimentoCabelo;
    }

    public void setComprimentoCabelo(ComprimentoCabelo comprimentoCabelo) {
        this.comprimentoCabelo = comprimentoCabelo;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public LocalDateTime getExcluidoEm() {
        return excluidoEm;
    }

    public void setExcluidoEm(LocalDateTime excluidoEm) {
        this.excluidoEm = excluidoEm;
    }
}
