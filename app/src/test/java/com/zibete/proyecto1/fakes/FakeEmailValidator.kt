package com.zibete.proyecto1.fakes

import com.zibete.proyecto1.core.validation.EmailValidator

class FakeEmailValidator(
    var result: Boolean = true
) : EmailValidator {
    override fun isValid(email: String): Boolean = result
}
