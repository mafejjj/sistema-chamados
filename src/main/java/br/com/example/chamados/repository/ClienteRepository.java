package br.com.example.chamados.repository;

import br.com.example.chamados.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long>{
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Long id);
    
}
