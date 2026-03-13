package controller;

import model.LoginModel;
import model.RegisterModel;
import model.UserModel;
import net.ClinicService;
import net.Request;
import net.Response;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.rmi.RemoteException;
import java.util.LinkedHashMap;
import java.util.Map;

 //Sends requests to the server via RMI and populates the model with results
public class LoginController {

    private final ClinicService clinicService;
    private final ObjectMapper mapper = JsonMapper.builder().build();

    public LoginController(ClinicService clinicService) {
        this.clinicService = clinicService;
    }

     // log in with the credentials in the model
     // Populates the model with result data from the server
    public LoginModel login(String username, String password) {
        LoginModel model = new LoginModel();
        model.setUsername(username);
        model.setPassword(password);

        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("username", username);
        payload.put("password", password);

        Request request = new Request();
        request.setAction("LOGIN");
        request.setEntity("user");
        request.setPayload(toJson(payload));

        Response response = sendRequest(request);

        if ("success".equals(response.getStatus())) {
            model.setAuthenticated(true);
            if (response.getData() != null) {
                try {
                    JsonNode data = mapper.readTree(response.getData());
                    UserModel user = new UserModel();
                    user.setId(text(data, "id"));
                    user.setUsername(username);
                    user.setRole(text(data, "role"));
                    user.setName(text(data, "name"));
                    user.setContact(text(data, "contact"));
                    user.setSpecialty(text(data, "specialty"));
                    model.setUser(user);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            model.setAuthenticated(false);
            model.setErrorMessage(response.getMessage());
        }

        return model;
    }

     //Attempt to register a new user
    //Populates the model with the result
    public RegisterModel register(String username, String password, String role,
                                  String name, String contact, String specialty) {
        RegisterModel model = new RegisterModel();
        model.setUsername(username);
        model.setPassword(password);
        model.setRole(role);
        model.setName(name);
        model.setContact(contact);
        model.setSpecialty(specialty);

        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("username", username);
        payload.put("password", password);
        payload.put("role", role);
        payload.put("name", name);
        payload.put("contact", contact);
        if ("doctor".equals(role) && specialty != null && !specialty.isEmpty()) {
            payload.put("specialty", specialty);
        }

        Request request = new Request();
        request.setAction("REGISTER");
        request.setEntity("user");
        request.setPayload(toJson(payload));

        Response response = sendRequest(request);

        if ("success".equals(response.getStatus())) {
            model.setSuccess(true);
            model.setMessage(response.getMessage());
        } else {
            model.setSuccess(false);
            model.setMessage(response.getMessage());
        }

        return model;
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

    private String text(JsonNode node, String field) {
        JsonNode val = node.get(field);
        return val != null && !val.isNull() ? val.asText() : "";
    }
}
