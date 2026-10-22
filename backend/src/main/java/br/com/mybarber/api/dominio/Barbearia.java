package br.com.mybarber.api.dominio;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "barbearia")
public class Barbearia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String documento;
    private String telefone;
    private String email;
    private String descricao;
    private String cep;
    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private String uf;
    private BigDecimal latitude;
    private BigDecimal longitude;

    @Column(name = "grade_minutos")
    private int gradeMinutos = 15;

    @Column(name = "antecedencia_minima_min")
    private int antecedenciaMinimaMin = 30;

    @Column(name = "prazo_cancelamento_min")
    private int prazoCancelamentoMin = 120;

    @Column(name = "prazo_resposta_remarcacao_horas")
    private int prazoRespostaRemarcacaoHoras = 12;

    @Enumerated(EnumType.STRING)
    private StatusBarbearia status = StatusBarbearia.ATIVA;

    @Column(name = "motivo_bloqueio")
    private String motivoBloqueio;

    @Column(name = "bloqueio_automatico")
    private boolean bloqueioAutomatico;

    @Column(name = "valor_mensalidade")
    private BigDecimal valorMensalidade;

    @Column(name = "dia_vencimento")
    private Integer diaVencimento;

    @Column(name = "dias_tolerancia")
    private int diasTolerancia = 5;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;

    public boolean bloqueada() {
        return status == StatusBarbearia.BLOQUEADA;
    }

    public String enderecoFormatado() {
        StringBuilder sb = new StringBuilder();
        if (logradouro != null) sb.append(logradouro);
        if (numero != null) sb.append(", ").append(numero);
        if (bairro != null) sb.append(" - ").append(bairro);
        if (cidade != null) sb.append(", ").append(cidade);
        if (uf != null) sb.append("/").append(uf);
        return sb.toString();
    }

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

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getCep() {
        return cep;
    }

    public void setCep(String cep) {
        this.cep = cep;
    }

    public String getLogradouro() {
        return logradouro;
    }

    public void setLogradouro(String logradouro) {
        this.logradouro = logradouro;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getComplemento() {
        return complemento;
    }

    public void setComplemento(String complemento) {
        this.complemento = complemento;
    }

    public String getBairro() {
        return bairro;
    }

    public void setBairro(String bairro) {
        this.bairro = bairro;
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public String getUf() {
        return uf;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public int getGradeMinutos() {
        return gradeMinutos;
    }

    public void setGradeMinutos(int gradeMinutos) {
        this.gradeMinutos = gradeMinutos;
    }

    public int getAntecedenciaMinimaMin() {
        return antecedenciaMinimaMin;
    }

    public void setAntecedenciaMinimaMin(int antecedenciaMinimaMin) {
        this.antecedenciaMinimaMin = antecedenciaMinimaMin;
    }

    public int getPrazoCancelamentoMin() {
        return prazoCancelamentoMin;
    }

    public void setPrazoCancelamentoMin(int prazoCancelamentoMin) {
        this.prazoCancelamentoMin = prazoCancelamentoMin;
    }

    public int getPrazoRespostaRemarcacaoHoras() {
        return prazoRespostaRemarcacaoHoras;
    }

    public void setPrazoRespostaRemarcacaoHoras(int prazoRespostaRemarcacaoHoras) {
        this.prazoRespostaRemarcacaoHoras = prazoRespostaRemarcacaoHoras;
    }

    public StatusBarbearia getStatus() {
        return status;
    }

    public void setStatus(StatusBarbearia status) {
        this.status = status;
    }

    public String getMotivoBloqueio() {
        return motivoBloqueio;
    }

    public void setMotivoBloqueio(String motivoBloqueio) {
        this.motivoBloqueio = motivoBloqueio;
    }

    public boolean isBloqueioAutomatico() {
        return bloqueioAutomatico;
    }

    public void setBloqueioAutomatico(boolean bloqueioAutomatico) {
        this.bloqueioAutomatico = bloqueioAutomatico;
    }

    public BigDecimal getValorMensalidade() {
        return valorMensalidade;
    }

    public void setValorMensalidade(BigDecimal valorMensalidade) {
        this.valorMensalidade = valorMensalidade;
    }

    public Integer getDiaVencimento() {
        return diaVencimento;
    }

    public void setDiaVencimento(Integer diaVencimento) {
        this.diaVencimento = diaVencimento;
    }

    public int getDiasTolerancia() {
        return diasTolerancia;
    }

    public void setDiasTolerancia(int diasTolerancia) {
        this.diasTolerancia = diasTolerancia;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }
}
