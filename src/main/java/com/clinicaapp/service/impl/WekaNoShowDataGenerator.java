package com.clinicaapp.service.impl;

import org.springframework.stereotype.Service;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Service
public class WekaNoShowDataGenerator {

    private final Random random = new Random();

    public Instances generarDatosEntrenamiento(int numInstancias) {
        ArrayList<Attribute> attributes = new ArrayList<>();

        // 1. Hora de la cita (Numérico: 8 a 19)
        attributes.add(new Attribute("hora_cita"));

        // 2. Día de la semana (Nominal)
        List<String> dias = Arrays.asList("LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO");
        attributes.add(new Attribute("dia_semana", dias));

        // 3. Estado de Pago (Nominal)
        List<String> pagos = Arrays.asList("PAGADO", "PENDIENTE");
        attributes.add(new Attribute("estado_pago", pagos));

        // 4. Clase: Faltará a la cita (No-Show) (Nominal)
        List<String> noShow = Arrays.asList("SI", "NO");
        Attribute claseNoShow = new Attribute("faltara", noShow);
        attributes.add(claseNoShow);

        Instances dataset = new Instances("NoShowDataset", attributes, numInstancias);
        dataset.setClassIndex(dataset.numAttributes() - 1);

        // Generar Datos Aleatorios con Lógica
        for (int i = 0; i < numInstancias; i++) {
            DenseInstance instancia = new DenseInstance(dataset.numAttributes());
            
            int hora = random.nextInt(12) + 8; // 8am a 19pm
            String dia = dias.get(random.nextInt(dias.size()));
            String pago = pagos.get(random.nextInt(pagos.size()));
            
            instancia.setValue(attributes.get(0), hora);
            instancia.setValue(attributes.get(1), dia);
            instancia.setValue(attributes.get(2), pago);

            // "Lógica" para enseñar a Weka
            // Regla: Citas PENDIENTES de pago un VIERNES o SABADO por la tarde (> 16h) suelen fallar
            String faltara = "NO";
            
            if (pago.equals("PENDIENTE")) {
                if ((dia.equals("VIERNES") || dia.equals("SABADO") || dia.equals("LUNES")) && hora >= 15) {
                    faltara = "SI";
                } else if (random.nextInt(100) < 40) { // 40% chance si no ha pagado
                    faltara = "SI";
                }
            } else {
                // Si pagó, es muy raro que falte
                if (random.nextInt(100) < 5) {
                    faltara = "SI";
                }
            }
            
            instancia.setValue(claseNoShow, faltara);
            dataset.add(instancia);
        }

        return dataset;
    }
}
