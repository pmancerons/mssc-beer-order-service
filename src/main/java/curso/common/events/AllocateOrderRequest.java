package curso.common.events;

import curso.common.model.BeerOrderDto;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AllocateOrderRequest {
    private BeerOrderDto beerOrderDto;
}
