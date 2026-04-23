package com.mauri.backend.service;

import com.mauri.backend.dto.dataset.CreateDatasetImportRequest;
import com.mauri.backend.dto.dataset.DatasetImportDto;
import com.mauri.backend.entity.DatasetImport;
import com.mauri.backend.entity.Patient;
import com.mauri.backend.entity.PatientAddress;
import com.mauri.backend.entity.VitalSigns;
import com.mauri.backend.enums.DatasetImportStatus;
import com.mauri.backend.repository.DatasetImportRepository;
import com.mauri.backend.repository.PatientAddressRepository;
import com.mauri.backend.repository.PatientRepository;
import com.mauri.backend.repository.VitalSignsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DatasetImportServiceIntegrationTest {

    @Autowired
    private DatasetImportService datasetImportService;

    @Autowired
    private DatasetImportRepository datasetImportRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PatientAddressRepository patientAddressRepository;

    @Autowired
    private VitalSignsRepository vitalSignsRepository;

    @Test
    void importNormalizedDatasetPersistsAllCoreRecords(@TempDir Path tempDir) throws IOException {
        write(tempDir.resolve("patient.csv"), """
                patientNumber,sourcePatientId,firstName,lastName,fullName,birthDate,gender,deceased,deathDate,maritalStatus,race,ethnicity
                P-1001,SRC-001,Ana,Jansen,Ana Jansen,1980-05-12,FEMALE,false,,MARRIED,WHITE,NON_HISPANIC
                """);
        write(tempDir.resolve("patient_address.csv"), """
                patientId,addressLine,city,state,county,zipCode
                SRC-001,Main Street 1,Amsterdam,Noord-Holland,Amsterdam,1000AA
                """);
        write(tempDir.resolve("vital_signs.csv"), """
                patientId,type,value,unit,measuredAt,sourceObservationCode,sourceDescription
                SRC-001,HEART_RATE,72,bpm,2026-04-21T10:15:30,8867-4,Heart rate
                SRC-001,BODY_TEMPERATURE,36.8,C,2026-04-21T10:15:30,8310-5,Body temperature
                """);
        write(tempDir.resolve("import_summary.csv"), """
                recordsReceived,recordsProcessed,recordsFailed,patientCount,patientAddressCount,vitalSignsCount,skippedRecords,validationSummary
                4,4,0,1,1,2,0,summary-ok
                """);

        CreateDatasetImportRequest request = new CreateDatasetImportRequest();
        request.setSourceName("synthea-normalized");
        request.setDatasetType("NORMALIZED_MEDICAL_DATA");
        request.setSourceDirectoryPath(tempDir.toString());
        request.setNotes("integration-test");

        DatasetImportDto response = datasetImportService.importNormalizedDataset(request);

        List<DatasetImport> imports = datasetImportRepository.findAll();
        List<Patient> patients = patientRepository.findAll();
        List<PatientAddress> addresses = patientAddressRepository.findAll();
        List<VitalSigns> vitalSigns = vitalSignsRepository.findAll();

        assertThat(response.getStatus()).isEqualTo(DatasetImportStatus.SUCCEEDED.name());
        assertThat(response.getPatientCount()).isEqualTo(1);
        assertThat(response.getPatientAddressCount()).isEqualTo(1);
        assertThat(response.getVitalSignsCount()).isEqualTo(2);
        assertThat(response.getValidationSummary()).contains("summary-ok");

        assertThat(imports).hasSize(1);
        assertThat(imports.getFirst().getStatus()).isEqualTo(DatasetImportStatus.SUCCEEDED);
        assertThat(imports.getFirst().getNotes()).contains("integration-test");

        assertThat(patients).hasSize(1);
        assertThat(patients.getFirst().getSourcePatientId()).isEqualTo("SRC-001");
        assertThat(addresses).hasSize(1);
        assertThat(addresses.getFirst().getPatient().getId()).isEqualTo(patients.getFirst().getId());
        assertThat(vitalSigns).hasSize(2);
        assertThat(vitalSigns).allMatch(v -> v.getPatient().getId().equals(patients.getFirst().getId()));
    }

    private void write(Path path, String contents) throws IOException {
        Files.writeString(path, contents);
    }
}
