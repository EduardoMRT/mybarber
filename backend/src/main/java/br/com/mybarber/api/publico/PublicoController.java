package br.com.mybarber.api.publico;

import br.com.mybarber.api.agenda.AgendaService;
import br.com.mybarber.api.agenda.AgendamentoDtos.HorariosDto;
import br.com.mybarber.api.publico.PublicoDtos.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Endpoints abertos (sem login). */
@RestController
@RequestMapping("/api/public")
public class PublicoController {

    private final PublicoService publico;
    private final AgendaService agenda;
    private final LegalService legal;

    public PublicoController(PublicoService publico, AgendaService agenda, LegalService legal) {
        this.publico = publico;
        this.agenda = agenda;
        this.legal = legal;
    }

    @GetMapping("/tipos-cabelo")
    public List<TipoCabeloDto> tiposCabelo() {
        return publico.tiposCabelo();
    }

    @GetMapping("/barbearias")
    public List<BarbeariaResumoDto> barbearias(@RequestParam(required = false) Double lat,
                                               @RequestParam(required = false) Double lng,
                                               @RequestParam(required = false) Double raioKm,
                                               @RequestParam(required = false) String busca) {
        return publico.buscar(lat, lng, raioKm, busca);
    }

    @GetMapping("/barbearias/{id}")
    public BarbeariaDetalheDto barbearia(@PathVariable Long id) {
        return publico.detalhe(id);
    }

    /** Horarios livres em que o servico cabe inteiro (regra de encaixe). */
    @GetMapping("/horarios")
    public HorariosDto horarios(@RequestParam Long barbeiroId, @RequestParam Long servicoId,
                                @RequestParam(required = false) Long tipoCabeloId,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return agenda.disponiveis(barbeiroId, servicoId, tipoCabeloId, data);
    }

    @GetMapping("/legal")
    public List<DocumentoLegalDto> documentos() {
        return legal.listar();
    }

    @GetMapping("/legal/{slug}")
    public DocumentoLegalDto documento(@PathVariable String slug) {
        return legal.documento(slug);
    }
}
