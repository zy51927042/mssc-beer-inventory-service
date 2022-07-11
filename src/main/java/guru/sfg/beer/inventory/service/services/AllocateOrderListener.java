package guru.sfg.beer.inventory.service.services;

import guru.sfg.beer.inventory.service.config.JmsConfig;
import guru.sfg.brewery.model.BeerOrderDto;
import guru.sfg.brewery.model.events.AllocateOrderRequest;
import guru.sfg.brewery.model.events.AllocateOrderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class AllocateOrderListener {

    private final AllocationService allocationService;
    private final JmsTemplate jmsTemplate;
    @JmsListener(destination = JmsConfig.ALLOCATE_ORDER_QUEUE)
    public void listen(AllocateOrderRequest event) {
        BeerOrderDto beerOrderDto = event.getBeerOrderDto();
        Boolean pendingInventory = false;
        Boolean allocationError = false;
        try {
            pendingInventory = !allocationService.allocateOrder(beerOrderDto);
        }catch(Exception e) {
            log.debug("Allocation failed for Order Id:" + event.getBeerOrderDto().getId());
            allocationError = true;
        }
        jmsTemplate.convertAndSend(JmsConfig.ALLOCATE_ORDER_RESULT_QUEUE, AllocateOrderResult.builder()
                .beerOrderDto(beerOrderDto)
                .pendingInventory(pendingInventory)
                .allocationError(allocationError)
                .build());
    }
}
