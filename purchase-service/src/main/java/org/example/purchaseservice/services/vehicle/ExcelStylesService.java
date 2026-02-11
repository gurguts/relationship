package org.example.purchaseservice.services.vehicle;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelStylesService {

    private static final short FONT_SIZE = 8;
    private static final String FONT_NAME = "Arial";
    private static final String DATE_FORMAT_PATTERN = "dd.mm.yyyy";
    private static final String NUMBER_FORMAT_PATTERN = "#,##0.00";

    public VehicleExcelGenerator.ExcelStyles createExcelStyles(Workbook workbook) {
        Font baseFont = createBaseFont(workbook, false);
        Font boldFont = createBaseFont(workbook, true);

        CellStyle headerStyle = createHeaderStyle(workbook, boldFont);
        CellStyle dataStyle   = createDataStyle(workbook, baseFont);
        CellStyle dateStyle   = createDateStyle(workbook, dataStyle);
        CellStyle numberStyle = createNumberStyle(workbook, dataStyle);

        return new VehicleExcelGenerator.ExcelStyles(headerStyle, dataStyle, dateStyle, numberStyle);
    }

    private Font createBaseFont(Workbook workbook, boolean bold) {
        Font font = workbook.createFont();
        font.setFontName(FONT_NAME);
        font.setFontHeightInPoints(FONT_SIZE);
        font.setBold(bold);
        return font;
    }

    private CellStyle createHeaderStyle(Workbook workbook, Font boldFont) {
        CellStyle style = workbook.createCellStyle();
        style.setFont(boldFont);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook, Font font) {
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createDateStyle(Workbook workbook, CellStyle baseStyle) {
        CellStyle style = workbook.createCellStyle();
        style.cloneStyleFrom(baseStyle);
        CreationHelper helper = workbook.getCreationHelper();
        style.setDataFormat(helper.createDataFormat().getFormat(DATE_FORMAT_PATTERN));
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createNumberStyle(Workbook workbook, CellStyle baseStyle) {
        CellStyle style = workbook.createCellStyle();
        style.cloneStyleFrom(baseStyle);
        CreationHelper helper = workbook.getCreationHelper();
        style.setDataFormat(helper.createDataFormat().getFormat(NUMBER_FORMAT_PATTERN));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }
}
