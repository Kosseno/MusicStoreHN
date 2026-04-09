package uth.pmo1.musicstorehn;

public class Cancion {
    private String nombreCancion, urlCancion;

    public Cancion() {
    }

    public Cancion(String nombreCancion, String urlCancion) {
        this.nombreCancion = nombreCancion;
        this.urlCancion = urlCancion;
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
}
