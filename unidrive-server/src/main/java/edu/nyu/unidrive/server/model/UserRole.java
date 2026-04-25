package edu.nyu.unidrive.server.model;

public enum UserRole {
    STUDENT,
    INSTRUCTOR;

    public static boolean isValid(String role) {
        if (role == null) {
            return false;
        }
        for (UserRole value : values()) {
            if (value.name().equals(role)) {
                return true;
            }
        }
        return false;
    }
}
