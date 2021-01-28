package guru.sfg.beer.order.service.services;

import guru.sfg.beer.order.service.domain.Customer;

import java.util.List;

public interface CustomerService {

    List<Customer> getAllCustomers();
}
