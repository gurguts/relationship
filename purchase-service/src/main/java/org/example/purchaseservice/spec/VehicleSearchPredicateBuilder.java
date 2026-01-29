package org.example.purchaseservice.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.purchaseservice.models.balance.Vehicle;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class VehicleSearchPredicateBuilder {
    
    private static final String FIELD_ID = "id";
    private static final String FIELD_VEHICLE_NUMBER = "vehicleNumber";
    private static final String FIELD_INVOICE_UA = "invoiceUa";
    private static final String FIELD_INVOICE_EU = "invoiceEu";
    private static final String FIELD_PRODUCT = "product";
    private static final String FIELD_PRODUCT_QUANTITY = "productQuantity";
    private static final String FIELD_DECLARATION_NUMBER = "declarationNumber";
    private static final String FIELD_DRIVER_FULL_NAME = "driverFullName";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_SENDER = "sender";
    private static final String FIELD_RECEIVER = "receiver";
    private static final String FIELD_CARRIER = "carrier";
    private static final String FIELD_TERMINAL = "terminal";
    private static final String FIELD_DESTINATION_COUNTRY = "destinationCountry";
    private static final String FIELD_DESTINATION_PLACE = "destinationPlace";
    private static final String FIELD_SENDER_NAME = "name";
    private static final String FIELD_RECEIVER_NAME = "name";
    private static final String FIELD_CARRIER_COMPANY_NAME = "companyName";
    private static final String FIELD_TERMINAL_NAME = "name";
    private static final String FIELD_DESTINATION_COUNTRY_NAME = "name";
    private static final String FIELD_DESTINATION_PLACE_NAME = "name";
    
    private final VehicleFilterValueParser valueParser;
    
    public List<Predicate> buildSearchPredicates(
            @NonNull String query,
            @NonNull Root<Vehicle> root,
            @NonNull CriteriaBuilder criteriaBuilder) {
        
        List<Predicate> searchPredicates = new ArrayList<>();
        String searchTerm = String.format("%%%s%%", query.toLowerCase());
        
        addIdSearchPredicate(searchPredicates, query, root, criteriaBuilder, searchTerm);
        addStringFieldSearchPredicate(searchPredicates, root, criteriaBuilder, FIELD_VEHICLE_NUMBER, searchTerm);
        addStringFieldSearchPredicate(searchPredicates, root, criteriaBuilder, FIELD_INVOICE_UA, searchTerm);
        addStringFieldSearchPredicate(searchPredicates, root, criteriaBuilder, FIELD_INVOICE_EU, searchTerm);
        addStringFieldSearchPredicate(searchPredicates, root, criteriaBuilder, FIELD_PRODUCT, searchTerm);
        addStringFieldSearchPredicate(searchPredicates, root, criteriaBuilder, FIELD_PRODUCT_QUANTITY, searchTerm);
        addStringFieldSearchPredicate(searchPredicates, root, criteriaBuilder, FIELD_DECLARATION_NUMBER, searchTerm);
        addStringFieldSearchPredicate(searchPredicates, root, criteriaBuilder, FIELD_DRIVER_FULL_NAME, searchTerm);
        addStringFieldSearchPredicate(searchPredicates, root, criteriaBuilder, FIELD_DESCRIPTION, searchTerm);
        
        addJoinFieldSearchPredicate(searchPredicates, root, criteriaBuilder, FIELD_SENDER, FIELD_SENDER_NAME, searchTerm);
        addJoinFieldSearchPredicate(searchPredicates, root, criteriaBuilder, FIELD_RECEIVER, FIELD_RECEIVER_NAME, searchTerm);
        addJoinFieldSearchPredicate(searchPredicates, root, criteriaBuilder, FIELD_CARRIER, FIELD_CARRIER_COMPANY_NAME, searchTerm);
        addJoinFieldSearchPredicate(searchPredicates, root, criteriaBuilder, FIELD_TERMINAL, FIELD_TERMINAL_NAME, searchTerm);
        addJoinFieldSearchPredicate(searchPredicates, root, criteriaBuilder, FIELD_DESTINATION_COUNTRY, FIELD_DESTINATION_COUNTRY_NAME, searchTerm);
        addJoinFieldSearchPredicate(searchPredicates, root, criteriaBuilder, FIELD_DESTINATION_PLACE, FIELD_DESTINATION_PLACE_NAME, searchTerm);
        
        return searchPredicates;
    }
    
    private void addIdSearchPredicate(
            @NonNull List<Predicate> searchPredicates,
            @NonNull String query,
            @NonNull Root<Vehicle> root,
            @NonNull CriteriaBuilder criteriaBuilder,
            @NonNull String searchTerm) {
        
        Long idValue = valueParser.tryParseLong(query);
        if (idValue != null) {
            searchPredicates.add(criteriaBuilder.equal(root.get(FIELD_ID), idValue));
        } else {
            searchPredicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(criteriaBuilder.toString(root.get(FIELD_ID))), searchTerm));
        }
    }
    
    private void addStringFieldSearchPredicate(
            @NonNull List<Predicate> searchPredicates,
            @NonNull Root<Vehicle> root,
            @NonNull CriteriaBuilder criteriaBuilder,
            @NonNull String fieldName,
            @NonNull String searchTerm) {
        
        searchPredicates.add(
                criteriaBuilder.like(criteriaBuilder.lower(root.get(fieldName)), searchTerm)
        );
    }
    
    private void addJoinFieldSearchPredicate(
            @NonNull List<Predicate> searchPredicates,
            @NonNull Root<Vehicle> root,
            @NonNull CriteriaBuilder criteriaBuilder,
            @NonNull String joinField,
            @NonNull String joinProperty,
            @NonNull String searchTerm) {
        
        Join<Vehicle, ?> join = root.join(joinField, JoinType.LEFT);
        searchPredicates.add(
                criteriaBuilder.like(criteriaBuilder.lower(join.get(joinProperty)), searchTerm)
        );
    }
}
