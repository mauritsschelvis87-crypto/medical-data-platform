package com.mauri.backend.service;

import com.mauri.backend.dto.vitals.CreateVitalSignsRequest;
import com.mauri.backend.dto.vitals.VitalSignsDto;
import com.mauri.backend.entity.Patient;
import com.mauri.backend.entity.VitalSigns;
import com.mauri.backend.enums.TimelineEventType;
import com.mauri.backend.enums.VitalSignSource;
import com.mauri.backend.mapper.VitalSignsMapper;
import com.mauri.backend.repository.VitalSignsRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class VitalSignsService {

    private final VitalSignsRepository vitalSignsRepository;
    private final PatientService patientService;
    private final VitalSignsMapper vitalSignsMapper;
    private final TimelineService timelineService;

    public VitalSignsService(VitalSignsRepository vitalSignsRepository,
                             PatientService patientService,
                             VitalSignsMapper vitalSignsMapper,
                             TimelineService timelineService) {
        this.vitalSignsRepository = vitalSignsRepository;
        this.patientService = patientService;
        this.vitalSignsMapper = vitalSignsMapper;
        this.timelineService = timelineService;
    }

    public List<VitalSignsDto> getVitalSignsForPatient(Long patientId) {
        Patient patient = patientService.getPatientEntityById(patientId);

        return vitalSignsRepository.findByPatientOrderByMeasuredAtDesc(patient)
                .stream()
                .map(vitalSignsMapper::toDto)
                .toList();
    }

    public List<VitalSignsDto> getLatestVitalSignsForPatient(Long patientId) {
        Patient patient = patientService.getPatientEntityById(patientId);

        return vitalSignsRepository.findTop10ByPatientOrderByMeasuredAtDesc(patient)
                .stream()
                .map(vitalSignsMapper::toDto)
                .toList();
    }

    public VitalSignsDto createVitalSigns(Long patientId, CreateVitalSignsRequest request) {
        Patient patient = patientService.getPatientEntityById(patientId);

        VitalSigns vitalSigns = new VitalSigns();
        vitalSigns.setPatient(patient);
        vitalSigns.setBloodPressureSystolic(request.getBloodPressureSystolic());
        vitalSigns.setBloodPressureDiastolic(request.getBloodPressureDiastolic());
        vitalSigns.setHeartRate(request.getHeartRate());
        vitalSigns.setTemperature(request.getTemperature());
        vitalSigns.setGlucose(request.getGlucose());
        vitalSigns.setBmi(request.getBmi());
        vitalSigns.setWeight(request.getWeight());
        vitalSigns.setOxygenSaturation(request.getOxygenSaturation());
        vitalSigns.setCholesterol(request.getCholesterol());
        vitalSigns.setMeasuredAt(request.getMeasuredAt() != null ? request.getMeasuredAt() : LocalDateTime.now());
        vitalSigns.setRecordedAt(LocalDateTime.now());
        vitalSigns.setSource(resolveSource(request.getSource()));

        VitalSigns savedVitalSigns = vitalSignsRepository.save(vitalSigns);

        timelineService.createEvent(
                patient,
                TimelineEventType.VITAL_SIGNS_RECORDED,
                savedVitalSigns.getId(),
                "VitalSigns",
                "Vital signs recorded",
                buildVitalSignsDescription(savedVitalSigns),
                savedVitalSigns.getMeasuredAt()
        );

        return vitalSignsMapper.toDto(savedVitalSigns);
    }

    private VitalSignSource resolveSource(String source) {
        if (source == null || source.isBlank()) {
            return VitalSignSource.MANUAL;
        }

        return VitalSignSource.valueOf(source.trim().toUpperCase());
    }

    private String buildVitalSignsDescription(VitalSigns vitalSigns) {
        StringBuilder description = new StringBuilder();

        if (vitalSigns.getBloodPressureSystolic() != null && vitalSigns.getBloodPressureDiastolic() != null) {
            description.append("Blood pressure: ")
                    .append(vitalSigns.getBloodPressureSystolic())
                    .append("/")
                    .append(vitalSigns.getBloodPressureDiastolic())
                    .append(". ");
        }

        if (vitalSigns.getHeartRate() != null) {
            description.append("Heart rate: ")
                    .append(vitalSigns.getHeartRate())
                    .append(". ");
        }

        if (vitalSigns.getWeight() != null) {
            description.append("Weight: ")
                    .append(vitalSigns.getWeight())
                    .append(". ");
        }

        return description.toString().trim();
    }
}