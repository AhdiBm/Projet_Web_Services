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
import jakarta.servlet.http.HttpSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet("/api/messages/*") // Note le /* pour pouvoir intercepter les IDs dans l'URL du type /api/messages/5
public class MessageServlet extends HttpServlet {

    private MessageDAO messageDAO;
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        this.messageDAO = new MessageDAOJDBC();
        this.objectMapper = new ObjectMapper();
    }


    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        // 1. Vérification de l'authentification
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("currentUser") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"status\":\"error\",\"code\":401,\"message\":\"Accès refusé : vous devez être connecté.\"}");
            return;
        }
        
        User currentUser = (User) session.getAttribute("currentUser");
        String channelIdParam = request.getParameter("channelId");

        if (channelIdParam == null || channelIdParam.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"status\":\"error\",\"code\":400,\"message\":\"Le paramètre 'channelId' est obligatoire.\"}");
            return;
        }

        try {
            int channelId = Integer.parseInt(channelIdParam);
            
            // Instanciation du ChannelDAO pour vérifier les propriétés du canal
            dao.ChannelDAO channelDAO = new dao.ChannelDAOJDBC();
            dto.Channel channel = channelDAO.findById(channelId);
            
            if (channel == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print("{\"status\":\"error\",\"code\":404,\"message\":\"Le canal spécifié n'existe pas.\"}");
                return;
            }

            // Si le canal est privé, on vérifie si l'utilisateur est membre
            if ("private".equalsIgnoreCase(channel.getType())) {
                boolean isAuthorized = channelDAO.isUserMember(channelId, currentUser.getId());
                
                if (!isAuthorized) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403 Forbidden
                    out.print("{\"status\":\"error\",\"code\":403,\"message\":\"Accès interdit : vous n'êtes pas membre de ce canal privé.\"}");
                    return;
                }
            }

            // Si public OU si l'utilisateur est membre du canal privé -> On charge les messages
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


    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        // 1. Récupération de la session existante (sans la créer si elle n'existe pas)
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("currentUser") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
            out.print("{\"status\":\"error\",\"code\":401,\"message\":\"Accès refusé : vous devez être connecté pour publier un message.\"}");
            return;
        }

        // On récupère l'utilisateur actuellement authentifié
        User currentUser = (User) session.getAttribute("currentUser");

        try {
            StringBuilder sb = new StringBuilder();
            String line;
            try (BufferedReader reader = request.getReader()) {
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }

            Message newMessage = objectMapper.readValue(sb.toString(), Message.class);
            
            
            // Avant d'insérer, on vérifie les droits sur le canal ciblé
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

            // EXTRACTION SÉCURISÉE : On force l'ID de l'auteur avec celui de la session !
            newMessage.setUserId(currentUser.getId()); 

            // Validation (on n'a plus besoin de vérifier le userId dans le JSON)
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
            
            // Lecture du nouveau contenu JSON envoyé dans le corps
            StringBuilder sb = new StringBuilder();
            String line;
            try (BufferedReader reader = request.getReader()) {
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }

            Message partialMessage = objectMapper.readValue(sb.toString(), Message.class);
            
            // Récupération du message original en BDD
            Message existingMessage = messageDAO.findById(messageId);
            if (existingMessage == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND); // 404
                out.print("{\"status\":\"error\",\"code\":404,\"message\":\"Le message avec l'ID " + messageId + " n'existe pas.\"}");
                return;
            }
            
            // Vérification de l'authentification en session
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("currentUser") == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print("{\"status\":\"error\",\"code\":401,\"message\":\"Authentification requise.\"}");
                return;
            }
            User currentUser = (User) session.getAttribute("currentUser");

            // Sécurité : Seul l'auteur peut modifier/supprimer son message
            if (existingMessage.getUserId() != currentUser.getId() && !"admin".equalsIgnoreCase(currentUser.getRole())){
                response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403
                out.print("{\"status\":\"error\",\"code\":403,\"message\":\"Accès interdit : vous n'êtes pas l'auteur de ce message et vous n'êtes pas administrateur.\"}");
                return;
            }

            // Mise à jour du contenu textuel
            existingMessage.setContent(partialMessage.getContent());

            if (messageDAO.update(existingMessage)) {
                response.setStatus(HttpServletResponse.SC_OK); // 200 OK
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


    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"status\":\"error\",\"code\":400,\"message\":\"L'ID du message à supprimer doit être spécifié dans l'URL.\"}");
            return;
        }

        try {
            int messageId = Integer.parseInt(pathInfo.substring(1));
            
            // Vérification de l'existence du message
            Message existingMessage = messageDAO.findById(messageId);
            if (existingMessage == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print("{\"status\":\"error\",\"code\":404,\"message\":\"Le message avec l'ID " + messageId + " n'existe pas.\"}");
                return;
            }

            // Vérification de l'authentification en session
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("currentUser") == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print("{\"status\":\"error\",\"code\":401,\"message\":\"Authentification requise.\"}");
                return;
            }
            User currentUser = (User) session.getAttribute("currentUser");

            // Sécurité : Seul l'auteur peut modifier/supprimer son message
            if (existingMessage.getUserId() != currentUser.getId() && !"admin".equalsIgnoreCase(currentUser.getRole())) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403
                out.print("{\"status\":\"error\",\"code\":403,\"message\":\"Accès interdit : vous n'êtes pas l'auteur de ce message.\"}");
                return;
            }

            // Exécution de la suppression
            if (messageDAO.delete(messageId)) {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT); // 204 No Content (Succès sans corps de réponse)
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
}