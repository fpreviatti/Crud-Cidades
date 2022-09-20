package br.edu.utfpr.cp.espjava.crudcidades.cidade;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.*;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

@Controller
public class CidadeController {

    private Set<Cidade> cidades;

    private final CidadeRepository repository;

    public CidadeController(CidadeRepository repository) {
        cidades = new HashSet<>();
        this.repository = repository;
    }

    @GetMapping("/")
    public String listar(Model memoria, Principal usuario, HttpSession sessao, HttpServletResponse response) {

        memoria.addAttribute("listaCidades", repository
                                                    .findAll()
                                                    .stream()
                                                    .map(cidade -> new Cidade(cidade.getNome(),cidade.getEstado()))
                                                    .collect(Collectors.toList()));

        response.addCookie(new Cookie("listar", LocalDateTime.now().toString()));

        sessao.setAttribute("usuarioAtual", usuario.getName());

        return "/crud";
    }
    
    @GetMapping("/cidades")
    public ResponseEntity<List<Cidade>> listarCidades(Model memoria, HttpSession sessao, HttpServletResponse response) {

        List<Cidade> cidades = repository
                .findAll()
                .stream()
                .map(cidade -> new Cidade(cidade.getNome(), cidade.getEstado()))
                .collect(Collectors.toList());

        return ResponseEntity.ok().body(cidades);

    }

    @PostMapping("/criar")
    public String criar(@Valid Cidade cidade, BindingResult validacao, Model memoria, HttpServletResponse response) {

        if (validacao.hasErrors()){

            validacao
                    .getFieldErrors()
                    .forEach(error ->
                                    memoria.addAttribute(
                                                    error.getField(),
                                                    error.getDefaultMessage())
                                    );

            response.addCookie(new Cookie("criar", LocalDateTime.now().toString()));

            memoria.addAttribute("nomeInformado", cidade.getNome());
            memoria.addAttribute("estadoInformado", cidade.getEstado());
            memoria.addAttribute("listaCidades", cidades);
            return ("/crud");
        }

        else{
            var cidadeAtual = repository.findByNomeAndEstado(cidade.getNome(),cidade.getEstado());

            if(!cidadeAtual.isPresent()){
                repository.save(cidade.clonar());
            }

        }

        return "redirect:/";
    }

    @GetMapping("/excluir")
    public String excluir(
            @RequestParam String nome, 
            @RequestParam String estado,
            HttpServletResponse response) {

        response.addCookie(new Cookie("excluir", LocalDateTime.now().toString()));

        var cidadeEstadoEncontrada = repository.findByNomeAndEstado(nome,estado);
        cidadeEstadoEncontrada.ifPresent(repository::delete);

        return "redirect:/";
    }

    @GetMapping("/preparaAlterar")
    public String preparaAlterar(
        @RequestParam String nome, 
        @RequestParam String estado,
        Model memoria) {

            var cidadeAtual = repository.findByNomeAndEstado(nome,estado);

            cidadeAtual.ifPresent(cidadeEncontrada -> {
                memoria.addAttribute("cidadeAtual", cidadeEncontrada);
                memoria.addAttribute("listaCidades", repository.findAll());
            });

            return "/crud";
    }

    @PostMapping("/alterar")
    public String alterar(
        @RequestParam String nomeAtual, 
        @RequestParam String estadoAtual,
        Cidade cidade,
        HttpServletResponse response) {

        response.addCookie(new Cookie("alterar", LocalDateTime.now().toString()));

        var cidadeAtual = repository.findByNomeAndEstado(nomeAtual, estadoAtual);

        if(cidadeAtual.isPresent()){
            var cidadeEncontrada = cidadeAtual.get();
            cidadeEncontrada.setNome(cidade.getNome());
            cidadeEncontrada.setEstado(cidade.getEstado());

            repository.saveAndFlush(cidadeEncontrada);
        }

            return "redirect:/";
    }

    @GetMapping("/mostrar")
    @ResponseBody
    public String mostraCookieAlterar(@CookieValue String listar){
        return "Último acesso ao método listar(): " +listar;
    }

    @PostMapping("/limpar")
    public String limpar(){
        repository.deleteAll();
        return "redirect:/";
    }
}

