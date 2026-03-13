package model;

public class UserModel { // Represents a user in the system (doctor or patient)
    private String id;   //This is the unified model backed by users.xml
    private String username;
    private String password;
    private String role;       // doctor or patient
    private String name;
    private String contact;
    private String specialty;  // only for doctors

    public UserModel() {}

    public UserModel(String id, String username, String password, String role,
                     String name, String contact, String specialty) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.name = name;
        this.contact = contact;
        this.specialty = specialty;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }

    @Override
    public String toString() {
        return "UserModel{id='" + id + "', username='" + username +
               "', role='" + role + "', name='" + name + "'}";
    }
}
