package uth.pmo1.musicstorehn;

/**
 * Modelo de contenido multimedia (audio o video) dentro de grupos
 * o como subida privada del usuario.
 *
 * Se eliminó el constructor duplicado — ahora fotoUrl es parte del
 * constructor principal (puede ser null) para evitar confusión.
 */
public class Multimedia {

    public static final String TIPO_AUDIO = "audio";
    public static final String TIPO_VIDEO = "video";
    public static final String VIS_PRIVADO = "privado";
    public static final String VIS_GRUPO = "grupo";

    private String id;
    private String nombre;
    private String url;
    private String tipo;      // audio | video
    private String userId;
    private String userName;
    private String visibilidad; // privado | grupo
    private String grupoId;   // null si es privado
    private String fotoUrl;   // miniatura/carátula (puede ser null)
    private long timestamp;

    public Multimedia() { }

    public Multimedia(String id, String nombre, String url, String tipo,
                      String userId, String userName,
                      String visibilidad, String grupoId,
                      String fotoUrl, long timestamp) {
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

    public boolean esVideo() { return TIPO_VIDEO.equals(tipo); }
    public boolean esAudio() { return TIPO_AUDIO.equals(tipo); }

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
