package org.springframework.batch.item.excel.transform;

import lombok.Getter;
import lombok.Setter;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.ResourceAwareItemWriterItemStream;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import sun.net.www.protocol.file.FileURLConnection;

import java.io.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Diogo
 * Date: 15/10/14
 * Time: 16:50
 */

public class DefaultExcelItemWriter<T> implements ResourceAwareItemWriterItemStream<T> {

    public static final short BORDER_THICK = 1;
    public static final short FONT_SIZE = 12;

    public static final String OUTPUT_EXTENSION_XLS = "xls";
    public static final String OUTPUT_EXTENSION_XLSX = "xlsx";


    private WritableResource resource;
    private OutputStream outputStream;

    @Getter
    @Setter
    private String outputExtension = OUTPUT_EXTENSION_XLSX;

    @Override
    public void write(List<? extends T> items) throws Exception {
        try{
            Assert.notNull(resource, "Set the resource for the writer before write");
            outputStream = resource.getOutputStream();
            Workbook workbook;
            if (outputExtension == OUTPUT_EXTENSION_XLSX) {
                workbook = new XSSFWorkbook();
            }else if (outputExtension == OUTPUT_EXTENSION_XLS){
                workbook = new HSSFWorkbook();
            }else{
                throw new Exception("Extension "+ outputExtension +" not supported");
            }
            Sheet sheet = workbook.createSheet("Sheet 1");

            if(!CollectionUtils.isEmpty(items)){
                // Create styles
                CellStyle headerStyle = createHeaderStyle(workbook);

                // Create the header and add data to the sheet
                Field[] fields = items.get(0).getClass().getDeclaredFields();
                Map<String, Integer> columnFieldMapper = new HashMap<String, Integer>();
                int totalColumns = createHeader(sheet, headerStyle, fields, columnFieldMapper);
                addDataToSheet(items, sheet, fields, columnFieldMapper, workbook);

                // Auto filter for the first row (header)
                sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, totalColumns - 1));

                // Set autosize for each column
                for(int i = 0; i < totalColumns; i++){
                    sheet.autoSizeColumn(i);
                }

                workbook.write(outputStream);
            }
        }finally{
            outputStream.close();
        }
    }

    /**
     * Add data to the sheet
     * @param objects the list of objects to be added to the sheet
     * @param sheet the sheet
     * @param fields the fields of the objects
     * @param columnFieldMapper the mapped header field name / index
     * @return The total of rows created
     * @throws IllegalAccessException
     */
    private int addDataToSheet(List<? extends T> objects, Sheet sheet, Field[] fields, Map<String, Integer> columnFieldMapper,Workbook workbook) throws IllegalAccessException {
        CreationHelper createHelper = workbook.getCreationHelper();
        int currentRow = 1;
        for(T object : objects){
            Row row = sheet.createRow(currentRow);
            for(Field field: fields){
                Cell cell = row.createCell(columnFieldMapper.get(field.getName()));
                CellStyle style  = (currentRow % 2 == 0)? createEvenRowStyle(workbook) : createOddRowStyle(workbook);

                Object value = field.get(object);
                checkForDateStyle(style, field, value, createHelper);
                if(value!= null){
                    setCellValue(cell, value);
                }
                cell.setCellStyle(style);
            }
            currentRow++;
        }
        return currentRow;
    }

    private void checkForDateStyle(CellStyle style, Field field,  Object value, CreationHelper createHelper) {
        if(value!= null && value instanceof Date){
            String pattern;

            DateTimeFormat annotation = field.getAnnotation(DateTimeFormat.class);
            if(annotation != null && !StringUtils.hasText(annotation.pattern())){
                pattern = annotation.pattern();
            }else{
                DateFormat formatter = DateFormat.getDateInstance();
                pattern = ((SimpleDateFormat)formatter).toPattern();
            }

            style.setDataFormat(createHelper.createDataFormat().getFormat(pattern));
        }
    }

    private void setCellValue(Cell cell, Object value){
        Class clazz = value.getClass();
        if(clazz ==  String.class){
            cell.setCellValue(value.toString());
            cell.setCellType(Cell.CELL_TYPE_STRING);
            return;
        }
        if(clazz == Boolean.class || clazz == boolean.class){
            cell.setCellValue(value.toString());
            cell.setCellType(Cell.CELL_TYPE_BOOLEAN);
            return;
        }
        if ((Short.class == clazz || short.class == clazz)
                || (Integer.class == clazz || int.class == clazz)
                || (Long.class == clazz || long.class == clazz)){
            cell.setCellValue(new Long(value.toString()));
            cell.setCellType(Cell.CELL_TYPE_NUMERIC);
            return;
        }
        if((Float.class == clazz || float.class == clazz)
                || (Double.class == clazz || double.class == clazz)
                || (BigDecimal.class == clazz)) {
            cell.setCellValue(new BigDecimal(value.toString()).doubleValue());
            cell.setCellType(Cell.CELL_TYPE_NUMERIC);
            return;
        }
        if(value instanceof Date){
            cell.setCellValue((Date) value);
            return;
        }
        cell.setCellValue(value.toString());
    }

    /**
     * Create the header from the list of fields, maps the name of field with it's column id in a Map<String, Integer>
     *     object. Returns the number of columns created.
     * @param sheet the sheet to add the header
     * @param headerStyle the style to be applied to the header
     * @param fields fields to be added to header
     * @param columnFieldMapper the map where fields name will be mapped to it's column id
     * @return the total of created columns
     */
    private int createHeader(Sheet sheet, CellStyle headerStyle, Field[] fields, Map<String, Integer> columnFieldMapper) {
        Row header = sheet.createRow(0);
        int currentColumn = 0;
        for(Field field:fields){
            columnFieldMapper.put(field.getName(), currentColumn);
            Cell cell = header.createCell(currentColumn, Cell.CELL_TYPE_STRING);
            cell.setCellStyle(headerStyle);
            cell.setCellValue(field.getName());
            ReflectionUtils.makeAccessible(field);
            currentColumn++;
        }
        return currentColumn;
    }

    // Styles Creation
    private CellStyle createHeaderStyle(Workbook workbook){
        CellStyle headerStyle = createBasicCellStyle(workbook);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
        headerStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);

        Font headerFont = createBasicFont(workbook);
        headerFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        return headerStyle;
    }

    private CellStyle createEvenRowStyle(Workbook workbook){
        CellStyle style = createBasicCellStyle(workbook);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(CellStyle.SOLID_FOREGROUND);

        Font font = createBasicFont(workbook);
        style.setFont(font);

        return style;
    }

    private CellStyle createOddRowStyle(Workbook workbook){
        CellStyle style = createBasicCellStyle(workbook);

        Font font = createBasicFont(workbook);
        style.setFont(font);

        return style;
    }

    private Font createBasicFont(Workbook workbook) {
        Font font = workbook.createFont();
        font.setFontHeightInPoints(FONT_SIZE);
        return font;
    }

    private CellStyle createBasicCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BORDER_THICK);
        style.setBorderLeft(BORDER_THICK);
        style.setBorderRight(BORDER_THICK);
        style.setBorderTop(BORDER_THICK);
        return style;
    }

    @Override
    public void setResource(Resource resource) {
        if(resource instanceof WritableResource){
            this.resource = (WritableResource)resource;
            return;
        }
        throw new RuntimeException("Resource must implement the WritableResource interface");
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        return;
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
       return;
    }

    @Override
    public void close() throws ItemStreamException {
        return;
    }
}
