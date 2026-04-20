package com.mauri.backend.repository;

import com.mauri.backend.entity.ConsultNote;
import com.mauri.backend.entity.ConsultNoteVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConsultNoteVersionRepository extends JpaRepository<ConsultNoteVersion, Long> {

    List<ConsultNoteVersion> findByConsultNoteOrderByCreatedAtDesc(ConsultNote consultNote);

    List<ConsultNoteVersion> findByConsultNoteOrderByVersionNumberDesc(ConsultNote consultNote);
}