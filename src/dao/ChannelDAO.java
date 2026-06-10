package dao;

import dto.Channel;
import java.util.List;

public interface ChannelDAO {
    // Récupérer un canal par son identifiant unique
    Channel findById(int channelId);
    
    // Récupérer tous les canaux existants
    List<Channel> findAll();
    
    // Récupérer uniquement les canaux publics
    List<Channel> findPublicChannels();

    // Récupérer les canaux auxquels un utilisateur spécifique est abonné
    List<Channel> findAvailableChannels(int userId);
    
    // Vérifier si un utilisateur est membre d'un canal spécifique
    boolean isUserMember(int channelId, int userId);
    
    // Ajouter un membre dans un canal
    boolean addMember(int channelId, int userId, boolean isRoomAdmin);

    // Créer un nouveau canal
    boolean create(dto.Channel channel);
}