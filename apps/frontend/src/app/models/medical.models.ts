export interface PatientSearchResult {
  id: string;
  patientNumber: string;
  fullName: string;
  birthDate: string;
}

export interface PatientAddress {
  id: string;
  patientId: string;
  addressLine?: string | null;
  city?: string | null;
  state?: string | null;
  county?: string | null;
  zipCode?: string | null;
}

export interface Patient {
  id: string;
  patientNumber: string;
  sourcePatientId?: string | null;
  firstName: string;
  lastName: string;
  fullName?: string;
  birthDate: string;
  gender: string;
  deceased?: boolean;
  deathDate?: string | null;
  maritalStatus?: string | null;
  race?: string | null;
  ethnicity?: string | null;
  address?: PatientAddress | null;
}

export interface VitalSigns {
  id: string;
  patientId?: string;
  type?: string | null;
  label?: string | null;
  value?: number | null;
  unit?: string | null;
  bloodPressureSystolic?: number | null;
  bloodPressureDiastolic?: number | null;
  heartRate?: number | null;
  temperature?: number | null;
  glucose?: number | null;
  bmi?: number | null;
  weight?: number | null;
  oxygenSaturation?: number | null;
  cholesterol?: number | null;
  measuredAt: string;
  clinicalStatus?: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL' | 'UNKNOWN' | string | null;
  freshnessStatus?: 'CURRENT' | 'AGING' | 'OUTDATED' | 'UNKNOWN' | string | null;
  clinicalMessage?: string | null;
  freshnessMessage?: string | null;
  ageGroup?: string | null;
  interpretationStatus?: string | null;
  interpretationMessage?: string | null;
  contextComplete?: boolean | null;
  editable?: boolean;
  source?: string | null;
  sourceObservationCode?: string | null;
  sourceDescription?: string | null;
}

export interface CreateVitalSignsPayload {
  type: string;
  value: number;
  unit: string;
  measuredAt?: string | null;
  source?: string | null;
  sourceObservationCode?: string | null;
  sourceDescription?: string | null;
}

export interface TimelineEvent {
  id: string;
  eventType: string;
  title: string;
  description?: string | null;
  eventTimestamp: string;
}

export interface Prediction {
  id: string;
  predictionType: string;
  riskLevel: string;
  riskScore: number;
  confidence: number;
  explanation?: string | null;
  mainPrediction: boolean;
  predictionTimestamp: string;
  previousRiskLevel?: string | null;
  riskIncreased: boolean;
  modelVersion?: string | null;
}

export interface ConsultNoteVersion {
  id: string;
  versionNumber: string;
  subjective?: string | null;
  objective?: string | null;
  assessment?: string | null;
  plan?: string | null;
  createdBy: string;
  createdAt: string;
}

export interface ConsultNote {
  id: string;
  status: string;
  createdBy: string;
  createdAt: string;
  currentVersion: ConsultNoteVersion;
}

export interface MedicationCatalogSearchItem {
  id: string;
  name: string;
}

export interface MedicationCatalogSelection {
  id: string;
  name: string;
  defaultDosage?: string | null;
  defaultFrequency?: string | null;
}

export interface PatientMedication {
  id: string;
  medicationName: string;
  dosage: string;
  frequency: string;
  startDate: string;
  endDate?: string | null;
  createdAt?: string | null;
  status: string;
}

export interface CreateConsultNotePayload {
  createdBy: string;
  subjective: string;
  objective: string;
  assessment: string;
  plan: string;
}

export interface CreateMedicationPayload {
  medicationCatalogId: string;
  dosage: string;
  frequency: string;
  startDate: string;
  endDate?: string | null;
  reason: string;
  prescribedBy: string;
}

export interface UpdatePatientAddressPayload {
  addressLine?: string | null;
  city?: string | null;
  state?: string | null;
  county?: string | null;
  zipCode?: string | null;
}

export interface UserProfile {
  name: string;
  shortName: string;
  specialty: string;
  department: string;
  accessLevel: string;
  status: string;
}

export type AppTheme = 'dark' | 'light';
export type AppLanguage = 'en' | 'nl';
