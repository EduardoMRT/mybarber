package br.com.mybarber.api.dominio;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "remarcacao")
public class Remarcacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agendamento_id")
    private Agendamento agendamento;

    @Column(name = "inicio_anterior")
    private LocalDateTime inicioAnterior;

    @Column(name = "inicio_sugerido")
    private LocalDateTime inicioSugerido;

    private String motivo;

    @Enumerated(EnumType.STRING)
    private StatusRemarcacao status;

    private LocalDateTime prazo;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;

    @Column(name = "respondido_em")
    private LocalDateTime respondidoEm;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Agendamento getAgendamento() {
        return agendamento;
    }

    public void setAgendamento(Agendamento agendamento) {
        this.agendamento = agendamento;
    }

    public LocalDateTime getInicioAnterior() {
        return inicioAnterior;
    }

    public void setInicioAnterior(LocalDateTime inicioAnterior) {
        this.inicioAnterior = inicioAnterior;
    }

    public LocalDateTime getInicioSugerido() {
        return inicioSugerido;
    }

    public void setInicioSugerido(LocalDateTime inicioSugerido) {
        this.inicioSugerido = inicioSugerido;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public StatusRemarcacao getStatus() {
        return status;
    }

    public void setStatus(StatusRemarcacao status) {
        this.status = status;
    }

    public LocalDateTime getPrazo() {
        return prazo;
    }

    public void setPrazo(LocalDateTime prazo) {
        this.prazo = prazo;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public LocalDateTime getRespondidoEm() {
        return respondidoEm;
    }

    public void setRespondidoEm(LocalDateTime respondidoEm) {
        this.respondidoEm = respondidoEm;
    }
}
