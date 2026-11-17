package com.umoar.minisuperhub.servicios;

import com.umoar.minisuperhub.modelos.Factura;
import com.umoar.minisuperhub.modelos.Producto;
import com.umoar.minisuperhub.modelos.Usuario;
import com.umoar.minisuperhub.repositorios.ClienteRepository;
import com.umoar.minisuperhub.repositorios.FacturaRepository;
import com.umoar.minisuperhub.repositorios.ProductoRepository;
import com.umoar.minisuperhub.repositorios.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FacturaServiceTests {

    private final FacturaRepository facturaRepository = mock(FacturaRepository.class);
    private final ClienteRepository clienteRepository = mock(ClienteRepository.class);
    private final ProductoRepository productoRepository = mock(ProductoRepository.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final FacturaService facturaService = new FacturaService(
            facturaRepository, clienteRepository, productoRepository, usuarioRepository);

    @Test
    void guardarDesdeFormularioCalculaTotalConPreciosDeCatalogo() {
        Producto producto = Producto.builder().id(5L).nombre("Arroz").precioVenta(1.25).build();
        Usuario vendedor = Usuario.builder().id(8L).username("empleado").build();
        when(productoRepository.findById(5L)).thenReturn(Optional.of(producto));
        when(usuarioRepository.findByUsername("empleado")).thenReturn(Optional.of(vendedor));
        when(facturaRepository.save(any(Factura.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Factura guardada = facturaService.guardarDesdeFormulario(
                Factura.builder().id(0L).build(), null, List.of(5L), List.of(4), "empleado", false);

        assertEquals(5.0, guardada.getTotal(), 0.0001);
        assertEquals(1, guardada.getDetalles().size());
        assertEquals(1.25, guardada.getDetalles().get(0).getPrecioUnitario(), 0.0001);
        assertEquals(4, guardada.getDetalles().get(0).getCantidad());
    }

    @Test
    void guardarDesdeFormularioRechazaCantidadesNoPositivasAntesDePersistir() {
        Producto producto = Producto.builder().id(5L).nombre("Arroz").precioVenta(1.25).build();
        when(productoRepository.findById(5L)).thenReturn(Optional.of(producto));

        assertThrows(IllegalArgumentException.class, () -> facturaService.guardarDesdeFormulario(
                Factura.builder().id(0L).build(), null, List.of(5L), List.of(0), "empleado", false));

        verify(facturaRepository, never()).save(any(Factura.class));
    }

    @Test
    void empleadoNoPuedeEditarFacturaDeOtroVendedor() {
        when(facturaRepository.findByIdAndVendedorUsername(12L, "empleado"))
                .thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> facturaService.guardarDesdeFormulario(
                Factura.builder().id(12L).build(), null, List.of(5L), List.of(1), "empleado", false));

        verify(productoRepository, never()).findById(any());
        verify(facturaRepository, never()).save(any(Factura.class));
    }
}
