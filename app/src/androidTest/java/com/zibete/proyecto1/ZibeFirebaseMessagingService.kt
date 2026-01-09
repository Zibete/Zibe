package com.zibete.proyecto1

import com.google.firebase.messaging.FirebaseMessagingService

/**
 * Esta clase "sombra" reemplaza a la real durante los tests.
 * Al NO tener @AndroidEntryPoint, Hilt no intentará inyectar nada
 * y el crash de "Component was not created" desaparecerá.
 */
class ZibeFirebaseMessagingService : FirebaseMessagingService() {
    // No hace nada
}