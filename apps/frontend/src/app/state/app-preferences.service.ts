import { Injectable, computed, effect, signal } from '@angular/core';
import { AppLanguage, AppTheme } from '../models/medical.models';

type TranslationMap = Record<string, { en: string; nl: string }>;

const TEXT: TranslationMap = {
  appTitle: { en: 'Medical Data Platform', nl: 'Medisch Dataplatform' },
  demoSession: { en: 'Demo session', nl: 'Demosessie' },
  fullAccess: { en: 'Full access', nl: 'Volledige toegang' },
  internalMedicineDepartment: { en: 'Internal Medicine Department', nl: 'Afdeling Interne Geneeskunde' },
  searchTitle: { en: 'Clinical Patient Search', nl: 'Patiënt zoeken' },
  searchSubtitle: {
    en: 'Search by patient number, name, or birth date to open the central patient record.',
    nl: 'Zoek op patiëntnummer, naam of geboortedatum om het centrale dossier te openen.'
  },
  searchPlaceholder: { en: 'Search patient by name, number, or birth date', nl: 'Zoek patiënt op naam, nummer of geboortedatum' },
  quickAccess: { en: 'Quick access', nl: 'Snelle toegang' },
  doctorSignedIn: { en: 'Signed in physician', nl: 'Ingelogde arts' },
  switchUser: { en: 'Switch User', nl: 'Wissel gebruiker' },
  login: { en: 'Login', nl: 'Inloggen' },
  logout: { en: 'Logout', nl: 'Uitloggen' },
  settings: { en: 'Settings', nl: 'Instellingen' },
  language: { en: 'Language', nl: 'Taal' },
  theme: { en: 'Theme', nl: 'Thema' },
  darkMode: { en: 'Dark mode', nl: 'Donkere modus' },
  lightMode: { en: 'Light mode', nl: 'Lichte modus' },
  patientOverview: { en: 'Patient overview', nl: 'Patiëntoverzicht' },
  timeline: { en: 'Timeline', nl: 'Tijdlijn' },
  vitals: { en: 'Vital signs', nl: 'Vitale waarden' },
  activeMedication: { en: 'Medication', nl: 'Medicatie' },
  consultNotes: { en: 'Consult notes', nl: 'Consultnotities' },
  riskSummary: { en: 'Risk summary', nl: 'Risicosamenvatting' },
  explanation: { en: 'Explanation', nl: 'Uitleg' },
  openDrawer: { en: 'Open clinical actions', nl: 'Open klinische acties' },
  closeDrawer: { en: 'Close panel', nl: 'Sluit paneel' },
  soapForm: { en: 'SOAP note', nl: 'SOAP-notitie' },
  medicationOrder: { en: 'Medication order', nl: 'Medicatie voorschrijven' },
  saveNote: { en: 'Save note', nl: 'Notitie opslaan' },
  saveMedication: { en: 'Save medication', nl: 'Medicatie opslaan' },
  save: { en: 'Save', nl: 'Opslaan' },
  saving: { en: 'Saving...', nl: 'Opslaan...' },
  cancel: { en: 'Cancel', nl: 'Annuleren' },
  patient: { en: 'Patient', nl: 'Patiënt' },
  birthDate: { en: 'Birth date', nl: 'Geboortedatum' },
  age: { en: 'Age', nl: 'Leeftijd' },
  address: { en: 'Address', nl: 'Adres' },
  editAddress: { en: 'Edit address', nl: 'Adres bewerken' },
  closeAddressEditor: { en: 'Close address editor', nl: 'Adresbewerking sluiten' },
  street: { en: 'Street', nl: 'Straat' },
  city: { en: 'City', nl: 'Plaats' },
  state: { en: 'State', nl: 'Provincie' },
  county: { en: 'County', nl: 'Gemeente' },
  zip: { en: 'ZIP', nl: 'Postcode' },
  updateValue: { en: 'Update value', nl: 'Waarde bijwerken' },
  calculatedFromSourceMeasurements: {
    en: 'Calculated from source measurements',
    nl: 'Berekend op basis van bronmetingen'
  },
  noPredictionAvailable: { en: 'No prediction available yet.', nl: 'Nog geen voorspelling beschikbaar.' },
  noMedicationRecords: { en: 'No medication records available yet.', nl: 'Nog geen medicatieregels beschikbaar.' },
  clinicalActionTabs: { en: 'Clinical action tabs', nl: 'Tabs voor klinische acties' },
  medicineTab: { en: 'Medicine', nl: 'Medicatie' },
  historyTab: { en: 'History', nl: 'Historie' },
  subjective: { en: 'Subjective', nl: 'Subjectief' },
  objective: { en: 'Objective', nl: 'Objectief' },
  assessment: { en: 'Assessment', nl: 'Beoordeling' },
  plan: { en: 'Plan', nl: 'Plan' },
  medication: { en: 'Medication', nl: 'Medicatie' },
  dosage: { en: 'Dosage', nl: 'Dosering' },
  frequency: { en: 'Frequency', nl: 'Frequentie' },
  startDate: { en: 'Start date', nl: 'Startdatum' },
  endDate: { en: 'End date', nl: 'Einddatum' },
  prescriptionReason: { en: 'Prescription reason', nl: 'Voorschrijfreden' },
  currentMedication: { en: 'Current medication', nl: 'Huidige medicatie' },
  entries: { en: 'entries', nl: 'items' },
  ongoing: { en: 'ongoing', nl: 'doorlopend' },
  unknown: { en: 'Unknown', nl: 'Onbekend' },
  noMedicationSaved: { en: 'No medication saved yet.', nl: 'Nog geen medicatie opgeslagen.' },
  noClinicalNotesSaved: { en: 'No clinical notes saved yet.', nl: 'Nog geen klinische notities opgeslagen.' },
  openNoteDetails: { en: 'Open note details', nl: 'Notitiedetails openen' },
  current: { en: 'Current', nl: 'Actueel' },
  outdated: { en: 'Outdated', nl: 'Verouderd' },
  stopped: { en: 'Stopped', nl: 'Gestopt' },
  inactive: { en: 'Inactive', nl: 'Inactief' },
  active: { en: 'Active', nl: 'Actief' },
  high: { en: 'High', nl: 'Hoog' },
  medium: { en: 'Medium', nl: 'Middel' },
  low: { en: 'Low', nl: 'Laag' },
  critical: { en: 'Critical', nl: 'Kritiek' },
  veryHigh: { en: 'Very high', nl: 'Zeer hoog' },
  outOfRange: { en: 'Out of range', nl: 'Buiten bereik' },
  aging: { en: 'Aging', nl: 'Verouderend' },
  insufficientContext: { en: 'Insufficient context', nl: 'Onvoldoende context' },
  notApplicable: { en: 'Not applicable', nl: 'Niet van toepassing' },
  notAvailable: { en: 'Not available', nl: 'Niet beschikbaar' },
  prediction: { en: 'Prediction', nl: 'Voorspelling' },
  predictionCurrentTitle: {
    en: 'Prediction is based on current vital sign measurements.',
    nl: 'De voorspelling is gebaseerd op actuele metingen van vitale waarden.'
  },
  predictionOutdatedTitle: {
    en: 'Prediction is based on older measurements or was calculated before the latest vital signs.',
    nl: 'De voorspelling is gebaseerd op oudere metingen of is berekend vóór de laatste vitale waarden.'
  },
  noMatchingVitalSignsAvailable: {
    en: 'No matching vital signs available.',
    nl: 'Geen overeenkomende vitale waarden beschikbaar.'
  },
  missing: { en: 'missing', nl: 'ontbreekt' },
  vitalSign: { en: 'Vital sign', nl: 'Vitale waarde' },
  bloodPressure: { en: 'Blood pressure', nl: 'Bloeddruk' },
  heartRate: { en: 'Heart rate', nl: 'Hartslag' },
  temperatureLabel: { en: 'Temperature', nl: 'Temperatuur' },
  glucoseLabel: { en: 'Glucose', nl: 'Glucose' },
  bmiLabel: { en: 'BMI', nl: 'BMI' },
  weightHeight: { en: 'Weight & height', nl: 'Gewicht en lengte' },
  weightLabel: { en: 'Weight', nl: 'Gewicht' },
  heightLabel: { en: 'Height', nl: 'Lengte' },
  oxygenSaturation: { en: 'O2 saturation', nl: 'O2-saturatie' },
  cholesterolLabel: { en: 'Cholesterol', nl: 'Cholesterol' },
  systolic: { en: 'Systolic', nl: 'Systolisch' },
  diastolic: { en: 'Diastolic', nl: 'Diastolisch' },
  male: { en: 'Male', nl: 'Man' },
  female: { en: 'Female', nl: 'Vrouw' },
  other: { en: 'Other', nl: 'Anders' },
  noResults: { en: 'No patients found yet.', nl: 'Nog geen patiënten gevonden.' },
  loading: { en: 'Loading data...', nl: 'Gegevens laden...' },
  staleWeight: { en: 'Weight measurement is older than one year.', nl: 'Gewichtsmeting is ouder dan één jaar.' },
  sessionNotice: {
    en: 'For this demo, changes stay simulated.',
    nl: 'Voor deze demo blijven wijzigingen gesimuleerd.'
  }
};

@Injectable({
  providedIn: 'root'
})
export class AppPreferencesService {
  readonly theme = signal<AppTheme>('dark');
  readonly language = signal<AppLanguage>('en');
  readonly currentDictionary = computed(() => this.language());

  constructor() {
    effect(() => {
      document.documentElement.setAttribute('data-theme', this.theme());
    });
  }

  setTheme(theme: AppTheme): void {
    this.theme.set(theme);
  }

  toggleTheme(): void {
    this.theme.set(this.theme() === 'dark' ? 'light' : 'dark');
  }

  setLanguage(language: AppLanguage): void {
    this.language.set(language);
  }

  t(key: keyof typeof TEXT): string {
    return TEXT[key][this.language()];
  }
}
