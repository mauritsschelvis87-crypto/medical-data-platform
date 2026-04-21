import { CommonModule } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { combineLatest, forkJoin, of, switchMap } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { PatientApiService } from '../api/patient-api.service';
import {
  ConsultNote,
  MedicationCatalogItem,
  Patient,
  PatientMedication,
  Prediction,
  TimelineEvent,
  VitalSigns
} from '../models/medical.models';
import { AppPreferencesService } from '../state/app-preferences.service';

@Component({
  selector: 'app-patient-detail-page',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './patient-detail-page.component.html',
  styleUrl: './patient-detail-page.component.scss'
})
export class PatientDetailPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(PatientApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(FormBuilder);

  protected readonly preferences = inject(AppPreferencesService);

  protected readonly loading = signal(true);
  protected readonly drawerOpen = signal(false);
  protected readonly medicationLookup = signal<MedicationCatalogItem[]>([]);
  protected readonly submittingNote = signal(false);
  protected readonly submittingMedication = signal(false);

  protected readonly patient = signal<Patient | null>(null);
  protected readonly timeline = signal<TimelineEvent[]>([]);
  protected readonly vitals = signal<VitalSigns[]>([]);
  protected readonly predictions = signal<Prediction[]>([]);
  protected readonly consultNotes = signal<ConsultNote[]>([]);
  protected readonly medications = signal<PatientMedication[]>([]);

  protected readonly noteForm = this.fb.nonNullable.group({
    subjective: ['', Validators.required],
    objective: ['', Validators.required],
    assessment: ['', Validators.required],
    plan: ['', Validators.required],
    changeReason: ['Initial consult documentation', Validators.required]
  });

  protected readonly medicationForm = this.fb.nonNullable.group({
    medicationCatalogId: [0, Validators.min(1)],
    medicationQuery: [''],
    dosage: ['', Validators.required],
    frequency: ['', Validators.required],
    startDate: [new Date().toISOString().slice(0, 10), Validators.required],
    endDate: [''],
    reason: ['', Validators.required]
  });

  protected readonly latestVital = computed(() => this.vitals()[0] ?? null);
  protected readonly mainPrediction = computed(
    () => this.predictions().find((prediction) => prediction.mainPrediction) ?? this.predictions()[0] ?? null
  );
  protected readonly otherPredictions = computed(() =>
    this.predictions().filter((prediction) => prediction.id !== this.mainPrediction()?.id)
  );
  protected readonly staleWeight = computed(() => {
    const latest = this.latestVital();
    if (!latest?.measuredAt || latest.weight == null) {
      return false;
    }
    const oneYearAgo = new Date();
    oneYearAgo.setFullYear(oneYearAgo.getFullYear() - 1);
    return new Date(latest.measuredAt) < oneYearAgo;
  });

  constructor() {
    this.route.paramMap
      .pipe(
        switchMap((params) => {
          const patientId = Number(params.get('patientId'));
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
            medications: this.api.getPatientMedications(patientId)
          });
        }),
        takeUntilDestroyed(this.destroyRef)
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
          this.loading.set(false);
        },
        error: () => {
          this.loading.set(false);
        }
      });

    this.medicationForm.controls.medicationQuery.valueChanges
      .pipe(
        switchMap((query) => {
          if (!query?.trim()) {
            return of([]);
          }
          return this.api.searchMedicationCatalog(query.trim());
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((items) => this.medicationLookup.set(items));
  }

  protected toggleDrawer(): void {
    this.drawerOpen.update((open) => !open);
  }

  protected selectMedication(item: MedicationCatalogItem): void {
    this.medicationForm.patchValue({
      medicationCatalogId: item.id,
      medicationQuery: `${item.dutchName}${item.latinName ? ` (${item.latinName})` : ''}`,
      dosage: item.defaultDosage ?? this.medicationForm.controls.dosage.value
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
        changeReason: this.noteForm.controls.changeReason.value
      })
      .pipe(
        switchMap(() => combineLatest([this.api.getConsultNotes(patient.id), this.api.getTimeline(patient.id)])),
        takeUntilDestroyed(this.destroyRef)
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
            changeReason: 'Clinical update'
          });
          this.submittingNote.set(false);
        },
        error: () => this.submittingNote.set(false)
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
        prescribedBy: 'Dr. Jonathan Hyde'
      })
      .pipe(
        switchMap(() => combineLatest([this.api.getPatientMedications(patient.id), this.api.getTimeline(patient.id)])),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: ([medications, timeline]) => {
          this.medications.set(medications);
          this.timeline.set(timeline);
          this.medicationForm.reset({
            medicationCatalogId: 0,
            medicationQuery: '',
            dosage: '',
            frequency: '',
            startDate: new Date().toISOString().slice(0, 10),
            endDate: '',
            reason: ''
          });
          this.medicationLookup.set([]);
          this.submittingMedication.set(false);
        },
        error: () => this.submittingMedication.set(false)
      });
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
      minute: '2-digit'
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
}
