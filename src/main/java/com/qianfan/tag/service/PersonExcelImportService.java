package com.qianfan.tag.service;

import com.qianfan.tag.common.BusinessException;
import com.qianfan.tag.common.Ids;
import com.qianfan.tag.domain.ImportBatch;
import com.qianfan.tag.domain.IndicatorDefinition;
import com.qianfan.tag.domain.IndicatorOption;
import com.qianfan.tag.dto.PersonImportRow;
import com.qianfan.tag.mapper.ImportMapper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class PersonExcelImportService {
    private static final int MAX_ROWS = 10000;
    private static final int HEADER_ROW_INDEX = 0;
    private static final int DESCRIPTION_ROW_INDEX = 1;
    private static final int DATA_START_ROW_INDEX = 2;
    private static final long MAX_FILE_SIZE = 20L * 1024L * 1024L;
    private static final List<String> BASE_HEADERS = Arrays.asList(
            "external_id", "name", "gender", "organization", "occupation", "address", "remark", "deleted");
    private static final List<String> BASE_HEADER_LABELS = Arrays.asList(
            "人员编码（必填）", "姓名（必填）", "性别（男/女/未知）", "所属机构",
            "职业", "地区或地址", "备注", "是否删除（false/true）");
    private final IndicatorService indicatorService;
    private final ImportMapper importMapper;
    private final ImportWriteService writeService;

    public PersonExcelImportService(IndicatorService indicatorService, ImportMapper importMapper,
                                    ImportWriteService writeService) {
        this.indicatorService = indicatorService;
        this.importMapper = importMapper;
        this.writeService = writeService;
    }

    public byte[] createTemplate() {
        return createWorkbook(indicatorService.importDefinitions(), Collections.<Map<String, String>>emptyList());
    }

    public byte[] createExample() {
        List<IndicatorDefinition> definitions = indicatorService.importDefinitions();
        return createWorkbook(definitions, createExampleRows(definitions));
    }

    private byte[] createWorkbook(List<IndicatorDefinition> definitions, List<Map<String, String>> dataRows) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("人员导入");
            Sheet optionsSheet = workbook.createSheet("枚举选项");
            workbook.setSheetHidden(workbook.getSheetIndex(optionsSheet), true);
            Row header = sheet.createRow(HEADER_ROW_INDEX);
            Row description = sheet.createRow(DESCRIPTION_ROW_INDEX);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle descriptionStyle = createDescriptionStyle(workbook);
            int column = 0;
            for (int i = 0; i < BASE_HEADERS.size(); i++) {
                writeHeaderCell(header, column, BASE_HEADERS.get(i), headerStyle);
                writeHeaderCell(description, column++, BASE_HEADER_LABELS.get(i), descriptionStyle);
            }
            for (IndicatorDefinition definition : definitions) {
                writeHeaderCell(header, column, definition.getCode(), headerStyle);
                writeHeaderCell(description, column++, indicatorLabel(definition), descriptionStyle);
            }

            header.setHeightInPoints(24);
            description.setHeightInPoints(42);
            sheet.createFreezePane(0, DATA_START_ROW_INDEX);
            applyColumnWidths(sheet, definitions, dataRows);
            addExplicitValidation(sheet, 2, new String[]{"男", "女", "未知"});
            addExplicitValidation(sheet, 7, new String[]{"false", "true"});

            DataFormat dataFormat = workbook.createDataFormat();
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(dataFormat.getFormat("yyyy-mm-dd"));
            CellStyle dateTimeStyle = workbook.createCellStyle();
            dateTimeStyle.setDataFormat(dataFormat.getFormat("yyyy-mm-dd hh:mm:ss"));
            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.setDataFormat(dataFormat.getFormat("0.######"));

            int optionColumn = 0;
            for (int i = 0; i < definitions.size(); i++) {
                IndicatorDefinition definition = definitions.get(i);
                int targetColumn = BASE_HEADERS.size() + i;
                if ("ENUM".equals(definition.getDataType())) {
                    List<IndicatorOption> options = indicatorService.enabledOptions(definition.getId());
                    if (!options.isEmpty()) {
                        for (int row = 0; row < options.size(); row++) {
                            Row optionRow = optionsSheet.getRow(row);
                            if (optionRow == null) optionRow = optionsSheet.createRow(row);
                            optionRow.createCell(optionColumn).setCellValue(options.get(row).getCode());
                        }
                        String rangeName = "OPT_" + definition.getCode();
                        Name name = workbook.createName();
                        name.setNameName(rangeName);
                        String col = excelColumn(optionColumn);
                        name.setRefersToFormula("'枚举选项'!$" + col + "$1:$" + col + "$" + options.size());
                        addFormulaValidation(sheet, targetColumn, rangeName);
                        optionColumn++;
                    }
                }
                CellStyle style = "NUMBER".equals(definition.getDataType()) ? numberStyle
                        : "DATE".equals(definition.getDataType()) ? dateStyle
                        : "DATETIME".equals(definition.getDataType()) ? dateTimeStyle : null;
                if (style != null) sheet.setDefaultColumnStyle(targetColumn, style);
            }
            writeDataRows(sheet, definitions, dataRows);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new BusinessException("EXCEL_CREATE_FAILED", "Excel文件生成失败");
        }
    }

    public ImportBatch importFile(String batchNo, MultipartFile file) {
        if (batchNo == null || batchNo.trim().isEmpty() || batchNo.trim().length() > 64) {
            throw new BusinessException("IMPORT_BATCH_NO_INVALID", "导入批次号不能为空且不能超过64个字符");
        }
        batchNo = batchNo.trim();
        validateFile(file);
        ImportBatch existing = importMapper.findByBatchNo(batchNo);
        if (existing != null && "SUCCESS".equals(existing.getStatus())) return existing;
        if (existing != null && "RUNNING".equals(existing.getStatus())) {
            throw new BusinessException("IMPORT_BATCH_RUNNING", "该导入批次正在执行");
        }
        Date now = new Date();
        if (existing == null) {
            ImportBatch batch = new ImportBatch();
            batch.setId(Ids.uuid());
            batch.setBatchNo(batchNo);
            batch.setFileName(safeFileName(file.getOriginalFilename()));
            batch.setStatus("RUNNING");
            batch.setTotalCount(0);
            batch.setSuccessCount(0);
            batch.setStartedAt(now);
            importMapper.insert(batch);
        } else {
            importMapper.restart(batchNo, safeFileName(file.getOriginalFilename()), now);
        }
        try {
            List<PersonImportRow> rows = parseAndValidate(file);
            int count = writeService.write(batchNo, rows);
            importMapper.finish(batchNo, "SUCCESS", rows.size(), count, null, new Date());
        } catch (RuntimeException ex) {
            importMapper.finish(batchNo, "FAILED", 0, 0, abbreviate(ex.getMessage()), new Date());
            throw ex;
        }
        return importMapper.findByBatchNo(batchNo);
    }

    public List<ImportBatch> recent() { return importMapper.findRecent(20); }

    private List<PersonImportRow> parseAndValidate(MultipartFile file) {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) throw new BusinessException("IMPORT_HEADER_MISSING", "Excel缺少表头");
            Map<Integer, String> headers = parseHeaders(headerRow);
            if (!headers.containsValue("external_id") || !headers.containsValue("name")) {
                throw new BusinessException("IMPORT_HEADER_INVALID", "Excel必须包含 external_id 和 name 列");
            }
            Map<String, IndicatorDefinition> indicatorByCode = new HashMap<String, IndicatorDefinition>();
            for (IndicatorDefinition definition : indicatorService.importDefinitions()) {
                indicatorByCode.put(definition.getCode(), definition);
            }
            for (String header : headers.values()) {
                if (!BASE_HEADERS.contains(header) && !indicatorByCode.containsKey(header)) {
                    throw new BusinessException("IMPORT_UNKNOWN_COLUMN", "Excel包含未知或未启用指标列：" + header);
                }
            }
            validateDescriptionRow(sheet.getRow(DESCRIPTION_ROW_INDEX), headers, indicatorByCode);

            List<PersonImportRow> rows = new ArrayList<PersonImportRow>();
            List<String> errors = new ArrayList<String>();
            Set<String> externalIds = new HashSet<String>();
            int lastRow = sheet.getLastRowNum();
            if (lastRow - DATA_START_ROW_INDEX + 1 > MAX_ROWS) {
                throw new BusinessException("IMPORT_TOO_MANY_ROWS", "单次导入最多 " + MAX_ROWS + " 行数据");
            }
            for (int rowIndex = DATA_START_ROW_INDEX; rowIndex <= lastRow; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isBlankRow(row, headers.keySet())) continue;
                try {
                    PersonImportRow item = parseRow(rowIndex + 1, row, headers, indicatorByCode);
                    if (!externalIds.add(item.getExternalId())) {
                        throw new BusinessException("IMPORT_DUPLICATE_PERSON", "文件中人员编码重复：" + item.getExternalId());
                    }
                    rows.add(item);
                } catch (BusinessException ex) {
                    if (errors.size() < 20) errors.add("第" + (rowIndex + 1) + "行：" + ex.getMessage());
                }
            }
            if (!errors.isEmpty()) {
                throw new BusinessException("IMPORT_VALIDATION_FAILED", join(errors));
            }
            if (rows.isEmpty()) throw new BusinessException("IMPORT_EMPTY", "Excel没有可导入的数据行");
            return rows;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("IMPORT_FILE_INVALID", "Excel文件无法读取或格式损坏");
        }
    }

    private PersonImportRow parseRow(int rowNo, Row row, Map<Integer, String> headers,
                                     Map<String, IndicatorDefinition> indicatorByCode) {
        Map<String, String> values = new LinkedHashMap<String, String>();
        for (Map.Entry<Integer, String> entry : headers.entrySet()) {
            IndicatorDefinition definition = indicatorByCode.get(entry.getValue());
            String value = definition == null ? readText(row.getCell(entry.getKey()))
                    : readIndicator(row.getCell(entry.getKey()), definition);
            values.put(entry.getValue(), value);
        }
        String externalId = required(values.get("external_id"), "external_id不能为空");
        String name = required(values.get("name"), "name不能为空");
        PersonImportRow item = new PersonImportRow();
        item.setRowNo(rowNo);
        item.setExternalId(externalId);
        item.setName(name);
        item.setGender(emptyToNull(values.get("gender")));
        item.setOrganization(emptyToNull(values.get("organization")));
        item.setOccupation(emptyToNull(values.get("occupation")));
        item.setAddress(emptyToNull(values.get("address")));
        item.setRemark(emptyToNull(values.get("remark")));
        item.setDeleted(parseDeleted(values.get("deleted")));
        for (Map.Entry<String, IndicatorDefinition> definition : indicatorByCode.entrySet()) {
            String raw = emptyToNull(values.get(definition.getKey()));
            if (raw != null) {
                indicatorService.validateRawValue(definition.getValue(), raw);
                item.getIndicators().put(definition.getKey(), raw);
            }
        }
        return item;
    }

    private Map<Integer, String> parseHeaders(Row row) {
        Map<Integer, String> result = new LinkedHashMap<Integer, String>();
        Set<String> names = new HashSet<String>();
        for (int i = 0; i < row.getLastCellNum(); i++) {
            String value = readText(row.getCell(i));
            if (value.isEmpty()) continue;
            String normalized = BASE_HEADERS.contains(value.toLowerCase(Locale.ROOT))
                    ? value.toLowerCase(Locale.ROOT) : value.toUpperCase(Locale.ROOT);
            if (!names.add(normalized)) throw new BusinessException("IMPORT_DUPLICATE_HEADER", "Excel表头重复：" + normalized);
            result.put(i, normalized);
        }
        return result;
    }

    private String readIndicator(Cell cell, IndicatorDefinition definition) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return "";
        if ("NUMBER".equals(definition.getDataType()) && cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
        }
        if (("DATE".equals(definition.getDataType()) || "DATETIME".equals(definition.getDataType()))
                && cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            String pattern = "DATE".equals(definition.getDataType()) ? "yyyy-MM-dd" : "yyyy-MM-dd HH:mm:ss";
            return new SimpleDateFormat(pattern).format(cell.getDateCellValue());
        }
        return readText(cell);
    }

    private String readText(Cell cell) {
        if (cell == null) return "";
        return new DataFormatter(Locale.CHINA).formatCellValue(cell).trim();
    }

    private boolean isBlankRow(Row row, Set<Integer> columns) {
        for (Integer column : columns) if (!readText(row.getCell(column)).isEmpty()) return false;
        return true;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BusinessException("IMPORT_FILE_EMPTY", "请选择Excel文件");
        if (file.getSize() > MAX_FILE_SIZE) throw new BusinessException("IMPORT_FILE_TOO_LARGE", "Excel文件不能超过20MB");
        String name = safeFileName(file.getOriginalFilename()).toLowerCase(Locale.ROOT);
        if (!name.endsWith(".xlsx")) throw new BusinessException("IMPORT_FILE_TYPE_INVALID", "只支持 .xlsx 文件");
    }

    private void addExplicitValidation(XSSFSheet sheet, int column, String[] values) {
        XSSFDataValidationHelper helper = new XSSFDataValidationHelper(sheet);
        DataValidationConstraint constraint = helper.createExplicitListConstraint(values);
        DataValidation validation = helper.createValidation(constraint,
                new CellRangeAddressList(DATA_START_ROW_INDEX, DATA_START_ROW_INDEX + MAX_ROWS - 1, column, column));
        validation.setShowErrorBox(true);
        sheet.addValidationData(validation);
    }

    private void addFormulaValidation(XSSFSheet sheet, int column, String rangeName) {
        XSSFDataValidationHelper helper = new XSSFDataValidationHelper(sheet);
        DataValidationConstraint constraint = helper.createFormulaListConstraint(rangeName);
        DataValidation validation = helper.createValidation(constraint,
                new CellRangeAddressList(DATA_START_ROW_INDEX, DATA_START_ROW_INDEX + MAX_ROWS - 1, column, column));
        validation.setShowErrorBox(true);
        sheet.addValidationData(validation);
    }

    private String excelColumn(int index) {
        StringBuilder result = new StringBuilder();
        int value = index;
        do {
            result.insert(0, (char) ('A' + value % 26));
            value = value / 26 - 1;
        } while (value >= 0);
        return result.toString();
    }

    private boolean parseDeleted(String value) {
        if (value == null || value.trim().isEmpty() || "false".equalsIgnoreCase(value) || "0".equals(value)) return false;
        if ("true".equalsIgnoreCase(value) || "1".equals(value)) return true;
        throw new BusinessException("IMPORT_DELETED_INVALID", "deleted只能是 true、false、1或0");
    }

    private List<Map<String, String>> createExampleRows(List<IndicatorDefinition> definitions) {
        List<Map<String, String>> rows = new ArrayList<Map<String, String>>();
        rows.add(examplePerson("IMPORT-SAMPLE-001", "导入示例-赵一", "男", "星海科技有限公司", "产品经理", "长沙市岳麓区", "人员导入示例数据"));
        rows.add(examplePerson("IMPORT-SAMPLE-002", "导入示例-钱二", "女", "远山物流有限公司", "运营主管", "株洲市天元区", "可直接用于验证导入流程"));
        rows.add(examplePerson("IMPORT-SAMPLE-003", "导入示例-孙三", "未知", "青禾教育中心", "讲师", "湘潭市雨湖区", "重复导入会按人员编码更新"));
        for (IndicatorDefinition definition : definitions) {
            List<IndicatorOption> options = "ENUM".equals(definition.getDataType())
                    ? indicatorService.enabledOptions(definition.getId()) : Collections.<IndicatorOption>emptyList();
            for (int row = 0; row < rows.size(); row++) {
                rows.get(row).put(definition.getCode(), exampleIndicatorValue(definition, options, row));
            }
        }
        return rows;
    }

    private Map<String, String> examplePerson(String externalId, String name, String gender,
                                               String organization, String occupation, String address, String remark) {
        Map<String, String> row = new LinkedHashMap<String, String>();
        row.put("external_id", externalId);
        row.put("name", name);
        row.put("gender", gender);
        row.put("organization", organization);
        row.put("occupation", occupation);
        row.put("address", address);
        row.put("remark", remark);
        row.put("deleted", "false");
        return row;
    }

    private String exampleIndicatorValue(IndicatorDefinition definition, List<IndicatorOption> options, int row) {
        if ("NUMBER".equals(definition.getDataType())) return exampleNumberValue(definition, row);
        if ("BOOLEAN".equals(definition.getDataType())) return row == 1 ? "false" : "true";
        if ("DATE".equals(definition.getDataType())) return Arrays.asList("2026-07-01", "2026-07-02", "2026-07-03").get(row);
        if ("DATETIME".equals(definition.getDataType())) {
            return Arrays.asList("2026-07-01 09:00:00", "2026-07-02 10:30:00", "2026-07-03 14:00:00").get(row);
        }
        if ("ENUM".equals(definition.getDataType())) {
            return options.isEmpty() ? "" : options.get(row % options.size()).getCode();
        }
        return "示例值" + (row + 1);
    }

    private String exampleNumberValue(IndicatorDefinition definition, int row) {
        String code = definition.getCode().toUpperCase(Locale.ROOT);
        String unit = definition.getUnit() == null ? "" : definition.getUnit().trim();
        if (code.contains("AGE") || "岁".equals(unit)) return Arrays.asList("28", "35", "42").get(row);
        if (code.contains("FLOW") || code.contains("AMOUNT") || "元".equals(unit)) {
            return Arrays.asList("12000000", "800000", "3500000").get(row);
        }
        return Arrays.asList("100", "200", "300").get(row);
    }

    private void writeDataRows(XSSFSheet sheet, List<IndicatorDefinition> definitions,
                               List<Map<String, String>> dataRows) {
        for (int index = 0; index < dataRows.size(); index++) {
            Row row = sheet.createRow(DATA_START_ROW_INDEX + index);
            int column = 0;
            for (String header : BASE_HEADERS) row.createCell(column++).setCellValue(dataRows.get(index).get(header));
            for (IndicatorDefinition definition : definitions) {
                row.createCell(column++).setCellValue(dataRows.get(index).get(definition.getCode()));
            }
        }
    }

    private void validateDescriptionRow(Row row, Map<Integer, String> headers,
                                        Map<String, IndicatorDefinition> indicatorByCode) {
        if (row == null) throw new BusinessException("IMPORT_DESCRIPTION_MISSING", "Excel第二行必须填写中文字段说明");
        for (Map.Entry<Integer, String> entry : headers.entrySet()) {
            IndicatorDefinition definition = indicatorByCode.get(entry.getValue());
            String expected = definition == null ? BASE_HEADER_LABELS.get(BASE_HEADERS.indexOf(entry.getValue()))
                    : indicatorLabel(definition);
            if (!expected.equals(readText(row.getCell(entry.getKey())))) {
                throw new BusinessException("IMPORT_DESCRIPTION_MISSING", "Excel第二行中文说明与当前模板不一致，请重新下载模板");
            }
        }
    }

    private void applyColumnWidths(XSSFSheet sheet, List<IndicatorDefinition> definitions,
                                   List<Map<String, String>> dataRows) {
        int column = 0;
        for (int i = 0; i < BASE_HEADERS.size(); i++) {
            sheet.setColumnWidth(column++, columnWidth(BASE_HEADERS.get(i), BASE_HEADER_LABELS.get(i), dataRows));
        }
        for (IndicatorDefinition definition : definitions) {
            sheet.setColumnWidth(column++, columnWidth(definition.getCode(), indicatorLabel(definition), dataRows));
        }
    }

    private int columnWidth(String header, String label, List<Map<String, String>> dataRows) {
        int length = Math.max(displayLength(header), displayLength(label));
        for (Map<String, String> row : dataRows) length = Math.max(length, displayLength(row.get(header)));
        return Math.min(32, Math.max(14, length + 3)) * 256;
    }

    private int displayLength(String value) {
        if (value == null) return 0;
        int length = 0;
        for (int index = 0; index < value.length(); index++) length += value.charAt(index) > 255 ? 2 : 1;
        return length;
    }

    private String indicatorLabel(IndicatorDefinition definition) {
        String unit = definition.getUnit() == null || definition.getUnit().trim().isEmpty()
                ? "" : "，单位：" + definition.getUnit().trim();
        return definition.getName() + "（" + indicatorTypeLabel(definition.getDataType()) + unit + "）";
    }

    private String indicatorTypeLabel(String dataType) {
        if ("NUMBER".equals(dataType)) return "数值";
        if ("DATE".equals(dataType)) return "日期";
        if ("DATETIME".equals(dataType)) return "日期时间";
        if ("BOOLEAN".equals(dataType)) return "是/否";
        if ("ENUM".equals(dataType)) return "下拉选择";
        return "文本";
    }

    private void writeHeaderCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createDescriptionStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        return style;
    }

    private String required(String value, String message) {
        if (value == null || value.trim().isEmpty()) throw new BusinessException("IMPORT_REQUIRED", message);
        return value.trim();
    }

    private String emptyToNull(String value) { return value == null || value.trim().isEmpty() ? null : value.trim(); }
    private String safeFileName(String name) { return name == null || name.trim().isEmpty() ? "persons.xlsx" : name.replace("\\", "_").replace("/", "_"); }
    private String abbreviate(String value) { return value == null ? "未知错误" : value.length() <= 2000 ? value : value.substring(0, 2000); }
    private String join(List<String> values) { return String.join("；", values); }
}
