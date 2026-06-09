package dao;

import dto.Message;
import java.util.List;

public interface MessageDAO {
    // Phase 2 - Exigence 1 : Récupérer la liste des messages d'un canal
    List<Message> findByChannelId(int channelId);
    
    // Phase 2 - Exigence 2 : Créer un message dans un canal
    boolean create(Message message);
    
    // Phase 2 - Exigence 3 : Mettre à jour un message
    boolean update(Message message);
    
    // Phase 2 - Exigence 4 : Supprimer un message
    boolean delete(int messageId);
    
    // Méthode utilitaire indispensable pour vérifier les droits d'auteur plus tard
    Message findById(int messageId);
}