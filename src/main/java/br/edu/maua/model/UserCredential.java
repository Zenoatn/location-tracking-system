package br.edu.maua.model;

public class UserCredential {

    private final long id;
    private final String name;
    private final String email;
    private final String passwordHash;
    private final String role;

    public UserCredential(long id, String name, String email, String passwordHash, String role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public String getPasswordHash() { return passwordHash; }
    public String getRole() { return role; }
}
