package server;

import database.JsonDatabase;
import model.ScheduleModel;
import model.UserModel;
import net.Request;
import net.Response;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Service layer for schedule and user operations.
// All business logic and JSON serialisation for schedule operations.
public class ScheduleService {

    private final JsonDatabase db;
    private final ActivityLogger logger;
    private final CallbackManager callbackManager;
    private final ObjectMapper mapper = JsonMapper.builder().build();

    public ScheduleService(JsonDatabase db, ActivityLogger logger, CallbackManager callbackManager) {
        this.db = db;
        this.logger = logger;
        this.callbackManager = callbackManager;
    }


    // Create monthly schedule slots for a doctor
    public Response createSchedule(Request req) {
        Map<String, String> map = parsePayload(req.getPayload());

        String doctorId = map.getOrDefault("doctorId", "");
        String yearStr  = map.getOrDefault("year", "");
        String monthStr = map.getOrDefault("month", "");
        String includeSat = map.getOrDefault("includeSaturday", "false");

        if (doctorId.isEmpty() || yearStr.isEmpty() || monthStr.isEmpty()) {
            return error(400, "doctorId, year, and month are required.");
        }

        int year, month;
        try {
            year  = Integer.parseInt(yearStr);
            month = Integer.parseInt(monthStr);
        } catch (NumberFormatException e) {
            return error(400, "Invalid year or month.");
        }

        List<String> slots = parseSlotsJson(req.getPayload());
        if (slots.isEmpty()) {
            return error(400, "At least one slot is required.");
        }

        int count = db.createMonthlySchedule(doctorId, year, month, slots, "true".equals(includeSat));
        logger.log(doctorId, "CREATE_SCHEDULE", "Created " + count + " slots for " + year + "-" + month);
        return ok("Created " + count + " schedule slots.", null);
    }

    // Read all schedule slots for a doctor
    public Response readSchedules(Request req) {
        Map<String, String> map = parsePayload(req.getPayload());
        String doctorId = map.getOrDefault("doctorId", "");

        if (doctorId.isEmpty()) {
            return error(400, "doctorId is required.");
        }

        List<ScheduleModel> schedules = db.getSchedulesByDoctor(doctorId);
        List<Map<String, String>> dataList = new ArrayList<>();
        for (ScheduleModel s : schedules) {
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("id", s.getId());
            entry.put("doctorId", s.getDoctorId());
            entry.put("date", s.getDate());
            entry.put("startTime", s.getStartTime());
            entry.put("endTime", s.getEndTime());
            entry.put("status", s.getStatus());
            entry.put("patientId", safe(s.getPatientId()));
            entry.put("patientName", safe(s.getPatientName()));
            dataList.add(entry);
        }
        return ok("Found " + schedules.size() + " slots.", toJson(dataList));
    }

    // Mark available slots for a doctor on a given date as Cancelled
    public Response deleteSchedule(Request req) {
        Map<String, String> map = parsePayload(req.getPayload());
        String doctorId = map.getOrDefault("doctorId", "");
        String date     = map.getOrDefault("date", "");

        if (doctorId.isEmpty() || date.isEmpty()) {
            return error(400, "doctorId and date are required.");
        }

        int count = db.deleteSchedulesByDate(doctorId, date);
        logger.log(doctorId, "CANCEL_SCHEDULE", "Cancelled " + count + " slots for " + date);
        return ok("Cancelled " + count + " available slots for " + date + ".", null);
    }

    // Apply half-day or off-day override for a doctor on a date
    public Response overrideDay(Request req) {
        Map<String, String> map = parsePayload(req.getPayload());
        String doctorId = map.getOrDefault("doctorId", "");
        String date     = map.getOrDefault("date", "");
        String type     = map.getOrDefault("type", "");

        if (doctorId.isEmpty() || date.isEmpty() || type.isEmpty()) {
            return error(400, "doctorId, date, and type (halfday/offday) are required.");
        }

        int count = db.overrideDay(doctorId, date, type);
        logger.log(doctorId, "OVERRIDE_DAY", type + " on " + date + ": cancelled " + count + " slots");
        return ok("Applied " + type + " for " + date + ". Cancelled " + count + " slots.", null);
    }

    // Search booked appointments by patient name or date keyword
    public Response searchAppointments(Request req) {
        Map<String, String> map = parsePayload(req.getPayload());
        String doctorId = map.getOrDefault("doctorId", "");
        String keyword  = map.getOrDefault("keyword", "");

        if (doctorId.isEmpty() || keyword.isEmpty()) {
            return error(400, "doctorId and keyword are required.");
        }

        List<ScheduleModel> results = db.searchAppointments(doctorId, keyword);
        List<Map<String, String>> dataList = new ArrayList<>();
        for (ScheduleModel s : results) {
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("id", s.getId());
            entry.put("date", s.getDate());
            entry.put("startTime", s.getStartTime());
            entry.put("endTime", s.getEndTime());
            entry.put("status", s.getStatus());
            entry.put("patientId", safe(s.getPatientId()));
            entry.put("patientName", safe(s.getPatientName()));
            dataList.add(entry);
        }
        return ok("Found " + results.size() + " matching appointments.", toJson(dataList));
    }

