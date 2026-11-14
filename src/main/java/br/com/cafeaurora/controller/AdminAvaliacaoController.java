package br.com.cafeaurora.controller;
import br.com.cafeaurora.enums.StatusModeracao;
import br.com.cafeaurora.service.AvaliacaoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller @RequestMapping("/admin/avaliacoes")
public class AdminAvaliacaoController {
    private final AvaliacaoService service;
    public AdminAvaliacaoController(AvaliacaoService service){this.service=service;}
    @GetMapping
    public String listar(@RequestParam(required=false) StatusModeracao status,@RequestParam(defaultValue="0") int pagina,Model model) {
        model.addAttribute("pagina",service.listar(status,pagina));model.addAttribute("situacoes",StatusModeracao.values());
        model.addAttribute("filtro",status);return "admin/avaliacoes";
    }
    @PostMapping("/{id}/aprovar")
    public String aprovar(@PathVariable Long id,RedirectAttributes flash){return moderar(id,StatusModeracao.APROVADA,flash);}
    @PostMapping("/{id}/rejeitar")
    public String rejeitar(@PathVariable Long id,RedirectAttributes flash){return moderar(id,StatusModeracao.REJEITADA,flash);}
    private String moderar(Long id,StatusModeracao status,RedirectAttributes flash) {
        service.moderar(id,status);flash.addFlashAttribute("sucesso","Moderação registrada");return "redirect:/admin/avaliacoes";
    }
}
