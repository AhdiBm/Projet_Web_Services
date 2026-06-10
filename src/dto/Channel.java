package dto;

public class Channel {
    private int id;
    private String name;
    private String type;
    private int creatorId;

    public Channel() {}

    public Channel(int id, String name, String type, int creatorId) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.creatorId = creatorId;
    }

    // Getters et setters pour les propriétés de la classe Channel
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    // Récupère et modifie le nom du canal.
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    // Récupère et modifie le type du canal (public ou privé).
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    // Récupère et modifie l'ID du créateur du canal, utilisé pour les permissions.
    public int getCreatorId() { return creatorId; }
    public void setCreatorId(int creatorId) { this.creatorId = creatorId; }
}