package com.clinicaapp.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@Document(collection = "notificaciones")
public class Notificacion {
    @Id
    private String id;
    private String usuarioId;
    private String titulo;
    private String mensaje;
    private String link;
    private boolean leida;
    private LocalDateTime fecha;

    public Notificacion() {}

    public Notificacion(String usuarioId, String titulo, String mensaje, String link) {
        this.usuarioId = usuarioId;
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.link = link;
        this.leida = false;
        this.fecha = LocalDateTime.now();
    }
}
