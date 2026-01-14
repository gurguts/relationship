package org.example.userservice.services.impl;

import lombok.NonNull;

import java.util.List;
import java.util.Map;

public interface ITransactionExportService {
    byte[] exportToExcel(@NonNull Map<String, List<String>> filters);
}
