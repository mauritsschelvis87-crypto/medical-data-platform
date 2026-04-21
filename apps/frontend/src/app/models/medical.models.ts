export interface PatientSearchResult {
  id: number;
  patientNumber: string;
  fullName: string;
  birthDate: string;
}

export interface Patient {
  id: number;
  patientNumber: string;
  firstName: string;
  lastName: string;
  birthDate: string;
  gender: string;
  phone?: string | null;
  email?: string | null;
  addressLine?: string | null;
  postalCode?: string | null;
  city?: string | null;
  country?: string | null;
}

export interface VitalSigns {
  id: number;
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
  recordedAt: string;
  source: string;
}

export interface TimelineEvent {
  id: number;
  eventType: string;
  title: string;
  description?: string | null;
  eventTimestamp: string;
}

export interface Prediction {
  id: number;
  predictionType: string;
  riskLevel: string;
  riskScore: number;
  confidence: number;
  explanation?: string | null;
  mainPrediction: boolean;
  predictionTimestamp: string;
  previousRiskLevel?: string | null;
  riskIncreased: boolean;
  requiresConfirmation: boolean;
  confirmed: boolean;
  confirmedAt?: string | null;
  confirmedBy?: string | null;
  modelVersion?: string | null;
  thresholdTriggered: boolean;
}

export interface ConsultNoteVersion {
  id: number;
  versionNumber: string;
  subjective?: string | null;
  objective?: string | null;
  assessment?: string | null;
  plan?: string | null;
  createdBy: string;
  createdAt: string;
}

export interface ConsultNote {
  id: number;
  status: string;
  createdBy: string;
  createdAt: string;
  currentVersion: ConsultNoteVersion;
}

export interface MedicationCatalogItem {
  id: number;
  code: string;
  dutchName: string;
  latinName?: string | null;
  defaultDosage?: string | null;
  advice?: string | null;
}

export interface PatientMedication {
  id: number;
  medicationName: string;
  dosage: string;
  frequency: string;
  startDate: string;
  endDate?: string | null;
  status: string;
}

export interface CreateConsultNotePayload {
  createdBy: string;
  subjective: string;
  objective: string;
  assessment: string;
  plan: string;
  changeReason: string;
}

export interface CreateMedicationPayload {
  medicationCatalogId: number;
  dosage: string;
  frequency: string;
  startDate: string;
  endDate?: string | null;
  reason: string;
  prescribedBy: string;
}

export interface DemoDoctor {
  name: string;
  shortName: string;
  specialty: string;
  department: string;
  accessLevel: string;
  status: string;
}

export type AppTheme = 'dark' | 'light';
export type AppLanguage = 'en' | 'nl';
