import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { map } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  ConsultNote,
  CreateConsultNotePayload,
  CreateMedicationPayload,
  CreateVitalSignsPayload,
  MedicationCatalogSearchItem,
  MedicationCatalogSelection,
  Patient,
  PatientMedication,
  PatientSearchResult,
  Prediction,
  TimelineEvent,
  UpdatePatientAddressPayload,
  VitalSigns,
} from '../models/medical.models';

@Injectable({
  providedIn: 'root',
})
export class PatientApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;

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

  updatePatientAddress(patientId: string, payload: UpdatePatientAddressPayload) {
    return this.http.put<Patient>(`${this.baseUrl}/patients/${patientId}/address`, payload);
  }

  getTimeline(patientId: string) {
    return this.http.get<TimelineEvent[]>(`${this.baseUrl}/patients/${patientId}/timeline`);
  }

  getLatestVitals(patientId: string) {
    return this.http.get<VitalSigns[]>(`${this.baseUrl}/patients/${patientId}/vitals/latest`);
  }

  createVitalSign(patientId: string, payload: CreateVitalSignsPayload) {
    return this.http.post<VitalSigns>(`${this.baseUrl}/patients/${patientId}/vitals`, payload);
  }

  getLatestPredictions(patientId: string) {
    return this.http
      .get<any>(`${this.baseUrl}/patients/${patientId}/predictions/latest`)
      .pipe(
        map((res: any) => this.mapPredictionResponse(res)),
      );
  }

  recalculatePredictions(patientId: string, triggerSource = 'DETAIL_VIEW') {
    return this.http.post<any>(
      `${this.baseUrl}/patients/${patientId}/predictions/calculate`,
      {
        patientId,
        triggerSource,
        predictionTypes: [
          'DIABETES_RISK',
          'CARDIOVASCULAR_RISK',
          'GENERAL_DETERIORATION',
        ],
        features: {},
      },
    ).pipe(
      map((res: any) => this.mapPredictionResponse(res)),
    );
  }

  calculatePredictions(patientId: string, payload: any) {
    return this.http.post<any>(
      `${this.baseUrl}/patients/${patientId}/predictions/calculate`,
      payload,
    ).pipe(
      map((res: any) => this.mapPredictionResponse(res)),
    );
  }

  getConsultNotes(patientId: string) {
    return this.http.get<ConsultNote[]>(`${this.baseUrl}/patients/${patientId}/consult-notes`);
  }

  getPatientMedications(patientId: string) {
    return this.http.get<PatientMedication[]>(`${this.baseUrl}/patients/${patientId}/medications`);
  }

  searchMedicationCatalog(query: string) {
    const params = new HttpParams().set('q', query);
    return this.http.get<MedicationCatalogSearchItem[]>(`${this.baseUrl}/medications/search`, { params });
  }

  getMedicationCatalogSelection(medicationId: string) {
    return this.http.get<MedicationCatalogSelection>(`${this.baseUrl}/medications/${medicationId}`);
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

  private mapPredictionResponse(res: any): Prediction[] {
    const predictions = Array.isArray(res?.predictions)
      ? res.predictions
      : Array.isArray(res)
        ? res
        : [];

    return predictions.map((prediction: any) => ({
      ...prediction,
      mainPrediction: prediction?.mainPrediction ?? prediction?.isMainPrediction ?? false,
    }));
  }
}
