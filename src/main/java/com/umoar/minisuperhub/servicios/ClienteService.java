package com.umoar.minisuperhub.servicios;

import com.umoar.minisuperhub.modelos.Cliente;
import com.umoar.minisuperhub.repositorios.ClienteRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public List<Cliente> listar() {
        return clienteRepository.findAll();
    }

    public Optional<Cliente> buscarPorId(Long id) {
        return clienteRepository.findById(id);
    }

    public Cliente guardar(Cliente cliente) {
        return clienteRepository.save(cliente);
    }

    public Cliente actualizar(Long id, Cliente cliente) {
        if (!clienteRepository.existsById(id)) {
            throw new EntityNotFoundException("No se encontró el cliente con id " + id);
        }
        cliente.setId(id);
        return clienteRepository.save(cliente);
    }

    public void eliminar(Long id) {
        if (!clienteRepository.existsById(id)) {
            throw new EntityNotFoundException("No se encontró el cliente con id " + id);
        }
        clienteRepository.deleteById(id);
    }
}
