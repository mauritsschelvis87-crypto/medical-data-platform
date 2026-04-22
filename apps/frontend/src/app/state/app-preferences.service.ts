import { Injectable, computed, effect, signal } from '@angular/core';
import { AppLanguage, AppTheme } from '../models/medical.models';

type TranslationMap = Record<string, { en: string; nl: string }>;

const TEXT: TranslationMap = {
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
  noResults: { en: 'No patients found yet.', nl: 'Nog geen patiënten gevonden.' },
  loading: { en: 'Loading data...', nl: 'Gegevens laden...' },
  staleWeight: { en: 'Weight measurement is older than one year.', nl: 'Gewichtsmeting is ouder dan één jaar.' },
  sessionNotice: {
    en: 'Login and logout stay simulated for this demo.',
    nl: 'In- en uitloggen blijft gesimuleerd voor deze demo.'
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
