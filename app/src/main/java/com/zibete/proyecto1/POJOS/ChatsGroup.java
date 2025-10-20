package com.zibete.proyecto1.POJOS;

import java.util.Objects;

public class ChatsGroup implements java.io.Serializable{

    String mensaje;
    String date;
    String name;
    String ID;
    int type_msg;
    int type_user;


    public ChatsGroup() {
        //Empty
    }

    public ChatsGroup(String mensaje, String date, String name, String ID, int type_msg, int type_user) {
        this.mensaje = mensaje;
        this.date = date;
        this.name = name;
        this.ID = ID;
        this.type_msg = type_msg;
        this.type_user = type_user;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public int getType_msg() {
        return type_msg;
    }

    public void setType_msg(int type_msg) {
        this.type_msg = type_msg;
    }

    public int getType_user() {
        return type_user;
    }

    public void setType_user(int type_user) {
        this.type_user = type_user;
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatsGroup chats = (ChatsGroup) o;
        return  date.equals(chats.date) &&
                name.equals(chats.name) &&
                mensaje.equals(chats.mensaje);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, mensaje, name);
    }



}