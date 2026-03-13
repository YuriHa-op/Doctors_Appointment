package database;

import model.ScheduleModel;
import model.UserModel;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

public class JsonDatabase {

    private final String usersFilePath;
    private final String schedulesFilePath;
    private final ObjectMapper mapper;

    public JsonDatabase(String dataDir) {
        this.usersFilePath = dataDir + "/users.json";
        this.schedulesFilePath = dataDir + "/schedules.json";
        this.mapper = JsonMapper.builder()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .build();
    }

    // USER METHODS

    public synchronized UserModel authenticate(String username, String password) {
        try {
            ArrayNode users = loadArray(usersFilePath);
            for (JsonNode node : users) {
                if (username.equals(text(node, "username"))
                        && password.equals(text(node, "password"))) {
                    return nodeToUser(node);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public synchronized UserModel register(String username, String password, String role,
                                           String name, String contact, String specialty) {
        try {
            ArrayNode users = loadArray(usersFilePath);

            for (JsonNode node : users) {
                if (username.equals(text(node, "username"))) {
                    return null; // username taken
                }
            }

            String prefix = "doctor".equals(role) ? "DOC-" : "PAT-";
            String id = prefix + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            ObjectNode userNode = mapper.createObjectNode();
            userNode.put("id", id);
            userNode.put("username", username);
            userNode.put("password", password);
            userNode.put("role", role);
            userNode.put("name", name);
            userNode.put("contact", contact != null ? contact : "");
            userNode.put("specialty", "doctor".equals(role) && specialty != null ? specialty : "");

            users.add(userNode);
            saveArray(users, usersFilePath);

            return new UserModel(id, username, password, role, name, contact, specialty);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public synchronized UserModel getUserById(String userId) {
        try {
            ArrayNode users = loadArray(usersFilePath);
            for (JsonNode node : users) {
                if (userId.equals(text(node, "id"))) {
                    return nodeToUser(node);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // SCHEDULE METHODS

    public synchronized ScheduleModel getScheduleById(String scheduleId) {
        try {
            ArrayNode schedules = loadArray(schedulesFilePath);
            for (JsonNode node : schedules) {
                if (scheduleId.equals(text(node, "id"))) {
                    return nodeToSchedule(node);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public synchronized int createMonthlySchedule(String doctorId, int year, int month,
                                                   List<String> slots, boolean includeSaturday) {
        try {
            ArrayNode schedules = loadArray(schedulesFilePath);

            LocalDate start = LocalDate.of(year, month, 1);
            LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
            int count = 0;

            for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                DayOfWeek dow = d.getDayOfWeek();
                if (dow == DayOfWeek.SUNDAY) continue;
                if (dow == DayOfWeek.SATURDAY && !includeSaturday) continue;

                for (String slot : slots) {
                    String[] parts = slot.split("-");
                    if (parts.length != 2) continue;
                    String startTime = parts[0].trim();
                    String endTime = parts[1].trim();
                    String dateStr = d.toString();

                    if (slotExists(schedules, doctorId, dateStr, startTime)) continue;

                    String id = "SCH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

                    ObjectNode schedNode = mapper.createObjectNode();
                    schedNode.put("id", id);
                    schedNode.put("doctorId", doctorId);
                    schedNode.put("date", dateStr);
                    schedNode.put("startTime", startTime);
                    schedNode.put("endTime", endTime);
                    schedNode.put("status", "Available");
                    schedNode.put("patientId", "");
                    schedNode.put("patientName", "");
                    schedules.add(schedNode);
                    count++;
                }
            }

            saveArray(schedules, schedulesFilePath);
            return count;

        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public synchronized List<ScheduleModel> getSchedulesByDoctor(String doctorId) {
        List<ScheduleModel> result = new ArrayList<>();
        try {
            ArrayNode schedules = loadArray(schedulesFilePath);
            for (JsonNode node : schedules) {
                if (doctorId.equals(text(node, "doctorId"))) {
                    ScheduleModel s = nodeToSchedule(node);
                    String patientName = text(node, "patientName");

                    // Backward compatibility for older records that only stored patientId.
                    if (patientName == null || patientName.isEmpty()) {
                        String patId = s.getPatientId();
                        if (patId != null && !patId.isEmpty()) {
                            UserModel patient = getUserById(patId);
                            patientName = patient != null ? patient.getName() : "Unknown";
                        }
                    }

                    s.setPatientName(patientName != null ? patientName : "");
                    result.add(s);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        result.sort((a, b) -> {
            int cmp = safe(a.getDate()).compareTo(safe(b.getDate()));
            if (cmp != 0) return cmp;
            return safe(a.getStartTime()).compareTo(safe(b.getStartTime()));
        });
        return result;
    }

    public synchronized int deleteSchedulesByDate(String doctorId, String date) {
        try {
            ArrayNode schedules = loadArray(schedulesFilePath);
            int cancelled = 0;

            for (int i = schedules.size() - 1; i >= 0; i--) {
                JsonNode node = schedules.get(i);
                if (doctorId.equals(text(node, "doctorId"))
                        && date.equals(text(node, "date"))
                        && "Available".equals(text(node, "status"))) {
                    schedules.remove(i);
                    cancelled++;
                }
            }

            saveArray(schedules, schedulesFilePath);
            return cancelled;

        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public synchronized int overrideDay(String doctorId, String date, String type) {
        try {
            ArrayNode schedules = loadArray(schedulesFilePath);
            int cancelled = 0;

            for (int i = schedules.size() - 1; i >= 0; i--) {
                JsonNode node = schedules.get(i);
                boolean match = doctorId.equals(text(node, "doctorId"))
                        && date.equals(text(node, "date"))
                        && "Available".equals(text(node, "status"));

                if (match) {
                    if ("offday".equals(type)) {
                        schedules.remove(i);
                        cancelled++;
                    } else if ("halfday".equals(type)) {
                        String startTime = text(node, "startTime");
                        if (startTime.compareTo("13:00") >= 0) {
                            schedules.remove(i);
                            cancelled++;
                        }
                    }
                }
            }

            saveArray(schedules, schedulesFilePath);
            return cancelled;

        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public synchronized List<ScheduleModel> searchAppointments(String doctorId, String keyword) {
        List<ScheduleModel> result = new ArrayList<>();
        try {
            ArrayNode schedules = loadArray(schedulesFilePath);
            String keywordLower = keyword.toLowerCase();

            for (JsonNode node : schedules) {
                if (!doctorId.equals(text(node, "doctorId"))) continue;

                String patientName = text(node, "patientName");
                if (patientName == null || patientName.isEmpty()) {
                    String patId = text(node, "patientId");
                    if (!patId.isEmpty()) {
                        UserModel patient = getUserById(patId);
                        if (patient != null) {
                            patientName = patient.getName();
                        }
                    }
                }

                if (patientName == null || patientName.isEmpty()) continue;

                String date = text(node, "date");
                boolean nameMatch = patientName.toLowerCase().contains(keywordLower);
                boolean dateMatch = date.contains(keyword);
                if (!nameMatch && !dateMatch) continue;

                ScheduleModel s = nodeToSchedule(node);
                s.setPatientName(patientName);
                result.add(s);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public synchronized boolean deleteSlotById(String scheduleId) {
        try {
            ArrayNode schedules = loadArray(schedulesFilePath);
            for (int i = 0; i < schedules.size(); i++) {
                JsonNode node = schedules.get(i);
                if (scheduleId.equals(text(node, "id"))) {
                    String status = text(node, "status");
                    if ("Available".equals(status)) {
                        schedules.remove(i);
                    } else {
                        ((ObjectNode) node).put("status", ScheduleModel.STATUS_CANCELLED);
                    }
                    saveArray(schedules, schedulesFilePath);
                    return true;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public synchronized boolean updateSlotTime(String scheduleId, String newStartTime, String newEndTime) {
        try {
            ArrayNode schedules = loadArray(schedulesFilePath);
            for (int i = 0; i < schedules.size(); i++) {
                JsonNode node = schedules.get(i);
                if (scheduleId.equals(text(node, "id"))) {
                    String currentStatus = text(node, "status");
                    ((ObjectNode) node).put("startTime", newStartTime);
                    ((ObjectNode) node).put("endTime", newEndTime);
                    if (ScheduleModel.STATUS_BOOKED.equals(currentStatus)
                            || ScheduleModel.STATUS_RESCHEDULED.equals(currentStatus)) {
                        ((ObjectNode) node).put("status", ScheduleModel.STATUS_RESCHEDULED);
                    }
                    saveArray(schedules, schedulesFilePath);
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // PATIENT METHODS

    public synchronized List<ScheduleModel> getAvailableSlots(String doctorId) {
        List<ScheduleModel> result = new ArrayList<>();
        try {
            ArrayNode schedules = loadArray(schedulesFilePath);
            for (JsonNode node : schedules) {
                if (!"Available".equals(text(node, "status"))) continue;
                if (doctorId != null && !doctorId.isEmpty()
                        && !doctorId.equals(text(node, "doctorId"))) continue;

                ScheduleModel s = nodeToSchedule(node);
                UserModel doctor = getUserById(s.getDoctorId());
                s.setDoctorName(doctor != null ? doctor.getName() : "Unknown");
                result.add(s);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        result.sort((a, b) -> {
            int cmp = safe(a.getDate()).compareTo(safe(b.getDate()));
            if (cmp != 0) return cmp;
            return safe(a.getStartTime()).compareTo(safe(b.getStartTime()));
        });
        return result;
    }

    public synchronized boolean bookSlot(String scheduleId, String patientId) {
        try {
            ArrayNode schedules = loadArray(schedulesFilePath);
            for (int i = 0; i < schedules.size(); i++) {
                JsonNode node = schedules.get(i);
                if (scheduleId.equals(text(node, "id"))) {
                    if (!"Available".equals(text(node, "status"))) return false;
                    UserModel patient = getUserById(patientId);
                    ((ObjectNode) node).put("status", "Booked");
                    ((ObjectNode) node).put("patientId", patientId);
                    ((ObjectNode) node).put("patientName", patient != null ? patient.getName() : "");
                    saveArray(schedules, schedulesFilePath);
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public synchronized boolean cancelBooking(String scheduleId, String patientId) {
        try {
            ArrayNode schedules = loadArray(schedulesFilePath);
            for (int i = 0; i < schedules.size(); i++) {
                JsonNode node = schedules.get(i);
                if (scheduleId.equals(text(node, "id"))) {
                    String status = text(node, "status");
                    if (!ScheduleModel.STATUS_BOOKED.equals(status)
                            && !ScheduleModel.STATUS_RESCHEDULED.equals(status)) return false;
                    if (!patientId.equals(text(node, "patientId"))) return false;
                    ((ObjectNode) node).put("status", ScheduleModel.STATUS_CANCELLED);
                    saveArray(schedules, schedulesFilePath);
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public synchronized List<ScheduleModel> getSchedulesByPatient(String patientId) {
        List<ScheduleModel> result = new ArrayList<>();
        try {
            ArrayNode schedules = loadArray(schedulesFilePath);
            for (JsonNode node : schedules) {
                String status = text(node, "status");
                if (!ScheduleModel.STATUS_BOOKED.equals(status)
                        && !ScheduleModel.STATUS_RESCHEDULED.equals(status)
                        && !ScheduleModel.STATUS_CANCELLED.equals(status)) continue;
                if (!patientId.equals(text(node, "patientId"))) continue;

                ScheduleModel s = nodeToSchedule(node);
                UserModel doctor = getUserById(s.getDoctorId());
                s.setDoctorName(doctor != null ? doctor.getName() : "Unknown");
                result.add(s);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        result.sort((a, b) -> {
            int cmp = safe(a.getDate()).compareTo(safe(b.getDate()));
            if (cmp != 0) return cmp;
            return safe(a.getStartTime()).compareTo(safe(b.getStartTime()));
        });
        return result;
    }

    public synchronized List<UserModel> getAllDoctors() {
        List<UserModel> result = new ArrayList<>();
        try {
            ArrayNode users = loadArray(usersFilePath);
            for (JsonNode node : users) {
                if ("doctor".equals(text(node, "role"))) {
                    result.add(nodeToUser(node));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    // HELPERS

    private boolean slotExists(ArrayNode schedules, String doctorId, String date, String startTime) {
        for (JsonNode node : schedules) {
            if (doctorId.equals(text(node, "doctorId"))
                    && date.equals(text(node, "date"))
                    && startTime.equals(text(node, "startTime"))) {
                return true;
            }
        }
        return false;
    }

    private ArrayNode loadArray(String path) throws Exception {
        File file = new File(path);
        if (!file.exists()) {
            return mapper.createArrayNode();
        }
        JsonNode node = mapper.readTree(file);
        if (node.isArray()) {
            return (ArrayNode) node;
        }
        return mapper.createArrayNode();
    }

    private void saveArray(ArrayNode array, String path) throws Exception {
        File file = new File(path);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        mapper.writeValue(file, array);
    }

    private String text(JsonNode node, String field) {
        JsonNode val = node.get(field);
        return val != null && !val.isNull() ? val.asText() : "";
    }

    private UserModel nodeToUser(JsonNode node) {
        UserModel user = new UserModel();
        user.setId(text(node, "id"));
        user.setUsername(text(node, "username"));
        user.setPassword(text(node, "password"));
        user.setRole(text(node, "role"));
        user.setName(text(node, "name"));
        user.setContact(text(node, "contact"));
        user.setSpecialty(text(node, "specialty"));
        return user;
    }

    private ScheduleModel nodeToSchedule(JsonNode node) {
        ScheduleModel s = new ScheduleModel();
        s.setId(text(node, "id"));
        s.setDoctorId(text(node, "doctorId"));
        s.setDate(text(node, "date"));
        s.setStartTime(text(node, "startTime"));
        s.setEndTime(text(node, "endTime"));
        s.setStatus(text(node, "status"));
        s.setPatientId(text(node, "patientId"));
        s.setPatientName(text(node, "patientName"));
        return s;
    }

    private String safe(String value) {
        return value != null ? value : "";
    }
}
