package guru.sfg.beer.order.service.web.mappers;

import guru.sfg.beer.order.service.domain.BeerOrderLine;
import guru.sfg.beer.order.service.exceptions.NotFoundException;
import guru.sfg.beer.order.service.services.beer.BeerService;
import curso.common.model.BeerDto;
import curso.common.model.BeerOrderLineDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public abstract class BeerOrderLineMapperDecorator implements BeerOrderLineMapper{

    private BeerService beerService;

    private BeerOrderLineMapper beerOrderLineMapper;

    @Autowired
    public void setBeerService(BeerService beerService){
        this.beerService = beerService;
    }

    @Autowired
    public void  setBeerOrderLineMapper(BeerOrderLineMapper mapper){
        this.beerOrderLineMapper = mapper;
    }

    @Override
    public BeerOrderLineDto beerOrderLineToDto(BeerOrderLine line){
        BeerOrderLineDto orderLineDto = beerOrderLineMapper.beerOrderLineToDto(line);

        BeerDto beerDto = beerService.getBeerByUpc(line.getUpc()).orElseThrow(() -> new NotFoundException());

        orderLineDto.setBeerName(beerDto.getBeerName());
        orderLineDto.setBeerId(beerDto.getId());

        //log.debug(orderLineDto.toString());
        return orderLineDto;
    }



}
