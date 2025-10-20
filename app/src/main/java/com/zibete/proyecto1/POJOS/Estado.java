package com.zibete.proyecto1.POJOS;

public class Estado {
    String estado;
    String fecha;
    String hora;

    public Estado() {
    }

    public Estado(String estado, String fecha, String hora) {
        this.estado = estado;
        this.fecha = fecha;
        this.hora = hora;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getHora() {
        return hora;
    }

    public void setHora(String hora) {
        this.hora = hora;
    }
}
