package controleur;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dao.UserDAO;
import dao.UserDAOJDBC;
import dto.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

@WebServlet("/api/login")
public class LoginServlet extends HttpServlet {

    private UserDAO userDAO;
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        // Initialisation du DAO utilisateur et de Jackson pour JSON
        this.userDAO = new UserDAOJDBC();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try {
            // Lire les identifiants de connexion envoyés en JSON
            StringBuilder sb = new StringBuilder();
            String line;
            try (BufferedReader reader = request.getReader()) {
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }

            Map<String, String> credentials = objectMapper.readValue(sb.toString(), new TypeReference<Map<String, String>>(){});
            String login = credentials.get("login");
            String password = credentials.get("pwd");

            if (login == null || password == null || !userDAO.checkCredentials(login, password)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print("{\"status\":\"error\",\"code\":401,\"message\":\"Identifiants incorrects.\"}");
                return;
            }

            User user = userDAO.findByLogin(login);
            user.setPwd(null);
            response.setStatus(HttpServletResponse.SC_OK);
            out.print("{\"status\":\"success\",\"message\":\"Connexion réussie !\",\"user\":" + objectMapper.writeValueAsString(user) + "}");

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"status\":\"error\",\"code\":400,\"message\":\"Format JSON invalide.\"}");
        }
        out.flush();
    }
}