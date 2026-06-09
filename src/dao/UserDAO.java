package dao;

import dto.User;
import java.util.List;

public interface UserDAO {
    User findById(int id);
    User findByLogin(String login);
    List<User> findAll();
    boolean checkCredentials(String login, String password);
}