package com.zibete.proyecto1.POJOS;

import java.util.Objects;

public class Users implements Comparable <Users>, java.io.Serializable{

    private String ID;
    private String Nombre;
    private String BirthDay;
    private String Date;
    private Integer Age;
    private String Mail;
    private String Foto;
    private boolean Estado;
    private String Token;
    private double Distance;
    private String Descripcion;
    private double Latitud;
    private double Longitud;

    public Users() {
    }



    public Users(String ID, String nombre, String birthDay, String date, Integer age, String mail, String foto, boolean estado, String token, double distance, String descripcion, double latitud, double longitud) {
        this.ID = ID;
        Nombre = nombre;
        BirthDay = birthDay;
        Date = date;
        Age = age;
        Mail = mail;
        Foto = foto;
        Estado = estado;
        Token = token;
        Distance = distance;
        Descripcion = descripcion;
        Latitud = latitud;
        Longitud = longitud;
    }

    public String getDate() {
        return Date;
    }

    public void setDate(String date) {
        Date = date;
    }

    public Integer getAge() {
        return Age;
    }

    public void setAge(Integer age) {
        Age = age;
    }

    public double getDistance() {
        return Distance;
    }

    public void setDistance(double distance) {
        Distance = distance;
    }

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public String getNombre() {
        return Nombre;
    }

    public void setNombre(String nombre) {
        Nombre = nombre;
    }

    public String getBirthDay() {
        return BirthDay;
    }

    public void setBirthDay(String birthDay) {
        BirthDay = birthDay;
    }

    public String getMail() {
        return Mail;
    }

    public void setMail(String mail) {
        Mail = mail;
    }

    public String getFoto() {
        return Foto;
    }

    public void setFoto(String foto) {
        Foto = foto;
    }

    public boolean getEstado() {
        return Estado;
    }

    public void setEstado(boolean estado) {
        Estado = estado;
    }

    public String getToken() {
        return Token;
    }

    public void setToken(String token) {
        Token = token;
    }

    public String getDescripcion() {
        return Descripcion;
    }

    public void setDescripcion(String descripcion) {
        Descripcion = descripcion;
    }

    public double getLatitud() {
        return Latitud;
    }

    public void setLatitud(double latitud) {
        Latitud = latitud;
    }

    public double getLongitud() {
        return Longitud;
    }

    public void setLongitud(double longitud) {
        Longitud = longitud;
    }

    @Override
    public int compareTo(Users o) {

        if(this.Distance<o.Distance)
            return 1;
        else if(o.Distance<this.Distance)
            return -1;
        return 0;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Users user = (Users) o;
        return  Nombre.equals(user.Nombre) &&
                ID.equals(user.ID) &&
                Token.equals(user.Token) &&
                Mail.equals(user.Mail);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ID,Nombre,Mail);
    }

}