package org.example.purchaseservice.services.impl;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public interface IVehicleProductsExportService {
    byte[] exportVehicleProductsToExcel(String query, LocalDate fromDate, LocalDate toDate, List<Long> managerIds) throws IOException;
}
