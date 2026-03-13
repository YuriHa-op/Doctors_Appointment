package model;

// Holds registration form input and the server result.
// Form fields delegate to a UserModel to avoid duplicating UserModel fields.
public class RegisterModel {

    // UserModel carries (username, password, role, name, contact, specialty)
    private final UserModel formData = new UserModel();

    // Result fields
    private boolean success;
    private String message;

    public RegisterModel() {
        this.success = false;
    }

    // Form field accessors — delegate to UserModel

    public String getUsername() { return formData.getUsername(); }
    public void setUsername(String username) { formData.setUsername(username); }

    public String getPassword() { return formData.getPassword(); }
    public void setPassword(String password) { formData.setPassword(password); }

    public String getRole() { return formData.getRole(); }
    public void setRole(String role) { formData.setRole(role); }

    public String getName() { return formData.getName(); }
    public void setName(String name) { formData.setName(name); }

    public String getContact() { return formData.getContact(); }
    public void setContact(String contact) { formData.setContact(contact); }

    public String getSpecialty() { return formData.getSpecialty(); }
    public void setSpecialty(String specialty) { formData.setSpecialty(specialty); }

    // Result field

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
