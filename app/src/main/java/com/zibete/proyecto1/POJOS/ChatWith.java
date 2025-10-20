package com.zibete.proyecto1.POJOS;

import java.util.Date;

public class ChatWith implements Comparable <ChatWith>, Cloneable {

    private String wMsg;
    private String wDate;
    private Date dateDate;
    private String wEnvia;
    private String wUserID;
    private String wUserName;
    private String wUserPhoto;
    private String estado;
    private Integer noVisto;
    private Integer wVisto;

    public ChatWith() {
    }

    public ChatWith(String wMsg, String wDate, Date dateDate, String wEnvia, String wUserID, String wUserName, String wUserPhoto, String estado, Integer noVisto, Integer wVisto) {

        this.wMsg = wMsg;
        this.wDate = wDate;
        this.dateDate = dateDate;
        this.wEnvia = wEnvia;
        this.wUserID = wUserID;
        this.wUserName = wUserName;
        this.wUserPhoto = wUserPhoto;
        this.estado = estado;
        this.noVisto = noVisto;
        this.wVisto = wVisto;
    }

    public Date getDateDate() {
        return dateDate;
    }

    public void setDateDate(Date dateDate) {
        this.dateDate = dateDate;
    }

    public String getwEnvia() {
        return wEnvia;
    }

    public void setwEnvia(String wEnvia) {
        this.wEnvia = wEnvia;
    }

    public Integer getwVisto() {
        return wVisto;
    }

    public void setwVisto(Integer wVisto) {
        this.wVisto = wVisto;
    }
/*
    public String getDeleteDate() {
        return deleteDate;
    }

    public void setDeleteDate(String deleteDate) {
        this.deleteDate = deleteDate;
    }

 */

    public String getwMsg() {
        return wMsg;
    }

    public void setwMsg(String wMsg) {
        this.wMsg = wMsg;
    }

    public Integer getNoVisto() {
        return noVisto;
    }

    public void setNoVisto(Integer noVisto) {
        this.noVisto = noVisto;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getwDate() {
        return wDate;
    }

    public void setwDate(String wDate) {
        this.wDate = wDate;
    }


    public String getwUserID() {
        return wUserID;
    }

    public void setwUserID(String wUserID) {
        this.wUserID = wUserID;
    }

    public String getwUserName() {
        return wUserName;
    }

    public void setwUserName(String wUserName) {
        this.wUserName = wUserName;
    }

    public String getwUserPhoto() {
        return wUserPhoto;
    }

    public void setwUserPhoto(String wUserPhoto) {
        this.wUserPhoto = wUserPhoto;
    }


    @Override
    public int compareTo(ChatWith o) {


            return getDateDate().compareTo(o.getDateDate());

    }

    @Override
    public ChatWith clone() {

        ChatWith clone;
        try {
            clone = (ChatWith) super.clone();

        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e); //should not happen
        }

        return clone;
    }




}
