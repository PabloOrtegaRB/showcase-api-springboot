package cl.valdora.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI documentacion() {
        return new OpenAPI().info(new Info()
                .title("API Valdora (demostrativa)")
                .version("1.0.0")
                .description("API REST de clientes, productos y solicitudes de servicio, con validaciones, "
                        + "paginación, filtros y errores en formato RFC 9457. "
                        + "**Proyecto demostrativo — desarrollo propio, no asociado a un cliente real.** "
                        + "Todos los datos son ficticios.")
                .contact(new Contact().name("Pablo Ortega Rubio").url("https://github.com/PabloOrtegaRB"))
                .license(new License().name("MIT")));
    }

    /** Reloj inyectable: permite fijar la hora en las pruebas. */
    @Bean
    Clock reloj() {
        return Clock.systemDefaultZone();
    }
}
