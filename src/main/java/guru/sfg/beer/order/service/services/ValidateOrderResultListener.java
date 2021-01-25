package guru.sfg.beer.order.service.services;

import curso.common.events.ValidateOrderResult;
import guru.sfg.beer.order.service.config.JmsConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ValidateOrderResultListener {

    private final BeerOrderManager beerOrderManager;

    @JmsListener(destination = JmsConfig.ORDER_VALIDATION_RESULT_QUEUE)
    public void getValidationOrderResult(ValidateOrderResult result){
        beerOrderManager.sendValidationOrderResult(result.getOrderId(),result.getIsValid());
    }
}
