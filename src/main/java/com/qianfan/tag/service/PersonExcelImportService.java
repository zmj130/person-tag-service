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
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
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
    private static final long MAX_FILE_SIZE = 20L * 1024L * 1024L;
    private static final List<String> BASE_HEADERS = Arrays.asList(
            "external_id", "name", "gender", "organization", "occupation", "address", "remark", "deleted");
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
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("人员导入");
            Sheet optionsSheet = workbook.createSheet("枚举选项");
            workbook.setSheetHidden(workbook.getSheetIndex(optionsSheet), true);
            Row header = sheet.createRow(0);
            int column = 0;
            for (String name : BASE_HEADERS) header.createCell(column++).setCellValue(name);
            List<IndicatorDefinition> definitions = indicatorService.importDefinitions();
            for (IndicatorDefinition definition : definitions) header.createCell(column++).setCellValue(definition.getCode());

            sheet.createFreezePane(0, 1);
            for (int i = 0; i < column; i++) sheet.setColumnWidth(i, i < 2 ? 5200 : 4200);
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
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new BusinessException("TEMPLATE_CREATE_FAILED", "Excel模板生成失败");
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

            List<PersonImportRow> rows = new ArrayList<PersonImportRow>();
            List<String> errors = new ArrayList<String>();
            Set<String> externalIds = new HashSet<String>();
            int lastRow = sheet.getLastRowNum();
            if (lastRow > MAX_ROWS) throw new BusinessException("IMPORT_TOO_MANY_ROWS", "单次导入最多 " + MAX_ROWS + " 行");
            for (int rowIndex = 1; rowIndex <= lastRow; rowIndex++) {
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
        DataValidation validation = helper.createValidation(constraint, new CellRangeAddressList(1, MAX_ROWS, column, column));
        validation.setShowErrorBox(true);
        sheet.addValidationData(validation);
    }

    private void addFormulaValidation(XSSFSheet sheet, int column, String rangeName) {
        XSSFDataValidationHelper helper = new XSSFDataValidationHelper(sheet);
        DataValidationConstraint constraint = helper.createFormulaListConstraint(rangeName);
        DataValidation validation = helper.createValidation(constraint, new CellRangeAddressList(1, MAX_ROWS, column, column));
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

    private String required(String value, String message) {
        if (value == null || value.trim().isEmpty()) throw new BusinessException("IMPORT_REQUIRED", message);
        return value.trim();
    }

    private String emptyToNull(String value) { return value == null || value.trim().isEmpty() ? null : value.trim(); }
    private String safeFileName(String name) { return name == null || name.trim().isEmpty() ? "persons.xlsx" : name.replace("\\", "_").replace("/", "_"); }
    private String abbreviate(String value) { return value == null ? "未知错误" : value.length() <= 2000 ? value : value.substring(0, 2000); }
    private String join(List<String> values) { return String.join("；", values); }
}
