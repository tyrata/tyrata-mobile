package com.tyrata.tyrata.data.model;

/**
 * User Model
 */
public class User {
    // Public access required for Firebase
    public String userName;
    public String email;
    public String phone;

    public User(String userName, String email, String phone) {
        this.userName = userName;
        this.email = email;
        this.phone = phone;
    }
}
