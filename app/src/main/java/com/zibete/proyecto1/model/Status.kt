package com.zibete.proyecto1.model

import com.google.firebase.database.PropertyName

class Status (
    @get:PropertyName("estado") @set:PropertyName("estado")
    var state: String = "",
    @get:PropertyName("fecha") @set:PropertyName("fecha")
    var date: String = "",
    @get:PropertyName("hora") @set:PropertyName("hora")
    var time: String = ""
)