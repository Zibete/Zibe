package com.zibete.proyecto1.POJOS;

import java.util.Objects;

public class Chats implements java.io.Serializable{

    String mensaje;
    String date;
    String envia;
    int type;
    int visto;

    public Chats() {
        //Empty
    }

    public Chats(String mensaje , String date, String envia, int type, int visto) {
        this.mensaje = mensaje;
        this.date = date;
        this.envia = envia;
        this.type = type;
        this.visto = visto;

    }

    public String getEnvia() {
        return envia;
    }

    public void setEnvia(String envia) {
        this.envia = envia;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getVisto() {
        return visto;
    }

    public void setVisto(int visto) {
        this.visto = visto;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Chats chats = (Chats) o;
        return  date.equals(chats.date) &&
                envia.equals(chats.envia) &&
                mensaje.equals(chats.mensaje);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, mensaje, envia);
    }



}