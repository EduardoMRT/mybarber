package br.com.mybarber.api.dominio;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "consentimento")
public class Consentimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    private String documento;
    private String versao;

    @Column(name = "aceito_em")
    private LocalDateTime aceitoEm;

    private String ip;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }

    public String getVersao() {
        return versao;
    }

    public void setVersao(String versao) {
        this.versao = versao;
    }

    public LocalDateTime getAceitoEm() {
        return aceitoEm;
    }

    public void setAceitoEm(LocalDateTime aceitoEm) {
        this.aceitoEm = aceitoEm;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}
