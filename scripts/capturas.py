"""Evidencia real de la API en ejecución: capturas de Swagger UI, openapi.json y ejemplos de respuestas.

1. Inicia la API:   ./mvnw spring-boot:run
2. Ejecuta:         python scripts/capturas.py
   Variables:       BASE_URL (por defecto http://localhost:8081), PW_CHANNEL=msedge
"""
import json
import os
import pathlib
import urllib.error
import urllib.request

from playwright.sync_api import sync_playwright

BASE = os.environ.get("BASE_URL", "http://localhost:8081").rstrip("/")
RAIZ = pathlib.Path(__file__).resolve().parents[1]
CAPTURAS = RAIZ / "docs" / "capturas"


def llamar(metodo, ruta, cuerpo=None):
    datos = json.dumps(cuerpo).encode("utf-8") if cuerpo is not None else None
    peticion = urllib.request.Request(BASE + ruta, data=datos, method=metodo,
                                      headers={"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(peticion) as r:
            return r.status, json.loads(r.read() or b"null")
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read() or b"null")


def ejemplos():
    casos = [
        ("Listar solicitudes recibidas", "GET", "/api/v1/solicitudes?estado=RECIBIDA&tamano=2", None),
        ("Crear cliente válido", "POST", "/api/v1/clientes",
         {"rut": "12.345.678-5", "razonSocial": "Comercial Demo SpA", "email": "contacto@demo.example", "comuna": "Santiago"}),
        ("Crear cliente con datos inválidos", "POST", "/api/v1/clientes",
         {"rut": "12.345.678-9", "razonSocial": "", "email": "no-es-correo", "comuna": "Santiago"}),
        ("Repetir el mismo RUT", "POST", "/api/v1/clientes",
         {"rut": "12345678-5", "razonSocial": "Otra SpA", "email": "otra@demo.example", "comuna": "Maipú"}),
        ("Transición no permitida (COMPLETADA → EN_PROCESO)", "PATCH", "/api/v1/solicitudes/3/estado",
         {"estado": "EN_PROCESO"}),
    ]
    lineas = ["# Ejemplos de respuestas reales", "",
              "Generado por `scripts/capturas.py` contra la API en ejecución, con datos ficticios.", ""]
    for titulo, metodo, ruta, cuerpo in casos:
        estado, respuesta = llamar(metodo, ruta, cuerpo)
        lineas += [f"## {titulo}", "", f"`{metodo} {ruta}` → **HTTP {estado}**", ""]
        if cuerpo is not None:
            lineas += ["Cuerpo enviado:", "", "```json", json.dumps(cuerpo, ensure_ascii=False, indent=2), "```", ""]
        lineas += ["Respuesta:", "", "```json", json.dumps(respuesta, ensure_ascii=False, indent=2), "```", ""]
    (RAIZ / "docs" / "ejemplos-respuestas.md").write_text("\n".join(lineas), encoding="utf-8")


def main():
    CAPTURAS.mkdir(parents=True, exist_ok=True)
    with urllib.request.urlopen(BASE + "/v3/api-docs") as r:
        especificacion = json.loads(r.read())
    (RAIZ / "docs" / "openapi.json").write_text(json.dumps(especificacion, ensure_ascii=False, indent=2), encoding="utf-8")
    ejemplos()
    with sync_playwright() as p:
        navegador = p.chromium.launch(channel=os.environ.get("PW_CHANNEL") or None)
        pagina = navegador.new_page(viewport={"width": 1366, "height": 900})
        pagina.goto(BASE + "/swagger-ui.html")
        pagina.wait_for_selector(".opblock-tag-section")
        pagina.screenshot(path=CAPTURAS / "01-swagger-resumen.png", full_page=True)
        operacion = pagina.locator(".opblock", has_text="Cambia el estado de una solicitud").first
        operacion.locator(".opblock-summary").click()
        operacion.locator(".try-out__btn").click()
        operacion.locator("input[placeholder='id']").fill("3")
        operacion.locator("textarea.body-param__text").fill('{\n  "estado": "EN_PROCESO",\n  "comentario": "Intento no permitido"\n}')
        operacion.locator(".execute").click()
        operacion.locator(".responses-wrapper .live-responses-table").wait_for()
        operacion.screenshot(path=CAPTURAS / "02-swagger-error-409.png")
        operacion.locator(".opblock-summary").click()
        listar = pagina.locator(".opblock", has_text="Lista clientes con filtros y paginación").first
        listar.locator(".opblock-summary").click()
        listar.locator(".try-out__btn").click()
        listar.locator(".execute").click()
        listar.locator(".responses-wrapper .live-responses-table").wait_for()
        listar.screenshot(path=CAPTURAS / "03-swagger-listar-clientes.png")
        navegador.close()
    print("Evidencia generada en", RAIZ / "docs")


if __name__ == "__main__":
    main()