    // Mark a single slot as Cancelled by its ID
    public Response deleteSlot(Request req) {
        Map<String, String> map = parsePayload(req.getPayload());
        String scheduleId = map.getOrDefault("scheduleId", "");

        if (scheduleId.isEmpty()) {
            return error(400, "scheduleId is required.");
        }

        // Look up slot before cancellation to find affected patient
        ScheduleModel slot = db.getScheduleById(scheduleId);
        boolean deleted = db.deleteSlotById(scheduleId);
        if (deleted) {
            logger.log(req.getUserId(), "CANCEL_SLOT", "Cancelled slot " + scheduleId);
            // Notify the patient if the slot was booked/rescheduled
            if (slot != null && ("Booked".equals(slot.getStatus()) || "Rescheduled".equals(slot.getStatus()))
                    && slot.getPatientId() != null && !slot.getPatientId().isEmpty()) {
                callbackManager.notifyUser(slot.getPatientId(), "SLOT_DELETED",
                        "Your appointment on " + slot.getDate() + " ("
                                + slot.getStartTime() + "-" + slot.getEndTime()
                                + ") has been cancelled by the doctor.");
            }
            return ok("Appointment/slot cancelled successfully.", null);
        }
        return error(404, "Slot not found.");
    }

    // Update start/end time of a slot
    public Response updateSlot(Request req) {
        Map<String, String> map = parsePayload(req.getPayload());
        String scheduleId = map.getOrDefault("scheduleId", "");
        String startTime  = map.getOrDefault("startTime", "");
        String endTime    = map.getOrDefault("endTime", "");

        if (scheduleId.isEmpty() || startTime.isEmpty() || endTime.isEmpty()) {
            return error(400, "scheduleId, startTime, and endTime are required.");
        }

        // Look up slot before update to find affected patient
        ScheduleModel slotBefore = db.getScheduleById(scheduleId);
        boolean updated = db.updateSlotTime(scheduleId, startTime, endTime);
        if (updated) {
            logger.log(req.getUserId(), "UPDATE_SLOT",
                    "Updated slot " + scheduleId + " to " + startTime + "-" + endTime);
            // Notify the patient if the slot was booked/rescheduled
            if (slotBefore != null && ("Booked".equals(slotBefore.getStatus()) || "Rescheduled".equals(slotBefore.getStatus()))
                    && slotBefore.getPatientId() != null && !slotBefore.getPatientId().isEmpty()) {
                callbackManager.notifyUser(slotBefore.getPatientId(), "SLOT_UPDATED",
                        "Your appointment on " + slotBefore.getDate()
                                + " has been rescheduled to " + startTime + "-" + endTime + ".");
                return ok("Appointment rescheduled successfully.", null);
            }
            return ok("Slot time updated successfully.", null);
        }
        return error(404, "Slot not found.");
    }

    // PATIENT OPERATIONS

    // Get all available slots, optionally filtered by doctorId
    public Response getAvailableSlots(Request req) {
        Map<String, String> map = parsePayload(req.getPayload());
        String doctorId = map.getOrDefault("doctorId", "");

        List<ScheduleModel> slots = db.getAvailableSlots(doctorId.isEmpty() ? null : doctorId);
        List<Map<String, String>> dataList = new ArrayList<>();
        for (ScheduleModel s : slots) {
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("id", s.getId());
            entry.put("doctorId", s.getDoctorId());
            entry.put("doctorName", safe(s.getDoctorName()));
            entry.put("date", s.getDate());
            entry.put("startTime", s.getStartTime());
            entry.put("endTime", s.getEndTime());
            entry.put("status", s.getStatus());
            dataList.add(entry);
        }
        return ok("Found " + slots.size() + " available slots.", toJson(dataList));
    }

