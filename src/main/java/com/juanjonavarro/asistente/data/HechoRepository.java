package com.juanjonavarro.asistente.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HechoRepository extends JpaRepository<Hecho, Long> {
    List<Hecho> findByClientidOrderByTsAsc(String clientid);
}
