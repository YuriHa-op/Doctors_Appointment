package server;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ActivityLogger {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String logFilePath;
    private final ObjectMapper mapper;

    public ActivityLogger(String logFilePath) {
        this.logFilePath = logFilePath;
        this.mapper = JsonMapper.builder()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .build();
        initLogFile();
    }

    // Append an entry to the JSON log file

    public synchronized void log(String user, String action, String detail) {
        try {
            ArrayNode entries = loadEntries();

            ObjectNode entry = mapper.createObjectNode();
            entry.put("timestamp", LocalDateTime.now().format(FORMATTER));
            entry.put("user", user != null ? user : "");
            entry.put("action", action != null ? action : "");
            entry.put("detail", detail != null ? detail : "");

            entries.add(entry);
            mapper.writeValue(new File(logFilePath), entries);

        } catch (Exception e) {
            System.err.println("[ActivityLogger] Failed to write log: " + e.getMessage());
        }
    }

    // Print all log entries to the console in a readable format

    public synchronized void printLog() {
        try {
            File file = new File(logFilePath);
            if (!file.exists()) {
                System.out.println("[Log] No log file found.");
                return;
            }

            ArrayNode entries = loadEntries();

            if (entries.isEmpty()) {
                System.out.println("[Log] No activity recorded yet.");
                return;
            }

            System.out.println("=== Activity Log ===");
            for (JsonNode entry : entries) {
                String timestamp = text(entry, "timestamp");
                String user      = text(entry, "user");
                String action    = text(entry, "action");
                String detail    = text(entry, "detail");
                System.out.printf("[%s] [%s] %s — %s%n", timestamp, user, action, detail);
            }
            System.out.println("====================");

        } catch (Exception e) {
            System.err.println("[ActivityLogger] Failed to read log: " + e.getMessage());
        }
    }

    // HELPERS

    private void initLogFile() {
        File file = new File(logFilePath);
        if (!file.exists()) {
            try {
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                mapper.writeValue(file, mapper.createArrayNode());
            } catch (Exception e) {
                System.err.println("[ActivityLogger] Could not create log file: " + e.getMessage());
            }
        }
    }

    private ArrayNode loadEntries() throws Exception {
        File file = new File(logFilePath);
        if (file.exists()) {
            JsonNode node = mapper.readTree(file);
            if (node.isArray()) return (ArrayNode) node;
        }
        return mapper.createArrayNode();
    }

    private String text(JsonNode node, String field) {
        JsonNode val = node.get(field);
        return val != null && !val.isNull() ? val.asText() : "";
    }
}
