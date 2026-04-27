package com.clinicaapp.service;

public interface ISmsService {
    // Envía un SMS a un número de teléfono
    void sendSms(String toPhoneNumber, String message);
}