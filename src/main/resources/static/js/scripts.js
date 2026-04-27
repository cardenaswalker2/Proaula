/* =================================================================
   CLÍNICA APP - SCRIPTS GLOBALES
================================================================= */

// Espera a que todo el contenido del DOM esté cargado antes de ejecutar cualquier script
document.addEventListener('DOMContentLoaded', function() {

    /**
     * Activa los tooltips de Bootstrap en toda la aplicación.
     * Para usarlo, añade el atributo `data-bs-toggle="tooltip"` y `title="Tu texto"` a cualquier elemento HTML.
     */
    function initBootstrapTooltips() {
        const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
        tooltipTriggerList.map(function (tooltipTriggerEl) {
            return new bootstrap.Tooltip(tooltipTriggerEl);
        });
    }

    /**
     * Gestiona el formulario de "Agendar Cita" de la Recepcionista.
     * Carga dinámicamente las mascotas de un cliente cuando este es seleccionado.
     */
    function initRecepcionistPetLoader() {
        const clienteSelect = document.getElementById('cliente');
        const mascotaSelect = document.getElementById('mascota');

        // Si el formulario no está en la página actual, no hagas nada.
        if (!clienteSelect || !mascotaSelect) {
            return;
        }

        clienteSelect.addEventListener('change', function() {
            const clienteId = this.value;
            mascotaSelect.innerHTML = '<option value="">Cargando...</option>';
            mascotaSelect.disabled = true;

            if (clienteId) {
                // Llamada a la API que creamos en el RecepcionistaController
                fetch(`/recepcion/api/mascotas-usuario/${clienteId}`)
                    .then(response => {
                        if (!response.ok) {
                            throw new Error('Error en la respuesta del servidor.');
                        }
                        return response.json();
                    })
                    .then(data => {
                        mascotaSelect.innerHTML = '<option value="">-- Seleccione una Mascota --</option>';
                        if (data.length > 0) {
                            data.forEach(mascota => {
                                const option = document.createElement('option');
                                option.value = mascota.id;
                                option.textContent = mascota.nombre;
                                mascotaSelect.appendChild(option);
                            });
                            mascotaSelect.disabled = false;
                        } else {
                            mascotaSelect.innerHTML = '<option value="">Este cliente no tiene mascotas</option>';
                        }
                    })
                    .catch(error => {
                        console.error('Error al cargar las mascotas:', error);
                        mascotaSelect.innerHTML = '<option value="">Error al cargar mascotas</option>';
                    });
            } else {
                mascotaSelect.innerHTML = '<option value="">-- Seleccione un cliente primero --</option>';
            }
        });
    }

    /**
     * Gestiona el formulario de "Añadir/Editar Mascota".
     * Muestra el campo de raza correspondiente (perro o gato) según la especie seleccionada.
     */
    function initSpeciesBreedToggle() {
        const especieSelect = document.getElementById('especie');
        const razaPerroDiv = document.getElementById('razaPerroDiv');
        const razaGatoDiv = document.getElementById('razaGatoDiv');

        // Si el formulario no está en la página actual, no hagas nada.
        if (!especieSelect || !razaPerroDiv || !razaGatoDiv) {
            return;
        }

        function toggleRazaFields() {
            const selectedSpecies = especieSelect.value;
            razaPerroDiv.style.display = (selectedSpecies === 'PERRO') ? 'block' : 'none';
            razaGatoDiv.style.display = (selectedSpecies === 'GATO') ? 'block' : 'none';
        }

        // Ejecutar la función una vez al cargar la página para establecer el estado inicial correcto
        toggleRazaFields();

        // Añadir el listener para cambios futuros
        especieSelect.addEventListener('change', toggleRazaFields);
    }

    // --- Inicializar todas las funciones ---
    initBootstrapTooltips();
    initRecepcionistPetLoader();
    initSpeciesBreedToggle();

});