package dao;

import dto.Message;
import java.util.List;

public interface MessageDAO {
    //Récupérer la liste des messages d'un canal
    List<Message> findByChannelId(int channelId);
    
    //Créer un message dans un canal
    boolean create(Message message);
    
    //Mettre à jour un message
    boolean update(Message message);
    
    //Supprimer un message
    boolean delete(int messageId);
    
    // Vérifier les droits d'auteur
    Message findById(int messageId);
}