package guru.sfg.beer.order.service.statemachine.actions;

import curso.common.events.AllocationFailureEvent;
import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.services.BeerOrderManagerImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AllocationFailureAction  implements Action<BeerOrderStatusEnum, BeerOrderEventEnum> {

    private final JmsTemplate jmsTemplate;

    @Override
    public void execute(StateContext<BeerOrderStatusEnum, BeerOrderEventEnum> stateContext) {
        String beerOrderId = stateContext.getMessageHeaders().get(BeerOrderManagerImpl.ORDER_ID_HEADER,String.class);

        log.info("sending message to: [" + JmsConfig.ORDER_ALLOCATION_FAILURE_QUEUE + "] to inform allocation failure order");

        jmsTemplate.convertAndSend(JmsConfig.ORDER_ALLOCATION_FAILURE_QUEUE,
                AllocationFailureEvent.builder().orderId(UUID.fromString(beerOrderId)).build());
    }

}
