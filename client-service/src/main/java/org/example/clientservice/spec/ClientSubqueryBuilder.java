package org.example.clientservice.spec;

import jakarta.persistence.criteria.*;
import org.example.clientservice.models.client.Client;
import org.example.clientservice.models.clienttype.ClientFieldValue;
import org.example.clientservice.models.clienttype.ClientTypeField;

import java.util.ArrayList;
import java.util.List;

public class ClientSubqueryBuilder {

    private static final String FIELD_CLIENT = "client";
    private static final String FIELD_FIELD = "field";
    private static final String FIELD_CLIENT_TYPE = "clientType";
    private static final String FIELD_ID = "id";

    public static SubqueryContext createFieldValueSubquery(CriteriaQuery<?> query) {
        Subquery<Long> fieldValueSubquery = query.subquery(Long.class);
        Root<ClientFieldValue> fieldValueRoot = fieldValueSubquery.from(ClientFieldValue.class);
        Join<ClientFieldValue, ClientTypeField> fieldJoin = fieldValueRoot.join(FIELD_FIELD);
        Join<ClientTypeField, ?> clientTypeJoin = fieldJoin.join(FIELD_CLIENT_TYPE);
        
        fieldValueSubquery.select(fieldValueRoot.get(FIELD_CLIENT).get(FIELD_ID));
        
        return new SubqueryContext(fieldValueSubquery, fieldValueRoot, fieldJoin, clientTypeJoin);
    }

    public static List<Predicate> buildWherePredicates(List<Predicate> basePredicates, 
                                                       Root<ClientFieldValue> fieldValueRoot,
                                                       Root<Client> root, 
                                                       CriteriaBuilder criteriaBuilder, 
                                                       Predicate valuePredicate) {
        List<Predicate> wherePredicates = new ArrayList<>(basePredicates);
        wherePredicates.add(criteriaBuilder.equal(fieldValueRoot.get(FIELD_CLIENT), root));
        wherePredicates.add(valuePredicate);
        return wherePredicates;
    }
}
