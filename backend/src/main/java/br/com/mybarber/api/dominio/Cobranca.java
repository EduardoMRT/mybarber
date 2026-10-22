package br.com.mybarber.api.dominio;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cobranca")
public class Cobranca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "barbearia_id")
    private Barbearia barbearia;

    /** Formato AAAA-MM. */
    private String competencia;
    private BigDecimal valor;
    private LocalDate vencimento;

    @Enumerated(EnumType.STRING)
    private StatusCobranca status;

    @Column(name = "pago_em")
    private LocalDate pagoEm;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento")
    private FormaPagamento formaPagamento;

    private String observacao;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;

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

    public String getCompetencia() {
        return competencia;
    }

    public void setCompetencia(String competencia) {
        this.competencia = competencia;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public LocalDate getVencimento() {
        return vencimento;
    }

    public void setVencimento(LocalDate vencimento) {
        this.vencimento = vencimento;
    }

    public StatusCobranca getStatus() {
        return status;
    }

    public void setStatus(StatusCobranca status) {
        this.status = status;
    }

    public LocalDate getPagoEm() {
        return pagoEm;
    }

    public void setPagoEm(LocalDate pagoEm) {
        this.pagoEm = pagoEm;
    }

    public FormaPagamento getFormaPagamento() {
        return formaPagamento;
    }

    public void setFormaPagamento(FormaPagamento formaPagamento) {
        this.formaPagamento = formaPagamento;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }
}
