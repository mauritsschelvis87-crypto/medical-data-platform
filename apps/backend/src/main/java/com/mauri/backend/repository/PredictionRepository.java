package com.mauri.backend.repository;

import com.mauri.backend.entity.Patient;
import com.mauri.backend.entity.Prediction;
import com.mauri.backend.enums.PredictionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PredictionRepository extends JpaRepository<Prediction, Long> {

    List<Prediction> findByPatientOrderByPredictionTimestampDesc(Patient patient);

    List<Prediction> findByPatientAndPredictionTypeOrderByPredictionTimestampDesc(
            Patient patient,
            PredictionType predictionType
    );

    List<Prediction> findByPatientAndMainPredictionTrueOrderByPredictionTimestampDesc(Patient patient);

    Optional<Prediction> findFirstByPatientAndPredictionTypeOrderByPredictionTimestampDesc(
            Patient patient,
            PredictionType predictionType
    );
}