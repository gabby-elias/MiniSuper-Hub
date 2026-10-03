package com.umoar.minisuperhub.servicios;

import com.umoar.minisuperhub.modelos.Categoria;
import com.umoar.minisuperhub.repositorios.CategoriaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public List<Categoria> listar() {
        return categoriaRepository.findAll();
    }

    public Optional<Categoria> buscarPorId(Long id) {
        return categoriaRepository.findById(id);
    }

    public Categoria guardar(Categoria categoria) {
        return categoriaRepository.save(categoria);
    }

    public Categoria actualizar(Long id, Categoria categoria) {
        if (!categoriaRepository.existsById(id)) {
            throw new EntityNotFoundException("No se encontró la categoría con id " + id);
        }
        categoria.setId(id);
        return categoriaRepository.save(categoria);
    }

    public void eliminar(Long id) {
        if (!categoriaRepository.existsById(id)) {
            throw new EntityNotFoundException("No se encontró la categoría con id " + id);
        }
        categoriaRepository.deleteById(id);
    }
}
