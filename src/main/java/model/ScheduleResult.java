package model;

import java.util.List;

public class ScheduleResult {

    private boolean success;
    private String message;
    private List<ScheduleModel> schedules;

    public ScheduleResult(boolean success, String message, List<ScheduleModel> schedules) {
        this.success = success;
        this.message = message;
        this.schedules = schedules;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public List<ScheduleModel> getSchedules() {
        return schedules;
    }
}
