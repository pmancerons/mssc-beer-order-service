package guru.sfg.beer.order.service.services;

import curso.common.events.AllocationOrderResult;
import guru.sfg.beer.order.service.config.JmsConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AllocationOrderResultListener {

    private final BeerOrderManager beerOrderManager;

    @JmsListener(destination = JmsConfig.ORDER_ALLOCATION_RESULT_QUEUE)
    public void listenAllocationOrderResult(AllocationOrderResult result){
        if(!result.getAllocationError() && !result.getPendingInventory()){
            //allocated normally
            beerOrderManager.beerOrderAllocationPassed(result.getBeerOrderDto());
        } else if(!result.getAllocationError() && result.getPendingInventory()) {
            //pending inventory
            beerOrderManager.beerOrderAllocationPendingInventory(result.getBeerOrderDto());
        } else if(result.getAllocationError()){
            //allocation error
            beerOrderManager.beerOrderAllocationFailed(result.getBeerOrderDto());
        }
    }
}
