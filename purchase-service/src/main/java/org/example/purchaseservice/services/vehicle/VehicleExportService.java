package org.example.purchaseservice.services.vehicle;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.balance.Vehicle;
import org.example.purchaseservice.repositories.VehicleRepository;
import org.example.purchaseservice.services.impl.IVehicleExportService;
import org.example.purchaseservice.spec.VehicleFilterBuilder;
import org.example.purchaseservice.spec.VehicleSearchPredicateBuilder;
import org.example.purchaseservice.spec.VehicleSpecification;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleExportService implements IVehicleExportService {
    
    private final VehicleRepository vehicleRepository;
    private final VehicleExportDataFetcher dataFetcher;
    private final VehicleExcelGenerator excelGenerator;
    private final VehicleExportHeaderBuilder headerBuilder;
    private final VehicleFilterBuilder filterBuilder;
    private final VehicleSearchPredicateBuilder searchPredicateBuilder;
    
    @Override
    @Transactional(readOnly = true)
    public byte[] exportToExcel(String query, Map<String, List<String>> filterParams) throws IOException {
        List<Vehicle> vehicles = loadVehicles(query, filterParams);

        if (vehicles.isEmpty()) {
            return excelGenerator.createEmptyWorkbook();
        }

        VehicleExportDataFetcher.VehicleData vehicleData = dataFetcher.loadVehicleData(vehicles);
        List<String> headerList = headerBuilder.buildHeaderList(
                vehicleData.sortedCategoryIds(), 
                vehicleData.categoryNameMap());
        
        return excelGenerator.generateWorkbook(vehicles, vehicleData, headerList);
    }

    private List<Vehicle> loadVehicles(String query, Map<String, List<String>> filterParams) {
        VehicleSpecification spec = new VehicleSpecification(query, filterParams, filterBuilder, searchPredicateBuilder);
        Sort sortBy = Sort.by(Sort.Direction.DESC, "createdAt");
        return vehicleRepository.findAll(spec, sortBy);
    }
    
}
