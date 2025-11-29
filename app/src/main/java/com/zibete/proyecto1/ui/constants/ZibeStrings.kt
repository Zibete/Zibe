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

// Permisos y legal
const val PERMISSION_LOCATION_MESSAGE =
    "Para ofrecerte una mejor experiencia, ZIBE utiliza tu ubicación mientras la app está en uso. Esto nos permite mostrarte personas cercanas y mejorar tus coincidencias."

const val PERMISSION_LEGAL_DISCLAIMER =
    "Al tocar “Comenzar”, aceptás nuestras Condiciones de Servicio y la Política de Privacidad."

// =========================
//    SESIÓN – TOKEN (SPLASH)
// =========================
const val TOKEN_DIALOG_TITLE = "Un momento…"
const val TOKEN_DIALOG_MESSAGE =
    "Este dispositivo ya tiene una cuenta vinculada.\n" +
            "Continuar desvinculará la cuenta asociada a %1\$s."

// =========================
//    SESIÓN – OTRO DISPOSITIVO (MAIN)
// =========================
const val SESSION_CONFLICT_TITLE = "Atención"
const val SESSION_CONFLICT_MESSAGE =
    "Se registró un inicio de sesión en otro dispositivo.\n" +
            "¿Qué deseas hacer?"

const val SESSION_CONFLICT_KEEP_HERE = "Continuar en este dispositivo"
const val SESSION_CONFLICT_LOGOUT = "Cerrar sesión"

const val BUTTON_START =
    "Comenzar"

const val PERMISSION_RATIONALE_TITLE =
    "Permiso de ubicación"

const val PERMISSION_RATIONALE_MESSAGE =
    "ZIBE necesita acceder a tu ubicación para funcionar correctamente y mostrarte personas cercanas."

const val PERMISSION_DENIED_TITLE =
    "Permiso denegado"

const val PERMISSION_DENIED_MESSAGE =
    "Zibe necesita acceso a tu ubicación para funcionar correctamente.\n \n" +
            "Podés activar el permiso desde: Ajustes > Aplicaciones > Zibe > Permisos > Ubicación."

const val DIALOG_ACCEPT ="Aceptar"
const val DIALOG_CANCEL ="Cancelar"
const val DIALOG_EXIT ="Salir"
const val DIALOG_OK = "OK"
const val DIALOG_CONTINUE = "Continuar"


// Content description
const val LOGO_CONTENT_DESC = "Logo de ZIBE"

const val ONBOARDING_TITLE_1 = "Chatea"
const val ONBOARDING_TITLE_2 = "Descubre"
const val ONBOARDING_TITLE_3 = "Socializa"


const val ONBOARDING_DESC_1 = "Chatea con familiares y amigos en tiempo real."
const val ONBOARDING_DESC_2 = "Encuentra personas cercanas y hace nuevos contactos."
const val ONBOARDING_DESC_3 = "Crea salas o unite a salas de chat."



