package dao;

import dto.Channel;
import java.util.List;

public interface ChannelDAO {
    // Récupérer un canal par son identifiant unique
    Channel findById(int channelId);
    
    // Récupérer tous les canaux existants
    List<Channel> findAll();
    
    // Récupérer uniquement les canaux publics (Utile pour la Phase 3 - Jalon 1)
    List<Channel> findPublicChannels();

    // Récupérer les canaux auxquels un utilisateur spécifique est abonné
    List<Channel> findAvailableChannels(int userId);
    
    // Vérifier si un utilisateur est membre d'un canal spécifique (Indispensable pour la Phase 4 - Jalon 2)
    boolean isUserMember(int channelId, int userId);
    
    // Ajouter un membre dans un canal
    boolean addMember(int channelId, int userId, boolean isRoomAdmin);
}