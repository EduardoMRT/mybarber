package br.com.mybarber.api.publico;

import br.com.mybarber.api.config.MyBarberProperties;
import br.com.mybarber.api.erro.ApiException;
import br.com.mybarber.api.publico.PublicoDtos.DocumentoLegalDto;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Documentos legais (Markdown em resources/legal), exibidos no app e no portal. */
@Service
public class LegalService {

    private static final Map<String, String> TITULOS = new LinkedHashMap<>();

    static {
        TITULOS.put("termos-de-uso", "Termos de Uso");
        TITULOS.put("politica-de-privacidade", "Política de Privacidade");
        TITULOS.put("lgpd", "Seus direitos (LGPD)");
        TITULOS.put("termos-barbearia", "Termos de Adesão para Barbearias");
    }

    private final MyBarberProperties props;

    public LegalService(MyBarberProperties props) {
        this.props = props;
    }

    public List<DocumentoLegalDto> listar() {
        return TITULOS.entrySet().stream()
                .map(e -> new DocumentoLegalDto(e.getKey(), e.getValue(), props.legal().versao(), null))
                .toList();
    }

    public DocumentoLegalDto documento(String slug) {
        String titulo = TITULOS.get(slug);
        if (titulo == null) {
            throw ApiException.naoEncontrado("Documento");
        }
        try (InputStream in = new ClassPathResource("legal/" + slug + ".md").getInputStream()) {
            return new DocumentoLegalDto(slug, titulo, props.legal().versao(), new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw ApiException.naoEncontrado("Documento");
        }
    }
}
