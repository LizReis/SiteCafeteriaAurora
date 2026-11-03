package br.com.cafeaurora.controller;

import br.com.cafeaurora.dto.*;
import br.com.cafeaurora.enums.*;
import br.com.cafeaurora.exception.RegraNegocioException;
import br.com.cafeaurora.service.*;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminPainelController {
    private final CategoriaService categorias;
    private final ProdutoService produtos;
    private final AdminPedidoService pedidos;

    public AdminPainelController(CategoriaService categorias, ProdutoService produtos,
                                AdminPedidoService pedidos) {
        this.categorias = categorias;
        this.produtos = produtos;
        this.pedidos = pedidos;
    }

    @GetMapping("/login")
    public String login() { return "admin/login"; }

    @GetMapping({"", "/"})
    public String inicio(Model model) {
        model.addAttribute("totalPedidos", pedidos.contar());
        model.addAttribute("pendentes", pedidos.contar(StatusPedido.RECEBIDO));
        model.addAttribute("emPreparo", pedidos.contar(StatusPedido.EM_PREPARO));
        model.addAttribute("prontos", pedidos.contar(StatusPedido.PRONTO));
        model.addAttribute("totalProdutos", produtos.listar().size());
        model.addAttribute("recentes", pedidos.listar(null, 0, 5).getContent());
        return "admin/index";
    }

    private void prepararCategorias(Model model) {
        model.addAttribute("categorias", categorias.listar());
        model.addAttribute("tipos", TipoCardapio.values());
    }

    @GetMapping("/categorias")
    public String categorias(@RequestParam(required = false) Long editar, Model model) {
        prepararCategorias(model);
        if (editar == null) model.addAttribute("form", new CategoriaRequest("", "", true, TipoCardapio.CAFE, 0));
        else {
            CategoriaResponse categoria = categorias.buscar(editar);
            model.addAttribute("form", new CategoriaRequest(categoria.nome(), categoria.descricao(),
                categoria.ativa(), categoria.tipoCardapio(), categoria.ordemExibicao()));
        }
        model.addAttribute("editando", editar);
        return "admin/categorias";
    }

    @PostMapping("/categorias/salvar")
    public String salvarCategoria(@RequestParam(required = false) Long id,
            @Valid @ModelAttribute("form") CategoriaRequest form, BindingResult erros,
            Model model, RedirectAttributes flash) {
        if (!erros.hasErrors()) {
            try {
                if (id == null) categorias.criar(form);
                else categorias.atualizar(id, form);
                flash.addFlashAttribute("sucesso", "Categoria salva com sucesso.");
                return "redirect:/admin/categorias";
            } catch (RegraNegocioException | DataIntegrityViolationException ex) {
                model.addAttribute("erroNegocio", ex instanceof RegraNegocioException ?
                    ex.getMessage() : "Já existe uma categoria com esses dados.");
            }
        }
        prepararCategorias(model);
        model.addAttribute("editando", id);
        return "admin/categorias";
    }

    @PostMapping("/categorias/{id}/desativar")
    public String desativarCategoria(@PathVariable Long id, RedirectAttributes flash) {
        categorias.desativar(id);
        flash.addFlashAttribute("sucesso", "Categoria desativada. Seus registros foram preservados.");
        return "redirect:/admin/categorias";
    }

    private void prepararProdutos(Model model) {
        model.addAttribute("produtos", produtos.listar());
        model.addAttribute("categorias", categorias.listar());
    }

    @GetMapping("/produtos")
    public String produtos(@RequestParam(required = false) Long editar, Model model) {
        prepararProdutos(model);
        if (editar == null) model.addAttribute("form",
            new ProdutoRequest("", "", null, "", null, true, true));
        else {
            ProdutoResponse produto = produtos.buscar(editar);
            model.addAttribute("form", new ProdutoRequest(produto.nome(), produto.descricao(),
                produto.preco(), produto.imagemUrl(), produto.categoriaId(),
                produto.ativo(), produto.disponivel()));
        }
        model.addAttribute("editando", editar);
        return "admin/produtos";
    }

    @PostMapping("/produtos/salvar")
    public String salvarProduto(@RequestParam(required = false) Long id,
            @Valid @ModelAttribute("form") ProdutoRequest form, BindingResult erros,
            Model model, RedirectAttributes flash) {
        if (!erros.hasErrors()) {
            try {
                if (id == null) produtos.criar(form);
                else produtos.atualizar(id, form);
                flash.addFlashAttribute("sucesso", "Produto salvo com sucesso.");
                return "redirect:/admin/produtos";
            } catch (RegraNegocioException | DataIntegrityViolationException ex) {
                model.addAttribute("erroNegocio", ex instanceof RegraNegocioException ?
                    ex.getMessage() : "Não foi possível salvar esse produto.");
            }
        }
        prepararProdutos(model);
        model.addAttribute("editando", id);
        return "admin/produtos";
    }

    @PostMapping("/produtos/{id}/desativar")
    public String desativarProduto(@PathVariable Long id, RedirectAttributes flash) {
        produtos.desativar(id);
        flash.addFlashAttribute("sucesso", "Produto desativado sem excluir o histórico.");
        return "redirect:/admin/produtos";
    }

    @GetMapping("/pedidos")
    public String pedidos(@RequestParam(required = false) StatusPedido status,
            @RequestParam(defaultValue = "0") int pagina, Model model) {
        Page<AdminPedidoService.Resumo> resultado = pedidos.listar(status, pagina, 15);
        model.addAttribute("pedidos", resultado.getContent());
        model.addAttribute("pagina", resultado);
        model.addAttribute("filtro", status);
        model.addAttribute("situacoes", StatusPedido.values());
        return "admin/pedidos";
    }

    @GetMapping("/pedidos/{id}")
    public String pedido(@PathVariable Long id, Model model) {
        model.addAttribute("pedido", pedidos.detalhar(id));
        return "admin/pedido";
    }

    @PostMapping("/pedidos/{id}/status")
    public String atualizarStatus(@PathVariable Long id, @RequestParam StatusPedido status,
            RedirectAttributes flash) {
        try {
            pedidos.atualizarStatus(id, status);
            flash.addFlashAttribute("sucesso", "Status do pedido atualizado.");
        } catch (RegraNegocioException ex) {
            flash.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/admin/pedidos/" + id;
    }
}
