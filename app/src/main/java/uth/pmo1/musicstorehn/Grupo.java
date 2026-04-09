package uth.pmo1.musicstorehn;

public class Grupo {
    private String id;
    private String nombre;
    private String descripcion;
    private String creadorId;
    private String creadorNombre;
    // ✅ MEJORA: Añadido timestamp para ordenar grupos por fecha de creación
    private long creadoEn;

    public Grupo() {
    }

    public Grupo(String id, String nombre, String descripcion, String creadorId, String creadorNombre, long creadoEn) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.creadorId = creadorId;
        this.creadorNombre = creadorNombre;
        this.creadoEn = creadoEn;
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
}