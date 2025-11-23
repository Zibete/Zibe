package com.zibete.proyecto1.ui.constants

// Textos animados de la pantalla de registro
val stringsSignUpScreen = listOf(
    "💬 Contá algo que te haga único — así otros pueden conectar con vos 😉",
    "🌟 Compartí quién sos, qué te gusta o qué te inspira",
    "✨ Un toque personal hace que tu perfil destaque entre los demás",
    "⚡ Mostrate auténtico: tus intereses dicen más que mil palabras."
)

// -----------------------------
// SignUp: mensajes de éxito / error
// -----------------------------

// Éxito
const val SIGNUP_MSG_SUCCESS = "Registro completado 🎉"
const val SIGNUP_ERR_EXCEPTION = "El usuario no pudo ser creado."

const val NO_INTERNET = "No hay conexión a Internet en este momento"

const val DELETE_ACCOUNT = "La cuenta ha sido eliminada"
const val DO_NOT_DELETE_ACCOUNT = "La cuenta se mantendrá activa"

const val ERR_ZIBE = "Ocurrió un error inesperado. Intentá nuevamente."






// Errores de FirebaseAuth
const val SIGNUP_ERR_EMAIL_IN_USE = "El correo electrónico ya está registrado."
const val SIGNUP_ERR_INVALID_PASSWORD = "Contraseña demasiado débil. Usá 6 caracteres o más."
const val SIGNUP_ERR_INVALID_EMAIL = "El formato de correo es inválido."
const val SIGNUP_ERR_GENERIC_PREFIX = "Error en el registro: código "
const val SIGNUP_ERR_UNEXPECTED_PREFIX = "Ocurrió un error inesperado: "

// Validaciones de formulario
const val ERR_EMAIL_REQUIRED = "Por favor, ingresá tu email"
const val ERR_PASSWORD_REQUIRED = "Por favor, ingresá una contraseña"
const val SIGNUP_ERR_NAME_REQUIRED = "Por favor, ingresá tu nombre"
const val SIGNUP_ERR_BIRTHDAY_REQUIRED = "Por favor, ingresá tu fecha de nacimiento"
const val ERR_UNDER_AGE = "Lo sentimos, debés ser mayor de 18 años para utilizar la App"
