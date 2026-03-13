package model;


//  Model for login form state and result
//  Wraps a UserModel for authenticated user data instead of duplicating fields

public class LoginModel {
    // Form fields
    private String username;
    private String password;

    // Result fields
    private boolean authenticated;
    private UserModel user;         // populated on successful login
    private String errorMessage;

    public LoginModel() {
        this.authenticated = false;
    }

    // Form fields
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    // Result fields
    public boolean isAuthenticated() { return authenticated; }
    public void setAuthenticated(boolean authenticated) { this.authenticated = authenticated; }

    public UserModel getUser() { return user; }
    public void setUser(UserModel user) { this.user = user; }

    // Convenience: get the authenticated user's fields
    public String getUserId() { return user != null ? user.getId() : null; }

    public String getRole() { return user != null ? user.getRole() : null; }

    public String getName() { return user != null ? user.getName() : null; }

    public String getContact() { return user != null ? user.getContact() : null; }

    public String getSpecialty() { return user != null ? user.getSpecialty() : null; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
