package com.clinicaapp.service;

import com.clinicaapp.model.MascotaVirtual;

public interface IJuegoService {
    MascotaVirtual obtenerMascotaUsuario(String usuarioId);
    MascotaVirtual realizarAccion(String usuarioId, String accion);
    void sincronizarEstado(MascotaVirtual mv);
}
