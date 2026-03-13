package server;

import net.ClinicCallback;
import net.ClinicService;
import net.Request;
import net.Response;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * RMI remote implementation of ClinicService.
 * Routes each incoming request to the appropriate service method
 * based on the action field — replaces the old socket-based ClientHandler.
 */
public class ClinicServiceImpl extends UnicastRemoteObject implements ClinicService {

    private final AuthService authService;
    private final ScheduleService scheduleService;
    private final CallbackManager callbackManager;

    public ClinicServiceImpl(AuthService authService, ScheduleService scheduleService,
                             CallbackManager callbackManager) throws RemoteException {
        super();
        this.authService = authService;
        this.scheduleService = scheduleService;
        this.callbackManager = callbackManager;
    }

    @Override
    public Response processRequest(Request request) throws RemoteException {
        String action = request.getAction() != null ? request.getAction().toUpperCase() : "";

        switch (action) {
            case "LOGIN":                 return authService.login(request);
            case "REGISTER":              return authService.register(request);
            case "CREATE_SCHEDULE":       return scheduleService.createSchedule(request);
            case "READ_SCHEDULES":        return scheduleService.readSchedules(request);
            case "DELETE_SCHEDULE":       return scheduleService.deleteSchedule(request);
            case "OVERRIDE_DAY":          return scheduleService.overrideDay(request);
            case "SEARCH":                return scheduleService.searchAppointments(request);
            case "DELETE_SLOT":           return scheduleService.deleteSlot(request);
            case "UPDATE_SLOT":           return scheduleService.updateSlot(request);
            case "GET_AVAILABLE_SLOTS":   return scheduleService.getAvailableSlots(request);
            case "BOOK_SLOT":             return scheduleService.bookSlot(request);
            case "CANCEL_BOOKING":        return scheduleService.cancelBooking(request);
            case "GET_PATIENT_SCHEDULES": return scheduleService.getPatientSchedules(request);
            case "GET_DOCTORS":           return scheduleService.getDoctors();
            default:
                Response err = new Response();
                err.setStatus("error");
                err.setCode(400);
                err.setMessage("Unknown action: " + action);
                return err;
        }
    }

    @Override
    public void registerCallback(String userId, ClinicCallback callback) throws RemoteException {
        callbackManager.register(userId, callback);
    }

    @Override
    public void unregisterCallback(String userId) throws RemoteException {
        callbackManager.unregister(userId);
    }
}
