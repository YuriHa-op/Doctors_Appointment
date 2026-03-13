package model;

import java.util.List;

// Result for controller methods that return a list of users (doctors)
public class UserListResult {

    private boolean success;
    private String message;
    private List<UserModel> users;

    public UserListResult(boolean success, String message, List<UserModel> users) {
        this.success = success;
        this.message = message;
        this.users = users;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public List<UserModel> getUsers() {
        return users;
    }
}
