package br.com.fiap.vacismart.web.controller;

import br.com.fiap.vacismart.service.FluxoOperacionalService;
import br.com.fiap.vacismart.web.dto.AplicacaoEntradaDto;
import br.com.fiap.vacismart.web.dto.AplicacaoSaidaDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/aplicacoes")
public class AplicacaoController {
    private final FluxoOperacionalService fluxo;

    public AplicacaoController(FluxoOperacionalService fluxo) { this.fluxo = fluxo; }

    @PostMapping
    @Operation(summary = "Verificar aplicação simulada", description = "FUNCIONARIO ativo da unidade ou GESTOR global. "
            + "Exige lote LIBERADO; demais estados geram auditoria imutável e 409 após o commit. "
            + "Sem auditoria para acesso negado. Operação exclusivamente demonstrativa: não autoriza aplicação real, "
            + "não registra administração clínica nem modifica estoque ou disponibilidade.")
    @ApiResponse(responseCode = "200", description = "Verificação demonstrativa permitida; sem administração real")
    @ApiResponse(responseCode = "404", description = "Lote inexistente no escopo do gestor")
    public AplicacaoSaidaDto verificar(@Valid @RequestBody AplicacaoEntradaDto dto) {
        if (!fluxo.verificarAplicacao(dto.loteId())) {
            throw new IllegalStateException("Verificação demonstrativa rejeitada: lote não LIBERADO");
        }
        return new AplicacaoSaidaDto("PERMITIDA",
                "Verificação demonstrativa permitiu a aplicação. Não autoriza nem registra administração real de vacina.");
    }
}
