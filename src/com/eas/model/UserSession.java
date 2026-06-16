<<<<<<< HEAD
package com.eas.model;

public class UserSession {

    private final int    id;
    private final String username;
    private final String fullName;
    private final String role;
    private final String email;

    public UserSession(int id, String username, String fullName, String role, String email) {
        this.id       = id;
        this.username = username;
        this.fullName = fullName;
        this.role     = role;
        this.email    = email;
    }

    public int    getId()       { return id;       }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getRole()     { return role;     }
    public String getEmail()    { return email;    }

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
=======
package com.eas.model;

public class UserSession {

    private final int    id;
    private final String username;
    private final String fullName;
    private final String role;
    private final String email;

    public UserSession(int id, String username, String fullName, String role, String email) {
        this.id       = id;
        this.username = username;
        this.fullName = fullName;
        this.role     = role;
        this.email    = email;
    }

    public int    getId()       { return id;       }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getRole()     { return role;     }
    public String getEmail()    { return email;    }

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
>>>>>>> 7ae65b2248b3a8392339438984f19f3471591009
}