package org.example.purchaseservice.services.impl;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface IVehicleExportService {
    byte[] exportToExcel(String query, Map<String, List<String>> filterParams) throws IOException;
}
