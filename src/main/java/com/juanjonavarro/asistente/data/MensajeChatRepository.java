package com.juanjonavarro.asistente.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MensajeChatRepository extends JpaRepository<MensajeChat, Long> {
    List<MensajeChat> findByClientidOrderByTsAsc(String clientid);

    List<MensajeChat> findByClientidAndIdGreaterThanEqualOrderByTsAsc(String clientid, Long ts);
}
