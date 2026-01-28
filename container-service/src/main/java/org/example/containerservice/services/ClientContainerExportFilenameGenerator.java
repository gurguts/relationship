package org.example.containerservice.services;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class ClientContainerExportFilenameGenerator {

    private static final String FILENAME_PREFIX = "container_data_";
    private static final String FILENAME_SUFFIX = ".xlsx";
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    public String generateFilename() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern(DATE_FORMAT));
        return FILENAME_PREFIX + dateStr + FILENAME_SUFFIX;
    }
}
