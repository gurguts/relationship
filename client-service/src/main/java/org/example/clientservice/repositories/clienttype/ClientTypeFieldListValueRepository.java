package org.example.clientservice.repositories.clienttype;

import lombok.NonNull;
import org.example.clientservice.models.clienttype.ClientTypeFieldListValue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientTypeFieldListValueRepository extends JpaRepository<ClientTypeFieldListValue, Long> {

    ClientTypeFieldListValue findByFieldIdAndValue(@NonNull Long fieldId, @NonNull String value);
}

