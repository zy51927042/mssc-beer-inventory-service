package guru.sfg.beer.inventory.service.services;

import guru.sfg.beer.inventory.service.config.JmsConfig;
import guru.sfg.brewery.model.BeerOrderDto;
import org.springframework.jms.annotation.JmsListener;

public interface AllocationService {
    Boolean allocateOrder(BeerOrderDto beerOrderDto);


}
