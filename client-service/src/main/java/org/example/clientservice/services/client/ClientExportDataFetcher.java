package org.example.clientservice.services.client;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.models.client.Client;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClientExportDataFetcher {

    @PersistenceContext
    private EntityManager entityManager;

    public List<Client> fetchClients(@NonNull Specification<Client> spec, @NonNull Sort sort) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Client> cq = cb.createQuery(Client.class);
        Root<Client> root = cq.from(Client.class);

        configureFetches(root);
        cq.distinct(true);

        Predicate specPredicate = spec.toPredicate(root, cq, cb);
        if (specPredicate != null) {
            cq.where(specPredicate);
        }

        applySorting(cq, root, cb, sort);
        
        TypedQuery<Client> typedQuery = entityManager.createQuery(cq);
        return typedQuery.getResultList();
    }

    private void configureFetches(@NonNull Root<Client> root) {
        root.fetch("clientType", JoinType.LEFT);

        Fetch<Object, Object> fieldValuesFetch = root.fetch("fieldValues", JoinType.LEFT);
        fieldValuesFetch.fetch("field", JoinType.LEFT);
        fieldValuesFetch.fetch("valueList", JoinType.LEFT);
    }

    private void applySorting(@NonNull CriteriaQuery<Client> cq, @NonNull Root<Client> root,
                             @NonNull CriteriaBuilder cb, @NonNull Sort sort) {
        List<Order> orders = new ArrayList<>();
        for (Sort.Order order : sort) {
            Path<?> path = root.get(order.getProperty());
            orders.add(order.isAscending() ? cb.asc(path) : cb.desc(path));
        }
        cq.orderBy(orders);
    }
}
