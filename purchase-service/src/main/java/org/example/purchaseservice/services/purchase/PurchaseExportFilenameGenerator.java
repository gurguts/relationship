package org.example.purchaseservice.services.purchase;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class PurchaseExportFilenameGenerator {
    
    private static final String DATE_PATTERN = "yyyy-MM-dd";
    private static final String EXCEL_FILENAME_PREFIX = "purchase_data_";
    private static final String EXCEL_FILENAME_SUFFIX = ".xlsx";
    
    public String generateFilename() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern(DATE_PATTERN));
        return EXCEL_FILENAME_PREFIX + dateStr + EXCEL_FILENAME_SUFFIX;
    }
}
