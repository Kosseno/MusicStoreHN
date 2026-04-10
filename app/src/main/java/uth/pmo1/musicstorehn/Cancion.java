package uth.pmo1.musicstorehn;

/**
 * Modelo de canción.
 * Añadidos respecto a la versión anterior:
 *  - userName: para mostrar "subido por" sin tener que hacer otra consulta.
 *  - timestamp: para ordenar por más recientes.
 *  - Constructor vacío obligatorio para Firebase.
 */
public class Cancion {

    private String nombreCancion;
    private String urlCancion;
    private String userId;
    private String userName;
    private String fotoUrl;
    private long timestamp;

    public Cancion() {
        // requerido por Firebase
    }

    public Cancion(String nombreCancion, String urlCancion, String userId,
                   String userName, String fotoUrl, long timestamp) {
        this.nombreCancion = nombreCancion;
        this.urlCancion = urlCancion;
        this.userId = userId;
        this.userName = userName;
        this.fotoUrl = fotoUrl;
        this.timestamp = timestamp;
    }

    // Constructor de conveniencia para cuando no tenemos el userName aún o el timestamp es "ahora"
    public Cancion(String nombreCancion, String urlCancion, String userId, String fotoUrl) {
        this.nombreCancion = nombreCancion;
        this.urlCancion = urlCancion;
        this.userId = userId;
        this.fotoUrl = fotoUrl;
        this.userName = "Usuario"; // Valor por defecto
        this.timestamp = System.currentTimeMillis();
    }

    public String getNombreCancion() { return nombreCancion; }
    public void setNombreCancion(String nombreCancion) { this.nombreCancion = nombreCancion; }

    public String getUrlCancion() { return urlCancion; }
    public void setUrlCancion(String urlCancion) { this.urlCancion = urlCancion; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
