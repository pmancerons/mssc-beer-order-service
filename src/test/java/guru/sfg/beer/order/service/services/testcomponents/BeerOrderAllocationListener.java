package guru.sfg.beer.order.service.services.testcomponents;

import curso.common.events.AllocateOrderRequest;
import curso.common.events.AllocationOrderResult;
import guru.sfg.beer.order.service.config.JmsConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class BeerOrderAllocationListener {

    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConfig.ORDER_ALLOCATION_QUEUE)
    public void listen(Message msg){

        log.info("allocating order in embedded server ");

        AllocateOrderRequest request = (AllocateOrderRequest) msg.getPayload();

        request.getBeerOrderDto().getBeerOrderLines().forEach(
                beerOrderLineDto -> {
                    beerOrderLineDto.setQuantityAllocated(beerOrderLineDto.getOrderQuantity());
                }
        );

        jmsTemplate.convertAndSend(JmsConfig.ORDER_ALLOCATION_RESULT_QUEUE,
                AllocationOrderResult.builder()
                        .allocationError(false)
                        .pendingInventory(false)
                        .beerOrderDto(request.getBeerOrderDto())
                        .build()
        );
    }
}
