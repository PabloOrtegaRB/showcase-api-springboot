# Ejemplos de respuestas reales

Generado por `scripts/capturas.py` contra la API en ejecución, con datos ficticios.

## Listar solicitudes recibidas

`GET /api/v1/solicitudes?estado=RECIBIDA&tamano=2` → **HTTP 200**

Respuesta:

```json
{
  "contenido": [
    {
      "id": 1,
      "titulo": "Mantención preventiva de chiller",
      "descripcion": "Solicitud de demostración número 1.",
      "prioridad": "BAJA",
      "estado": "RECIBIDA",
      "estadosSiguientes": [
        "EN_PROCESO",
        "CANCELADA"
      ],
      "cliente": {
        "id": 1,
        "razonSocial": "Bodegas Arrayán Sur SpA"
      },
      "producto": null,
      "comentarioEstado": null,
      "creadaEn": "2026-11-13T07:30:35",
      "actualizadaEn": "2026-11-13T09:30:35"
    },
    {
      "id": 5,
      "titulo": "Falla en bomba de condensado",
      "descripcion": "Solicitud de demostración número 5.",
      "prioridad": "BAJA",
      "estado": "RECIBIDA",
      "estadosSiguientes": [
        "EN_PROCESO",
        "CANCELADA"
      ],
      "cliente": {
        "id": 5,
        "razonSocial": "Alimentos Coigüe Ltda."
      },
      "producto": {
        "id": 5,
        "sku": "CLI-3001",
        "nombre": "Refrigerante R-410A, cilindro 11 kg"
      },
      "comentarioEstado": null,
      "creadaEn": "2026-11-01T07:30:35",
      "actualizadaEn": "2026-11-01T09:30:35"
    }
  ],
  "pagina": 0,
  "tamano": 2,
  "totalElementos": 3,
  "totalPaginas": 2
}
```

## Crear cliente válido

`POST /api/v1/clientes` → **HTTP 201**

Cuerpo enviado:

```json
{
  "rut": "12.345.678-5",
  "razonSocial": "Comercial Demo SpA",
  "email": "contacto@demo.example",
  "comuna": "Santiago"
}
```

Respuesta:

```json
{
  "id": 9,
  "rut": "12345678-5",
  "razonSocial": "Comercial Demo SpA",
  "email": "contacto@demo.example",
  "telefono": null,
  "comuna": "Santiago",
  "activo": true,
  "fechaRegistro": "2026-11-13"
}
```

## Crear cliente con datos inválidos

`POST /api/v1/clientes` → **HTTP 400**

Cuerpo enviado:

```json
{
  "rut": "12.345.678-9",
  "razonSocial": "",
  "email": "no-es-correo",
  "comuna": "Santiago"
}
```

Respuesta:

```json
{
  "type": "about:blank",
  "title": "Datos inválidos",
  "status": 400,
  "detail": "La solicitud tiene campos inválidos.",
  "instance": "/api/v1/clientes",
  "errores": [
    {
      "campo": "rut",
      "mensaje": "El RUT no es válido: revisa el dígito verificador."
    },
    {
      "campo": "razonSocial",
      "mensaje": "Ingresa la razón social."
    },
    {
      "campo": "email",
      "mensaje": "Revisa el formato del correo."
    }
  ]
}
```

## Repetir el mismo RUT

`POST /api/v1/clientes` → **HTTP 409**

Cuerpo enviado:

```json
{
  "rut": "12345678-5",
  "razonSocial": "Otra SpA",
  "email": "otra@demo.example",
  "comuna": "Maipú"
}
```

Respuesta:

```json
{
  "type": "about:blank",
  "title": "Conflicto con el estado actual",
  "status": 409,
  "detail": "Ya existe un cliente con el RUT 12345678-5.",
  "instance": "/api/v1/clientes"
}
```

## Transición no permitida (COMPLETADA → EN_PROCESO)

`PATCH /api/v1/solicitudes/3/estado` → **HTTP 409**

Cuerpo enviado:

```json
{
  "estado": "EN_PROCESO"
}
```

Respuesta:

```json
{
  "type": "about:blank",
  "title": "Conflicto con el estado actual",
  "status": 409,
  "detail": "No se puede pasar de COMPLETADA a EN_PROCESO. Estados permitidos: ninguno (estado final).",
  "instance": "/api/v1/solicitudes/3/estado"
}
```
