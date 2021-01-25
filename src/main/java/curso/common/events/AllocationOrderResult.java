package curso.common.events;

import curso.common.model.BeerOrderDto;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AllocationOrderResult {

    private BeerOrderDto beerOrderDto;
    private Boolean allocationError;
    private Boolean pendingInventory;

}
