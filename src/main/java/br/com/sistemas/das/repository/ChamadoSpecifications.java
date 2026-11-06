package br.com.sistemas.das.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import br.com.sistemas.das.entity.Chamado;
import br.com.sistemas.das.entity.Prioridade;
import br.com.sistemas.das.entity.StatusChamado;
import jakarta.persistence.criteria.Predicate;

public final class ChamadoSpecifications {
    private ChamadoSpecifications() { }

    public static Specification<Chamado> comFiltros(
            StatusChamado status, Prioridade prioridade, Long clienteId) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> filtros = new ArrayList<>();
            if (status != null) {
                filtros.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (prioridade != null) {
                filtros.add(criteriaBuilder.equal(root.get("prioridade"), prioridade));
            }
            if (clienteId != null) {
                filtros.add(criteriaBuilder.equal(root.get("cliente").get("id"), clienteId));
            }
            return criteriaBuilder.and(filtros.toArray(Predicate[]::new));
        };
    }
}
