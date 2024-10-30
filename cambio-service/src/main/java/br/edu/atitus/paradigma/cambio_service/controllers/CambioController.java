package br.edu.atitus.paradigma.cambio_service.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.edu.atitus.paradigma.cambio_service.client.CotacaoClient;
import br.edu.atitus.paradigma.cambio_service.client.CotacaoResponse;
import br.edu.atitus.paradigma.cambio_service.entities.CambioEntity;
import br.edu.atitus.paradigma.cambio_service.repositories.CambioRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@RestController
@RequestMapping("cambio-service")
public class CambioController {
	private final CambioRepository cambioRepository;
	private final CotacaoClient cotacaoBCB;
	private final CacheManager cacheMenger;
	
	private final String CAMBIO_CACHE = "cambio_cache";
	

	public CambioController(CambioRepository cambioRepository, CotacaoClient cotacaoBCB, CacheManager cacheMenger) {
		super();
		this.cambioRepository = cambioRepository;
		this.cotacaoBCB = cotacaoBCB;
		this.cacheMenger = cacheMenger;
	}
	
	@Value("${server.port}")
	private int porta;
	
	@GetMapping("/{valor}/{origem}/{destino}")
	@CircuitBreaker(name="cotacaoClient", fallbackMethod = "getCambioFromDB" )
	public ResponseEntity<CambioEntity> getCambio (
			@PathVariable double valor,
			@PathVariable String origem,
			@PathVariable String destino) throws Exception{
		

		String keyCambioCache  = origem + "_" + destino;
		
		CambioEntity cambio = cacheMenger.getCache(CAMBIO_CACHE).get(keyCambioCache, CambioEntity.class);
		
		if (cambio == null) {
			cambio = getCambioFromBancoCentral(origem, destino);
			cacheMenger.getCache(CAMBIO_CACHE).put(keyCambioCache, cambio);
		}
		
		
		cambio.setValorConvertido(valor*cambio.getFator());
		cambio.setAmbiente("Cambio-Service run in port: " + porta);
		return ResponseEntity.ok(cambio);
		
	}
	
	public CambioEntity getCambioFromBancoCentral (String origem, String destino) {
		CambioEntity cambio = new CambioEntity();
		cambio.setOrigem(origem);
		cambio.setDestino(destino);
		double fator;
		System.out.println("Tentativa via Banco Central");
		CotacaoResponse cotacaoOrigem = cotacaoBCB.getCotacaoMoedaDia(origem, "10-16-2024");
		double fatorOrigem = cotacaoOrigem.getValue().get(0).getCotacaoVenda();
		
		if (destino.equals("BRL")) {
			fator = fatorOrigem;
		}else {
			CotacaoResponse cotacaoDestino = cotacaoBCB.getCotacaoMoedaDia(destino, "10-16-2024");
			double fatorDestino = cotacaoDestino.getValue().get(0).getCotacaoVenda();
			fator = fatorOrigem / fatorDestino;
		}
		
		cambio.setFator(fator);
		return cambio;
	}
	
	public ResponseEntity<CambioEntity> getCambioFromDB(
			double valor,
			String origem,
			String destino,
			Throwable e) throws Exception{
		CambioEntity cambio = cambioRepository.findByOrigemAndDestino(origem, destino)
				.orElseThrow(() -> new Exception("Cambio não encontrado para esta origem e destino"));
		cambio.setValorConvertido(valor*cambio.getFator());
		cambio.setAmbiente("Cambio-Service run in port: " + porta + " (From DB)");
		return ResponseEntity.ok(cambio);
	}
			
	
	@ExceptionHandler(Exception.class)
	public ResponseEntity<String> handleException(Exception e){
		String cleanMessage = e.getMessage().replaceAll("[\\r\\n]", "");
		return ResponseEntity.badRequest().body(cleanMessage);
	}

}
