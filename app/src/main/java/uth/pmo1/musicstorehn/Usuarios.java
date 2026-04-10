package uth.pmo1.musicstorehn;

public class Usuarios {

    private String uid;
    private String correo;
    private String usuario;
    private String fotoUrl;
    private String descripcion;
    private String carrera;
    private String fcmToken;

    public Usuarios() { }

    public Usuarios(String uid, String correo, String usuario,
                    String fotoUrl, String descripcion, String carrera) {
        this.uid = uid;
        this.correo = correo;
        this.usuario = usuario;
        this.fotoUrl = fotoUrl;
        this.descripcion = descripcion;
        this.carrera = carrera;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getCarrera() { return carrera; }
    public void setCarrera(String carrera) { this.carrera = carrera; }

    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
}
