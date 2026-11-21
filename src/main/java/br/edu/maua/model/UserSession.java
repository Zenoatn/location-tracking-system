package br.edu.maua.model;

import java.time.Instant;

public class UserSession {

    private final long userId;
    private final String name;
    private final String role;
    private final Instant expiresAt;

    public UserSession(long userId, String name, String role, Instant expiresAt) {
        this.userId = userId;
        this.name = name;
        this.role = role;
        this.expiresAt = expiresAt;
    }

    public long getUserId(){ 
        return userId; 
    }
    
    public String getName(){ 
        return name; 
    }

    public String getRole(){
        return role; 
    }

    public Instant getExpiresAt(){ 
        return expiresAt; 
    }
}
