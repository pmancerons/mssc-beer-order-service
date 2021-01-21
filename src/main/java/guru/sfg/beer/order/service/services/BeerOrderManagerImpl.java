package guru.sfg.beer.order.service.services;

import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.statemachine.OrderStateChangeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BeerOrderManagerImpl implements BeerOrderManager {

    public static final String ORDER_ID_HEADER = "order_id";

    private final StateMachineFactory<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachineFactory;
    private final BeerOrderRepository beerOrderRepository;
    private final OrderStateChangeInterceptor orderStateChangeInterceptor;

    @Override
    public BeerOrder newBeerOrder(BeerOrder beerOrder) {
        beerOrder.setId(null);
        beerOrder.setOrderStatus(BeerOrderStatusEnum.NEW);

        BeerOrder savedOrder = beerOrderRepository.save(beerOrder);

        sendBeerOrderEvent(savedOrder,BeerOrderEventEnum.VALIDATE_ORDER);

        return savedOrder;
    }

    @Transactional
    @Override
    public void sendValidationOrderResult(UUID beerOrderId, Boolean validationResult){
        BeerOrder beerOrder = beerOrderRepository.getOne(beerOrderId);

        if(validationResult){
            sendBeerOrderEvent(beerOrder,BeerOrderEventEnum.VALIDATION_PASSED);
        }else{
            sendBeerOrderEvent(beerOrder,BeerOrderEventEnum.VALIDATION_FAILED);
        }
    }

    private void sendBeerOrderEvent(BeerOrder beerOrder, BeerOrderEventEnum event){
        StateMachine<BeerOrderStatusEnum,BeerOrderEventEnum> sm = build(beerOrder);

        Message msg = MessageBuilder.withPayload(event)
                .setHeader(ORDER_ID_HEADER,beerOrder.getId().toString())
                .build();

        sm.sendEvent(msg);
    }

    private StateMachine<BeerOrderStatusEnum,BeerOrderEventEnum> build (BeerOrder beerOrder){
        StateMachine<BeerOrderStatusEnum,BeerOrderEventEnum> sm = stateMachineFactory.getStateMachine(beerOrder.getId());

        sm.stop();

        sm.getStateMachineAccessor()
                .doWithAllRegions(sma-> {
                    sma.addStateMachineInterceptor(orderStateChangeInterceptor);
                    sma.resetStateMachine(new DefaultStateMachineContext<>(beerOrder.getOrderStatus(),null,null,null));
                });

        sm.start();

        return sm;

    }

}
