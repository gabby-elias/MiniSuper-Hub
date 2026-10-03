package com.umoar.minisuperhub;

import com.umoar.minisuperhub.modelos.Categoria;
import com.umoar.minisuperhub.modelos.DetalleFactura;
import com.umoar.minisuperhub.modelos.Cliente;
import com.umoar.minisuperhub.modelos.Factura;
import com.umoar.minisuperhub.modelos.Producto;
import com.umoar.minisuperhub.repositorios.UsuarioRepository;
import com.umoar.minisuperhub.servicios.CategoriaService;
import com.umoar.minisuperhub.servicios.ClienteService;
import com.umoar.minisuperhub.servicios.FacturaService;
import com.umoar.minisuperhub.servicios.ProductoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ThymeleafViewsTests {

    @Autowired
    private MockMvc mockMvc;

    @Value("${app.demo.admin.username}")
    private String adminUsername;

    @Value("${app.demo.admin.password}")
    private String adminPassword;

    @Value("${app.demo.employee.username}")
    private String employeeUsername;

    @Value("${app.demo.employee.password}")
    private String employeePassword;

    @Autowired
    private CategoriaService categoriaService;

    @Autowired
    private ProductoService productoService;

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private FacturaService facturaService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void vistasAdministrativasSeRenderizanConRolAdmin() throws Exception {
        MockHttpSession session = iniciarSesion(adminUsername, adminPassword);
        String[] rutas = {
                "/dashboard", "/categorias", "/categorias/nuevo",
                "/clientes", "/clientes/nuevo", "/productos", "/productos/nuevo",
                "/facturas", "/facturas/nuevo", "/detalles-factura",
                "/detalles-factura/nuevo", "/usuarios", "/usuarios/nuevo", "/reportes"
        };

        for (String ruta : rutas) {
            mockMvc.perform(get(ruta).session(session))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @Transactional
    void elFormularioGuardaPrecioYExistenciasDelProducto() throws Exception {
        Categoria categoria = categoriaService.guardar(Categoria.builder()
                .nombreCategoria("Categoría de prueba")
                .build());
        MockHttpSession session = iniciarSesion(adminUsername, adminPassword);

        mockMvc.perform(post("/productos/guardar")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("nombre", "Producto de prueba")
                        .param("unidadMedida", "unidad")
                        .param("stock", "7")
                        .param("precioVenta", "1.03")
                        .param("categoria.id", String.valueOf(categoria.getId())))
                .andExpect(status().is3xxRedirection());

        Producto guardado = productoService.listar().stream()
                .filter(producto -> "Producto de prueba".equals(producto.getNombre()))
                .findFirst()
                .orElseThrow();
        assertEquals(1.03, guardado.getPrecioVenta(), 0.0001);
        assertEquals(7, guardado.getStock());
    }

    @Test
    @Transactional
    void formularioDeFacturaCalculaTotalGuardaDetallesYAsignaVendedor() throws Exception {
        Categoria categoria = categoriaService.guardar(Categoria.builder()
                .nombreCategoria("Categoría factura de prueba")
                .build());
        Producto producto = productoService.guardar(Producto.builder()
                .nombre("Producto factura de prueba")
                .unidadMedida("unidad")
                .stock(8)
                .precioVenta(0.25)
                .categoria(categoria)
                .build());
        Cliente cliente = clienteService.guardar(Cliente.builder()
                .nombre("Cliente factura de prueba")
                .documento("PRUEBA-001")
                .build());
        MockHttpSession session = iniciarSesion(adminUsername, adminPassword);

        mockMvc.perform(post("/facturas/guardar")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("id", "0")
                        .param("fecha", "2026-09-30T12:00")
                        .param("clienteId", String.valueOf(cliente.getId()))
                        .param("productoIds", String.valueOf(producto.getId()))
                        .param("cantidades", "2"))
                .andExpect(status().is3xxRedirection());

        Factura factura = facturaService.listar().stream()
                .filter(item -> item.getCliente() != null && item.getCliente().getId() == cliente.getId())
                .findFirst()
                .orElseThrow();
        assertEquals(0.50, factura.getTotal(), 0.0001);
        assertEquals(1, factura.getDetalles().size());
        assertEquals(2, factura.getDetalles().get(0).getCantidad());
        assertEquals("admin", factura.getVendedor().getUsername());
        assertFalse(factura.isAnulada());
    }

    @Test
    @Transactional
    void permiteGuardarFacturaSinRegistrarCliente() throws Exception {
        Categoria categoria = categoriaService.guardar(Categoria.builder()
                .nombreCategoria("Categoría consumidor final")
                .build());
        Producto producto = productoService.guardar(Producto.builder()
                .nombre("Producto consumidor final")
                .unidadMedida("unidad")
                .stock(4)
                .precioVenta(0.83)
                .categoria(categoria)
                .build());
        MockHttpSession session = iniciarSesion(adminUsername, adminPassword);

        mockMvc.perform(post("/facturas/guardar")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("id", "0")
                        .param("fecha", "2026-10-02T12:00")
                        .param("clienteId", "")
                        .param("productoIds", String.valueOf(producto.getId()))
                        .param("cantidades", "2"))
                .andExpect(status().is3xxRedirection());

        Factura factura = facturaService.listar().stream()
                .filter(item -> item.getCliente() == null && item.getDetalles().stream()
                        .anyMatch(detalle -> detalle.getProducto().getId() == producto.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals(1.66, factura.getTotal(), 0.0001);
    }

    @Test
    @Transactional
    void anularFacturaComoAdminConservaSusDetalles() throws Exception {
        Categoria categoria = categoriaService.guardar(Categoria.builder()
                .nombreCategoria("Categoría anulación")
                .build());
        Producto producto = productoService.guardar(Producto.builder()
                .nombre("Producto anulación")
                .unidadMedida("unidad")
                .stock(2)
                .precioVenta(3.50)
                .categoria(categoria)
                .build());
        Cliente cliente = clienteService.guardar(Cliente.builder()
                .nombre("Cliente anulación")
                .documento("ANULAR-001")
                .build());
        Factura factura = facturaService.guardarConDetalles(
                Factura.builder().fecha(LocalDateTime.now()).cliente(cliente).total(3.50).build(),
                List.of(DetalleFactura.builder().producto(producto).cantidad(1)
                        .precioUnitario(3.50).subtotal(3.50).build()),
                usuarioRepository.findByUsername("admin").orElseThrow());
        MockHttpSession session = iniciarSesion(adminUsername, adminPassword);

        mockMvc.perform(post("/facturas/anular/{id}", factura.getId())
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection());

        Factura anulada = facturaService.buscarPorId(factura.getId()).orElseThrow();
        assertTrue(anulada.isAnulada());
        assertEquals(1, anulada.getDetalles().size());
    }

    @Test
    void empleadoPuedeConsultarCatalogosPeroNoAdministrarNiVerFacturasAjena() throws Exception {
        MockHttpSession session = iniciarSesion(employeeUsername, employeePassword);
        for (String ruta : List.of("/dashboard", "/categorias", "/productos", "/clientes", "/facturas", "/facturas/nuevo")) {
            mockMvc.perform(get(ruta).session(session)).andExpect(status().isOk());
        }
        for (String ruta : List.of("/categorias/nuevo", "/productos/nuevo", "/clientes/editar/1",
                "/detalles-factura", "/usuarios", "/reportes")) {
            mockMvc.perform(get(ruta).session(session)).andExpect(status().isForbidden());
        }
    }

    @Test
    @Transactional
    void empleadoSoloListaYConsultaSusPropiasFacturas() throws Exception {
        Cliente cliente = clienteService.guardar(Cliente.builder()
                .nombre("Cliente control de acceso")
                .documento("ACCESO-001")
                .build());
        var admin = usuarioRepository.findByUsername("admin").orElseThrow();
        var empleado = usuarioRepository.findByUsername("empleado").orElseThrow();
        Factura ajena = facturaService.guardarConDetalles(
                Factura.builder().fecha(LocalDateTime.now()).cliente(cliente).total(10).build(), List.of(), admin);
        Factura propia = facturaService.guardarConDetalles(
                Factura.builder().fecha(LocalDateTime.now()).cliente(cliente).total(20).build(), List.of(), empleado);
        MockHttpSession session = iniciarSesion(employeeUsername, employeePassword);

        MvcResult listado = mockMvc.perform(get("/facturas").session(session))
                .andExpect(status().isOk())
                .andReturn();
        @SuppressWarnings("unchecked")
        List<Factura> facturasVisibles = (List<Factura>) listado.getModelAndView().getModel().get("facturas");
        assertNotNull(facturasVisibles);
        assertTrue(facturasVisibles.stream().anyMatch(factura -> factura.getId() == propia.getId()));
        assertFalse(facturasVisibles.stream().anyMatch(factura -> factura.getId() == ajena.getId()));

        mockMvc.perform(get("/facturas/ver/{id}", propia.getId()).session(session))
                .andExpect(status().isOk());
        mockMvc.perform(get("/facturas/ver/{id}", ajena.getId()).session(session))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/facturas/descargar/{id}", propia.getId()).session(session))
                .andExpect(status().isOk());
        mockMvc.perform(get("/facturas/descargar/{id}", ajena.getId()).session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void empleadoNoPuedeEnviarIdentificadoresParaModificarFacturasNiClientes() throws Exception {
        Cliente cliente = clienteService.guardar(Cliente.builder()
                .nombre("Cliente protegido")
                .documento("PROTEGIDO-001")
                .build());
        Factura factura = facturaService.guardarConDetalles(
                Factura.builder().fecha(LocalDateTime.now()).cliente(cliente).total(1).build(),
                List.of(), usuarioRepository.findByUsername("admin").orElseThrow());
        MockHttpSession session = iniciarSesion(employeeUsername, employeePassword);

        mockMvc.perform(post("/facturas/guardar")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("id", String.valueOf(factura.getId()))
                        .param("clienteId", String.valueOf(cliente.getId()))
                        .param("productoIds", "1")
                        .param("cantidades", "1"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/clientes/guardar")
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("id", String.valueOf(cliente.getId()))
                        .param("nombre", "Alterado por empleado"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void descargaPdfIncluyeElEncabezadoYSeSirveComoArchivo() throws Exception {
        Categoria categoria = categoriaService.guardar(Categoria.builder()
                .nombreCategoria("Categoría PDF")
                .build());
        Producto producto = productoService.guardar(Producto.builder()
                .nombre("Producto PDF")
                .unidadMedida("unidad")
                .stock(3)
                .precioVenta(2.50)
                .categoria(categoria)
                .build());
        Factura factura = facturaService.guardarConDetalles(
                Factura.builder().fecha(LocalDateTime.now()).total(5.00).build(),
                List.of(DetalleFactura.builder().producto(producto).cantidad(2)
                        .precioUnitario(2.50).subtotal(5.00).build()),
                usuarioRepository.findByUsername(adminUsername).orElseThrow());

        MvcResult resultado = mockMvc.perform(get("/facturas/descargar/{id}", factura.getId())
                        .session(iniciarSesion(adminUsername, adminPassword)))
                .andExpect(status().isOk())
                .andReturn();

        assertEquals("application/pdf", resultado.getResponse().getContentType());
        assertTrue(resultado.getResponse().getHeader("Content-Disposition")
                .contains("factura-" + factura.getId() + ".pdf"));
        assertTrue(new String(resultado.getResponse().getContentAsByteArray(), 0, 5,
                java.nio.charset.StandardCharsets.ISO_8859_1).startsWith("%PDF-"));
    }

    @Test
    void rutasPrivadasRedirigenAlLoginSinSesion() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection());
    }

    private MockHttpSession iniciarSesion(String username, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/login")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("username", username)
                        .param("password", password))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertTrue(session != null);
        return session;
    }
}
