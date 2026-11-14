package br.com.cafeaurora.service;

import br.com.cafeaurora.dto.*;
import br.com.cafeaurora.enums.*;
import br.com.cafeaurora.exception.*;
import br.com.cafeaurora.model.*;
import br.com.cafeaurora.repository.*;
import jakarta.validation.Validator;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.data.domain.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Service
public class AvaliacaoService {
    private final AvaliacaoRepository avaliacoes;
    private final AvaliacaoFotoRepository fotos;
    private final PedidoRepository pedidos;
    private final Validator validator;
    private final FotoAvaliacaoService imagens;
    private final TransactionTemplate transacao;
    public AvaliacaoService(AvaliacaoRepository avaliacoes,AvaliacaoFotoRepository fotos,PedidoRepository pedidos,
        Validator validator,FotoAvaliacaoService imagens,PlatformTransactionManager manager) {
        this.avaliacoes=avaliacoes;this.fotos=fotos;this.pedidos=pedidos;this.validator=validator;this.imagens=imagens;
        this.transacao=new TransactionTemplate(manager);
    }
    public void registrar(AvaliacaoRequest req,MultipartFile arquivo) {
        if(req==null)throw new EntradaInvalidaException("Dados da avaliação inválidos");
        if(req.referencia()==null||!req.referencia().matches("[A-Za-z0-9_-]{43}"))throw inexistente();
        if(!validator.validate(req).isEmpty())throw new EntradaInvalidaException("Informe nome (2–100), nota (1–5), comentário (5–1000) e autorize a publicação");
        var elegibilidade=pedidos.consultarElegibilidadeAvaliacao(req.referencia()).orElseThrow(this::inexistente);
        validarElegibilidade(elegibilidade.getStatus(),elegibilidade.isJaAvaliado());
        // A consulta leve já terminou; a decodificação não segura lock no pedido.
        var foto=imagens.validar(arquivo);
        try {
            transacao.executeWithoutResult(tx->{
                var pedido=pedidos.buscarParaAvaliacao(req.referencia()).orElseThrow(this::inexistente);
                validarElegibilidade(pedido.getStatus(),avaliacoes.existsByPedidoId(pedido.getId()));
                var avaliacao=avaliacoes.saveAndFlush(new Avaliacao(pedido,req.nomeAutor(),req.nota(),req.comentario()));
                if(foto!=null)fotos.saveAndFlush(new AvaliacaoFoto(avaliacao,foto.tipoMime(),foto.conteudo()));
            });
        } catch(DataAccessException ex) {
            // A transação com eventual violação de unicidade já foi revertida antes desta leitura.
            boolean existe=transacao.execute(tx->pedidos.findByReferenciaPublica(req.referencia())
                .map(p->avaliacoes.existsByPedidoId(p.getId())).orElse(false));
            if(existe)throw new RegraNegocioException("A avaliação deste pedido já foi registrada");
            throw new AvaliacaoPersistenciaException();
        }
    }
    private void validarElegibilidade(StatusPedido status,boolean jaAvaliado) {
        if(status!=StatusPedido.CONCLUIDO)throw new RegraNegocioException("Somente pedidos concluídos podem ser avaliados");
        if(jaAvaliado)throw new RegraNegocioException("A avaliação deste pedido já foi registrada");
    }
    @Transactional(readOnly=true)
    public List<AvaliacaoPublica> publicas() {
        return avaliacoes.findByStatusModeracao(StatusModeracao.APROVADA,pagina(0,5)).stream().map(a->{
            String imagem=fotos.existsByAvaliacaoId(a.getId())?"/api/avaliacoes/"+a.getId()+"/foto":
                a.getPedido().getItens().stream().map(i->CardapioProduto.imagemSegura(i.getImagemUrl())).findFirst().orElse("/img/cupPrincipal.jpg");
            return new AvaliacaoPublica(a.getId(),a.getNomeAutor(),a.getNota(),a.getComentario(),a.getCriadaEm(),imagem);
        }).toList();
    }
    @Transactional(readOnly=true)
    public Page<AvaliacaoAdmin> listar(StatusModeracao status,int numero) {
        var lista=status==null?avaliacoes.findAll(pagina(numero,15)):avaliacoes.findByStatusModeracao(status,pagina(numero,15));
        return lista.map(a->new AvaliacaoAdmin(a.getId(),a.getPedido().getId(),a.getNomeAutor(),a.getNota(),a.getComentario(),
            a.getCriadaEm(),a.getStatusModeracao(),a.getModeradaEm(),fotos.existsByAvaliacaoId(a.getId())));
    }
    @Transactional
    public void moderar(Long id,StatusModeracao status) {obter(id).moderar(status);}
    @Transactional(readOnly=true)
    public FotoAvaliacaoService.Foto foto(Long id,boolean administrativa) {
        var a=obter(id);
        if(!administrativa&&a.getStatusModeracao()!=StatusModeracao.APROVADA)throw inexistente();
        var f=fotos.findByAvaliacaoId(id).orElseThrow(this::inexistente);
        return new FotoAvaliacaoService.Foto(f.getTipoMime(),f.getConteudo());
    }
    private Avaliacao obter(Long id){return avaliacoes.findById(id).orElseThrow(this::inexistente);}
    private RecursoNaoEncontradoException inexistente(){return new RecursoNaoEncontradoException("Recurso não encontrado");}
    private Pageable pagina(int numero,int tamanho){return PageRequest.of(Math.max(0,numero),tamanho,Sort.by(Sort.Order.desc("criadaEm"),Sort.Order.desc("id")));}
}
