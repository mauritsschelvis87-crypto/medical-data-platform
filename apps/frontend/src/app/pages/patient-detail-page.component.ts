import { CommonModule } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { catchError, combineLatest, debounceTime, distinctUntilChanged, forkJoin, of, switchMap } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { PatientApiService } from '../api/patient-api.service';
import {
  ConsultNote,
  MedicationCatalogSearchItem,
  Patient,
  PatientAddress,
  PatientMedication,
  Prediction,
  TimelineEvent,
  UpdatePatientAddressPayload,
  VitalSigns,
} from '../models/medical.models';
import { AppNoticeService } from '../state/app-notice.service';
import { AppPreferencesService } from '../state/app-preferences.service';

type VitalMetricKey =
  | 'bloodPressure'
  | 'heartRate'
  | 'temperature'
  | 'glucose'
  | 'bmi'
  | 'weight'
  | 'oxygenSaturation'
  | 'cholesterol';

interface VitalMetricConfig {
  key: VitalMetricKey;
  label: string;
  primaryType: string;
  secondaryType?: string;
  primaryUnit: string;
  secondaryUnit?: string;
  primaryLabel?: string;
  secondaryLabel?: string;
  editable?: boolean;
}

@Component({
  selector: 'app-patient-detail-page',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './patient-detail-page.component.html',
  styleUrl: './patient-detail-page.component.scss',
})
export class PatientDetailPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(PatientApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(FormBuilder);
  private readonly noticeService = inject(AppNoticeService);

  protected readonly preferences = inject(AppPreferencesService);

  protected readonly loading = signal(true);
  protected readonly drawerOpen = signal(false);
  protected readonly actionTab = signal<'soap' | 'medicine' | 'history'>('soap');
  protected readonly editingAddress = signal(false);
  protected readonly medicationLookup = signal<MedicationCatalogSearchItem[]>([]);
  protected readonly medicationSuggestionsOpen = signal(false);
  protected readonly selectedMedicationId = signal<string | null>(null);
  protected readonly selectedMedicationName = signal<string | null>(null);
  protected readonly submittingAddress = signal(false);
  protected readonly submittingNote = signal(false);
  protected readonly submittingMedication = signal(false);
  protected readonly editingVitalKey = signal<VitalMetricKey | null>(null);
  protected readonly savingVitalKey = signal<VitalMetricKey | null>(null);

  protected readonly patient = signal<Patient | null>(null);
  protected readonly timeline = signal<TimelineEvent[]>([]);
  protected readonly vitals = signal<VitalSigns[]>([]);
  protected readonly predictions = signal<Prediction[]>([]);
  protected readonly consultNotes = signal<ConsultNote[]>([]);
  protected readonly medications = signal<PatientMedication[]>([]);

  private readonly predictionTypesForSummary = [
    'CARDIOVASCULAR_RISK',
    'DIABETES_RISK',
    'GENERAL_DETERIORATION',
    'SEPSIS_RISK',
    'RESPIRATORY_RISK',
  ] as const;

  protected readonly vitalMetricConfigs: VitalMetricConfig[] = [
    {
      key: 'bloodPressure',
      label: 'Blood pressure',
      primaryType: 'BLOOD_PRESSURE_SYSTOLIC',
      secondaryType: 'BLOOD_PRESSURE_DIASTOLIC',
      primaryUnit: 'mmHg',
      secondaryUnit: 'mmHg',
      primaryLabel: 'Systolic',
      secondaryLabel: 'Diastolic',
    },
    { key: 'heartRate', label: 'Heart rate', primaryType: 'HEART_RATE', primaryUnit: 'bpm' },
    { key: 'temperature', label: 'Temperature', primaryType: 'BODY_TEMPERATURE', primaryUnit: 'C' },
    { key: 'glucose', label: 'Glucose', primaryType: 'GLUCOSE', primaryUnit: 'mmol/L' },
    { key: 'bmi', label: 'BMI', primaryType: 'BMI', primaryUnit: 'kg/m2', editable: false },
    {
      key: 'weight',
      label: 'Weight & height',
      primaryType: 'WEIGHT',
      secondaryType: 'HEIGHT',
      primaryUnit: 'kg',
      secondaryUnit: 'cm',
      primaryLabel: 'Weight',
      secondaryLabel: 'Height',
    },
    { key: 'oxygenSaturation', label: 'O2 saturation', primaryType: 'OXYGEN_SATURATION', primaryUnit: '%' },
    { key: 'cholesterol', label: 'Cholesterol', primaryType: 'CHOLESTEROL', primaryUnit: 'mmol/L' },
  ];

  protected readonly sortedVitalMetricConfigs = computed(() =>
    this.vitalMetricConfigs
      .map((config, index) => ({ config, index }))
      .sort((left, right) => {
        const clinicalDifference =
          this.getRiskPriority(this.getRawClinicalStatusForMetric(right.config)) -
          this.getRiskPriority(this.getRawClinicalStatusForMetric(left.config));
        if (clinicalDifference !== 0) {
          return clinicalDifference;
        }

        const freshnessDifference =
          this.getFreshnessPriority(this.getFreshnessStatusForMetric(right.config)) -
          this.getFreshnessPriority(this.getFreshnessStatusForMetric(left.config));
        if (freshnessDifference !== 0) {
          return freshnessDifference;
        }

        return left.index - right.index;
      })
      .map(({ config }) => config),
  );

  protected readonly noteForm = this.fb.nonNullable.group({
    subjective: ['', Validators.required],
    objective: ['', Validators.required],
    assessment: ['', Validators.required],
    plan: ['', Validators.required],
  });

  protected readonly medicationForm = this.fb.nonNullable.group({
    medicationCatalogId: ['', Validators.required],
    medicationQuery: [''],
    dosage: ['', Validators.required],
    frequency: ['', Validators.required],
    startDate: [new Date().toISOString().slice(0, 10), Validators.required],
    endDate: [''],
    reason: ['', Validators.required],
  });

  protected readonly addressForm = this.fb.nonNullable.group({
    addressLine: [''],
    city: [''],
    state: [''],
    county: [''],
    zipCode: [''],
  });

  protected readonly vitalEditForm = this.fb.nonNullable.group({
    primaryValue: ['', Validators.required],
    secondaryValue: [''],
  });

  protected readonly mainPrediction = computed(
    () =>
      this.predictions().find((prediction) => prediction.mainPrediction) ??
      this.predictions()[0] ??
      null,
  );

  protected readonly otherPredictions = computed(() =>
    this.predictions().filter((prediction) => prediction.id !== this.mainPrediction()?.id),
  );

  protected readonly sortedPredictions = computed(() =>
    this.dedupePredictionsByType(this.predictions())
      .filter((prediction) => this.shouldDisplayPrediction(prediction))
      .sort((left, right) => this.comparePredictionsByRisk(left, right)),
  );

  protected readonly sortedConsultNotes = computed(() =>
    [...this.consultNotes()].sort(
      (left, right) =>
        this.getConsultNoteSortTimestamp(right) - this.getConsultNoteSortTimestamp(left),
    ),
  );

  protected readonly sortedMedications = computed(() =>
    [...this.medications()].sort(
      (left, right) =>
        this.getMedicationSortTimestamp(right) - this.getMedicationSortTimestamp(left),
    ),
  );

  constructor() {
    this.route.paramMap
      .pipe(
        switchMap((params) => {
          const patientId = params.get('patientId');
          if (!patientId) {
            return of(null);
          }

          this.loading.set(true);

          return forkJoin({
            patient: this.api.getPatient(patientId),
            timeline: this.api.getTimeline(patientId).pipe(catchError(() => of([]))),
            vitals: this.api.getLatestVitals(patientId).pipe(catchError(() => of([]))),
            predictions: this.api.getLatestPredictions(patientId).pipe(catchError(() => of([]))),
            consultNotes: this.api.getConsultNotes(patientId).pipe(catchError(() => of([]))),
            medications: this.api.getPatientMedications(patientId).pipe(catchError(() => of([]))),
          });
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (data) => {
          if (!data) {
            this.loading.set(false);
            return;
          }

          this.patient.set(data.patient);
          this.timeline.set(data.timeline);
          this.vitals.set(data.vitals);
          this.predictions.set(data.predictions);
          this.consultNotes.set(data.consultNotes);
          this.medications.set(data.medications);
          this.patchAddressForm(data.patient.address);
          this.loading.set(false);
          this.ensurePredictionsAvailable(data.patient.id, data.vitals, data.predictions);
        },
        error: () => {
          this.loading.set(false);
        },
      });

    this.medicationForm.controls.medicationQuery.valueChanges
      .pipe(
        debounceTime(250),
        distinctUntilChanged(),
        switchMap((query) => {
          const normalizedQuery = (query ?? '').trim();

          if (!this.matchesSelectedMedicationQuery(normalizedQuery)) {
            this.selectedMedicationId.set(null);
            this.selectedMedicationName.set(null);
            this.medicationForm.patchValue({ medicationCatalogId: '' }, { emitEvent: false });
          }

          if (!normalizedQuery || this.matchesSelectedMedicationQuery(normalizedQuery)) {
            return of<MedicationCatalogSearchItem[]>([]);
          }

          return this.api.searchMedicationCatalog(normalizedQuery).pipe(catchError(() => of([])));
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((items) => this.syncMedicationSuggestions(items));
  }

  protected toggleDrawer(): void {
    this.drawerOpen.update((open) => !open);
  }

  protected setActionTab(tab: 'soap' | 'medicine' | 'history'): void {
    this.actionTab.set(tab);
  }

  protected toggleAddressEdit(): void {
    const patient = this.patient();
    if (!patient) {
      return;
    }

    if (!this.editingAddress()) {
      this.patchAddressForm(patient.address);
    }

    this.editingAddress.update((open) => !open);
  }

  protected cancelAddressEdit(): void {
    this.patchAddressForm(this.patient()?.address);
    this.editingAddress.set(false);
  }

  protected saveAddress(): void {
    const patient = this.patient();
    if (!patient) {
      return;
    }

    this.submittingAddress.set(true);
    this.api
      .updatePatientAddress(patient.id, this.buildAddressPayload())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updatedPatient) => {
          this.patient.set(updatedPatient);
          this.patchAddressForm(updatedPatient.address);
          this.editingAddress.set(false);
          this.submittingAddress.set(false);
          this.noticeService.show(this.preferences.t('sessionNotice'));
        },
        error: () => this.submittingAddress.set(false),
      });
  }

  protected selectMedication(item: MedicationCatalogSearchItem): void {
    this.api
      .getMedicationCatalogSelection(item.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((selection) => {
        const displayName = this.getMedicationDisplayName(selection);
        this.selectedMedicationId.set(selection.id);
        this.selectedMedicationName.set(displayName);
        this.medicationLookup.set([]);
        this.medicationSuggestionsOpen.set(false);
        this.medicationForm.patchValue(
          {
            medicationCatalogId: selection.id,
            medicationQuery: displayName,
            dosage: selection.defaultDosage ?? '',
            frequency: selection.defaultFrequency ?? '',
          },
          { emitEvent: false },
        );
      });
  }

  protected getMedicationDisplayName(item: { name: string }): string {
    return item.name;
  }

  protected hasMedicationSuggestions(): boolean {
    return this.medicationSuggestionsOpen() && this.medicationLookup().length > 0;
  }

  protected isSelectedMedication(item: MedicationCatalogSearchItem): boolean {
    return this.selectedMedicationId() === item.id;
  }

  protected saveMedicationLabel(): string {
    return this.submittingMedication()
      ? this.preferences.language() === 'nl'
        ? 'Medicatie wordt opgeslagen...'
        : 'Saving medication...'
      : this.preferences.t('saveMedication');
  }

  protected getConsultNotePreview(value?: string | null): string {
    if (!value?.trim()) {
      return '—';
    }

    const normalized = value.replace(/\s+/g, ' ').trim();
    return normalized.length > 180 ? `${normalized.slice(0, 177)}...` : normalized;
  }

  protected saveNote(): void {
    const patient = this.patient();
    if (!patient || this.noteForm.invalid) {
      this.noteForm.markAllAsTouched();
      return;
    }

    this.submittingNote.set(true);
    this.api
      .createConsultNote(patient.id, {
        createdBy: 'Dr. Jonathan Hyde',
        subjective: this.noteForm.controls.subjective.value,
        objective: this.noteForm.controls.objective.value,
        assessment: this.noteForm.controls.assessment.value,
        plan: this.noteForm.controls.plan.value,
      })
      .pipe(
        switchMap((savedNote) => {
          const fallbackConsultNotes = this.mergeConsultNoteIntoList(this.consultNotes(), savedNote);
          this.consultNotes.set(fallbackConsultNotes);

          return forkJoin({
            consultNotes: this.api.getConsultNotes(patient.id).pipe(
              catchError(() => of(fallbackConsultNotes)),
            ),
            timeline: this.api.getTimeline(patient.id).pipe(
              catchError(() => of(this.timeline())),
            ),
            predictions: this.api.getLatestPredictions(patient.id).pipe(
              catchError(() => of(this.predictions())),
            ),
          });
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: ({ consultNotes, timeline, predictions }) => {
          this.consultNotes.set(consultNotes);
          this.timeline.set(timeline);
          this.predictions.set(predictions);
          this.actionTab.set('history');
          this.noteForm.reset({
            subjective: '',
            objective: '',
            assessment: '',
            plan: '',
          });
          this.submittingNote.set(false);
        },
        error: () => this.submittingNote.set(false),
      });
  }

  protected saveMedication(): void {
    const patient = this.patient();
    if (!patient || this.medicationForm.invalid) {
      this.medicationForm.markAllAsTouched();
      return;
    }

    this.submittingMedication.set(true);
    this.api
      .createMedication(patient.id, {
        medicationCatalogId: this.medicationForm.controls.medicationCatalogId.value,
        dosage: this.medicationForm.controls.dosage.value,
        frequency: this.medicationForm.controls.frequency.value,
        startDate: this.medicationForm.controls.startDate.value,
        endDate: this.medicationForm.controls.endDate.value || null,
        reason: this.medicationForm.controls.reason.value,
        prescribedBy: 'Dr. Jonathan Hyde',
      })
      .pipe(
        switchMap((savedMedication) => {
          const fallbackMedications = this.mergeMedicationIntoList(this.medications(), savedMedication);
          this.medications.set(fallbackMedications);
          this.noticeService.show(this.getMedicationSavedNotice());

          return forkJoin({
            medications: this.api.getPatientMedications(patient.id).pipe(
              catchError(() => of(fallbackMedications)),
            ),
            timeline: this.api.getTimeline(patient.id).pipe(
              catchError(() => of(this.timeline())),
            ),
            predictions: this.api.getLatestPredictions(patient.id).pipe(
              catchError(() => of(this.predictions())),
            ),
          });
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: ({ medications, timeline, predictions }) => {
          this.medications.set(medications);
          this.timeline.set(timeline);
          this.predictions.set(predictions);
          this.actionTab.set('medicine');
          this.medicationForm.reset({
            medicationCatalogId: '',
            medicationQuery: '',
            dosage: '',
            frequency: '',
            startDate: new Date().toISOString().slice(0, 10),
            endDate: '',
            reason: '',
          });
          this.selectedMedicationId.set(null);
          this.selectedMedicationName.set(null);
          this.medicationLookup.set([]);
          this.medicationSuggestionsOpen.set(false);
          this.submittingMedication.set(false);
        },
        error: () => {
          this.submittingMedication.set(false);
          this.noticeService.show(this.getMedicationSaveErrorNotice());
        },
      });
  }

  protected getVitalRow(type: string): VitalSigns | null {
    return this.vitals().find((row) => row.type === type) ?? null;
  }

  protected getVitalValue(metric: VitalMetricKey): string {
    switch (metric) {
      case 'bloodPressure': {
        const systolic = this.getVitalRow('BLOOD_PRESSURE_SYSTOLIC')?.value;
        const diastolic = this.getVitalRow('BLOOD_PRESSURE_DIASTOLIC')?.value;
        if (systolic == null && diastolic == null) {
          return '—';
        }
        return `${systolic ?? '—'}/${diastolic ?? '—'}`;
      }
      case 'heartRate':
        return this.formatNumericValue(this.getVitalRow('HEART_RATE')?.value, ' bpm');
      case 'temperature':
        return this.formatNumericValue(this.getVitalRow('BODY_TEMPERATURE')?.value, ' °C');
      case 'glucose':
        return this.formatNumericValue(this.getVitalRow('GLUCOSE')?.value);
      case 'bmi':
        return this.formatNumericValue(this.getVitalRow('BMI')?.value);
      case 'weight':
        return this.getWeightAndHeightValue();
      case 'oxygenSaturation':
        return this.formatNumericValue(this.getVitalRow('OXYGEN_SATURATION')?.value, '%');
      case 'cholesterol':
        return this.formatNumericValue(this.getVitalRow('CHOLESTEROL')?.value);
      default:
        return '—';
    }
  }

  protected getVitalMeasuredAt(type: string): string | null {
    return this.getVitalRow(type)?.measuredAt ?? null;
  }

  protected getVitalMeasuredAtForMetric(config: VitalMetricConfig): string | null {
    return this.getLatestVitalRowForMetric(config)?.measuredAt ?? null;
  }

  protected getClinicalStatusForMetric(config: VitalMetricConfig): string {
    return this.getClinicalDisplayStatus(this.getRawClinicalStatusForMetric(config));
  }

  protected getFreshnessStatusForMetric(config: VitalMetricConfig): string {
    const rows = this.getVitalRowsForMetric(config);
    if (rows.length === 0) {
      return 'UNKNOWN';
    }

    return rows.every((row) => this.normalizeFreshnessStatus(row.freshnessStatus) === 'CURRENT')
      ? 'CURRENT'
      : 'OUTDATED';
  }

  protected getFreshnessLabelForMetric(config: VitalMetricConfig): string {
    return this.getFreshnessStatusForMetric(config) === 'CURRENT' ? 'CURRENT' : 'OUTDATED';
  }

  protected getClinicalMessageForMetric(config: VitalMetricConfig): string {
    return [this.getVitalRow(config.primaryType), config.secondaryType ? this.getVitalRow(config.secondaryType) : null]
      .filter((row): row is VitalSigns => !!row)
      .map((row) => row.interpretationMessage ?? row.clinicalMessage)
      .filter((message): message is string => !!message)
      .join(' ');
  }

  protected shouldShowClinicalStatus(config: VitalMetricConfig): boolean {
    return this.getClinicalStatusForMetric(config).length > 0;
  }

  protected getClinicalSummaryForMetric(config: VitalMetricConfig): string {
    return this.getClinicalMessageForMetric(config);
  }

  protected getFreshnessMessageForMetric(config: VitalMetricConfig): string {
    return [this.getVitalRow(config.primaryType), config.secondaryType ? this.getVitalRow(config.secondaryType) : null]
      .filter((row): row is VitalSigns => !!row)
      .map((row) => row.freshnessMessage)
      .filter((message): message is string => !!message)
      .join(' ');
  }

  protected isVitalEditable(config: VitalMetricConfig): boolean {
    if (config.editable === false) {
      return false;
    }
    const primary = this.getVitalRow(config.primaryType);
    return primary?.editable ?? true;
  }

  protected startVitalEdit(config: VitalMetricConfig): void {
    if (!this.isVitalEditable(config)) {
      return;
    }

    this.editingVitalKey.set(config.key);
    this.vitalEditForm.reset({
      primaryValue: this.getVitalRow(config.primaryType)?.value?.toString() ?? '',
      secondaryValue: config.secondaryType ? this.getVitalRow(config.secondaryType)?.value?.toString() ?? '' : '',
    });
  }

  protected cancelVitalEdit(): void {
    this.editingVitalKey.set(null);
    this.vitalEditForm.reset({ primaryValue: '', secondaryValue: '' });
  }

  protected saveVitalMetric(config: VitalMetricConfig): void {
    const patient = this.patient();
    if (!patient) {
      return;
    }

    const primaryValue = Number(this.vitalEditForm.controls.primaryValue.value);
    const secondaryValue = Number(this.vitalEditForm.controls.secondaryValue.value);
    if (!Number.isFinite(primaryValue) || (config.secondaryType && !Number.isFinite(secondaryValue))) {
      this.vitalEditForm.markAllAsTouched();
      return;
    }

    const requests = [
      this.api.createVitalSign(patient.id, {
        type: config.primaryType,
        value: primaryValue,
        unit: config.primaryUnit,
        source: 'MANUAL',
      }),
    ];

    if (config.secondaryType && config.secondaryUnit) {
      requests.push(
        this.api.createVitalSign(patient.id, {
          type: config.secondaryType,
          value: secondaryValue,
          unit: config.secondaryUnit,
          source: 'MANUAL',
        }),
      );
    }

    this.savingVitalKey.set(config.key);
    forkJoin(requests)
      .pipe(
        switchMap(() =>
          forkJoin({
            vitals: this.api.getLatestVitals(patient.id),
            predictions: this.api.getLatestPredictions(patient.id),
            timeline: this.api.getTimeline(patient.id),
          }),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: ({ vitals, predictions, timeline }) => {
          this.vitals.set(vitals);
          this.predictions.set(predictions);
          this.timeline.set(timeline);
          this.savingVitalKey.set(null);
          this.cancelVitalEdit();
        },
        error: () => this.savingVitalKey.set(null),
      });
  }

  protected patientStreetLine(): string {
    const address = this.patient()?.address;
    return address?.addressLine?.trim() || 'Not available';
  }

  protected patientCityLine(): string {
    const address = this.patient()?.address;
    if (!address) {
      return 'Not available';
    }

    const parts = [address.city, address.state, address.zipCode].filter((part) => !!part?.trim());
    return parts.length > 0 ? parts.join(', ') : 'Not available';
  }

  protected isMedicationActive(medication: PatientMedication): boolean {
    return (medication.status || '').toLowerCase() === 'active';
  }

  protected medicationStatusLabel(medication: PatientMedication): string {
    return this.isMedicationActive(medication)
      ? 'CURRENT'
      : (medication.status || 'INACTIVE').replaceAll('_', ' ');
  }

  protected medicationStatusDate(medication: PatientMedication): string {
    return this.formatDateOnly(medication.endDate || medication.startDate);
  }

  protected medicationPanelTitle(): string {
    return 'Medication';
  }

  protected isMedicationTimelineEventActive(event: TimelineEvent): boolean {
    const description = (event.description || '').toLowerCase();
    return (
      (event.eventType || '').toLowerCase().includes('medication') &&
      !description.includes('inactive') &&
      !description.includes('stopped') &&
      !description.includes('ended')
    );
  }

  protected isMedicationTimelineEventInactive(event: TimelineEvent): boolean {
    const description = (event.description || '').toLowerCase();
    return (
      (event.eventType || '').toLowerCase().includes('medication') &&
      (description.includes('inactive') ||
        description.includes('stopped') ||
        description.includes('ended'))
    );
  }

  protected isNoteTimelineEvent(event: TimelineEvent): boolean {
    const type = (event.eventType || '').toLowerCase();
    return type.includes('consult') || type.includes('note');
  }

  protected formatPredictionType(prediction: Prediction): string {
    const formattedType = this.removeFallbackText(prediction.predictionType.replaceAll('_', ' '));
    return formattedType || 'Prediction';
  }

  protected formatRiskLevel(level?: string | null): string {
    return (level || 'UNKNOWN').replaceAll('_', ' ');
  }

  protected normalizeRiskLevel(level?: string | null): string {
    const normalizedLevel = (level || 'UNKNOWN').toUpperCase();
    return normalizedLevel === 'MED' ? 'MEDIUM' : normalizedLevel;
  }

  protected getMetricCardTone(config: VitalMetricConfig): string | null {
    return this.getClinicalToneFromStatus(this.getRawClinicalStatusForMetric(config));
  }

  protected getMetricValueLine(config: VitalMetricConfig): string {
    const value = this.getVitalValue(config.key);
    const status = this.getClinicalStatusForMetric(config);
    return status ? `${status} | ${value}` : value;
  }

  protected getPredictionCardTone(prediction: Prediction): string | null {
    return this.getPredictionTone(this.normalizeRiskLevel(prediction.riskLevel));
  }

  protected getPredictionRiskLine(prediction: Prediction): string {
    const level = this.getPredictionDisplayLevel(prediction);
    const score = this.formatRiskScore(prediction.riskScore);
    return level ? `${level} | ${score}` : score;
  }

  protected cleanPredictionExplanation(explanation?: string | null): string {
    return this.capitalizeLeadingLetter(this.removeFallbackText(explanation || ''));
  }

  protected predictionDataFreshnessStatus(prediction: Prediction): 'CURRENT' | 'OUTDATED' {
    const sourceTypes = this.getPredictionSourceTypes(prediction);
    const sourceRows = this.getPredictionSourceVitals(prediction);
    const statuses = sourceRows.map((row) => row.freshnessStatus ?? 'UNKNOWN');
    if (sourceRows.length !== sourceTypes.length || statuses.some((status) => status !== 'CURRENT')) {
      return 'OUTDATED';
    }

    const latestMeasurementTimestamp = this.getLatestMeasurementTimestamp(sourceRows);
    const predictionTimestamp = new Date(prediction.predictionTimestamp).getTime();

    return predictionTimestamp >= latestMeasurementTimestamp ? 'CURRENT' : 'OUTDATED';
  }

  protected predictionDataFreshnessLabel(prediction: Prediction): string {
    return this.predictionDataFreshnessStatus(prediction);
  }

  protected predictionDataFreshnessTitle(prediction: Prediction): string {
    return this.predictionDataFreshnessStatus(prediction) === 'OUTDATED'
      ? 'Prediction is based on older measurements or was calculated before the latest vital signs.'
      : 'Prediction is based on current vital sign measurements.';
  }

  protected getPredictionSourceSummary(prediction: Prediction): string {
    return this.getPredictionSourceLines(prediction).join(', ');
  }

  protected getPredictionSourceLines(prediction: Prediction): string[] {
    const sourceTypes = this.getPredictionSourceTypes(prediction);
    const sourceRows = this.getPredictionSourceVitals(prediction);
    if (sourceRows.length === 0) {
      return ['No matching vital signs available.'];
    }

    const missingSources = sourceTypes
      .filter((type) => !sourceRows.some((row) => row.type === type))
      .map((type) => `${this.formatVitalType(type)} missing`);
    return [...sourceRows.map((row) => this.formatVitalSource(row)), ...missingSources];
  }

  protected formatDate(value?: string | null): string {
    if (!value) {
      return 'Not available';
    }

    return new Intl.DateTimeFormat(this.preferences.language() === 'en' ? 'en-GB' : 'nl-NL', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }).format(new Date(value));
  }

  protected formatDateOnly(value?: string | null): string {
    if (!value) {
      return 'Not available';
    }

    return new Intl.DateTimeFormat(this.preferences.language() === 'en' ? 'en-GB' : 'nl-NL', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
    }).format(new Date(value));
  }

  protected patientAge(): number | null {
    const patient = this.patient();
    if (!patient?.birthDate) {
      return null;
    }

    const today = new Date();
    const birth = new Date(patient.birthDate);
    let age = today.getFullYear() - birth.getFullYear();
    const monthDifference = today.getMonth() - birth.getMonth();

    if (monthDifference < 0 || (monthDifference === 0 && today.getDate() < birth.getDate())) {
      age--;
    }

    return age;
  }

  private formatNumericValue(value?: number | null, suffix = ''): string {
    if (value == null) {
      return '—';
    }
    return `${value}${suffix}`;
  }

  private getWeightAndHeightValue(): string {
    const weight = this.formatNumericValue(this.getVitalRow('WEIGHT')?.value, ' kg');
    const height = this.formatNumericValue(this.getVitalRow('HEIGHT')?.value, ' cm');

    if (weight === '—' && height === '—') {
      return '—';
    }

    return `${weight} | ${height}`;
  }

  private removeFallbackText(value: string): string {
    return value
      .replace(/\bfallback backend prediction\b[.:\-\s]*/gi, '')
      .replace(/\bfallback backend prediction based on latest patient context\b[.:\-\s]*/gi, '')
      .replace(/\bfallback predictions?(?:\s+based\s+on\s+text)?\b[.:\-\s]*/gi, '')
      .replace(/\s{2,}/g, ' ')
      .replace(/^[\s.:-]+|[\s.:-]+$/g, '')
      .trim();
  }

  private capitalizeLeadingLetter(value: string): string {
    if (!value) {
      return '';
    }

    return value.charAt(0).toUpperCase() + value.slice(1);
  }

  private comparePredictionsByRisk(left: Prediction, right: Prediction): number {
    const riskDifference = this.getRiskPriority(right.riskLevel) - this.getRiskPriority(left.riskLevel);
    if (riskDifference !== 0) {
      return riskDifference;
    }

    return (right.riskScore ?? 0) - (left.riskScore ?? 0);
  }

  private dedupePredictionsByType(predictions: Prediction[]): Prediction[] {
    const latestByType = new Map<string, Prediction>();

    for (const prediction of predictions) {
      const existing = latestByType.get(prediction.predictionType);
      if (!existing || this.comparePredictionFreshness(prediction, existing) < 0) {
        latestByType.set(prediction.predictionType, prediction);
      }
    }

    return Array.from(latestByType.values());
  }

  private comparePredictionFreshness(left: Prediction, right: Prediction): number {
    const leftTimestamp = new Date(left.predictionTimestamp).getTime();
    const rightTimestamp = new Date(right.predictionTimestamp).getTime();

    if (leftTimestamp !== rightTimestamp) {
      return rightTimestamp - leftTimestamp;
    }

    if (left.mainPrediction !== right.mainPrediction) {
      return left.mainPrediction ? -1 : 1;
    }

    return this.comparePredictionsByRisk(left, right);
  }

  private getRiskPriority(level?: string | null): number {
    switch (this.normalizeRiskLevel(level)) {
      case 'CRITICAL':
      case 'OUT_OF_RANGE':
      case 'VERY_HIGH':
        return 4;
      case 'HIGH':
        return 3;
      case 'MED':
      case 'MEDIUM':
        return 2;
      case 'LOW':
      case 'NOT_APPLICABLE':
      case 'INSUFFICIENT_CONTEXT':
        return 1;
      default:
        return 0;
    }
  }

  private getFreshnessPriority(level?: string | null): number {
    switch ((level || '').toUpperCase()) {
      case 'OUTDATED':
        return 3;
      case 'AGING':
        return 2;
      case 'CURRENT':
        return 1;
      default:
        return 0;
    }
  }

  private getVitalRowsForMetric(config: VitalMetricConfig): VitalSigns[] {
    return [this.getVitalRow(config.primaryType), config.secondaryType ? this.getVitalRow(config.secondaryType) : null]
      .filter((row): row is VitalSigns => !!row);
  }

  private getLatestVitalRowForMetric(config: VitalMetricConfig): VitalSigns | null {
    return this.getVitalRowsForMetric(config)
      .sort((left, right) => new Date(right.measuredAt).getTime() - new Date(left.measuredAt).getTime())[0] ?? null;
  }

  private getPredictionSourceVitals(prediction: Prediction): VitalSigns[] {
    return this.getPredictionSourceTypes(prediction)
      .map((type) => this.getVitalRow(type))
      .filter((row): row is VitalSigns => !!row);
  }

  private getPredictionSourceTypes(prediction: Prediction): string[] {
    return this.getPredictionSourceTypesForType(prediction.predictionType);
  }

  private getPredictionSourceTypesForType(predictionType?: string | null): string[] {
    switch ((predictionType || '').toUpperCase()) {
      case 'CARDIOVASCULAR_RISK':
        return ['BLOOD_PRESSURE_SYSTOLIC', 'CHOLESTEROL'];
      case 'DIABETES_RISK':
        return ['GLUCOSE', 'BMI'];
      case 'SEPSIS_RISK':
        return ['BODY_TEMPERATURE', 'HEART_RATE'];
      case 'RESPIRATORY_RISK':
        return ['OXYGEN_SATURATION', 'HEART_RATE'];
      case 'GENERAL_DETERIORATION':
        return ['HEART_RATE', 'BODY_TEMPERATURE', 'OXYGEN_SATURATION'];
      default:
        return this.vitalMetricConfigs.map((config) => config.primaryType);
    }
  }

  private getLatestMeasurementTimestamp(rows: VitalSigns[]): number {
    return Math.max(...rows.map((row) => new Date(row.measuredAt).getTime()));
  }

  private shouldDisplayPrediction(prediction: Prediction): boolean {
    const sourceTypes = this.getPredictionSourceTypes(prediction);
    const sourceRows = this.getPredictionSourceVitals(prediction);

    if (this.vitals().length === 0) {
      return true;
    }

    return this.hasUsablePredictionSources(sourceRows, sourceTypes);
  }

  private isUsablePredictionSourceRow(row: VitalSigns): boolean {
    const status = this.getVitalInterpretationStatus(row);
    return (
      row.value != null &&
      !!row.measuredAt &&
      row.contextComplete !== false &&
      !['UNKNOWN', 'INSUFFICIENT_CONTEXT', 'NOT_APPLICABLE', 'OUT_OF_RANGE'].includes(status)
    );
  }

  private ensurePredictionsAvailable(patientId: string, vitals: VitalSigns[], predictions: Prediction[]): void {
    if (predictions.length > 0 || !this.canGenerateRiskSummary(vitals)) {
      return;
    }

    this.api
      .recalculatePredictions(patientId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (recalculatedPredictions) => this.predictions.set(recalculatedPredictions),
        error: () => undefined,
      });
  }

  private canGenerateRiskSummary(vitals: VitalSigns[]): boolean {
    return this.predictionTypesForSummary.some((predictionType) =>
      this.hasUsablePredictionSources(
        this.getPredictionSourceVitalsFromList(vitals, this.getPredictionSourceTypesForType(predictionType)),
        this.getPredictionSourceTypesForType(predictionType),
      ),
    );
  }

  private getPredictionSourceVitalsFromList(vitals: VitalSigns[], sourceTypes: string[]): VitalSigns[] {
    return sourceTypes
      .map((type) => vitals.find((row) => row.type === type) ?? null)
      .filter((row): row is VitalSigns => !!row);
  }

  private hasUsablePredictionSources(sourceRows: VitalSigns[], sourceTypes: string[]): boolean {
    return sourceRows.length === sourceTypes.length && sourceRows.every((row) => this.isUsablePredictionSourceRow(row));
  }

  private formatVitalSource(row: VitalSigns): string {
    const label = this.formatVitalType(row.type);
    const value = row.value == null ? 'not available' : `${row.value}${row.unit ? ` ${row.unit}` : ''}`;
    return `${label} ${value} (${row.freshnessStatus ?? 'UNKNOWN'})`;
  }

  private formatVitalType(type?: string | null): string {
    return (type || 'Vital sign')
      .replaceAll('_', ' ')
      .toLowerCase()
      .replace(/\b\w/g, (letter) => letter.toUpperCase());
  }

  private maxStatus(statuses: Array<string | null | undefined>): string {
    const priority = new Map<string, number>([
      ['LOW', 0],
      ['NOT_APPLICABLE', 0],
      ['INSUFFICIENT_CONTEXT', 0],
      ['CURRENT', 0],
      ['MEDIUM', 1],
      ['AGING', 1],
      ['HIGH', 2],
      ['OUT_OF_RANGE', 2],
      ['CRITICAL', 3],
      ['OUTDATED', 2],
      ['UNKNOWN', -1],
    ]);

    return statuses.reduce<string>((max, status) => {
      const normalizedStatus = this.normalizeStatus(status);
      return (priority.get(normalizedStatus) ?? -1) > (priority.get(max) ?? -1)
        ? normalizedStatus
        : max;
    }, 'UNKNOWN');
  }

  private getRawClinicalStatusForMetric(config: VitalMetricConfig): string {
    return this.maxStatus(
      this.getVitalRowsForMetric(config).map((row) => this.getVitalInterpretationStatus(row)),
    );
  }

  private getClinicalToneFromStatus(status?: string | null): string | null {
    switch (this.normalizeStatus(status)) {
      case 'HIGH':
      case 'OUT_OF_RANGE':
      case 'VERY_HIGH':
        return 'HIGH';
      case 'MEDIUM':
        return 'MEDIUM';
      case 'LOW':
        return 'LOW';
      default:
        return null;
    }
  }

  private getClinicalDisplayStatus(status?: string | null): string {
    switch (this.normalizeStatus(status)) {
      case 'HIGH':
      case 'CRITICAL':
      case 'OUT_OF_RANGE':
      case 'VERY_HIGH':
        return 'HIGH';
      case 'MEDIUM':
      case 'LOW':
        return this.normalizeStatus(status);
      default:
        return '';
    }
  }

  private getPredictionTone(level: string): string | null {
    switch (level) {
      case 'HIGH':
      case 'CRITICAL':
      case 'VERY_HIGH':
        return 'HIGH';
      case 'MEDIUM':
        return 'MEDIUM';
      case 'LOW':
        return 'LOW';
      default:
        return null;
    }
  }

  private getPredictionDisplayLevel(prediction: Prediction): string {
    const normalizedLevel = this.normalizeRiskLevel(prediction.riskLevel);
    if (normalizedLevel === 'UNKNOWN') {
      return '';
    }

    return normalizedLevel === 'VERY_HIGH' || normalizedLevel === 'CRITICAL' ? 'HIGH' : normalizedLevel;
  }

  private getVitalInterpretationStatus(row?: VitalSigns | null): string {
    return this.normalizeStatus(row?.interpretationStatus ?? row?.clinicalStatus);
  }

  private normalizeStatus(status?: string | null): string {
    const normalizedStatus = (status || 'UNKNOWN').toUpperCase();
    return normalizedStatus === 'MED' ? 'MEDIUM' : normalizedStatus;
  }

  private normalizeFreshnessStatus(status?: string | null): string {
    const normalizedStatus = (status || 'UNKNOWN').toUpperCase();
    return normalizedStatus === 'CURRENT' ? 'CURRENT' : normalizedStatus;
  }

  private formatRiskScore(value?: number | null): string {
    if (value == null) {
      return '—';
    }
    const percentage = value <= 1 ? value * 100 : value;
    return `${percentage.toFixed(0)}%`;
  }

  private patchAddressForm(address?: PatientAddress | null): void {
    this.addressForm.reset({
      addressLine: address?.addressLine ?? '',
      city: address?.city ?? '',
      state: address?.state ?? '',
      county: address?.county ?? '',
      zipCode: address?.zipCode ?? '',
    });
  }

  private buildAddressPayload(): UpdatePatientAddressPayload {
    return {
      addressLine: this.normalizeAddressValue(this.addressForm.controls.addressLine.value),
      city: this.normalizeAddressValue(this.addressForm.controls.city.value),
      state: this.normalizeAddressValue(this.addressForm.controls.state.value),
      county: this.normalizeAddressValue(this.addressForm.controls.county.value),
      zipCode: this.normalizeAddressValue(this.addressForm.controls.zipCode.value),
    };
  }

  private normalizeAddressValue(value: string): string | null {
    const trimmed = value.trim();
    return trimmed.length > 0 ? trimmed : null;
  }

  private matchesSelectedMedicationQuery(query: string): boolean {
    const selectedMedicationName = this.selectedMedicationName();
    if (!selectedMedicationName) {
      return false;
    }

    return selectedMedicationName === query.trim();
  }

  private syncMedicationSuggestions(items: MedicationCatalogSearchItem[]): void {
    const query = this.medicationForm.controls.medicationQuery.value.trim();
    this.medicationLookup.set(items);
    this.medicationSuggestionsOpen.set(items.length > 0 && query.length > 0 && !this.matchesSelectedMedicationQuery(query));
  }

  private mergeMedicationIntoList(
    medications: PatientMedication[],
    savedMedication: PatientMedication,
  ): PatientMedication[] {
    return [savedMedication, ...medications.filter((medication) => medication.id !== savedMedication.id)];
  }

  private mergeConsultNoteIntoList(
    consultNotes: ConsultNote[],
    savedNote: ConsultNote,
  ): ConsultNote[] {
    return [savedNote, ...consultNotes.filter((note) => note.id !== savedNote.id)];
  }

  private getMedicationSavedNotice(): string {
    return this.preferences.language() === 'nl' ? 'Medicatie opgeslagen.' : 'Medication saved.';
  }

  private getMedicationSaveErrorNotice(): string {
    return this.preferences.language() === 'nl'
      ? 'Medicatie kon niet worden opgeslagen.'
      : 'Medication could not be saved.';
  }

  private getConsultNoteSortTimestamp(note: ConsultNote): number {
    const versionTimestamp = note.currentVersion?.createdAt
      ? new Date(note.currentVersion.createdAt).getTime()
      : NaN;
    if (Number.isFinite(versionTimestamp)) {
      return versionTimestamp;
    }

    const createdAtTimestamp = note.createdAt ? new Date(note.createdAt).getTime() : NaN;
    if (Number.isFinite(createdAtTimestamp)) {
      return createdAtTimestamp;
    }

    return 0;
  }

  private getMedicationSortTimestamp(medication: PatientMedication): number {
    const createdAtTimestamp = medication.createdAt ? new Date(medication.createdAt).getTime() : NaN;
    if (Number.isFinite(createdAtTimestamp)) {
      return createdAtTimestamp;
    }

    const startDateTimestamp = medication.startDate ? new Date(medication.startDate).getTime() : NaN;
    if (Number.isFinite(startDateTimestamp)) {
      return startDateTimestamp;
    }

    return 0;
  }
}
