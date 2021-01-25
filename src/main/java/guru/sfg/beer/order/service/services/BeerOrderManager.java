package guru.sfg.beer.order.service.services;

import curso.common.model.BeerOrderDto;
import guru.sfg.beer.order.service.domain.BeerOrder;

import java.util.UUID;

public interface BeerOrderManager {

    BeerOrder newBeerOrder(BeerOrder beerOrder);

    void sendValidationOrderResult(UUID beerOrderId, Boolean validationResult);

    void beerOrderAllocationPassed(BeerOrderDto beerOrder);

    void beerOrderAllocationPendingInventory(BeerOrderDto beerOrder);

    void beerOrderAllocationFailed(BeerOrderDto beerOrder);
}
