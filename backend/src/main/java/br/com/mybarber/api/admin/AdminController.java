package br.com.mybarber.api.admin;

import br.com.mybarber.api.admin.AdminDtos.*;
import br.com.mybarber.api.cobranca.CobrancaDtos.*;
import br.com.mybarber.api.cobranca.CobrancaService;
import br.com.mybarber.api.dominio.StatusBarbearia;
import br.com.mybarber.api.dominio.StatusCobranca;
import br.com.mybarber.api.painel.PainelDtos.BarbeariaDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Portal do super admin (dono do MyBarber). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService admin;
    private final CobrancaService cobrancas;

    public AdminController(AdminService admin, CobrancaService cobrancas) {
        this.admin = admin;
        this.cobrancas = cobrancas;
    }

    @GetMapping("/dashboard")
    public AdminDashboardDto dashboard() {
        return admin.dashboard();
    }

    // barbearias
    @GetMapping("/barbearias")
    public List<BarbeariaDto> barbearias(@RequestParam(required = false) String busca,
                                         @RequestParam(required = false) StatusBarbearia status) {
        return admin.barbearias(busca, status);
    }

    @GetMapping("/barbearias/{id}")
    public BarbeariaDto barbearia(@PathVariable Long id) {
        return admin.barbearia(id);
    }

    @PostMapping("/barbearias")
    @ResponseStatus(HttpStatus.CREATED)
    public BarbeariaDto criar(@Valid @RequestBody CriarBarbeariaRequest req) {
        return admin.criar(req);
    }

    @PutMapping("/barbearias/{id}")
    public BarbeariaDto atualizar(@PathVariable Long id, @Valid @RequestBody AtualizarBarbeariaRequest req) {
        return admin.atualizar(id, req);
    }

    @PostMapping("/barbearias/{id}/bloquear")
    public BarbeariaDto bloquear(@PathVariable Long id, @Valid @RequestBody BloquearRequest req) {
        return admin.bloquear(id, req);
    }

    @PostMapping("/barbearias/{id}/desbloquear")
    public BarbeariaDto desbloquear(@PathVariable Long id) {
        return admin.desbloquear(id);
    }

    @PostMapping("/barbearias/{id}/redefinir-senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void redefinirSenha(@PathVariable Long id, @Valid @RequestBody RedefinirSenhaRequest req) {
        admin.redefinirSenhaDono(id, req);
    }

    // cobrancas
    @GetMapping("/cobrancas")
    public List<CobrancaDto> cobrancas(@RequestParam(required = false) StatusCobranca status,
                                       @RequestParam(required = false) String competencia,
                                       @RequestParam(required = false) Long barbeariaId) {
        return cobrancas.listar(status, competencia, barbeariaId);
    }

    @PostMapping("/cobrancas")
    @ResponseStatus(HttpStatus.CREATED)
    public CobrancaDto criarCobranca(@Valid @RequestBody NovaCobrancaRequest req) {
        return cobrancas.criar(req);
    }

    @PostMapping("/cobrancas/gerar")
    public GeradasDto gerar(@Valid @RequestBody GerarRequest req) {
        return cobrancas.gerar(req.competencia());
    }

    @PostMapping("/cobrancas/{id}/pagar")
    public CobrancaDto pagar(@PathVariable Long id, @Valid @RequestBody(required = false) PagarRequest req) {
        return cobrancas.pagar(id, req);
    }

    @PostMapping("/cobrancas/{id}/reabrir")
    public CobrancaDto reabrir(@PathVariable Long id) {
        return cobrancas.reabrir(id);
    }

    @PostMapping("/cobrancas/{id}/cancelar")
    public CobrancaDto cancelar(@PathVariable Long id) {
        return cobrancas.cancelar(id);
    }

    @PostMapping("/cobrancas/rotina")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void executarRotina() {
        cobrancas.rotinaDiaria();
    }

    // tipos de cabelo
    @GetMapping("/tipos-cabelo")
    public List<TipoCabeloAdminDto> tipos() {
        return admin.tiposCabelo();
    }

    @PostMapping("/tipos-cabelo")
    @ResponseStatus(HttpStatus.CREATED)
    public TipoCabeloAdminDto criarTipo(@Valid @RequestBody TipoCabeloRequest req) {
        return admin.criarTipo(req);
    }

    @PutMapping("/tipos-cabelo/{id}")
    public TipoCabeloAdminDto atualizarTipo(@PathVariable Long id, @Valid @RequestBody TipoCabeloRequest req) {
        return admin.atualizarTipo(id, req);
    }
}
