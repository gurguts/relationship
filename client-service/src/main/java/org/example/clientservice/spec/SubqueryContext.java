package org.example.clientservice.spec;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.example.clientservice.models.clienttype.ClientFieldValue;
import org.example.clientservice.models.clienttype.ClientTypeField;

public record SubqueryContext(
        Subquery<Long> subquery,
        Root<ClientFieldValue> fieldValueRoot,
        Join<ClientFieldValue, ClientTypeField> fieldJoin,
        Join<ClientTypeField, ?> clientTypeJoin
) {
}
