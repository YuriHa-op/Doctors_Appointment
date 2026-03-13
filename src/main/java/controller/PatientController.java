package controller;

import model.ActionResult;
import model.ScheduleModel;
import model.ScheduleResult;
import model.UserListResult;
import model.UserModel;
import net.ClinicService;
import net.Request;
import net.Response;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PatientController {

    private final ClinicService clinicService;
    private final ObjectMapper mapper = JsonMapper.builder().build();

    public PatientController(ClinicService clinicService) {
        this.clinicService = clinicService;
    }

    public ScheduleResult getPatientSchedules(String patientId) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("patientId", patientId);

        Request req = new Request();
        req.setAction("GET_PATIENT_SCHEDULES");
        req.setEntity("schedule");
        req.setUserId(patientId);
        req.setPayload(toJson(payload));
        return toScheduleResult(sendRequest(req));
    }

    public ScheduleResult getAvailableSlots(String doctorId) {
        Map<String, String> payload = new LinkedHashMap<>();
        if (doctorId != null && !doctorId.isEmpty()) {
            payload.put("doctorId", doctorId);
        }

        Request req = new Request();
        req.setAction("GET_AVAILABLE_SLOTS");
        req.setEntity("schedule");
        req.setPayload(toJson(payload));
        return toScheduleResult(sendRequest(req));
    }

    public ActionResult bookSlot(String patientId, String scheduleId) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("scheduleId", scheduleId);
        payload.put("patientId", patientId);

        Request req = new Request();
        req.setAction("BOOK_SLOT");
        req.setEntity("schedule");
        req.setUserId(patientId);
        req.setPayload(toJson(payload));
        return toActionResult(sendRequest(req));
    }

    public ActionResult cancelBooking(String patientId, String scheduleId) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("scheduleId", scheduleId);
        payload.put("patientId", patientId);

        Request req = new Request();
        req.setAction("CANCEL_BOOKING");
        req.setEntity("schedule");
        req.setUserId(patientId);
        req.setPayload(toJson(payload));
        return toActionResult(sendRequest(req));
    }

    public ActionResult reschedule(String patientId, String oldScheduleId, String newScheduleId) {
        // Cancel old
        ActionResult cancelResult = cancelBooking(patientId, oldScheduleId);
        if (!cancelResult.isSuccess()) {
            return cancelResult;
        }
        // Book new
        ActionResult bookResult = bookSlot(patientId, newScheduleId);
        if (!bookResult.isSuccess()) {
            // Try to rebook old slot if new booking fails
            bookSlot(patientId, oldScheduleId);
            return bookResult;
        }
        return new ActionResult(true, "Appointment rescheduled successfully!");
    }

    public UserListResult getDoctors() {
        Request req = new Request();
        req.setAction("GET_DOCTORS");
        req.setEntity("user");
        Response response = sendRequest(req);
        if (!"success".equals(response.getStatus())) {
            return new UserListResult(false, response.getMessage(), new ArrayList<>());
        }
        List<UserModel> list = parseDoctorList(response.getData());
        return new UserListResult(true, response.getMessage(), list);
    }

    private ScheduleResult toScheduleResult(Response response) {
        if (!"success".equals(response.getStatus())) {
            return new ScheduleResult(false, response.getMessage(), new ArrayList<>());
        }
        List<ScheduleModel> list = parseScheduleList(response.getData());
        return new ScheduleResult(true, response.getMessage(), list);
    }

    private ActionResult toActionResult(Response response) {
        if ("success".equals(response.getStatus())) {
            return new ActionResult(true, response.getMessage());
        }
        return new ActionResult(false, response.getMessage());
    }

    private Response sendRequest(Request request) {
        try {
            return clinicService.processRequest(request);
        } catch (RemoteException e) {
            Response errorResp = new Response();
            errorResp.setStatus("error");
            errorResp.setCode(500);
            errorResp.setMessage("Could not connect to server: " + e.getMessage());
            return errorResp;
        }
    }

    private String toJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            e.printStackTrace();
            return "{}";
        }
    }

    private List<ScheduleModel> parseScheduleList(String json) {
        List<ScheduleModel> list = new ArrayList<>();
        if (json == null || json.isEmpty()) return list;
        try {
            JsonNode array = mapper.readTree(json);
            if (array.isArray()) {
                for (JsonNode node : array) {
                    ScheduleModel s = new ScheduleModel();
                    s.setId(text(node, "id"));
                    s.setDoctorId(text(node, "doctorId"));
                    s.setDoctorName(text(node, "doctorName"));
                    s.setDate(text(node, "date"));
                    s.setStartTime(text(node, "startTime"));
                    s.setEndTime(text(node, "endTime"));
                    s.setStatus(text(node, "status"));
                    s.setPatientId(text(node, "patientId"));
                    s.setPatientName(text(node, "patientName"));
                    list.add(s);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private List<UserModel> parseDoctorList(String json) {
        List<UserModel> list = new ArrayList<>();
        if (json == null || json.isEmpty()) return list;
        try {
            JsonNode array = mapper.readTree(json);
            if (array.isArray()) {
                for (JsonNode node : array) {
                    UserModel u = new UserModel();
                    u.setId(text(node, "id"));
                    u.setName(text(node, "name"));
                    u.setSpecialty(text(node, "specialty"));
                    u.setContact(text(node, "contact"));
                    list.add(u);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private String text(JsonNode node, String field) {
        JsonNode val = node.get(field);
        return val != null && !val.isNull() ? val.asText() : "";
    }
}
