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
import jakarta.servlet.http.HttpSession;

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
            // 1. Lecture du JSON contenant login et pwd
            StringBuilder sb = new StringBuilder();
            String line;
            try (BufferedReader reader = request.getReader()) {
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }

            // Extraction sous forme de Map (clé-valeur)
            Map<String, String> credentials = objectMapper.readValue(sb.toString(), new TypeReference<Map<String, String>>(){});
            String login = credentials.get("login");
            String password = credentials.get("pwd");

            // 2. Vérification des identifiants via le DAO
            if (login == null || password == null || !userDAO.checkCredentials(login, password)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 Unauthorized
                out.print("{\"status\":\"error\",\"code\":401,\"message\":\"Identifiants incorrects.\"}");
                return;
            }

            // 3. Récupération de l'utilisateur complet pour le stocker en session
            User user = userDAO.findByLogin(login);

            // 4. Création/Récupération de la session HTTP
            HttpSession session = request.getSession(true);
            session.setAttribute("currentUser", user); // On attache l'objet user à la session

            // 5. Réponse de succès (On renvoie l'utilisateur sans son mot de passe par sécurité)
            user.setPwd(null); 
            response.setStatus(HttpServletResponse.SC_OK); // 200 OK
            out.print("{\"status\":\"success\",\"message\":\"Connexion réussie !\",\"user\":" + objectMapper.writeValueAsString(user) + "}");

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400
            out.print("{\"status\":\"error\",\"code\":400,\"message\":\"Format JSON invalide.\"}");
        }
        out.flush();
    }
}