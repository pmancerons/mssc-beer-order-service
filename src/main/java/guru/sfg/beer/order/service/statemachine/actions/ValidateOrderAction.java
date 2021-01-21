package guru.sfg.beer.order.service.statemachine.actions;

import curso.common.model.events.ValidateOrderRequest;
import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.services.BeerOrderManagerImpl;
import guru.sfg.beer.order.service.web.mappers.BeerOrderMapper;
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
public class ValidateOrderAction implements Action<BeerOrderStatusEnum, BeerOrderEventEnum> {

    private final JmsTemplate jmsTemplate;
    private final BeerOrderRepository beerOrderRepository;
    private final BeerOrderMapper beerOrderMapper;

    @Override
    public void execute(StateContext<BeerOrderStatusEnum, BeerOrderEventEnum> stateContext) {
        String beerOrderId = stateContext.getMessageHeaders().get(BeerOrderManagerImpl.ORDER_ID_HEADER,String.class);

        BeerOrder beerOrder = beerOrderRepository.getOne(UUID.fromString(beerOrderId));

        log.info("sending message to: [" + JmsConfig.ORDER_VALIDATION_QUEUE + "] to validate order");

        jmsTemplate.convertAndSend(JmsConfig.ORDER_VALIDATION_QUEUE,
                ValidateOrderRequest.builder()
                        .beerOrderDto(beerOrderMapper.beerOrderToDto(beerOrder))
                        .build()
            );
    }
}
