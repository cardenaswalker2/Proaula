package com.clinicaapp.service.impl;

import com.clinicaapp.service.IStripeService;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// --- IMPORT CORREGIDO ---
import jakarta.annotation.PostConstruct;
// ------------------------

@Service
public class StripeServiceImpl implements IStripeService {

    @Value("${stripe.api.key.secret}")
    private String secretKey;

    @PostConstruct
    public void init() {
        // Inicializa la API de Stripe con la clave secreta una vez que el servicio es creado.
        Stripe.apiKey = secretKey;
    }

    @Override
public PaymentIntent createPaymentIntent(Long amount, String currency, String description) throws StripeException {
    PaymentIntentCreateParams params =
            PaymentIntentCreateParams.builder()
                    .setAmount(amount)
                    .setCurrency(currency)
                    .setDescription(description)
                    // --- CAMBIO CLAVE ---
                    // No especificamos 'card'. Usamos 'automatic_payment_methods'
                    .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                          .setEnabled(true)
                          .build()
                    )
                    // --------------------
                    .build();

    return PaymentIntent.create(params);
}

    @Override
    public PaymentIntent confirmPaymentIntent(String paymentIntentId) throws StripeException {
        PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
        // La confirmación real usualmente se hace desde el frontend con la clave pública
        // y el client_secret de la intención de pago. Este método es para confirmaciones del lado del servidor.
        return paymentIntent.confirm();
    }

    @Override
    public void handleStripeWebhook(String payload, String sigHeader) {
        // Lógica para manejar eventos de webhook (muy importante para producción)
        // Se necesita una clave secreta de webhook para validar la firma.
        // Aquí es donde confirmarías que un pago fue exitoso y actualizarías el estado de la cita/factura en tu BD.
        System.out.println("Webhook de Stripe recibido. Implementar validación y lógica de negocio.");
    }
}