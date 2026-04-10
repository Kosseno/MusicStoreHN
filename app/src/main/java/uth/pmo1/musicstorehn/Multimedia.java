package uth.pmo1.musicstorehn;

public class Multimedia {
    private String id;
    private String nombre;
    private String url;
    private String tipo; // "audio" o "video"
    private String userId;
    private String userName;
    private String visibilidad; // "privado" o "grupo"
    private String grupoId; // null si es privado
    private String fotoUrl; // URL de la miniatura o carátula
    private long timestamp;

    public Multimedia() {
    }

    public Multimedia(String id, String nombre, String url, String tipo, String userId, String userName, String visibilidad, String grupoId, long timestamp) {
        this.id = id;
        this.nombre = nombre;
        this.url = url;
        this.tipo = tipo;
        this.userId = userId;
        this.userName = userName;
        this.visibilidad = visibilidad;
        this.grupoId = grupoId;
        this.timestamp = timestamp;
    }

    public Multimedia(String id, String nombre, String url, String tipo, String userId, String userName, String visibilidad, String grupoId, String fotoUrl, long timestamp) {
        this.id = id;
        this.nombre = nombre;
        this.url = url;
        this.tipo = tipo;
        this.userId = userId;
        this.userName = userName;
        this.visibilidad = visibilidad;
        this.grupoId = grupoId;
        this.fotoUrl = fotoUrl;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getVisibilidad() { return visibilidad; }
    public void setVisibilidad(String visibilidad) { this.visibilidad = visibilidad; }
    public String getGrupoId() { return grupoId; }
    public void setGrupoId(String grupoId) { this.grupoId = grupoId; }
    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
