package com.umoar.minisuperhub.configuracion;

import com.umoar.minisuperhub.modelos.Categoria;
import com.umoar.minisuperhub.modelos.Producto;
import com.umoar.minisuperhub.repositorios.CategoriaRepository;
import com.umoar.minisuperhub.repositorios.ProductoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class CatalogoProductosInitializer implements ApplicationRunner {

    private static final String CLAVE_SEMILLA = "catalogo-productos-v1";
    private static final int TOTAL_PRODUCTOS = 1000;
    private static final List<String> PRODUCTOS = List.of(
            "Arroz blanco", "Frijoles rojos", "Azúcar", "Sal", "Harina de maíz",
            "Aceite vegetal", "Café molido", "Leche entera", "Queso fresco", "Huevos",
            "Pan francés", "Pasta corta", "Atún en agua", "Sardinas", "Agua purificada",
            "Gaseosa", "Jugo de naranja", "Galletas", "Jabón de baño", "Detergente",
            "Champú", "Papel higiénico", "Pasta dental", "Maíz enlatado", "Salsa de tomate",
            "Mayonesa", "Cereal", "Papas fritas", "Chocolate", "Lentejas",
            "Crema", "Margarina", "Yogur", "Salchichas", "Avena",
            "Sopa instantánea", "Miel", "Tortillas", "Suavizante", "Toallas de papel"
    );
    private static final List<String> MARCAS = List.of(
            "Ahorro", "Buen Precio", "Del Campo", "La Canasta", "Selecto"
    );
    private static final List<String> UNIDADES = List.of("unidad", "paquete", "bolsa", "botella", "libra");

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.catalogo.productos.enabled:true}")
    private boolean habilitado;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!habilitado) return;

        jdbcTemplate.execute("create table if not exists app_demo_seed (seed_key varchar(100) primary key)");
        Boolean inicializado = jdbcTemplate.queryForObject(
                "select exists (select 1 from app_demo_seed where seed_key = ?)",
                Boolean.class, CLAVE_SEMILLA);
        if (Boolean.TRUE.equals(inicializado)) return;

        List<String> nombresEsperados = generarNombres();
        Map<String, Integer> faltantesPorNombre = new HashMap<>();
        nombresEsperados.forEach(nombre -> faltantesPorNombre.merge(nombre, 1, Integer::sum));
        productoRepository.findAll().forEach(producto -> {
            Integer faltantes = faltantesPorNombre.get(producto.getNombre());
            if (faltantes != null && faltantes > 0) {
                faltantesPorNombre.put(producto.getNombre(), faltantes - 1);
            }
        });

        List<Categoria> categorias = categoriaRepository.findAll();
        List<Producto> nuevos = new ArrayList<>();
        for (int i = 0; i < nombresEsperados.size(); i++) {
            String nombre = nombresEsperados.get(i);
            int faltantes = faltantesPorNombre.get(nombre);
            if (faltantes == 0) continue;

            double precio = Math.round((0.35 + (i * 137 % 5000) / 100.0) * 100.0) / 100.0;
            nuevos.add(Producto.builder()
                    .nombre(nombre)
                    .unidadMedida(UNIDADES.get(i % UNIDADES.size()))
                    .stock(i * 17 % 101)
                    .precioVenta(precio)
                    .precio(precio)
                    .categoria(categorias.isEmpty() ? null : categorias.get(i % categorias.size()))
                    .build());
            faltantesPorNombre.put(nombre, faltantes - 1);
        }

        if (!nuevos.isEmpty()) productoRepository.saveAll(nuevos);
        jdbcTemplate.update("insert into app_demo_seed (seed_key) values (?)", CLAVE_SEMILLA);
        log.info("Catálogo inicializado: {} productos nuevos agregados.", nuevos.size());
    }

    private List<String> generarNombres() {
        List<String> nombres = new ArrayList<>(TOTAL_PRODUCTOS);
        for (int i = 0; i < TOTAL_PRODUCTOS; i++) {
            nombres.add(PRODUCTOS.get(i % PRODUCTOS.size()) + " "
                    + MARCAS.get((i / PRODUCTOS.size()) % MARCAS.size()));
        }
        return nombres;
    }
}
