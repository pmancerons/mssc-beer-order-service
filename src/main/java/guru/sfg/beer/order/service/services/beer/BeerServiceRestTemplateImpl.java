package guru.sfg.beer.order.service.services.beer;

import guru.sfg.beer.order.service.services.beer.model.BeerDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@ConfigurationProperties(prefix = "sfg.brewery", ignoreUnknownFields = false)
@Component
public class BeerServiceRestTemplateImpl implements BeerService {

    private final String BEER_PATH_UPC = "/api/v1/beerUpc/";
    private final String BEER_PATH_ID = "/api/v1/beer/";
    private final RestTemplate restTemplate;

    private String beerServiceHost;

    public void setBeerServiceHost(String beerServiceHost) {
        this.beerServiceHost = beerServiceHost;
    }

    public BeerServiceRestTemplateImpl(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    @Override
    public Optional<BeerDto> getBeerByUpc(String upc) {

        log.debug("Calling Beer Service upc: " + upc);

        return Optional.of(restTemplate.getForObject(beerServiceHost + BEER_PATH_UPC + upc , BeerDto.class));

    }

    @Override
    public Optional<BeerDto> getBeerById(UUID beerId) {
        log.debug("Calling Beer Service id: " + beerId);

        return Optional.of(restTemplate.getForObject(beerServiceHost + BEER_PATH_ID + beerId , BeerDto.class));
    }
}
