package guru.sfg.beer.order.service.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import curso.common.events.AllocationFailureEvent;
import curso.common.events.DeallocateOrderRequest;
import curso.common.model.BeerDto;
import de.mkammerer.wiremock.WireMockExtension;
import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderLine;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.domain.Customer;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.repositories.CustomerRepository;
import guru.sfg.beer.order.service.services.beer.BeerServiceRestTemplateImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@SpringBootTest
public class BeerOrderManagementIT {

    @Autowired
    BeerOrderManager beerOrderManager;

    @Autowired
    BeerOrderRepository beerOrderRepository;

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JmsTemplate jmsTemplate;

    Customer testCustomer;

    UUID beerId = UUID.randomUUID();

    @RegisterExtension
    WireMockExtension wireMock = new WireMockExtension(options().port(8083));

    @BeforeEach
    void setUp() {

        testCustomer = customerRepository.save(
                Customer.builder()
                        .customerName("test customer")
                        .build()
        );
    }

    @Test
    void testNewToAllocated() throws JsonProcessingException{

        BeerDto beerDto = BeerDto.builder().id(beerId).upc("12345").build();

        BeerOrder beerOrder = createBeerOrder();

        wireMock.stubFor(
                get(BeerServiceRestTemplateImpl.BEER_PATH_UPC + "12345")
                        .willReturn(okJson(objectMapper.writeValueAsString(beerDto)))
        );

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted( () -> {
            BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();

            assertEquals(BeerOrderStatusEnum.ALLOCATED , foundOrder.getOrderStatus());
        });

        await().untilAsserted( () -> {
            BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();

            BeerOrderLine beerOrderLine = foundOrder.getBeerOrderLines().iterator().next();
            assertEquals(beerOrderLine.getOrderQuantity() , beerOrderLine.getQuantityAllocated());
        });

        savedBeerOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();

        assertNotNull(savedBeerOrder);
        assertEquals(BeerOrderStatusEnum.ALLOCATED, savedBeerOrder.getOrderStatus());

        savedBeerOrder.getBeerOrderLines().forEach(
                beerOrderLine -> {
                    assertEquals(beerOrderLine.getOrderQuantity(),beerOrderLine.getQuantityAllocated());
                }
        );
    }

    @Test
    void testNewToPickedUp() throws JsonProcessingException {

        BeerDto beerDto = BeerDto.builder().id(beerId).upc("12345").build();

        BeerOrder beerOrder = createBeerOrder();

        wireMock.stubFor(
                get(BeerServiceRestTemplateImpl.BEER_PATH_UPC + "12345")
                        .willReturn(okJson(objectMapper.writeValueAsString(beerDto)))
        );

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted( () -> {
            BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();

            assertEquals(BeerOrderStatusEnum.ALLOCATED , foundOrder.getOrderStatus());
        });

        beerOrderManager.beerOrderPickedUp(savedBeerOrder.getId());

        await().untilAsserted( () -> {
            BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();

            assertEquals(BeerOrderStatusEnum.PICKED_UP , foundOrder.getOrderStatus());
        });

        savedBeerOrder = beerOrderRepository.findById(beerOrder.getId()).get();
        assertEquals(BeerOrderStatusEnum.PICKED_UP , savedBeerOrder.getOrderStatus());
    }

    @Test
    void testNewToValidationFailed() throws JsonProcessingException {

        BeerDto beerDto = BeerDto.builder().id(beerId).upc("12345").build();

        BeerOrder beerOrder = createBeerOrder();
        beerOrder.setCustomerRef("validation-failed");

        wireMock.stubFor(
                get(BeerServiceRestTemplateImpl.BEER_PATH_UPC + "12345")
                        .willReturn(okJson(objectMapper.writeValueAsString(beerDto)))
        );

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted( () -> {
            BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();

            assertEquals(BeerOrderStatusEnum.VALIDATION_EXCEPTION , foundOrder.getOrderStatus());
        });
    }

    @Test
    void testNewToAllocationFailed() throws JsonProcessingException {

        BeerDto beerDto = BeerDto.builder().id(beerId).upc("12345").build();

        BeerOrder beerOrder = createBeerOrder();
        beerOrder.setCustomerRef("allocation-failed");

        wireMock.stubFor(
                get(BeerServiceRestTemplateImpl.BEER_PATH_UPC + "12345")
                        .willReturn(okJson(objectMapper.writeValueAsString(beerDto)))
        );

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted( () -> {
            BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();

            assertEquals(BeerOrderStatusEnum.ALLOCATION_EXCEPTION , foundOrder.getOrderStatus());
        });

        AllocationFailureEvent allocationFailureEvent = (AllocationFailureEvent) jmsTemplate.receiveAndConvert(JmsConfig.ORDER_ALLOCATION_FAILURE_QUEUE);

        assertNotNull(allocationFailureEvent);
        assertThat(allocationFailureEvent.getOrderId()).isEqualTo(savedBeerOrder.getId());
    }

