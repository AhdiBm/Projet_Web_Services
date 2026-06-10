package controleur;

import com.fasterxml.jackson.databind.ObjectMapper;
import dao.MessageDAO;
import dao.MessageDAOJDBC;
import dto.Message;
import dto.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet("/api/messages/*")
public class MessageServlet extends HttpServlet {

    private MessageDAO messageDAO;
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        // Initialisation du DAO message et de Jackson pour JSON
        this.messageDAO = new MessageDAOJDBC();
        this.objectMapper = new ObjectMapper();
    }

    // Récupère la liste des messages d'un canal, en vérifiant les droits d'accès pour les canaux privés.
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        // Vérifier que l'utilisateur est connecté
        User currentUser = getAuthenticatedUser(request);
        if (currentUser == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"status\":\"error\",\"code\":401,\"message\":\"Accès refusé : Authentification Basic REST requise.\"}");
            return;
        }
        
        String channelIdParam = request.getParameter("channelId");

        // Vérifier que le canal est précisé
        if (channelIdParam == null || channelIdParam.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"status\":\"error\",\"code\":400,\"message\":\"Le paramètre 'channelId' est obligatoire.\"}");
            return;
        }

        try {
            int channelId = Integer.parseInt(channelIdParam);
            
            dao.ChannelDAO channelDAO = new dao.ChannelDAOJDBC();
            dto.Channel channel = channelDAO.findById(channelId);
            
            if (channel == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print("{\"status\":\"error\",\"code\":404,\"message\":\"Le canal spécifié n'existe pas.\"}");
                return;
            }

            if ("private".equalsIgnoreCase(channel.getType())) {
                boolean isAuthorized = channelDAO.isUserMember(channelId, currentUser.getId());
                
                if (!isAuthorized) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    out.print("{\"status\":\"error\",\"code\":403,\"message\":\"Accès interdit : vous n'êtes pas membre de ce canal privé.\"}");
                    return;
                }
            }

            List<Message> messages = messageDAO.findByChannelId(channelId);
            out.print(objectMapper.writeValueAsString(messages));
            response.setStatus(HttpServletResponse.SC_OK);
            
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"status\":\"error\",\"code\":400,\"message\":\"Le paramètre 'channelId' doit être un entier.\"}");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"status\":\"error\",\"code\":500,\"message\":\"Erreur interne : " + e.getMessage() + "\"}");
        }
        out.flush();
    }

    // Permet à un utilisateur connecté de publier un message dans un canal, en vérifiant les droits d'accès pour les canaux privés.
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        // Vérifier que l'utilisateur est connecté avant de publier un message
        User currentUser = getAuthenticatedUser(request);
        if (currentUser == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"status\":\"error\",\"code\":401,\"message\":\"Accès refusé : vous devez être connecté pour publier un message.\"}");
            return;
        }

        try {
            StringBuilder sb = new StringBuilder();
            String line;
            try (BufferedReader reader = request.getReader()) {
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }

            Message newMessage = objectMapper.readValue(sb.toString(), Message.class);
            
            
            dao.ChannelDAO channelDAO = new dao.ChannelDAOJDBC();
            dto.Channel channel = channelDAO.findById(newMessage.getChannelId());
            
            if (channel == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print("{\"status\":\"error\",\"code\":404,\"message\":\"Le canal spécifié n'existe pas.\"}");
                return;
            }

            if ("private".equalsIgnoreCase(channel.getType())) {
                if (!channelDAO.isUserMember(channel.getId(), currentUser.getId())) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403
                    out.print("{\"status\":\"error\",\"code\":403,\"message\":\"Accès interdit : vous ne pouvez pas publier dans un canal privé dont vous n'êtes pas membre.\"}");
                    return;
                }
            }

            newMessage.setUserId(currentUser.getId()); 

            if (newMessage.getContent() == null || newMessage.getContent().trim().isEmpty() || newMessage.getChannelId() <= 0) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"status\":\"error\",\"code\":400,\"message\":\"Champs requis manquants ou invalides (content, channelId).\"}");
                return;
            }

            if (messageDAO.create(newMessage)) {
                response.setStatus(HttpServletResponse.SC_CREATED);
                out.print(objectMapper.writeValueAsString(newMessage));
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print("{\"status\":\"error\",\"code\":500,\"message\":\"Erreur lors de la sauvegarde du message.\"}");
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"status\":\"error\",\"code\":400,\"message\":\"JSON invalide.\"}");
        }
        out.flush();
    }

    // Permet à un utilisateur de modifier le contenu d'un message qu'il a publié, ou à un administrateur de modifier n'importe quel message.
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        // Extraction de l'ID du message depuis le chemin de l'URL
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"status\":\"error\",\"code\":400,\"message\":\"L'ID du message à modifier doit être spécifié dans l'URL (ex: /api/messages/id).\"}");
            return;
        }

        try {
            int messageId = Integer.parseInt(pathInfo.substring(1));
            
            StringBuilder sb = new StringBuilder();
            String line;
            try (BufferedReader reader = request.getReader()) {
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }

            Message partialMessage = objectMapper.readValue(sb.toString(), Message.class);
            
            Message existingMessage = messageDAO.findById(messageId);
            if (existingMessage == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print("{\"status\":\"error\",\"code\":404,\"message\":\"Le message avec l'ID " + messageId + " n'existe pas.\"}");
                return;
            }
            
            User currentUser = getAuthenticatedUser(request);
            if (currentUser == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print("{\"status\":\"error\",\"code\":401,\"message\":\"Authentification requise.\"}");
                return;
            }

            if (existingMessage.getUserId() != currentUser.getId() && !"admin".equalsIgnoreCase(currentUser.getRole())) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.print("{\"status\":\"error\",\"code\":403,\"message\":\"Accès interdit : vous devez être l'auteur du message ou administrateur pour effectuer cette action.\"}");
                return;
            }

            existingMessage.setContent(partialMessage.getContent());

            if (messageDAO.update(existingMessage)) {
                response.setStatus(HttpServletResponse.SC_OK);
                out.print(objectMapper.writeValueAsString(existingMessage));
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print("{\"status\":\"error\",\"code\":500,\"message\":\"Impossible de mettre à jour le message.\"}");
            }

        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"status\":\"error\",\"code\":400,\"message\":\"L'ID du message doit être un entier valide.\"}");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"status\":\"error\",\"code\":400,\"message\":\"Format JSON incorrect.\"}");
        }
        out.flush();
    }

    // Permet à un utilisateur de supprimer un message qu'il a publié, ou à un administrateur de supprimer n'importe quel message.
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        // Extraction de l'ID du message à supprimer depuis l'URL
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"status\":\"error\",\"code\":400,\"message\":\"L'ID du message à supprimer doit être spécifié dans l'URL.\"}");
            return;
        }

        try {
            int messageId = Integer.parseInt(pathInfo.substring(1));
            
            Message existingMessage = messageDAO.findById(messageId);
            if (existingMessage == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print("{\"status\":\"error\",\"code\":404,\"message\":\"Le message avec l'ID " + messageId + " n'existe pas.\"}");
                return;
            }

            User currentUser = getAuthenticatedUser(request);
            if (currentUser == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print("{\"status\":\"error\",\"code\":401,\"message\":\"Authentification requise.\"}");
                return;
            }

            if (existingMessage.getUserId() != currentUser.getId() && !"admin".equalsIgnoreCase(currentUser.getRole())) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403
                out.print("{\"status\":\"error\",\"code\":403,\"message\":\"Accès interdit : vous devez être l'auteur du message ou administrateur pour effectuer cette action.\"}");
                return;
            }

            if (messageDAO.delete(messageId)) {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print("{\"status\":\"error\",\"code\":500,\"message\":\"Impossible de supprimer le message.\"}");
            }

        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"status\":\"error\",\"code\":400,\"message\":\"L'ID du message doit être un entier valide.\"}");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"status\":\"error\",\"code\":500,\"message\":\"Erreur système lors du traitement.\"}");
        }
        out.flush();
    }

    // Méthode utilitaire pour extraire l'utilisateur authentifié à partir de l'en-tête HTTP Authorization
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
