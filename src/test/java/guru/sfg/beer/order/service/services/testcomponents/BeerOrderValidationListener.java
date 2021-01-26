package guru.sfg.beer.order.service.services.testcomponents;


import curso.common.events.ValidateOrderRequest;
import curso.common.events.ValidateOrderResult;
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
public class BeerOrderValidationListener {

    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConfig.ORDER_VALIDATION_QUEUE)
    public void listen(Message msg){

        log.info("validating order in embedded server ");

        ValidateOrderRequest request = (ValidateOrderRequest) msg.getPayload();

        jmsTemplate.convertAndSend(JmsConfig.ORDER_VALIDATION_RESULT_QUEUE,
                ValidateOrderResult.builder()
                    .isValid(true)
                    .orderId(request.getBeerOrderDto().getId())
                    .build()
        );
    }
}
