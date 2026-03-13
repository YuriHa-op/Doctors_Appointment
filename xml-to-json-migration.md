# XML to JSON Migration Documentation

## Overview
This document describes the Phase 2 migration of the **WellnessLink Clinic Appointment Scheduler** from XML-based data handling to JSON using **Jackson 3.1.0**.

---

## Technology Stack

| Component      | Before          | After                                  |
|----------------|-----------------|----------------------------------------|
| Data Format    | XML             | JSON                                   |
| Data Files     | `.xml`          | `.json`                                |
| Parsing Library| Java DOM API    | Jackson 3.1.0 (`tools.jackson`)        |
| Database Class | `XMLDatabase`   | `JsonDatabase`                         |
| Payload Format | XML tags        | JSON strings                           |
| Model Parser   | `XMLModelParser`| Inline Jackson `ObjectMapper` parsing  |
| XML Helper     | `SimpleXMLParser`| Removed (not needed with JSON)        |

---

## Dependency Changes (`pom.xml`)

Added Jackson 3.1.0 dependency:

```xml
<dependency>
    <groupId>tools.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>3.1.0</version>
</dependency>
```

> **Note:** Jackson 3.x uses the `tools.jackson` package namespace (not `com.fasterxml.jackson`).

---

## Data File Conversions

### `users.xml` → `users.json`

**Before (XML):**
```xml
<users>
  <user>
    <id>DOC-7482E551</id>
    <username>Yuri</username>
    <password>1111</password>
    <role>doctor</role>
    <name>Yuriha</name>
    <contact>096767676</contact>
    <specialty>Neuro</specialty>
  </user>
  ...
</users>
```

**After (JSON):**
```json
[
  {
    "id": "DOC-7482E551",
    "username": "Yuri",
    "password": "1111",
    "role": "doctor",
    "name": "Yuriha",
    "contact": "096767676",
    "specialty": "Neuro"
  }
]
```

### `schedules.xml` → `schedules.json`

**Before (XML):**
```xml
<schedules>
  <schedule>
    <id>SCH-001</id>
    <doctorId>DOC-001</doctorId>
    <date>2025-06-09</date>
    <startTime>09:00</startTime>
    <endTime>10:00</endTime>
    <status>available</status>
    <patientId/>
  </schedule>
</schedules>
```

**After (JSON):**
```json
[
  {
    "id": "SCH-001",
    "doctorId": "DOC-001",
    "date": "2025-06-09",
    "startTime": "09:00",
    "endTime": "10:00",
    "status": "available",
    "patientId": ""
  }
]
```

### `activity_log.xml` → `activity_log.json`

**Before (XML):**
```xml
<activityLog>
  <entry>
    <timestamp>2025-06-10T08:00:00</timestamp>
    <userId>DOC-001</userId>
    <action>LOGIN</action>
    <details>User logged in</details>
  </entry>
</activityLog>
```

**After (JSON):**
```json
[
  {
    "timestamp": "2025-06-10T08:00:00",
    "userId": "DOC-001",
    "action": "LOGIN",
    "details": "User logged in"
  }
]
```

---

## Class-Level Changes

### 1. `JsonDatabase.java` (NEW — replaces `XMLDatabase.java`)

- **Purpose:** Central data access layer for users and schedules
- **Library:** Jackson `ObjectMapper` via `JsonMapper.builder()`
- **File format:** JSON arrays stored in `users.json` and `schedules.json`
- **Key patterns:**
  - `loadArray(File)` / `saveArray(File, ArrayNode)` for reading/writing JSON files
  - `nodeToUser(JsonNode)` / `nodeToSchedule(JsonNode)` for converting JSON nodes to model objects
  - All public methods match the previous `XMLDatabase` API signature

### 2. `ActivityLogger.java` (MODIFIED)

- **Before:** Used Java DOM API (`DocumentBuilderFactory`, `TransformerFactory`)
- **After:** Uses Jackson `ObjectMapper` with `ArrayNode` / `ObjectNode`
- **File:** `activity_log.json` (was `activity_log.xml`)
- `log()` loads the JSON array, appends a new entry object, writes back
- `printLog()` reads and prints all entries

### 3. `Request.java` (MODIFIED)

- Field renamed: `xmlPayload` → `payload`
- Getter/setter: `getPayload()` / `setPayload()` (was `getXmlPayload()` / `setXmlPayload()`)

### 4. `Response.java` (MODIFIED)

- Field renamed: `xmlData` → `data`
- Getter/setter: `getData()` / `setData()` (was `getXmlData()` / `setXmlData()`)

### 5. `AuthService.java` (MODIFIED)

