package guru.sfg.beer.order.service.services;

import curso.common.model.CustomerDto;
import guru.sfg.beer.order.service.repositories.CustomerRepository;
import guru.sfg.beer.order.service.web.mappers.CustomerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    private final CustomerMapper customerMapper;

    @Override
    public List<CustomerDto> getAllCustomers() {
        List<CustomerDto> customers = new ArrayList<>();

         customerRepository.findAll().forEach(
           customer -> {
               customers.add(customerMapper.customerToCustomerDTO(customer));
           }
         );

        return customers;
    }
}
