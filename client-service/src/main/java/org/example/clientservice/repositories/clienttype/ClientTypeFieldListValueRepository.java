package org.example.clientservice.repositories.clienttype;

import org.example.clientservice.models.clienttype.ClientTypeFieldListValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientTypeFieldListValueRepository extends JpaRepository<ClientTypeFieldListValue, Long> {
    List<ClientTypeFieldListValue> findByFieldIdOrderByDisplayOrderAsc(Long fieldId);
    
    void deleteByFieldId(Long fieldId);
    
    ClientTypeFieldListValue findByFieldIdAndValue(Long fieldId, String value);
}

