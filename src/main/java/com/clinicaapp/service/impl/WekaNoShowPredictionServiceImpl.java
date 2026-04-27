package com.clinicaapp.service.impl;

import com.clinicaapp.model.Cita;
import com.clinicaapp.service.WekaNoShowPredictionService;
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
import java.time.LocalDateTime;

@Service
public class WekaNoShowPredictionServiceImpl implements WekaNoShowPredictionService {

    private static final String MODEL_PATH = "no_show_citas.model";
    private J48 classifier;
    private Instances structure;

    @Autowired
    private WekaNoShowDataGenerator dataGenerator;

    @PostConstruct
    public void init() {
        try {
            File modelFile = new File(MODEL_PATH);
            if (modelFile.exists()) {
                System.out.println("🤖 Weka No-Show: Modelo encontrado. Cargando...");
                Object[] data = (Object[]) SerializationHelper.readAll(MODEL_PATH);
                classifier = (J48) data[0];
                structure = (Instances) data[1];
            } else {
                System.out.println("🤖 Weka No-Show: Generando dataset (1000 instancias)...");
                Instances dataset = dataGenerator.generarDatosEntrenamiento(1000);
                
                System.out.println("🤖 Weka No-Show: Guardando dataset_no_show.arff...");
                ArffSaver saver = new ArffSaver();
                saver.setInstances(dataset);
                saver.setFile(new File("dataset_no_show.arff"));
                saver.writeBatch();
                
                System.out.println("🤖 Weka No-Show: Entrenando árbol de decisión J48...");
                classifier = new J48();
                classifier.buildClassifier(dataset);
                
                structure = new Instances(dataset, 0); 
                SerializationHelper.writeAll(MODEL_PATH, new Object[]{classifier, structure});
                System.out.println("✅ Weka No-Show: Modelo entrenado y guardado.");
            }
        } catch (Exception e) {
            System.err.println("❌ Error Weka No-Show: " + e.getMessage());
        }
    }

    @Override
    public Double predecirProbabilidadAusencia(Cita cita) {
        if (classifier == null || structure == null) return 0.0;

        try {
            DenseInstance instance = new DenseInstance(structure.numAttributes());
            instance.setDataset(structure);

            // Extraer hora y día
            LocalDateTime fechaHora = cita.getFechaHora();
            int hora = 12;
            String dia = "LUNES";
            
            if (fechaHora != null) {
                hora = fechaHora.getHour();
                
                // Mapear DayOfWeek de Java al de Weka
                switch (fechaHora.getDayOfWeek()) {
                    case MONDAY: dia = "LUNES"; break;
                    case TUESDAY: dia = "MARTES"; break;
                    case WEDNESDAY: dia = "MIERCOLES"; break;
                    case THURSDAY: dia = "JUEVES"; break;
                    case FRIDAY: dia = "VIERNES"; break;
                    case SATURDAY: dia = "SABADO"; break;
                    case SUNDAY: dia = "DOMINGO"; break;
                }
            }
            
            String estadoPago = cita.getEstadoPago() != null ? cita.getEstadoPago() : "PENDIENTE";

            // Setear valores
            instance.setValue(structure.attribute("hora_cita"), hora);
            
            Attribute diaAttr = structure.attribute("dia_semana");
            if(diaAttr.indexOfValue(dia) != -1) instance.setValue(diaAttr, dia);
            
            Attribute pagoAttr = structure.attribute("estado_pago");
            if(pagoAttr.indexOfValue(estadoPago) != -1) instance.setValue(pagoAttr, estadoPago);

            // Pedir a Weka la distribución de probabilidades
            // index 0 puede ser "SI" y 1 "NO". Averiguamos cuál es "SI"
            double[] fDistribution = classifier.distributionForInstance(instance);
            int indexSi = structure.classAttribute().indexOfValue("SI");
            
            if (indexSi != -1 && fDistribution.length > indexSi) {
                return fDistribution[indexSi] * 100.0; // Devolver en %
            }

            return 0.0;

        } catch (Exception e) {
            return 0.0;
        }
    }
}
