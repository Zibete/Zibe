package com.zibete.proyecto1.model;

public class UserGroup implements Comparable <UserGroup>, java.io.Serializable{

    String user_id;
    String user_name;
    int type;

    public UserGroup() {
        //Empty
    }

    public UserGroup(String user_id, String user_name, int type) {
        this.user_id = user_id;
        this.user_name = user_name;
        this.type = type;
    }


    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
    public String getUser_id() {
        return user_id;
    }
    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }
    public String getUser_name() {
        return user_name;
    }
    public void setUser_name(String user_name) {
        this.user_name = user_name;
    }


    @Override
    public int compareTo(UserGroup o) {

        return o.getUser_name().compareToIgnoreCase(getUser_name());

    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserGroup userGroup = (UserGroup) o;
        return  user_id.equals(userGroup.user_id) &&
                user_name.equals(userGroup.user_name);
    }


}