package com.umoar.minisuperhub.modelos;

import jakarta.persistence.*;
import lombok.*;

import java.text.Normalizer;
import java.util.Locale;

@Entity
@Table(name = "productos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String nombre;
    private String unidadMedida;
    private int stock;

    private double precioVenta;

    // La tabla existente conserva ambas columnas como obligatorias.
    @Column(name = "precio")
    private double precio;

    @ManyToOne
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @Transient
    public String getIcono() {
        String texto = normalizar(nombre + " " + (categoria == null ? "" : categoria.getNombreCategoria()));

        if (texto.contains("pollo") || texto.contains("gallina")) return "🍗";
        if (texto.contains("carne") || texto.contains("res ") || texto.startsWith("res")
                || texto.contains("cerdo") || texto.contains("puerco")) return "🥩";
        if (texto.contains("hamburguesa") || texto.contains("burger")) return "🍔";
        if (texto.contains("hot dog") || texto.contains("hotdog") || texto.contains("salchicha")) return "🌭";
        if (texto.contains("papas fritas") || texto.contains("french fries") || texto.contains("fritura")) return "🍟";
        if (texto.contains("pasta") || texto.contains("macarron") || texto.contains("espagueti")
                || texto.contains("fideo") || texto.contains("noodle")) return "🍝";
        if (texto.contains("pizza")) return "🍕";
        if (texto.contains("tortilla") || texto.contains("taco")) return "🌮";
        if (texto.contains("galleta") || texto.contains("cookie")) return "🍪";
        if (texto.contains("chocolate") || texto.contains("dulce") || texto.contains("caramelo")) return "🍫";
        if (texto.contains("pastel") || texto.contains("postre")) return "🍰";
        if (texto.contains("snack") || texto.contains("palomita") || texto.contains("chips")) return "🍿";
        if (texto.contains("pan") || texto.contains("bread")) return "🍞";
        if (texto.contains("manzana") || texto.contains("apple")) return "🍎";
        if (texto.contains("banana") || texto.contains("banano") || texto.contains("platano")) return "🍌";
        if (texto.contains("naranja")) return "🍊";
        if (texto.contains("uva")) return "🍇";
        if (texto.contains("fruta")) return "🍎";
        if (texto.contains("huevo") || texto.contains("egg")) return "🥚";
        if (texto.contains("queso") || texto.contains("cheese")) return "🧀";
        if (texto.contains("leche") || texto.contains("milk") || texto.contains("lacteo")) return "🥛";
        if (texto.contains("arroz") || texto.contains("rice")) return "🍚";
        if (texto.contains("frijol") || texto.contains("lenteja") || texto.contains("garbanzo")) return "🫘";
        if (texto.contains("cereal")) return "🥣";
        if (texto.contains("tomate")) return "🍅";
        if (texto.contains("papa") || texto.contains("patata")) return "🥔";
        if (texto.contains("pescado") || texto.contains("fish")) return "🐟";
        if (texto.contains("verdura") || texto.contains("vegetal") || texto.contains("brocoli")) return "🥦";
        if (texto.contains("coca") || texto.contains("cola") || texto.contains("gaseosa")
                || texto.contains("refresco") || texto.contains("bebida") || texto.contains("soda")
                || texto.contains("jugo") || texto.contains("agua") || texto.contains("cafe")) return "🥤";
        if (texto.contains("comida rapida")) return "🍔";
        return "🍽️";
    }

    private String normalizar(String valor) {
        return Normalizer.normalize(valor == null ? "" : valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    @PrePersist
    @PreUpdate
    private void sincronizarPrecioLegado() {
        precio = precioVenta;
    }
}
