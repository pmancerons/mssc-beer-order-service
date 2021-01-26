package guru.sfg.beer.order.service.services.beer;

import curso.common.model.BeerDto;

import java.util.Optional;
import java.util.UUID;

public interface BeerService {
    Optional<BeerDto> getBeerByUpc(String upc);

    Optional<BeerDto> getBeerById(UUID beerId);
}
