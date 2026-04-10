package uth.pmo1.musicstorehn;

public class Cancion {
    private String nombreCancion, urlCancion, userId;

    public Cancion() {
    }

    public Cancion(String nombreCancion, String urlCancion, String userId) {
        this.nombreCancion = nombreCancion;
        this.urlCancion = urlCancion;
        this.userId = userId;
    }

    public String getNombreCancion() {
        return nombreCancion;
    }

    public void setNombreCancion(String nombreCancion) {
        this.nombreCancion = nombreCancion;
    }

    public String getUrlCancion() {
        return urlCancion;
    }

    public void setUrlCancion(String urlCancion) {
        this.urlCancion = urlCancion;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
