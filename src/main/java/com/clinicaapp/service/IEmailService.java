package com.clinicaapp.service;

public interface IEmailService {
    void sendSimpleMessage(String to, String subject, String text);
    void sendMessageWithAttachment(String to, String subject, String text, String pathToAttachment);
    
    // --- NUEVO MÉTODO MÁS POTENTE ---
    void sendMessageWithAttachment(String to, String subject, String text, String attachmentName, byte[] attachmentBytes);
    
}