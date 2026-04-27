package com.clinicaapp.service;

import com.stripe.model.PaymentIntent;
import com.stripe.exception.StripeException;

public interface IStripeService {
    // Crea una intención de pago
    PaymentIntent createPaymentIntent(Long amount, String currency, String description) throws StripeException;

    // Confirma un pago
    PaymentIntent confirmPaymentIntent(String paymentIntentId) throws StripeException;

    // Maneja eventos de webhook de Stripe
    void handleStripeWebhook(String payload, String sigHeader);
}