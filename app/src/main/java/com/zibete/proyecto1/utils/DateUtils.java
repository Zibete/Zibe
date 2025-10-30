package com.zibete.proyecto1.utils;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;

public class DateUtils {

    public static int calcularEdad(String birthDay) {
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDate fechaNac = LocalDate.parse(birthDay, fmt);
            LocalDate hoy = LocalDate.now();
            return Period.between(fechaNac, hoy).getYears();
        } catch (Exception e) {
            return -1; // error de formato
        }
    }
}
