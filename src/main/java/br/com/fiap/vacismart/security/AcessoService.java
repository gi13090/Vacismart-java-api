package br.com.fiap.vacismart.security;

import br.com.fiap.vacismart.domain.entity.*;
import br.com.fiap.vacismart.domain.enums.Perfil;
import br.com.fiap.vacismart.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class AcessoService {
    private final UsuarioRepository usuarios;
    private final TransporteRepository transportes;
    private final LoteRepository lotes;
    public AcessoService(UsuarioRepository usuarios, TransporteRepository transportes, LoteRepository lotes) {
        this.usuarios = usuarios; this.transportes = transportes; this.lotes = lotes;
    }
    public Usuario atual() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new BadCredentialsException("Autenticação necessária");
        UUID id;
        try { id = UUID.fromString(auth.getName()); }
        catch (IllegalArgumentException ex) { throw new BadCredentialsException("Identidade inválida"); }
        return usuarios.findById(id).filter(Usuario::isAtivo)
                .orElseThrow(() -> new BadCredentialsException("Identidade inválida"));
    }
    public void operacional() {
        if (atual().getPerfil() == Perfil.CIDADAO) throw new AccessDeniedException("Acesso negado");
    }
    public void gestor() {
        if (atual().getPerfil() != Perfil.GESTOR) throw new AccessDeniedException("Acesso negado");
    }
    public UUID unidadeEscopo() {
        operacional();
        var usuario = atual();
        return usuario.getPerfil() == Perfil.GESTOR ? null : usuario.getUnidadeSaude().getId();
    }
    public UUID filtrarUnidade(UUID solicitada) {
        UUID escopo = unidadeEscopo();
        if (escopo != null && solicitada != null && !escopo.equals(solicitada)) throw new AccessDeniedException("Acesso negado");
        return escopo == null ? solicitada : escopo;
    }
    public void unidade(UUID id) { filtrarUnidade(id); }
    public void transporte(UUID id) {
        UUID escopo = unidadeEscopo();
        var transporte = transportes.findById(id);
        if (escopo != null && (transporte.isEmpty() || !escopo.equals(transporte.get().getUnidadeSaude().getId())))
            throw new AccessDeniedException("Acesso negado");
        if (transporte.isEmpty()) throw new NoSuchElementException("Recurso não encontrado");
    }
    public void lote(UUID id) {
        UUID escopo = unidadeEscopo();
        var lote = lotes.findById(id);
        if (escopo != null && (lote.isEmpty() || !escopo.equals(lote.get().getTransporte().getUnidadeSaude().getId())))
            throw new AccessDeniedException("Acesso negado");
        if (lote.isEmpty()) throw new NoSuchElementException("Recurso não encontrado");
    }
}
