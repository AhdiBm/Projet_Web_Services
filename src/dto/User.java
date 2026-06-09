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

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getPwd() { return pwd; }
    public void setPwd(String pwd) { this.pwd = pwd; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}