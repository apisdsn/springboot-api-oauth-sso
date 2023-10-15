package demo.app.service;

import demo.app.entity.Address;
import demo.app.entity.Employee;
import demo.app.model.AddressRequest;
import demo.app.model.AddressResponse;
import demo.app.repository.AddressRepository;
import demo.app.repository.EmployeeRepository;
import demo.app.utils.AuthoritiesManager;
import demo.app.validator.ValidationHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
@Slf4j
public class AddressService {

    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private AddressRepository addressRepository;
    @Autowired
    private AuthoritiesManager authoritiesManager;
    @Autowired
    private ValidationHelper validationHelper;

    public AddressResponse updateAddress(AddressRequest request, OAuth2AuthenticatedPrincipal principal) {
        validationHelper.validate(request);
        String clientId = getClientIdFromPrincipal(principal);
        Employee employee = findEmployeeByClientId(clientId);
        Address address = employee.getAddress();
        if (address == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found for the employee");
        }
        updateAddressFields(address, request);
        addressRepository.save(address);
        return toAddressResponse(address);
    }

    // admin or manager service
    public AddressResponse updateAddressByClientId(AddressRequest request, String clientId, OAuth2AuthenticatedPrincipal principal) {
        validationHelper.validate(request);
        if (authoritiesManager.checkIfUserIsAdminOrManager(principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You are not authorized to perform this operation");
        }
        Employee employee = findEmployeeByClientId(clientId);
        Address address = employee.getAddress();
        if (address == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found for the employee");
        }
        updateAddressFields(address, request);
        addressRepository.save(address);
        return toAddressResponse(address);
    }

    public AddressResponse toAddressResponse(Address address) {
        return AddressResponse.builder()
                .addressId(address.getAddressId())
                .street(address.getStreet())
                .city(address.getCity())
                .province(address.getProvince())
                .country(address.getCountry())
                .postalCode(address.getPostalCode())
                .build();
    }

    private String getClientIdFromPrincipal(OAuth2AuthenticatedPrincipal principal) {
        Map<String, Object> attributes = principal.getAttributes();
        return attributes.get("client_id").toString();
    }

    private Employee findEmployeeByClientId(String clientId) {
        return employeeRepository.findByClientId(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee not found"));
    }

    private void updateAddressFields(Address address, AddressRequest request) {
        address.setStreet(request.getStreet());
        address.setCity(request.getCity());
        address.setProvince(request.getProvince());
        address.setCountry(request.getCountry());
        address.setPostalCode(request.getPostalCode());
    }
}