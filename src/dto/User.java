package dto;

public class User {
    private int id;
    private String login;
    private String pwd;
    private String role;

    public User() {}

    public User(int id, String login, String pwd, String role) {
        this.id = id;
        this.login = login;
        this.pwd = pwd;
        this.role = role;
    }

    // Récupère l'ID de l'utilisateur.
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    // Récupère le login de l'utilisateur.
    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    // Récupère le mot de passe de l'utilisateur.
    public String getPwd() { return pwd; }
    public void setPwd(String pwd) { this.pwd = pwd; }

    // Récupère le rôle de l'utilisateur (ex: "user", "admin").
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}