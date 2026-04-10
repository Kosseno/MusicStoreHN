package uth.pmo1.musicstorehn;

/**
 * Modelo de grupo musical.
 *
 * Nuevos campos respecto a tu versión:
 *  - fotoUrl: portada del grupo (opcional).
 *  - miembrosCount: cache local para mostrar "12 miembros" sin otra query.
 */
public class Grupo {

    private String id;
    private String nombre;
    private String descripcion;
    private String creadorId;
    private String creadorNombre;
    private long creadoEn;
    private boolean esPrivado;
    private String fotoUrl;
    private int miembrosCount;

    public Grupo() { }

    public Grupo(String id, String nombre, String descripcion,
                 String creadorId, String creadorNombre,
                 long creadoEn, boolean esPrivado,
                 String fotoUrl, int miembrosCount) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.creadorId = creadorId;
        this.creadorNombre = creadorNombre;
        this.creadoEn = creadoEn;
        this.esPrivado = esPrivado;
        this.fotoUrl = fotoUrl;
        this.miembrosCount = miembrosCount;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getCreadorId() { return creadorId; }
    public void setCreadorId(String creadorId) { this.creadorId = creadorId; }
    public String getCreadorNombre() { return creadorNombre; }
    public void setCreadorNombre(String creadorNombre) { this.creadorNombre = creadorNombre; }
    public long getCreadoEn() { return creadoEn; }
    public void setCreadoEn(long creadoEn) { this.creadoEn = creadoEn; }
    public boolean isEsPrivado() { return esPrivado; }
    public void setEsPrivado(boolean esPrivado) { this.esPrivado = esPrivado; }
    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }
    public int getMiembrosCount() { return miembrosCount; }
    public void setMiembrosCount(int miembrosCount) { this.miembrosCount = miembrosCount; }
}