    // Book an available slot for a patient
    public Response bookSlot(Request req) {
        Map<String, String> map = parsePayload(req.getPayload());
        String scheduleId = map.getOrDefault("scheduleId", "");
        String patientId  = map.getOrDefault("patientId", "");

        if (scheduleId.isEmpty() || patientId.isEmpty()) {
            return error(400, "scheduleId and patientId are required.");
        }

        // Look up slot to find the doctor before booking
        ScheduleModel slotInfo = db.getScheduleById(scheduleId);
        boolean booked = db.bookSlot(scheduleId, patientId);
        if (booked) {
            logger.log(patientId, "BOOK_SLOT", "Booked slot " + scheduleId);
            // Notify the doctor that a slot was booked
            if (slotInfo != null) {
                callbackManager.notifyUser(slotInfo.getDoctorId(), "SLOT_BOOKED",
                        "A patient has booked your slot on " + slotInfo.getDate()
                                + " (" + slotInfo.getStartTime() + "-" + slotInfo.getEndTime() + ").");
            }
            return ok("Appointment booked successfully!", null);
        }
        return error(409, "Slot is no longer available.");
    }

    // Cancel a patient's booking
    public Response cancelBooking(Request req) {
        Map<String, String> map = parsePayload(req.getPayload());
        String scheduleId = map.getOrDefault("scheduleId", "");
        String patientId  = map.getOrDefault("patientId", "");

        if (scheduleId.isEmpty() || patientId.isEmpty()) {
            return error(400, "scheduleId and patientId are required.");
        }

        // Look up slot to find the doctor before cancelling
        ScheduleModel cancelSlot = db.getScheduleById(scheduleId);
        boolean cancelled = db.cancelBooking(scheduleId, patientId);
        if (cancelled) {
            logger.log(patientId, "CANCEL_BOOKING", "Cancelled booking " + scheduleId);
            // Notify the doctor that a booking was cancelled
            if (cancelSlot != null) {
                callbackManager.notifyUser(cancelSlot.getDoctorId(), "BOOKING_CANCELLED",
                        "A patient has cancelled the appointment on " + cancelSlot.getDate()
                                + " (" + cancelSlot.getStartTime() + "-" + cancelSlot.getEndTime() + ").");
            }
            return ok("Booking cancelled successfully.", null);
        }
        return error(404, "Booking not found or doesn't belong to you.");
    }

    // Get all booked appointments for a patient
    public Response getPatientSchedules(Request req) {
        Map<String, String> map = parsePayload(req.getPayload());
        String patientId = map.getOrDefault("patientId", "");

        if (patientId.isEmpty()) {
            return error(400, "patientId is required.");
        }

        List<ScheduleModel> schedules = db.getSchedulesByPatient(patientId);
        List<Map<String, String>> dataList = new ArrayList<>();
        for (ScheduleModel s : schedules) {
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("id", s.getId());
            entry.put("doctorId", s.getDoctorId());
            entry.put("doctorName", safe(s.getDoctorName()));
            entry.put("date", s.getDate());
            entry.put("startTime", s.getStartTime());
            entry.put("endTime", s.getEndTime());
            entry.put("status", s.getStatus());
            entry.put("patientId", safe(s.getPatientId()));
            dataList.add(entry);
        }
        return ok("Found " + schedules.size() + " appointments.", toJson(dataList));
    }

    // Get all doctors
    public Response getDoctors() {
        List<UserModel> doctors = db.getAllDoctors();
        List<Map<String, String>> dataList = new ArrayList<>();
        for (UserModel d : doctors) {
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("id", d.getId());
            entry.put("name", d.getName());
            entry.put("specialty", safe(d.getSpecialty()));
            entry.put("contact", safe(d.getContact()));
            dataList.add(entry);
        }
        return ok("Found " + doctors.size() + " doctors.", toJson(dataList));
    }

    // HELPERS

    private Response ok(String message, String jsonData) {
        Response r = new Response();
        r.setStatus("success");
        r.setCode(200);
        r.setMessage(message);
        if (jsonData != null) r.setData(jsonData);
        return r;
    }

    private Response error(int code, String message) {
        Response r = new Response();
        r.setStatus("error");
        r.setCode(code);
        r.setMessage(message);
        return r;
    }

    private String safe(String value) {
        return value != null ? value : "";
    }

    private Map<String, String> parsePayload(String json) {
        Map<String, String> map = new LinkedHashMap<>();
        if (json == null || json.isEmpty()) return map;
        try {
            JsonNode node = mapper.readTree(json);
            node.propertyNames().forEach(field -> {
                JsonNode val = node.get(field);
                if (!val.isArray()) {
                    map.put(field, val.asText());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    private String toJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            e.printStackTrace();
            return "[]";
        }
    }

    // Parse "slots" array from JSON payload: {"slots":["09:00-10:00","10:00-11:00"]}
    private List<String> parseSlotsJson(String json) {
        List<String> slots = new ArrayList<>();
        if (json == null || json.isEmpty()) return slots;
        try {
            JsonNode node = mapper.readTree(json);
            JsonNode slotsNode = node.get("slots");
            if (slotsNode != null && slotsNode.isArray()) {
                for (JsonNode s : slotsNode) {
                    slots.add(s.asText());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return slots;
    }
}

