package com.clinicaapp.dto;

import java.util.List;
import java.util.ArrayList;

public class PrediccionSaludDTO {
    private String analisisGeneral;
    private List<RiesgoSaludDTO> riesgos = new ArrayList<>();
    private String recomendacionNova;

    public PrediccionSaludDTO() {}

    public PrediccionSaludDTO(String analisisGeneral, List<RiesgoSaludDTO> riesgos, String recomendacionNova) {
        this.analisisGeneral = analisisGeneral;
        this.riesgos = riesgos != null ? riesgos : new ArrayList<>();
        this.recomendacionNova = recomendacionNova;
    }

    public String getAnalisisGeneral() { return analisisGeneral; }
    public void setAnalisisGeneral(String analisisGeneral) { this.analisisGeneral = analisisGeneral; }

    public List<RiesgoSaludDTO> getRiesgos() { return riesgos; }
    public void setRiesgos(List<RiesgoSaludDTO> riesgos) { this.riesgos = riesgos; }

    public String getRecomendacionNova() { return recomendacionNova; }
    public void setRecomendacionNova(String recomendacionNova) { this.recomendacionNova = recomendacionNova; }
    
    public void addRiesgo(RiesgoSaludDTO riesgo) {
        this.riesgos.add(riesgo);
    }
}
