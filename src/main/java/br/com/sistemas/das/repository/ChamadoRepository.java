package br.com.sistemas.das.repository;

import br.com.sistemas.das.entity.Chamado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ChamadoRepository
        extends JpaRepository<Chamado, Long>, JpaSpecificationExecutor<Chamado> {
    Page<Chamado> findByClienteId(Long clienteId, Pageable pageable);
}