    @Test
    void testNewToPartialAllocation() throws JsonProcessingException {

        BeerDto beerDto = BeerDto.builder().id(beerId).upc("12345").build();

        BeerOrder beerOrder = createBeerOrder();
        beerOrder.setCustomerRef("allocation-partial");

        wireMock.stubFor(
                get(BeerServiceRestTemplateImpl.BEER_PATH_UPC + "12345")
                        .willReturn(okJson(objectMapper.writeValueAsString(beerDto)))
        );

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted( () -> {
            BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();

            assertEquals(BeerOrderStatusEnum.PENDING_INVENTORY , foundOrder.getOrderStatus());
        });
    }

    @Test
    void tesValidationPendingToCancelled() throws JsonProcessingException {

        BeerDto beerDto = BeerDto.builder().id(beerId).upc("12345").build();

        BeerOrder beerOrder = createBeerOrder();
        beerOrder.setCustomerRef("dont-validate");

        wireMock.stubFor(
                get(BeerServiceRestTemplateImpl.BEER_PATH_UPC + "12345")
                        .willReturn(okJson(objectMapper.writeValueAsString(beerDto)))
        );

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted( () -> {
            BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();

            assertEquals(BeerOrderStatusEnum.VALIDATION_PENDING , foundOrder.getOrderStatus());
        });

        beerOrderManager.cancelOrder(savedBeerOrder.getId());

        await().untilAsserted( () -> {
            BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();

            assertEquals(BeerOrderStatusEnum.CANCELLED , foundOrder.getOrderStatus());
        });

    }

    @Test
    void testValidatedToCancelled(){

    }

    @Test
    void testAllocationPendingToCancelled() throws JsonProcessingException {

        BeerDto beerDto = BeerDto.builder().id(beerId).upc("12345").build();

        BeerOrder beerOrder = createBeerOrder();
        beerOrder.setCustomerRef("dont-allocate");

        wireMock.stubFor(
                get(BeerServiceRestTemplateImpl.BEER_PATH_UPC + "12345")
                        .willReturn(okJson(objectMapper.writeValueAsString(beerDto)))
        );

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted( () -> {
            BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();

            assertEquals(BeerOrderStatusEnum.ALLOCATION_PENDING , foundOrder.getOrderStatus());
        });

        beerOrderManager.cancelOrder(savedBeerOrder.getId());

        await().untilAsserted( () -> {
            BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();

            assertEquals(BeerOrderStatusEnum.CANCELLED , foundOrder.getOrderStatus());
        });
    }

    @Test
    void testAllocatedToCancelled() throws JsonProcessingException {

        BeerDto beerDto = BeerDto.builder().id(beerId).upc("12345").build();

        BeerOrder beerOrder = createBeerOrder();

        wireMock.stubFor(
                get(BeerServiceRestTemplateImpl.BEER_PATH_UPC + "12345")
                        .willReturn(okJson(objectMapper.writeValueAsString(beerDto)))
        );

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted( () -> {
            BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();

            assertEquals(BeerOrderStatusEnum.ALLOCATED , foundOrder.getOrderStatus());
        });

        beerOrderManager.cancelOrder(savedBeerOrder.getId());

        await().untilAsserted( () -> {
            BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();

            assertEquals(BeerOrderStatusEnum.CANCELLED , foundOrder.getOrderStatus());
        });

        DeallocateOrderRequest deallocationEvent = (DeallocateOrderRequest) jmsTemplate.receiveAndConvert(JmsConfig.ORDER_DEALLOCATION_QUEUE);

        assertNotNull(deallocationEvent);
        assertThat(deallocationEvent.getBeerOrderDto().getId()).isEqualTo(savedBeerOrder.getId());

    }

    public BeerOrder createBeerOrder(){
        BeerOrder beerOrder = BeerOrder.builder()
                .customer(testCustomer)
                .build();

        Set<BeerOrderLine> lines = new HashSet<>();

        lines.add(
                BeerOrderLine.builder()
                .beerId(beerId)
                .orderQuantity(1)
                .beerOrder(beerOrder)
                .upc("12345")
                .build()
        );

        beerOrder.setBeerOrderLines(lines);

        return beerOrder;
    }
}
