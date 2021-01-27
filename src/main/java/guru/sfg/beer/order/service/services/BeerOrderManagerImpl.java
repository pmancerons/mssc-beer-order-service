package guru.sfg.beer.order.service.services;

import curso.common.model.BeerOrderDto;
import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.statemachine.OrderStateChangeInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BeerOrderManagerImpl implements BeerOrderManager {

    public static final String ORDER_ID_HEADER = "order_id";

    private final StateMachineFactory<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachineFactory;
    private final BeerOrderRepository beerOrderRepository;
    private final OrderStateChangeInterceptor orderStateChangeInterceptor;

    @Transactional
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

        log.info("result of validating order : " + validationResult);

        if(validationResult){
            sendBeerOrderEvent(beerOrder,BeerOrderEventEnum.VALIDATION_PASSED);

            BeerOrder beerOrderValidated = beerOrderRepository.findOneById(beerOrderId);

            sendBeerOrderEvent(beerOrderValidated, BeerOrderEventEnum.ALLOCATE_ORDER);
        }else{
            sendBeerOrderEvent(beerOrder,BeerOrderEventEnum.VALIDATION_FAILED);
        }
    }

    @Transactional
    @Override
    public void beerOrderAllocationPassed(BeerOrderDto beerOrderDto) {
        BeerOrder beerOrder = beerOrderRepository.getOne(beerOrderDto.getId());
        sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_SUCCESS);
        updateAllocatedQty(beerOrderDto);
    }

    @Transactional
    @Override
    public void beerOrderAllocationPendingInventory(BeerOrderDto beerOrderDto) {
        BeerOrder beerOrder = beerOrderRepository.getOne(beerOrderDto.getId());
        sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_NO_INVENTORY);

        updateAllocatedQty(beerOrderDto);
    }

    void updateAllocatedQty(BeerOrderDto beerOrderDto) {
        BeerOrder allocatedOrder = beerOrderRepository.getOne(beerOrderDto.getId());

        allocatedOrder.getBeerOrderLines().forEach(beerOrderLine -> {
            beerOrderDto.getBeerOrderLines().forEach(beerOrderLineDto -> {
                if(beerOrderLine.getId() .equals(beerOrderLineDto.getId())){
                    beerOrderLine.setQuantityAllocated(beerOrderLineDto.getQuantityAllocated());
                }
            });
        });

        beerOrderRepository.saveAndFlush(allocatedOrder);
    }

    @Transactional
    @Override
    public void beerOrderAllocationFailed(BeerOrderDto beerOrderDto) {
        BeerOrder beerOrder = beerOrderRepository.getOne(beerOrderDto.getId());
        sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_FAILED);
    }

    @Transactional
    @Override
    public void beerOrderPickedUp(UUID id) {
        Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(id);

        beerOrderOptional.ifPresentOrElse(beerOrder -> {
            sendBeerOrderEvent(beerOrder,BeerOrderEventEnum.BEER_ORDER_PICKED_UP);
        },() -> log.error("order not found, id: " + id));
    }

    private void sendBeerOrderEvent(BeerOrder beerOrder, BeerOrderEventEnum event){
        log.info("*sending event: " + event + ", beer order status : " + beerOrder.getOrderStatus());

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
