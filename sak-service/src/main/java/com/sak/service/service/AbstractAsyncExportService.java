package com.sak.service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sak.service.config.StaticResourceConfig;
import com.sak.service.entity.SysExportRecord;
import lombok.RequiredArgsConstructor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RequiredArgsConstructor
public abstract class AbstractAsyncExportService<Q, T> {

    private final ExportRecordService exportRecordService;
    private final StaticResourceConfig staticResourceConfig;
    private final ObjectMapper objectMapper;
    private final ExportAsyncInvoker exportAsyncInvoker;

    public SysExportRecord submitExport(Q query, String operator) {
        SysExportRecord record = exportRecordService.createPendingRecord(
                getBizType(),
                buildFileName(query),
                serializeQuery(query),
                operator
        );
        exportAsyncInvoker.runAsync(() -> executeExport(record.getId(), query));
        return record;
    }

    public void executeExport(Long recordId, Q query) {
        exportRecordService.markRunning(recordId);
        Path filePath = null;
        try {
            filePath = staticResourceConfig.createExportFile(buildRelativeFilePath(query));
            long totalCount = 0L;
            long lastId = 0L;
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(filePath));
                 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(zipOutputStream, StandardCharsets.UTF_8))) {
                zipOutputStream.putNextEntry(new ZipEntry(buildCsvEntryFileName(query)));
                writer.write('\uFEFF');
                writeCsvRow(writer, buildHeaderColumns());
                while (true) {
                    List<T> batch = fetchBatch(lastId, getBatchSize(), query);
                    if (batch.isEmpty()) {
                        break;
                    }
                    for (T item : batch) {
                        writeCsvRow(writer, mapRow(item));
                        lastId = extractId(item);
                        totalCount++;
                    }
                }
                writer.flush();
                zipOutputStream.closeEntry();
            }
            exportRecordService.markSuccess(recordId, staticResourceConfig.toStorageRelativePath(filePath), Files.size(filePath), totalCount);
        } catch (Exception ex) {
            if (filePath != null) {
                try {
                    Files.deleteIfExists(filePath);
                } catch (IOException ignored) {
                }
            }
            exportRecordService.markFailed(recordId, truncateError(ex.getMessage()));
        }
    }

    protected int getBatchSize() {
        return 500;
    }

    protected abstract String getBizType();

    protected abstract List<String> buildHeaderColumns();

    protected abstract List<String> mapRow(T item);

    protected abstract List<T> fetchBatch(long lastId, int batchSize, Q query);

    protected abstract long extractId(T item);

    protected abstract String buildFileName(Q query);

    protected String buildCsvEntryFileName(Q query) {
        String zipName = buildFileName(query);
        if (zipName.endsWith(".zip")) {
            return zipName.substring(0, zipName.length() - 4) + ".csv";
        }
        return zipName + ".csv";
    }

    protected String buildRelativeFilePath(Q query) {
        return "exports/" + getBizType().toLowerCase() + "/" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "/" + buildFileName(query);
    }

    protected String serializeQuery(Q query) {
        try {
            return objectMapper.writeValueAsString(query);
        } catch (Exception ex) {
            throw new IllegalStateException("序列化导出查询条件失败", ex);
        }
    }

    private void writeCsvRow(BufferedWriter writer, List<String> values) throws IOException {
        writer.write(values.stream().map(this::escapeCsv).reduce((left, right) -> left + "," + right).orElse(""));
        writer.newLine();
    }

    private String escapeCsv(String value) {
        String normalized = value == null ? "" : value;
        String escaped = normalized.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private String truncateError(String message) {
        if (message == null || message.isBlank()) {
            return "导出任务执行失败";
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
