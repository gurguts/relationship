package org.example.clientservice.services.field;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.exceptions.field.BusinessNotFoundException;
import org.example.clientservice.models.field.Business;
import org.example.clientservice.repositories.field.BusinessRepository;
import org.example.clientservice.services.impl.IBusinessService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessService implements IBusinessService {
    private final BusinessRepository businessRepository;

    @Override
    @Cacheable(value = "businesses", key = "#id")
    public Business getBusiness(Long id) {
        return businessRepository.findById(id)
                .orElseThrow(() -> new BusinessNotFoundException(String.format("Business not found with id: %d", id)));
    }

    @Override
    @Cacheable(value = "businesses", key = "'allBusinesses'")
    public List<Business> getAllBusinesses() {
        return (List<Business>) businessRepository.findAll();
    }

    @Override
    @Transactional
    @CacheEvict(value = {"businesses", "businessNames", "businessSearch"}, allEntries = true)
    public Business createBusiness(Business business) {
        return businessRepository.save(business);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"businesses", "businessNames", "businessSearch"}, allEntries = true)
    public Business updateBusiness(Long id, Business business) {
        Business existingBusiness = findBusiness(id);
        existingBusiness.setName(business.getName());
        return businessRepository.save(existingBusiness);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"businesses", "businessNames", "businessSearch"}, allEntries = true)
    public void deleteBusiness(Long id) {
        Business business = findBusiness(id);
        businessRepository.delete(business);
    }

    @Override
    @Cacheable(value = "businessNames", key = "'businessNames'")
    public Map<Long, String> getBusinessNames() {
        List<Business> businesses = (List<Business>) businessRepository.findAll();
        return businesses.stream()
                .collect(Collectors.toMap(Business::getId, Business::getName));
    }

    @Override
    @Cacheable(value = "businessSearch", key = "#query")
    public List<Business> findByNameContaining(String query) {
        return businessRepository.findByNameContainingIgnoreCase(query);
    }

    private Business findBusiness(Long id) {
        return businessRepository.findById(id).orElseThrow(() ->
                new BusinessNotFoundException(String.format("Business not found with id: %d", id)));
    }
}
