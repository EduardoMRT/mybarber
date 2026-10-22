package br.com.mybarber.api.dominio;

import jakarta.persistence.*;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "horario_trabalho")
public class HorarioTrabalho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "barbeiro_id")
    private Barbeiro barbeiro;

    /** 1 = segunda ... 7 = domingo (ISO). */
    @Column(name = "dia_semana")
    private int diaSemana;

    private LocalTime inicio;
    private LocalTime fim;

    /** Servicos aceitos nesta faixa. Vazio = todos. */
    @ElementCollection
    @CollectionTable(name = "horario_trabalho_servico", joinColumns = @JoinColumn(name = "horario_id"))
    @Column(name = "servico_id")
    private Set<Long> servicosIds = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Barbeiro getBarbeiro() {
        return barbeiro;
    }

    public void setBarbeiro(Barbeiro barbeiro) {
        this.barbeiro = barbeiro;
    }

    public int getDiaSemana() {
        return diaSemana;
    }

    public void setDiaSemana(int diaSemana) {
        this.diaSemana = diaSemana;
    }

    public LocalTime getInicio() {
        return inicio;
    }

    public void setInicio(LocalTime inicio) {
        this.inicio = inicio;
    }

    public LocalTime getFim() {
        return fim;
    }

    public void setFim(LocalTime fim) {
        this.fim = fim;
    }

    public Set<Long> getServicosIds() {
        return servicosIds;
    }

    public void setServicosIds(Set<Long> servicosIds) {
        this.servicosIds = servicosIds;
    }
}
