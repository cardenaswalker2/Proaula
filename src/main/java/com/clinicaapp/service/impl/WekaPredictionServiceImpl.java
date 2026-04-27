package com.clinicaapp.service.impl;

import com.clinicaapp.model.Mascota;
import com.clinicaapp.service.WekaPredictionService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.SerializationHelper;

import weka.core.converters.ArffSaver;

import java.io.File;

@Service
public class WekaPredictionServiceImpl implements WekaPredictionService {

    private static final String MODEL_PATH = "riesgo_articular.model";
    private J48 classifier;
    private Instances structure;

    @Autowired
    private WekaDataGenerator dataGenerator;

    @PostConstruct
    public void init() {
        try {
            File modelFile = new File(MODEL_PATH);
            if (modelFile.exists()) {
                System.out.println("🤖 Weka: Modelo encontrado. Cargando desde " + MODEL_PATH);
                Object[] data = (Object[]) SerializationHelper.readAll(MODEL_PATH);
                classifier = (J48) data[0];
                structure = (Instances) data[1];
            } else {
                System.out.println("🤖 Weka: Modelo no encontrado. Generando datos aleatorios (1000 instancias)...");
                Instances dataset = dataGenerator.generarDatosEntrenamiento(1000);
                
                // GUARDAR EL DATASET EN FORMATO ARFF PARA ABRIRLO EN WEKA GUI
                System.out.println("🤖 Weka: Guardando dataset en archivo ARFF para visualización...");
                ArffSaver saver = new ArffSaver();
                saver.setInstances(dataset);
                saver.setFile(new File("dataset_riesgo_articular.arff"));
                saver.writeBatch();
                
                System.out.println("🤖 Weka: Entrenando modelo J48 (Árbol de decisión)...");
                classifier = new J48();
                classifier.buildClassifier(dataset);
                
                System.out.println("🤖 Weka: Guardando modelo en disco...");
                structure = new Instances(dataset, 0); // Estructura vacía
                SerializationHelper.writeAll(MODEL_PATH, new Object[]{classifier, structure});
                
                System.out.println("✅ Weka: Modelo entrenado y guardado correctamente.");
            }
        } catch (Exception e) {
            System.err.println("❌ Error al inicializar Weka: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public String predecirRiesgoArticular(Mascota mascota) {
        if (classifier == null || structure == null) {
            return "NO_DISPONIBLE";
        }

        try {
            DenseInstance instance = new DenseInstance(structure.numAttributes());
            instance.setDataset(structure);

            int edad = mascota.getEdad();
            String razaOriginal = mascota.getRaza() != null ? mascota.getRaza().toUpperCase() : "OTRA";
            
            // Buscar la raza en nuestros atributos permitidos
            Attribute razaAttr = structure.attribute("raza");
            String razaAUsar = "OTRA";
            if (razaAttr.indexOfValue(razaOriginal) != -1) {
                razaAUsar = razaOriginal;
            }

            String pesoAUsar = "NORMAL"; // Podría conectarse al historial en el futuro

            instance.setValue(structure.attribute("edad"), edad);
            instance.setValue(structure.attribute("raza"), razaAUsar);
            instance.setValue(structure.attribute("peso"), pesoAUsar);

            double resultIndex = classifier.classifyInstance(instance);
            return structure.classAttribute().value((int) resultIndex);

        } catch (Exception e) {
            System.err.println("❌ Error al predecir con Weka: " + e.getMessage());
            return "ERROR";
        }
    }
}
