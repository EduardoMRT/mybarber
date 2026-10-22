package br.com.mybarber.api.dominio;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "agendamento")
public class Agendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "barbearia_id")
    private Barbearia barbearia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "barbeiro_id")
    private Barbeiro barbeiro;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servico_id")
    private Servico servico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Usuario cliente;

    @Column(name = "cliente_nome")
    private String clienteNome;

    @Column(name = "cliente_telefone")
    private String clienteTelefone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_cabelo_id")
    private TipoCabelo tipoCabelo;

    private LocalDateTime inicio;
    private LocalDateTime fim;
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    private StatusAgendamento status;

    @Enumerated(EnumType.STRING)
    private OrigemAgendamento origem;

    private String observacao;

    @Column(name = "motivo_cancelamento")
    private String motivoCancelamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento")
    private FormaPagamento formaPagamento;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    public String nomeCliente() {
        return cliente != null ? cliente.getNome() : clienteNome;
    }

    public String telefoneCliente() {
        return cliente != null ? cliente.getTelefone() : clienteTelefone;
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

    public Barbeiro getBarbeiro() {
        return barbeiro;
    }

    public void setBarbeiro(Barbeiro barbeiro) {
        this.barbeiro = barbeiro;
    }

    public Servico getServico() {
        return servico;
    }

    public void setServico(Servico servico) {
        this.servico = servico;
    }

    public Usuario getCliente() {
        return cliente;
    }

    public void setCliente(Usuario cliente) {
        this.cliente = cliente;
    }

    public String getClienteNome() {
        return clienteNome;
    }

    public void setClienteNome(String clienteNome) {
        this.clienteNome = clienteNome;
    }

    public String getClienteTelefone() {
        return clienteTelefone;
    }

    public void setClienteTelefone(String clienteTelefone) {
        this.clienteTelefone = clienteTelefone;
    }

    public TipoCabelo getTipoCabelo() {
        return tipoCabelo;
    }

    public void setTipoCabelo(TipoCabelo tipoCabelo) {
        this.tipoCabelo = tipoCabelo;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public void setInicio(LocalDateTime inicio) {
        this.inicio = inicio;
    }

    public LocalDateTime getFim() {
        return fim;
    }

    public void setFim(LocalDateTime fim) {
        this.fim = fim;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public StatusAgendamento getStatus() {
        return status;
    }

    public void setStatus(StatusAgendamento status) {
        this.status = status;
    }

    public OrigemAgendamento getOrigem() {
        return origem;
    }

    public void setOrigem(OrigemAgendamento origem) {
        this.origem = origem;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public String getMotivoCancelamento() {
        return motivoCancelamento;
    }

    public void setMotivoCancelamento(String motivoCancelamento) {
        this.motivoCancelamento = motivoCancelamento;
    }

    public FormaPagamento getFormaPagamento() {
        return formaPagamento;
    }

    public void setFormaPagamento(FormaPagamento formaPagamento) {
        this.formaPagamento = formaPagamento;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }
}
