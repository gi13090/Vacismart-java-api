package br.com.fiap.vacismart.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springdoc.core.annotations.ParameterObject;

import br.com.fiap.vacismart.service.*;
import br.com.fiap.vacismart.web.dto.*;
import br.com.fiap.vacismart.domain.enums.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Pageable;
import jakarta.validation.Valid;
import java.util.UUID;
import java.net.URI;

@RestController
@RequestMapping("/api/v1/transportes/{id}")
public class OperacaoTransporteController {
    private final MonitoramentoService monitoramento;
    private final ConsultaOperacionalService consultas;
    public OperacaoTransporteController(MonitoramentoService monitoramento, ConsultaOperacionalService consultas) { this.monitoramento = monitoramento; this.consultas = consultas; }
    @PostMapping("/leituras")
    @Operation(summary = "Registrar leitura simulada", description = "FUNCIONARIO da unidade ou GESTOR. Ingestão humana simulada; não há credencial própria de dispositivo. Transporte EM_TRANSPORTE; transporteId do corpo igual à rota. medidaEm entre início e recebimento pelo servidor. Precisão numeric(10,3), sem arredondamento. Classifica por vacina e bloqueia seletivamente; normalização não libera lote.")
    @ApiResponse(responseCode = "201", description = "Criado; Location identifica o recurso")
    public ResponseEntity<LeituraSaidaDto> registrar(@PathVariable UUID id, @Valid @RequestBody LeituraEntradaDto dto) {
        if (!id.equals(dto.transporteId())) throw new IllegalArgumentException("transporteId deve coincidir com a rota");
        var leitura = monitoramento.registrarLeitura(id, dto.dispositivoId(), dto.temperaturaC(), dto.medidaEm());
        var resposta = MapeadorDto.de(leitura);
        return ResponseEntity.created(URI.create("/api/v1/transportes/" + id + "/leituras/" + resposta.id())).body(resposta);
    }
    @GetMapping("/leituras/{leituraId}")
    @Operation(summary = "Consultar leitura do transporte", description = "FUNCIONARIO da unidade ou GESTOR. Leitura precisa pertencer ao transporte autorizado.")
    public LeituraSaidaDto leitura(@PathVariable UUID id, @PathVariable UUID leituraId) { return consultas.leitura(id, leituraId); }
    @GetMapping("/leituras")
    @Operation(summary = "Listar leituras do transporte", description = "FUNCIONARIO da unidade ou GESTOR. Ordenação: id, medidaEm, recebidaEm, temperaturaC, classificacao; padrão id.")
    public PaginaDto<LeituraSaidaDto> leituras(@PathVariable UUID id, @ParameterObject Pageable pageable) { return consultas.leituras(id, pageable); }
    @GetMapping("/ocorrencias")
    @Operation(summary = "Listar ocorrências do transporte", description = "FUNCIONARIO da unidade ou GESTOR. Filtro opcional por estado; somente consulta. Ordenação: id, abertaEm, encerradaEm, tipo, estado; padrão id.")
    public PaginaDto<OcorrenciaSaidaDto> ocorrencias(@PathVariable UUID id,
            @RequestParam(required = false) EstadoOcorrencia estado, @ParameterObject Pageable pageable) { return consultas.ocorrencias(id, estado, pageable); }
    @GetMapping("/historico")
    @Operation(summary = "Consultar histórico do transporte", description = "FUNCIONARIO da unidade ou GESTOR. Somente eventos da entidade Transporte, sem agregar eventos dos lotes. Ordenação: id, dataHora, acao; padrão id.")
    public PaginaDto<EventoSaidaDto> historico(@PathVariable UUID id, @ParameterObject Pageable pageable) { return consultas.historicoTransporte(id, pageable); }
}
