import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import {
  ConsultNote,
  CreateConsultNotePayload,
  CreateMedicationPayload,
  MedicationCatalogItem,
  Patient,
  PatientMedication,
  PatientSearchResult,
  Prediction,
  TimelineEvent,
  VitalSigns,
} from '../models/medical.models';

@Injectable({
  providedIn: 'root',
})
export class PatientApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8081/api';

  searchPatients(query: string) {
    const params = new HttpParams().set('q', query);
    return this.http.get<PatientSearchResult[]>(`${this.baseUrl}/patients/search`, { params });
  }

  getInitialPatients() {
    return this.http.get<PatientSearchResult[]>(`${this.baseUrl}/patients`);
  }

  getPatient(patientId: string) {
    return this.http.get<Patient>(`${this.baseUrl}/patients/${patientId}`);
  }

  getTimeline(patientId: string) {
    return this.http.get<TimelineEvent[]>(`${this.baseUrl}/patients/${patientId}/timeline`);
  }

  getLatestVitals(patientId: string) {
    return this.http.get<VitalSigns[]>(`${this.baseUrl}/patients/${patientId}/vitals/latest`);
  }

  getLatestPredictions(patientId: string) {
    return this.http.get<Prediction[]>(`${this.baseUrl}/patients/${patientId}/predictions/latest`);
  }

  getConsultNotes(patientId: string) {
    return this.http.get<ConsultNote[]>(`${this.baseUrl}/patients/${patientId}/consult-notes`);
  }

  getPatientMedications(patientId: string) {
    return this.http.get<PatientMedication[]>(`${this.baseUrl}/patients/${patientId}/medications`);
  }

  searchMedicationCatalog(query: string) {
    const params = new HttpParams().set('q', query);
    return this.http.get<MedicationCatalogItem[]>(`${this.baseUrl}/medications/search`, { params });
  }

  createConsultNote(patientId: string, payload: CreateConsultNotePayload) {
    return this.http.post<ConsultNote>(
      `${this.baseUrl}/patients/${patientId}/consult-notes`,
      payload,
    );
  }

  createMedication(patientId: string, payload: CreateMedicationPayload) {
    return this.http.post<PatientMedication>(
      `${this.baseUrl}/patients/${patientId}/medications`,
      payload,
    );
  }
}
