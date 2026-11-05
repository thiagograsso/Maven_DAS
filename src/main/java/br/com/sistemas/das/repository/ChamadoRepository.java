package br.com.sistemas.das.repository;

import br.com.sistemas.das.entity.Chamado;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChamadoRepository extends JpaRepository<Chamado, Long> {
}
