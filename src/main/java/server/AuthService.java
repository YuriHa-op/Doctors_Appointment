package server;

import database.JsonDatabase;
import model.UserModel;
import net.Request;
import net.Response;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;

// authentication and registration.
public class AuthService {

    private final JsonDatabase db;
    private final ActivityLogger logger;
    private final ObjectMapper mapper = JsonMapper.builder().build();

    public AuthService(JsonDatabase db, ActivityLogger logger) {
        this.db = db;
        this.logger = logger;
    }

    // Authenticate a user and return their profile data
    public Response login(Request req) {
        Map<String, String> payload = parsePayload(req.getPayload());
        String username = payload.getOrDefault("username", "");
        String password = payload.getOrDefault("password", "");

        if (username.isEmpty() || password.isEmpty()) {
            return error(400, "Username and password are required.");
        }

        UserModel user = db.authenticate(username, password);
        if (user == null) {
            logger.log(username, "LOGIN", "Failed login attempt");
            return error(401, "Invalid username or password.");
        }

        logger.log(user.getUsername(), "LOGIN", "User logged in: " + user.getId());

        Map<String, String> data = new LinkedHashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("role", user.getRole());
        data.put("name", user.getName());
        data.put("contact", user.getContact());
        if (user.getSpecialty() != null && !user.getSpecialty().isEmpty()) {
            data.put("specialty", user.getSpecialty());
        }

        return ok("Login successful.", toJson(data));
    }

    // Register a new user account
    public Response register(Request req) {
        Map<String, String> payload = parsePayload(req.getPayload());
        String username  = payload.getOrDefault("username", "");
        String password  = payload.getOrDefault("password", "");
        String role      = payload.getOrDefault("role", "");
        String name      = payload.getOrDefault("name", "");
        String contact   = payload.getOrDefault("contact", "");
        String specialty = payload.getOrDefault("specialty", "");

        if (username.isEmpty() || password.isEmpty() || role.isEmpty() || name.isEmpty()) {
            return error(400, "Username, password, role, and name are required.");
        }
        if (!"doctor".equals(role) && !"patient".equals(role)) {
            return error(400, "Role must be 'doctor' or 'patient'.");
        }

        UserModel created = db.register(username, password, role, name, contact, specialty);
        if (created == null) {
            logger.log(username, "REGISTER", "Registration failed - username taken");
            return error(409, "Username '" + username + "' is already taken.");
        }

        logger.log(created.getUsername(), "REGISTER",
                "New " + role + " registered: " + created.getId());

        Response resp = new Response();
        resp.setStatus("success");
        resp.setCode(201);
        resp.setMessage("Registration successful! You can now log in.");
        return resp;
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

    private Map<String, String> parsePayload(String json) {
        Map<String, String> map = new LinkedHashMap<>();
        if (json == null || json.isEmpty()) return map;
        try {
            JsonNode node = mapper.readTree(json);
            node.propertyNames().forEach(field ->
                    map.put(field, node.get(field).asText()));
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
            return "{}";
        }
    }
}

