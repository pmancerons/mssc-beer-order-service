package guru.sfg.beer.order.service.statemachine.actions;

import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.services.BeerOrderManagerImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PartialAllocationAction implements Action<BeerOrderStatusEnum, BeerOrderEventEnum> {

    @Override
    public void execute(StateContext<BeerOrderStatusEnum, BeerOrderEventEnum> stateContext) {

        String beerOrderId = stateContext.getMessageHeaders().get(BeerOrderManagerImpl.ORDER_ID_HEADER,String.class);

        log.error("compensatin... partial allocation on beerOrderId: " + beerOrderId);
    }

}