- **Before:** `SimpleXMLParser.extract()` for parsing, string concatenation (`appendTag`) for building XML
- **After:** `mapper.readTree(json)` for parsing, `mapper.writeValueAsString(map)` for building JSON
- Uses `parsePayload()` helper to convert JSON payload → `Map<String,String>`
- Response data serialized as `Map<String,String>` JSON

### 6. `ScheduleService.java` (MODIFIED)

- **Before:** `SimpleXMLParser.extract()` / `appendTag()` for XML handling, custom `parseSlotsXml()` method
- **After:** Jackson `readTree()` / `writeValueAsString()` for JSON, `parseSlotsJson()` reads `"slots"` array
- Schedule list responses serialized as `List<Map<String,String>>` JSON arrays

### 7. `LoginController.java` (MODIFIED)

- **Before:** XML payloads via `setXmlPayload("<tag>value</tag>")`
- **After:** JSON payloads via `setPayload(toJson(map))` using `Map<String,String>`
- Response parsing: `mapper.readTree(response.getData())` with `text(node, field)` helper

### 8. `DoctorController.java` (MODIFIED)

- **Before:** XML payloads, `XMLModelParser.parseScheduleList()` for response parsing
- **After:** JSON payloads via Maps, inline `parseScheduleList(json)` using Jackson
- `createMonthlySchedule` uses `Map<String,Object>` with `"slots"` key containing a `List<Map<String,String>>`

### 9. `PatientController.java` (MODIFIED)

- **Before:** XML payloads, `XMLModelParser.parseScheduleList()` and `XMLModelParser.parseDoctorList()`
- **After:** JSON payloads via Maps, inline `parseScheduleList(json)` and `parseDoctorList(json)` using Jackson
- Same helper pattern: `toJson()`, `text()` methods

### 10. `ServerMain.java` (MODIFIED)

- `XMLDatabase` → `JsonDatabase`
- `activity_log.xml` → `activity_log.json`

---

## Removed Files

| File                          | Reason                                          |
|-------------------------------|--------------------------------------------------|
| `database/XMLDatabase.java`  | Replaced by `JsonDatabase.java`                  |
| `model/XMLModelParser.java`  | Inline Jackson parsing in controllers replaces it|
| `net/SimpleXMLParser.java`   | No longer needed — JSON parsed via Jackson        |
| `resources/users.xml`        | Replaced by `users.json`                          |
| `resources/schedules.xml`    | Replaced by `schedules.json`                      |
| `resources/activity_log.xml` | Replaced by `activity_log.json`                   |

---

## JSON Payload Patterns

### Request Payloads (Client → Server)

Controllers build payloads as serialized `Map<String,String>`:

```java
Map<String, String> payload = new LinkedHashMap<>();
payload.put("username", username);
payload.put("password", password);
req.setPayload(mapper.writeValueAsString(payload));
```

For schedule creation with slot arrays, `Map<String,Object>` is used:

```java
Map<String, Object> payload = new LinkedHashMap<>();
payload.put("doctorId", doctorId);
List<Map<String, String>> slotList = new ArrayList<>();
// ... populate slotList
payload.put("slots", slotList);
req.setPayload(mapper.writeValueAsString(payload));
```

### Response Data (Server → Client)

Services build response data as serialized JSON:

- **Single object:** `mapper.writeValueAsString(Map<String,String>)` — e.g., login response with user info
- **List of objects:** `mapper.writeValueAsString(List<Map<String,String>>)` — e.g., schedule list, doctor list

### Parsing Pattern

Both services and controllers use the same Jackson parsing approach:

```java
JsonNode root = mapper.readTree(jsonString);
// For objects:
String value = root.has("key") ? root.get("key").asText() : "";
// For arrays:
if (root.isArray()) {
    for (JsonNode node : root) {
        String field = node.has("field") ? node.get("field").asText() : "";
    }
}
```

---

## ObjectMapper Configuration

| Location            | Configuration                                                  |
|---------------------|----------------------------------------------------------------|
| `JsonDatabase`      | `JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build()` |
| `ActivityLogger`    | `JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build()` |
| `AuthService`       | `JsonMapper.builder().build()`                                 |
| `ScheduleService`   | `JsonMapper.builder().build()`                                 |
| Controllers         | `JsonMapper.builder().build()`                                 |

> Database and logger use indented output for readable file storage. Services and controllers use compact output for network payloads.

---

## Migration Summary

- **16 files affected** (6 new JSON data files, 1 new class, 9 modified classes, 6 deleted files)
- **Zero external XML dependencies** remain in the active codebase
- All data flows now use JSON end-to-end: storage → server services → network payloads → client controllers
- Jackson 3.1.0 (`tools.jackson` package) used throughout
