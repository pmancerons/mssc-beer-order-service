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

        Boolean allocationError = "allocation-failed".equals(request.getBeerOrderDto().getCustomerRef());

        Boolean partialAllocation = "allocation-partial".equals(request.getBeerOrderDto().getCustomerRef());

        request.getBeerOrderDto().getBeerOrderLines().forEach(
                beerOrderLineDto -> {
                    beerOrderLineDto.setQuantityAllocated(beerOrderLineDto.getOrderQuantity());
                }
        );

        if(!"dont-allocate".equals(request.getBeerOrderDto().getCustomerRef())) {
            jmsTemplate.convertAndSend(JmsConfig.ORDER_ALLOCATION_RESULT_QUEUE,
                    AllocationOrderResult.builder()
                            .allocationError(allocationError)
                            .pendingInventory(partialAllocation)
                            .beerOrderDto(request.getBeerOrderDto())
                            .build()
            );
        }
    }
}
