package controleur;

import com.fasterxml.jackson.databind.ObjectMapper;
import dao.ChannelDAO;
import dao.ChannelDAOJDBC;
import dto.Channel;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedReader;

import java.util.List;
import dto.User;

@WebServlet("/api/channels")
public class ChannelServlet extends HttpServlet {
    
    private ChannelDAO channelDAO;
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        // Initialisation du DAO et de Jackson pour JSON
        this.channelDAO = new ChannelDAOJDBC();
        this.objectMapper = new ObjectMapper();
    }

    // Récupérer la liste des canaux disponibles pour l'utilisateur connecté
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try {
            User currentUser = getAuthenticatedUser(request);
            if (currentUser == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print("{\"status\":\"error\",\"code\":401,\"message\":\"Accès refusé : Authentification Basic REST requise.\"}");
                return;
            }

            List<Channel> availableChannels = channelDAO.findAvailableChannels(currentUser.getId());
            
            String jsonResponse = objectMapper.writeValueAsString(availableChannels);
            response.setStatus(HttpServletResponse.SC_OK);
            out.print(jsonResponse);
            out.flush();
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"status\":\"error\",\"code\":500,\"message\":\"Erreur : " + e.getMessage() + "\"}");
        }
    }

    // Créer un nouveau canal
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        User currentUser = getAuthenticatedUser(request);
        if (currentUser == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"status\":\"error\",\"code\":401,\"message\":\"Accès refusé : Authentification Basic REST requise.\"}");
            return;
        }

        try {
            // Lire le corps JSON de la requête
            StringBuilder sb = new StringBuilder();
            String line;
            try (BufferedReader reader = request.getReader()) {
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }

            dto.Channel newChannel = objectMapper.readValue(sb.toString(), dto.Channel.class);
            
            newChannel.setCreatorId(currentUser.getId());

            // Valider les champs obligatoires
            if (newChannel.getName() == null || newChannel.getName().trim().isEmpty() || newChannel.getType() == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"status\":\"error\",\"code\":400,\"message\":\"Champs requis manquants (name, type).\"}");
                return;
            }

            dao.ChannelDAO channelDAO = new dao.ChannelDAOJDBC();
            if (channelDAO.create(newChannel)) {
                response.setStatus(HttpServletResponse.SC_CREATED);
                out.print(objectMapper.writeValueAsString(newChannel));
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print("{\"status\":\"error\",\"code\":500,\"message\":\"Impossible de sauvegarder le canal en base de données.\"}");
            }

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"status\":\"error\",\"code\":400,\"message\":\"Format JSON invalide.\"}");
        }
        out.flush();
    }

    // Extraire l'utilisateur authentifié à partir de l'en-tête HTTP Authorization
    private User getAuthenticatedUser(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return null;
        }
        
        try {
            String token = authHeader.substring("Basic ".length()).trim();
            
            byte[] decodedBytes = java.util.Base64.getDecoder().decode(token);
            String credentials = new String(decodedBytes);
            
            String[] lm = credentials.split(":", 2);
            if (lm.length != 2) return null;
            
            String login = lm[0].trim();
            String pwd = lm[1].trim();
            
            dao.UserDAO userDAO = new dao.UserDAOJDBC();
            if (userDAO.checkCredentials(login, pwd)) {
                return userDAO.findByLogin(login);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}