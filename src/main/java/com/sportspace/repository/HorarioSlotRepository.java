package com.sportspace.repository;

import com.sportspace.entity.HorarioSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface HorarioSlotRepository extends JpaRepository<HorarioSlot, Long> {
    List<HorarioSlot> findByCanchaId(Long canchaId);

    @Modifying @Transactional
    @Query("DELETE FROM HorarioSlot s WHERE s.cancha.id = :canchaId")
    void deleteByCanchaId(Long canchaId);
}