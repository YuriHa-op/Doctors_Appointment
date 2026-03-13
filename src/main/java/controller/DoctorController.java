package controller;

import model.ActionResult;
import model.ScheduleModel;
import model.ScheduleResult;
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

public class DoctorController {

    private final ClinicService clinicService;
    private final ObjectMapper mapper = JsonMapper.builder().build();

    public DoctorController(ClinicService clinicService) {
        this.clinicService = clinicService;
    }

    public ActionResult createMonthlySchedule(String doctorId, int year, int month,
                                          List<String> timeSlots, boolean includeSaturday) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("doctorId", doctorId);
        payload.put("year", String.valueOf(year));
        payload.put("month", String.valueOf(month));
        payload.put("includeSaturday", String.valueOf(includeSaturday));
        payload.put("slots", timeSlots);

        Request req = new Request();
        req.setAction("CREATE_SCHEDULE");
        req.setEntity("schedule");
        req.setUserId(doctorId);
        req.setPayload(toJson(payload));
        return toActionResult(sendRequest(req));
    }


    public ScheduleResult getSchedulesByDoctor(String doctorId) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("doctorId", doctorId);

        Request req = new Request();
        req.setAction("READ_SCHEDULES");
        req.setEntity("schedule");
        req.setUserId(doctorId);
        req.setPayload(toJson(payload));
        return toScheduleResult(sendRequest(req));
    }


    public ActionResult deleteSchedulesByDate(String doctorId, String date) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("doctorId", doctorId);
        payload.put("date", date);

        Request req = new Request();
        req.setAction("DELETE_SCHEDULE");
        req.setEntity("schedule");
        req.setUserId(doctorId);
        req.setPayload(toJson(payload));
        return toActionResult(sendRequest(req));
    }

    public ActionResult overrideDay(String doctorId, String date, String type) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("doctorId", doctorId);
        payload.put("date", date);
        payload.put("type", type);

        Request req = new Request();
        req.setAction("OVERRIDE_DAY");
        req.setEntity("schedule");
        req.setUserId(doctorId);
        req.setPayload(toJson(payload));
        return toActionResult(sendRequest(req));
    }

    public ScheduleResult searchAppointments(String doctorId, String keyword) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("doctorId", doctorId);
        payload.put("keyword", keyword);

        Request req = new Request();
        req.setAction("SEARCH");
        req.setEntity("schedule");
        req.setUserId(doctorId);
        req.setPayload(toJson(payload));
        return toScheduleResult(sendRequest(req));
    }

    public ActionResult deleteSlotById(String doctorId, String scheduleId) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("scheduleId", scheduleId);

        Request req = new Request();
        req.setAction("DELETE_SLOT");
        req.setEntity("schedule");
        req.setUserId(doctorId);
        req.setPayload(toJson(payload));
        return toActionResult(sendRequest(req));
    }

    public ActionResult updateSlotTime(String doctorId, String scheduleId,
                                    String startTime, String endTime) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("scheduleId", scheduleId);
        payload.put("startTime", startTime);
        payload.put("endTime", endTime);

        Request req = new Request();
        req.setAction("UPDATE_SLOT");
        req.setEntity("schedule");
        req.setUserId(doctorId);
        req.setPayload(toJson(payload));
        return toActionResult(sendRequest(req));
    }


    // Business rule: a booked slot cannot be deleted from the main schedule view
    public ActionResult validateSlotDeletion(String status) {
        if ("Booked".equals(status) || "Rescheduled".equals(status)) {
            return new ActionResult(false, "Cannot cancel from main schedule for booked/rescheduled appointments. Use Booked Schedules.");
        }
        return new ActionResult(true, "OK");
    }

    // Business rule: cannot update time of a booked slot
    public ActionResult validateSlotTimeUpdate(String status) {
        if ("Booked".equals(status) || "Rescheduled".equals(status)) {
            return new ActionResult(false, "Cannot update booked/rescheduled appointments from main schedule. Use Booked Schedules.");
        }
        return new ActionResult(true, "OK");
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

    private String text(JsonNode node, String field) {
        JsonNode val = node.get(field);
        return val != null && !val.isNull() ? val.asText() : "";
    }
}