package br.ucsal.educonnect.web;

import br.ucsal.educonnect.model.AnexoDenuncia;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** Resposta de download de anexos: sempre como arquivo (attachment) e sem farejar o tipo. */
final class Downloads {

    private Downloads() { }

    static ResponseEntity<byte[]> anexo(AnexoDenuncia a) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(a.getNomeArquivo(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.parseMediaType(a.getTipoConteudo()))
                .body(a.getDados());
    }
}
