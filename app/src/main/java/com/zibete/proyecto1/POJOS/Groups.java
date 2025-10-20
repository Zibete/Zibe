package com.zibete.proyecto1.POJOS;

public class Groups implements Comparable <Groups> {

    private String name;
    private String data;
    private String ID_creator;
    private Integer category;
    private Integer users;
    private String dateCreate;

    public Groups(String name, String data, String ID_creator, Integer category, Integer users, String dateCreate) {
        this.name = name;
        this.data = data;
        this.ID_creator = ID_creator;
        this.category = category;
        this.users = users;
        this.dateCreate = dateCreate;
    }

    public Groups() {
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getID_creator() {
        return ID_creator;
    }

    public void setID_creator(String ID_creator) {
        this.ID_creator = ID_creator;
    }

    public Integer getCategory() {
        return category;
    }

    public void setCategory(Integer category) {
        this.category = category;
    }

    public Integer getUsers() {
        return users;
    }

    public void setUsers(Integer users) {
        this.users = users;
    }

    public String getDateCreate() {
        return dateCreate;
    }

    public void setDateCreate(String dateCreate) {
        this.dateCreate = dateCreate;
    }

    @Override
    public int compareTo(Groups o) {
        return o.getUsers().compareTo(getUsers());
    }
}
