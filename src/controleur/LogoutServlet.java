package controleur;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/api/logout")
public class LogoutServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        // 1. Récupération de la session sans la créer si elle n'existe pas
        HttpSession session = request.getSession(false);

        if (session != null) {
            // 2. Destruction de la session côté serveur
            session.invalidate();
        }

        // 3. Réponse de succès (Même s'il n'y avait pas de session, le résultat final est le même : déconnecté)
        response.setStatus(HttpServletResponse.SC_OK); // 200 OK
        out.print("{\"status\":\"success\",\"message\":\"Déconnexion réussie. Session invalidée.\"}");
        out.flush();
    }
}