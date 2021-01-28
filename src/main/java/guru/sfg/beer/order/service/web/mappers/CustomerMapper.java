package guru.sfg.beer.order.service.web.mappers;

import curso.common.model.CustomerDto;
import guru.sfg.beer.order.service.domain.Customer;
import org.mapstruct.Mapper;

@Mapper(uses = {DateMapper.class})
public interface CustomerMapper {
    CustomerDto customerToCustomerDTO(Customer source);
}
