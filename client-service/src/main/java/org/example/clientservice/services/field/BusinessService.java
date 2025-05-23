package org.example.clientservice.services.field;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.exceptions.field.BusinessNotFoundException;
import org.example.clientservice.models.field.Business;
import org.example.clientservice.repositories.field.BusinessRepository;
import org.example.clientservice.services.impl.IBusinessService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessService implements IBusinessService {
    private final BusinessRepository businessRepository;

    @Override
    public Business getBusiness(Long id) {
        return businessRepository.findById(id)
                .orElseThrow(() -> new BusinessNotFoundException(String.format("Business not found with id: %d", id)));
    }

    @Override
    public List<Business> getAllBusinesses() {
        return (List<Business>) businessRepository.findAll();
    }

    @Override
    public Business createBusiness(Business business) {
        return businessRepository.save(business);
    }

    @Override
    public Business updateBusiness(Long id, Business business) {
        Business existingBusiness = getBusiness(id);
        existingBusiness.setName(business.getName());
        return businessRepository.save(existingBusiness);
    }

    @Override
    public void deleteBusiness(Long id) {
        Business business = getBusiness(id);
        businessRepository.delete(business);
    }

    @Override
    public Map<Long, String> getBusinessNames() {
        List<Business> businesses = (List<Business>) businessRepository.findAll();
        return businesses.stream()
                .collect(Collectors.toMap(Business::getId, Business::getName));
    }

    @Override
    public List<Business> findByNameContaining(String query) {
        return businessRepository.findByNameContainingIgnoreCase(query);
    }
}
