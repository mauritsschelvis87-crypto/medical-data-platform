import { CommonModule } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { combineLatest, forkJoin, of, switchMap } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { PatientApiService } from '../api/patient-api.service';
import {
  ConsultNote,
  MedicationCatalogItem,
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
  protected readonly editingAddress = signal(false);
  protected readonly medicationLookup = signal<MedicationCatalogItem[]>([]);
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
    { key: 'weight', label: 'Weight', primaryType: 'WEIGHT', primaryUnit: 'kg' },
    { key: 'oxygenSaturation', label: 'O2 saturation', primaryType: 'OXYGEN_SATURATION', primaryUnit: '%' },
    { key: 'cholesterol', label: 'Cholesterol', primaryType: 'CHOLESTEROL', primaryUnit: 'mmol/L' },
  ];

  protected readonly sortedVitalMetricConfigs = computed(() =>
    this.vitalMetricConfigs
      .map((config, index) => ({ config, index }))
      .sort((left, right) => {
        const clinicalDifference =
          this.getRiskPriority(this.getClinicalStatusForMetric(right.config)) -
          this.getRiskPriority(this.getClinicalStatusForMetric(left.config));
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
    changeReason: ['Initial consult documentation', Validators.required],
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
    [...this.predictions()].sort((left, right) => this.comparePredictionsByRisk(left, right)),
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
            timeline: this.api.getTimeline(patientId),
            vitals: this.api.getLatestVitals(patientId),
            predictions: this.api.getLatestPredictions(patientId),
            consultNotes: this.api.getConsultNotes(patientId),
            medications: this.api.getPatientMedications(patientId),
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
        },
        error: () => {
          this.loading.set(false);
        },
      });

    this.medicationForm.controls.medicationQuery.valueChanges
      .pipe(
        switchMap((query) => {
          if (!query?.trim()) {
            return of([]);
          }
          return this.api.searchMedicationCatalog(query.trim());
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((items) => this.medicationLookup.set(items));
  }

  protected toggleDrawer(): void {
    this.drawerOpen.update((open) => !open);
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

  protected selectMedication(item: MedicationCatalogItem): void {
    this.medicationForm.patchValue({
      medicationCatalogId: item.id,
      medicationQuery: `${item.dutchName}${item.latinName ? ` (${item.latinName})` : ''}`,
      dosage: item.defaultDosage ?? this.medicationForm.controls.dosage.value,
    });
    this.medicationLookup.set([]);
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
        changeReason: this.noteForm.controls.changeReason.value,
      })
      .pipe(
        switchMap(() =>
          combineLatest([this.api.getConsultNotes(patient.id), this.api.getTimeline(patient.id)]),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: ([consultNotes, timeline]) => {
          this.consultNotes.set(consultNotes);
          this.timeline.set(timeline);
          this.noteForm.reset({
            subjective: '',
            objective: '',
            assessment: '',
            plan: '',
            changeReason: 'Clinical update',
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
        switchMap(() =>
          combineLatest([
            this.api.getPatientMedications(patient.id),
            this.api.getTimeline(patient.id),
          ]),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: ([medications, timeline]) => {
          this.medications.set(medications);
          this.timeline.set(timeline);
          this.medicationForm.reset({
            medicationCatalogId: '',
            medicationQuery: '',
            dosage: '',
            frequency: '',
            startDate: new Date().toISOString().slice(0, 10),
            endDate: '',
            reason: '',
          });
          this.medicationLookup.set([]);
          this.submittingMedication.set(false);
        },
        error: () => this.submittingMedication.set(false),
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
        return this.formatNumericValue(this.getVitalRow('WEIGHT')?.value, ' kg');
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
    return this.getVitalMeasuredAt(config.primaryType) || (config.secondaryType ? this.getVitalMeasuredAt(config.secondaryType) : null);
  }

  protected getClinicalStatusForMetric(config: VitalMetricConfig): string {
    if (config.key === 'bmi') {
      return this.getBmiClinicalStatus();
    }

    return this.maxStatus([
      this.getVitalRow(config.primaryType)?.clinicalStatus,
      config.secondaryType ? this.getVitalRow(config.secondaryType)?.clinicalStatus : null,
    ]);
  }

  protected getFreshnessStatusForMetric(config: VitalMetricConfig): string {
    return this.maxStatus([
      this.getVitalRow(config.primaryType)?.freshnessStatus,
      config.secondaryType ? this.getVitalRow(config.secondaryType)?.freshnessStatus : null,
    ]);
  }

  protected getClinicalMessageForMetric(config: VitalMetricConfig): string {
    const message = [this.getVitalRow(config.primaryType), config.secondaryType ? this.getVitalRow(config.secondaryType) : null]
      .filter((row): row is VitalSigns => !!row)
      .map((row) => row.clinicalMessage)
      .filter((message): message is string => !!message)
      .join(' ');

    if (message || config.key !== 'bmi') {
      return message;
    }

    return this.getBmiClinicalMessage();
  }

  protected shouldShowClinicalStatus(config: VitalMetricConfig): boolean {
    const status = this.getClinicalStatusForMetric(config);
    return status !== 'LOW' && status !== 'UNKNOWN';
  }

  protected getClinicalSummaryForMetric(config: VitalMetricConfig): string {
    if (!this.shouldShowClinicalStatus(config)) {
      return '';
    }

    const direction = this.getClinicalDirectionForMetric(config);

    return `${config.label}: ${direction}`;
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

  protected cleanPredictionExplanation(explanation?: string | null): string {
    return this.removeFallbackText(explanation || '');
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

  private removeFallbackText(value: string): string {
    return value
      .replace(/\bfallback backend prediction based on latest patient context\b[.:\-\s]*/gi, '')
      .replace(/\bfallback predictions?(?:\s+based\s+on\s+text)?\b[.:\-\s]*/gi, '')
      .replace(/\s{2,}/g, ' ')
      .replace(/^[\s.:-]+|[\s.:-]+$/g, '')
      .trim();
  }

  private comparePredictionsByRisk(left: Prediction, right: Prediction): number {
    const riskDifference = this.getRiskPriority(right.riskLevel) - this.getRiskPriority(left.riskLevel);
    if (riskDifference !== 0) {
      return riskDifference;
    }

    return (right.riskScore ?? 0) - (left.riskScore ?? 0);
  }

  private getRiskPriority(level?: string | null): number {
    switch ((level || '').toUpperCase()) {
      case 'CRITICAL':
      case 'VERY_HIGH':
        return 4;
      case 'HIGH':
        return 3;
      case 'MED':
      case 'MEDIUM':
        return 2;
      case 'LOW':
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

  private getBmiClinicalStatus(): string {
    const bmi = this.getVitalRow('BMI')?.value;
    if (bmi == null) {
      return this.getVitalRow('BMI')?.clinicalStatus ?? 'UNKNOWN';
    }

    if (bmi < 16 || bmi >= 40) {
      return 'CRITICAL';
    }

    if (bmi < 18.5 || bmi >= 30) {
      return 'HIGH';
    }

    if (bmi >= 25) {
      return 'MEDIUM';
    }

    return 'LOW';
  }

  private getBmiClinicalMessage(): string {
    const bmi = this.getVitalRow('BMI')?.value;
    if (bmi == null) {
      return '';
    }

    if (bmi < 16) {
      return 'Critical underweight risk';
    }

    if (bmi < 18.5) {
      return 'Underweight risk';
    }

    if (bmi >= 40) {
      return 'Critical obesity risk';
    }

    if (bmi >= 30) {
      return 'Obesity risk';
    }

    if (bmi >= 25) {
      return 'Overweight risk';
    }

    return '';
  }

  private getClinicalDirectionForMetric(config: VitalMetricConfig): string {
    switch (config.key) {
      case 'bloodPressure':
        return this.getBloodPressureDirection();
      case 'heartRate':
        return this.getRangeDirection('HEART_RATE', 50, 100);
      case 'temperature':
        return this.getRangeDirection('BODY_TEMPERATURE', 35.5, 37.8);
      case 'oxygenSaturation':
        return 'low';
      case 'bmi':
        return this.getBmiDirection();
      case 'glucose':
      case 'cholesterol':
        return 'high';
      default:
        return this.getClinicalStatusForMetric(config).toLowerCase();
    }
  }

  private getBloodPressureDirection(): string {
    const systolic = this.getVitalRow('BLOOD_PRESSURE_SYSTOLIC')?.value;
    const diastolic = this.getVitalRow('BLOOD_PRESSURE_DIASTOLIC')?.value;

    if ((systolic != null && systolic >= 140) || (diastolic != null && diastolic >= 90)) {
      return 'high';
    }

    return 'abnormal';
  }

  private getRangeDirection(type: string, lowInclusive: number, highInclusive: number): string {
    const value = this.getVitalRow(type)?.value;
    if (value == null) {
      return 'abnormal';
    }

    if (value < lowInclusive) {
      return 'low';
    }

    if (value > highInclusive) {
      return 'high';
    }

    return 'abnormal';
  }

  private getBmiDirection(): string {
    const bmi = this.getVitalRow('BMI')?.value;
    if (bmi == null) {
      return 'abnormal';
    }

    if (bmi < 18.5) {
      return 'underweight';
    }

    if (bmi >= 30) {
      return 'obesity';
    }

    if (bmi >= 25) {
      return 'overweight';
    }

    return 'abnormal';
  }

  private maxStatus(statuses: Array<string | null | undefined>): string {
    const priority = new Map<string, number>([
      ['LOW', 0],
      ['CURRENT', 0],
      ['MEDIUM', 1],
      ['AGING', 1],
      ['HIGH', 2],
      ['CRITICAL', 3],
      ['OUTDATED', 2],
      ['UNKNOWN', -1],
    ]);

    return statuses.reduce<string>((max, status) => {
      const normalizedStatus = status ?? 'UNKNOWN';
      return (priority.get(normalizedStatus) ?? -1) > (priority.get(max) ?? -1)
        ? normalizedStatus
        : max;
    }, 'UNKNOWN');
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
}
