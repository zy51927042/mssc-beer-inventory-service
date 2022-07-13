package guru.sfg.beer.inventory.service.services;

import guru.sfg.beer.inventory.service.domain.BeerInventory;
import guru.sfg.beer.inventory.service.repositories.BeerInventoryRepository;
import guru.sfg.brewery.model.BeerOrderDto;
import guru.sfg.brewery.model.BeerOrderLineDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor
public class AllocationServiceImpl implements AllocationService {
    private final BeerInventoryRepository beerInventoryRepository;
    @Override
    public Boolean allocateOrder(BeerOrderDto beerOrderDto) {
        log.debug("Allocating OrderId:" + beerOrderDto.getId());
        AtomicInteger totoalOrdered = new AtomicInteger();
        AtomicInteger totoalAllocated = new AtomicInteger();

        beerOrderDto.getBeerOrderLines().forEach( beerOrderLine -> {
            if((beerOrderLine.getOrderQuantity()!=null ?beerOrderLine.getOrderQuantity():0)
                  -(beerOrderLine.getQuantityAllocated()!=null? beerOrderLine.getOrderQuantity() : 0)>0){
                allocateBeerOrderLine(beerOrderLine);
            }
            totoalOrdered.set(totoalOrdered.get() + beerOrderLine.getOrderQuantity());
            totoalAllocated.set(totoalAllocated.get() + (beerOrderLine.getQuantityAllocated() !=null
              ? beerOrderLine.getQuantityAllocated() : 0));
        });
        log.debug("Total Ordered:" + totoalOrdered.get() + "Total Allocated:" + totoalAllocated.get());
        return totoalOrdered.get() == totoalAllocated.get();
    }

    @Override
    public void deallocateOrder(BeerOrderDto beerOrderDto) {
        log.debug("Deallocating OrderId:" + beerOrderDto.getId());
        beerOrderDto.getBeerOrderLines().forEach(beerOrderLine -> {
            BeerInventory beerInventory = BeerInventory.builder()
                    .beerId(beerOrderLine.getBeerId())
                    .upc(beerOrderLine.getUpc())
                    .quantityOnHand(beerOrderLine.getQuantityAllocated())
                    .build();
           BeerInventory savedInventory = beerInventoryRepository.save(beerInventory);
           log.debug("Saved Inventory for beer upc: " + savedInventory.getUpc()+"inventory id: " +savedInventory.getId());
        });
    }

    private void allocateBeerOrderLine(BeerOrderLineDto beerOrderLineDto) {
        List<BeerInventory> beerInventoryList = beerInventoryRepository.findAllByUpc(beerOrderLineDto.getUpc());

        beerInventoryList.forEach(beerInventory -> {
            int inventory = (beerInventory.getQuantityOnHand() == null) ? 0 : beerInventory.getQuantityOnHand();
            int orderQty = (beerOrderLineDto.getOrderQuantity() == null) ? 0 : beerOrderLineDto.getOrderQuantity();
            int allocatedQty = (beerOrderLineDto.getQuantityAllocated() == null) ? 0 : beerOrderLineDto.getQuantityAllocated();
            int qtyToAllocate = orderQty - allocatedQty;
            if(inventory >= qtyToAllocate) {
                //full allocation
                inventory = inventory - qtyToAllocate;
                beerOrderLineDto.setQuantityAllocated(orderQty);
                beerInventory.setQuantityOnHand(inventory);

                beerInventoryRepository.save(beerInventory);
            }else if(inventory > 0) {
                //partial allocation
                beerOrderLineDto.setQuantityAllocated(allocatedQty + inventory);
                beerInventory.setQuantityOnHand(0);
            }
            if(beerInventory.getQuantityOnHand() == 0) {
                beerInventoryRepository.delete(beerInventory);
            }
        });


    }
}
