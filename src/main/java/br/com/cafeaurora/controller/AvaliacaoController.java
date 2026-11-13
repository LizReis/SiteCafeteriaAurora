package br.com.cafeaurora.controller;

import br.com.cafeaurora.dto.AvaliacaoRequest;
import br.com.cafeaurora.exception.EntradaInvalidaException;
import br.com.cafeaurora.service.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import java.util.*;

@RestController
public class AvaliacaoController {
    private final AvaliacaoService service;
    public AvaliacaoController(AvaliacaoService service){this.service=service;}
    public record Confirmacao(String mensagem) {}
    @PostMapping(value="/api/avaliacoes",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Confirmacao> registrar(MultipartHttpServletRequest request) {
        if(request.getQueryString()!=null)throw new EntradaInvalidaException("Envie os dados somente no corpo multipart");
        var permitidos=Set.of("referencia","nomeAutor","nota","comentario","consentimentoPublicacao","_csrf");
        request.getParameterMap().forEach((nome,valores)->{
            if(!permitidos.contains(nome)||valores.length!=1)throw new EntradaInvalidaException("Campo não permitido ou repetido");
        });
        if(request.getMultiFileMap().keySet().stream().anyMatch(n->!n.equals("foto"))||request.getFiles("foto").size()>1)
            throw new EntradaInvalidaException("Envie no máximo uma foto");
        Integer nota;
        try {nota=request.getParameter("nota")==null?null:Integer.valueOf(request.getParameter("nota"));}
        catch(NumberFormatException ex){throw new EntradaInvalidaException("Nota deve ser um inteiro de 1 a 5");}
        String consentimento=request.getParameter("consentimentoPublicacao");
        service.registrar(new AvaliacaoRequest(request.getParameter("referencia"),request.getParameter("nomeAutor"),nota,
            request.getParameter("comentario"),"true".equals(consentimento)),request.getFile("foto"));
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
            .body(new Confirmacao("Avaliação registrada e aguardando aprovação"));
    }
    @GetMapping("/api/avaliacoes/{id}/foto")
    public ResponseEntity<byte[]> publica(@PathVariable Long id){return imagem(service.foto(id,false));}
    @GetMapping("/admin/avaliacoes/{id}/foto")
    public ResponseEntity<byte[]> administrativa(@PathVariable Long id){return imagem(service.foto(id,true));}
    private ResponseEntity<byte[]> imagem(FotoAvaliacaoService.Foto foto) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff")
            .contentType(MediaType.parseMediaType(foto.tipoMime())).body(foto.conteudo());
    }
}
