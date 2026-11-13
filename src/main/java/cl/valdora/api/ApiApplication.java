package cl.valdora.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API REST de clientes, productos y solicitudes de servicio.
 * Proyecto demostrativo — desarrollo propio, no asociado a un cliente real.
 */
@SpringBootApplication
public class ApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }
}
