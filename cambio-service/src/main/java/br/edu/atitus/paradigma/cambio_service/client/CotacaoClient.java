package br.edu.atitus.paradigma.cambio_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "bancoCentralClient", url = "https://olinda.bcb.gov.br/olinda/servico/PTAX/versao/v1/odata")
public interface CotacaoClient {

    @GetMapping("/CotacaoMoedaDia(moeda='{moeda}',dataCotacao='{dataCotacao}')?$format=json")
    CotacaoResponse getCotacaoMoedaDia(
        @PathVariable("moeda") String moeda, 
        @PathVariable("dataCotacao") String dataCotacao
    );
}
