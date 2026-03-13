package model;

import java.util.Objects;

// Represents a schedule slot / appointment in the system.
// Supported statuses: Available, Booked, Cancelled, Rescheduled.
public class ScheduleModel {

    // Valid status constants
    public static final String STATUS_AVAILABLE = "Available";
    public static final String STATUS_BOOKED    = "Booked";
    public static final String STATUS_CANCELLED = "Cancelled";
    public static final String STATUS_RESCHEDULED = "Rescheduled";

    private String id;
    private String doctorId;
    private String doctorName;
    private String patientId;
    private String patientName;
    private String date;
    private String startTime;
    private String endTime;
    private String status;

    // No-arg constructor used by parsers
    public ScheduleModel() {}

    // Partial constructor (no patient/doctor names — resolved later)
    public ScheduleModel(String id, String doctorId, String date,
                         String startTime, String endTime, String status) {
        this.id = id;
        this.doctorId = doctorId;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        setStatus(status);
    }

    // Full constructor — all fields
    public ScheduleModel(String id, String doctorId, String doctorName,
                         String patientId, String patientName,
                         String date, String startTime, String endTime, String status) {
        this.id = id;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.patientId = patientId;
        this.patientName = patientName;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        setStatus(status);
    }

    // Getters & Setters

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getStatus() { return status; }

    // Validates status before setting — prevents silent bad data
    public void setStatus(String status) {
        if (status != null
                && !STATUS_AVAILABLE.equals(status)
                && !STATUS_BOOKED.equals(status)
                && !STATUS_CANCELLED.equals(status)
                && !STATUS_RESCHEDULED.equals(status)) {
            throw new IllegalArgumentException(
                    "Status must be one of: Available, Booked, Cancelled, Rescheduled. Got: " + status);
        }
        this.status = status;
    }

    // Convenience helpers
    public boolean isAvailable() { return STATUS_AVAILABLE.equals(status); }
    public boolean isBooked()    { return STATUS_BOOKED.equals(status); }

    // Two ScheduleModels are equal if they share the same id
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScheduleModel)) return false;
        ScheduleModel other = (ScheduleModel) o;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "ScheduleModel{id='" + id + "', doctorId='" + doctorId +
               "', date='" + date + "', time='" + startTime + "-" + endTime +
               "', status='" + status + "'}";
    }
}
