package com.clinicaapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Exponer la carpeta 'uploads' que está en la raíz del proyecto
        // Usamos 'file:uploads/' que es una ruta relativa a la raíz de ejecución
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}
