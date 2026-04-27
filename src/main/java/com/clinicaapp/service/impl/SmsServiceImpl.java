package com.clinicaapp.service.impl;

import com.clinicaapp.service.ISmsService;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// --- IMPORT CORREGIDO ---
import jakarta.annotation.PostConstruct;
// ------------------------

@Service
public class SmsServiceImpl implements ISmsService {

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.phone-number}")
    private String twilioPhoneNumber;

    @PostConstruct
    public void init() {
        // Este método se ejecuta automáticamente después de que se construye el bean
        // y se inyectan las dependencias. Es ideal para inicializar APIs como Twilio.
        Twilio.init(accountSid, authToken);
    }

    @Override
    public void sendSms(String toPhoneNumber, String messageBody) {
        try {
            Message message = Message.creator(
                    new PhoneNumber(toPhoneNumber),       // A
                    new PhoneNumber(twilioPhoneNumber),   // De
                    messageBody)
                    .create();
            System.out.println("SMS enviado con SID: " + message.getSid());
        } catch (Exception e) {
            throw new RuntimeException("Error al enviar SMS: " + e.getMessage(), e);
        }
    }
}