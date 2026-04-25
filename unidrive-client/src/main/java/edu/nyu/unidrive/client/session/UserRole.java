package edu.nyu.unidrive.client.session;

public enum UserRole {
    STUDENT,
    INSTRUCTOR;

    public static UserRole fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("role is required");
        }
        return UserRole.valueOf(value);
    }
}
