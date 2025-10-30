package com.zibete.proyecto1.utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;

// utils/FirebaseRefs.java
public class FirebaseRefs {

    public static final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    public static final FirebaseAuth auth = FirebaseAuth.getInstance();    // auth = autenticación
    public static final FirebaseDatabase db = FirebaseDatabase.getInstance(); // db = base de datos en tiempo real
    public static final DatabaseReference usersRef = db.getReference("Usuarios");    // ref a "Usuarios"
    public static final FirebaseStorage storage = FirebaseStorage.getInstance();    // storage = almacenamiento de archivos
    public static final DatabaseReference ref_datos = db.getReference("Usuarios").child("Datos");
    public static final DatabaseReference ref_cuentas = db.getReference("Usuarios").child("Cuentas");
    public static final DatabaseReference ref_chat = db.getReference("Chats").child("Chats");
    public static final DatabaseReference ref_chat_unknown = db.getReference("Chats").child("Unknown");
    public static final DatabaseReference ref_zibe = db.getReference("Zibe");
    public static final DatabaseReference ref_groups = db.getReference("Groups").child("Data");
    public static final DatabaseReference ref_group_chat = db.getReference("Groups").child("Chat");

    public static final DatabaseReference ref_group_users = db.getReference("Groups").child("Users");
    public static final DatabaseReference ref_chat_path = db.getReference("Chats");
}
