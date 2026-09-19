package com.ejemplo.laboratorio05.controller;

import com.ejemplo.laboratorio05.exception.ProductoDuplicadoException;
import com.ejemplo.laboratorio05.exception.RecursoNoEncontradoException;
import com.ejemplo.laboratorio05.model.Producto;
import com.ejemplo.laboratorio05.service.ProductoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Suite de Integración MockMvc - ProductoController (Lab 05A)")
class ProductoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductoService productoService;

    @Nested
    @DisplayName("GET /api/productos - Listar y Consultar")
    class GetEndpoints {

        @Test
        @DisplayName("Debe retornar 200 OK y la lista de productos")
        void listarTodos_debeRetornar200OK() throws Exception {
            Producto p1 = new Producto(1L, "Monitor LG 24", "Monitores", 699.90, 8);
            Producto p2 = new Producto(2L, "Teclado Mecánico", "Periféricos", 150.00, 2);

            when(productoService.listarTodos()).thenReturn(List.of(p1, p2));

            mockMvc.perform(get("/api/productos")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].nombre").value("Monitor LG 24"))
                    .andExpect(jsonPath("$[0].estadoStock").value("OK"))
                    .andExpect(jsonPath("$[1].estadoStock").value("INSUFICIENTE"));
        }

        @Test
        @DisplayName("Debe retornar 200 OK cuando el producto existe por ID")
        void buscarPorId_existente_debeRetornar200OK() throws Exception {
            Producto p = new Producto(1L, "Monitor LG 24", "Monitores", 699.90, 3);
            when(productoService.buscarPorId(1L)).thenReturn(p);

            mockMvc.perform(get("/api/productos/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.nombre").value("Monitor LG 24"))
                    .andExpect(jsonPath("$.estadoStock").value("INSUFICIENTE"));
        }

        @Test
        @DisplayName("Debe retornar 404 NOT FOUND cuando el ID no existe")
        void buscarPorId_inexistente_debeRetornar404NotFound() throws Exception {
            when(productoService.buscarPorId(999L))
                    .thenThrow(new RecursoNoEncontradoException("No existe el producto con id 999"));

            mockMvc.perform(get("/api/productos/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.estado").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.mensaje").value("No existe el producto con id 999"))
                    .andExpect(jsonPath("$.fecha").exists());
        }
    }

    @Nested
    @DisplayName("POST /api/productos - Creación y Validaciones")
    class PostEndpoints {

        @Test
        @DisplayName("Debe retornar 201 CREATED cuando el payload es válido")
        void crear_valido_debeRetornar201Created() throws Exception {
            Producto entrada = new Producto(null, "Laptop Gamer", "Computadoras", 4500.0, 10);
            Producto guardado = new Producto(10L, "Laptop Gamer", "Computadoras", 4500.0, 10);

            when(productoService.crear(any(Producto.class))).thenReturn(guardado);

            mockMvc.perform(post("/api/productos")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(entrada)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(10L))
                    .andExpect(jsonPath("$.nombre").value("Laptop Gamer"))
                    .andExpect(jsonPath("$.categoria").value("Computadoras"))
                    .andExpect(jsonPath("$.estadoStock").value("OK"));
        }

        @Test
        @DisplayName("Debe retornar 400 BAD REQUEST con desglose de campos ante fallos de validación")
        void crear_invalido_debeRetornar400BadRequest() throws Exception {
            Producto invalido = new Producto(null, "", "", -50.0, -5);

            mockMvc.perform(post("/api/productos")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalido)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.estado").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.validaciones.nombre").exists())
                    .andExpect(jsonPath("$.validaciones.categoria").exists())
                    .andExpect(jsonPath("$.validaciones.precio").value("El precio debe ser mayor que cero"))
                    .andExpect(jsonPath("$.validaciones.stock").value("El stock no puede ser negativo"));
        }

        @Test
        @DisplayName("Debe retornar 409 CONFLICT cuando el nombre del producto ya existe (Reto Adicional)")
        void crear_duplicado_debeRetornar409Conflict() throws Exception {
            Producto repetido = new Producto(null, "Monitor LG 24", "Monitores", 699.90, 5);

            when(productoService.crear(any(Producto.class)))
                    .thenThrow(new ProductoDuplicadoException("Ya existe un producto con el nombre: Monitor LG 24"));

            mockMvc.perform(post("/api/productos")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(repetido)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.estado").value(409))
                    .andExpect(jsonPath("$.error").value("Conflict"))
                    .andExpect(jsonPath("$.mensaje").value("Ya existe un producto con el nombre: Monitor LG 24"));
        }
    }

    @Nested
    @DisplayName("PUT y DELETE - Actualización y Eliminación")
    class PutDeleteEndpoints {

        @Test
        @DisplayName("Debe retornar 200 OK al actualizar correctamente")
        void actualizar_valido_debeRetornar200OK() throws Exception {
            Producto update = new Producto(null, "Monitor LG 24 Pro", "Monitores", 750.0, 15);
            Producto actualizado = new Producto(1L, "Monitor LG 24 Pro", "Monitores", 750.0, 15);

            when(productoService.actualizar(eq(1L), any(Producto.class))).thenReturn(actualizado);

            mockMvc.perform(put("/api/productos/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(update)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nombre").value("Monitor LG 24 Pro"))
                    .andExpect(jsonPath("$.estadoStock").value("OK"));
        }

        @Test
        @DisplayName("Debe retornar 204 NO CONTENT al eliminar un recurso existente")
        void eliminar_existente_debeRetornar204NoContent() throws Exception {
            doNothing().when(productoService).eliminar(1L);

            mockMvc.perform(delete("/api/productos/1"))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));
        }

        @Test
        @DisplayName("Debe retornar 404 NOT FOUND al intentar eliminar recurso inexistente")
        void eliminar_inexistente_debeRetornar404NotFound() throws Exception {
            doThrow(new RecursoNoEncontradoException("No existe el producto con id 99"))
                    .when(productoService).eliminar(99L);

            mockMvc.perform(delete("/api/productos/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.estado").value(404));
        }
    }
}
