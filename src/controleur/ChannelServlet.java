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
import java.util.List;

@WebServlet("/api/channels")
public class ChannelServlet extends HttpServlet {
    
    private ChannelDAO channelDAO;
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        // Initialisation de notre DAO et du convertisseur JSON Jackson
        this.channelDAO = new ChannelDAOJDBC();
        this.objectMapper = new ObjectMapper();
    }

    // Phase 2 - Jalon 1 : Récupération de la liste des canaux publics
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try {
            // Récupération de la session
            jakarta.servlet.http.HttpSession session = request.getSession(false);
            List<Channel> availableChannels;

            if (session != null && session.getAttribute("currentUser") != null) {
                // Si l'utilisateur est connecté, on charge les canaux publics + ses canaux privés
                dto.User currentUser = (dto.User) session.getAttribute("currentUser");
                availableChannels = channelDAO.findAvailableChannels(currentUser.getId());
            } else {
                // Si personne n'est connecté, on ne renvoie que les canaux publics par défaut
                availableChannels = channelDAO.findPublicChannels();
            }
            
            String jsonResponse = objectMapper.writeValueAsString(availableChannels);
            response.setStatus(HttpServletResponse.SC_OK);
            out.print(jsonResponse);
            
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"status\":\"error\",\"code\":500,\"message\":\"Erreur : " + e.getMessage() + "\"}");
        }
        out.flush();
    }
}