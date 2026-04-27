package com.clinicaapp.service.impl;

import com.github.javafaker.Faker;
import org.springframework.stereotype.Service;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Service
public class WekaDataGenerator {

    private final Random random = new Random();

    public Instances generarDatosEntrenamiento(int numInstancias) {
        // 1. Definir Atributos
        ArrayList<Attribute> attributes = new ArrayList<>();

        // Edad (Numérico)
        attributes.add(new Attribute("edad"));

        // Raza (Nominal) - Algunas razas representativas
        List<String> razas = Arrays.asList("PASTOR_ALEMAN", "GOLDEN_RETRIEVER", "BULLDOG", "CHIHUAHUA", "MESTIZO", "OTRA");
        attributes.add(new Attribute("raza", razas));

        // Peso (Nominal)
        List<String> pesos = Arrays.asList("SOBREPESO", "NORMAL", "BAJO_PESO");
        attributes.add(new Attribute("peso", pesos));

        // Clase: Riesgo Articular (Nominal)
        List<String> riesgos = Arrays.asList("ALTO", "MEDIO", "BAJO");
        Attribute claseRiesgo = new Attribute("riesgo_articular", riesgos);
        attributes.add(claseRiesgo);

        // 2. Crear el dataset (Instances)
        Instances dataset = new Instances("RiesgoArticularDataset", attributes, numInstancias);
        dataset.setClassIndex(dataset.numAttributes() - 1); // La última columna es la clase

        // 3. Generar Datos Aleatorios con "Lógica" para que Weka aprenda patrones
        for (int i = 0; i < numInstancias; i++) {
            DenseInstance instancia = new DenseInstance(dataset.numAttributes());
            
            int edad = random.nextInt(18) + 1; // 1 a 18 años
            String raza = razas.get(random.nextInt(razas.size()));
            String peso = pesos.get(random.nextInt(pesos.size()));
            
            instancia.setValue(attributes.get(0), edad);
            instancia.setValue(attributes.get(1), raza);
            instancia.setValue(attributes.get(2), peso);

            // "Lógica" para enseñar al modelo (Reglas que Weka debe descubrir estadísticamente)
            String riesgo = "BAJO";
            if (edad >= 8 || raza.equals("PASTOR_ALEMAN") || raza.equals("GOLDEN_RETRIEVER") || raza.equals("BULLDOG")) {
                riesgo = "ALTO";
            } else if (peso.equals("SOBREPESO") || edad >= 5) {
                riesgo = "MEDIO";
            }
            
            // Añadir ruido (5%) para que no sea un árbol de decisión perfecto y parezca un modelo del mundo real
            if (random.nextInt(100) < 5) {
                riesgo = riesgos.get(random.nextInt(riesgos.size()));
            }

            instancia.setValue(claseRiesgo, riesgo);
            
            dataset.add(instancia);
        }

        return dataset;
    }
}
