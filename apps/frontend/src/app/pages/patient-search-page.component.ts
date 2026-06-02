import { CommonModule } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { debounceTime, distinctUntilChanged, filter, switchMap, tap } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { PatientApiService } from '../api/patient-api.service';
import { PatientSearchResult } from '../models/medical.models';
import { AppPreferencesService } from '../state/app-preferences.service';

@Component({
  selector: 'app-patient-search-page',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './patient-search-page.component.html',
  styleUrl: './patient-search-page.component.scss'
})
export class PatientSearchPageComponent {
  private readonly api = inject(PatientApiService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly preferences = inject(AppPreferencesService);
  protected readonly searchControl = new FormControl('', { nonNullable: true });
  protected readonly loading = signal(false);
  protected readonly results = signal<PatientSearchResult[]>([]);
  protected readonly highlighted = signal<number>(0);

  constructor() {
    this.loading.set(true);
    this.api
      .getInitialPatients()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (results) => {
          this.results.set(results);
          this.highlighted.set(0);
          this.loading.set(false);
        },
        error: () => {
          this.results.set([]);
          this.loading.set(false);
        }
      });

    this.searchControl.valueChanges
      .pipe(
        debounceTime(220),
        distinctUntilChanged(),
        tap((query) => {
          if (query.trim().length < 2) {
            this.highlighted.set(0);
            this.loading.set(false);
            this.api
              .getInitialPatients()
              .pipe(takeUntilDestroyed(this.destroyRef))
              .subscribe({
                next: (results) => {
                  this.results.set(results);
                  this.highlighted.set(0);
                },
                error: () => {
                  this.results.set([]);
                }
              });
          } else {
            this.loading.set(true);
          }
        }),
        filter((query) => query.trim().length >= 2),
        switchMap((query) => this.api.searchPatients(query.trim())),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (results) => {
          this.results.set(results);
          this.highlighted.set(0);
          this.loading.set(false);
        },
        error: () => {
          this.loading.set(false);
        }
      });
  }

  protected openPatient(patientId: string): void {
    this.router.navigate(['/patients', patientId]);
  }

  protected clearSearch(): void {
    this.searchControl.setValue('');
  }

  protected onKeydown(event: KeyboardEvent): void {
    const results = this.results();
    if (results.length === 0) {
      return;
    }

    if (event.key === 'ArrowDown') {
      event.preventDefault();
      this.highlighted.set((this.highlighted() + 1) % results.length);
    }

    if (event.key === 'ArrowUp') {
      event.preventDefault();
      this.highlighted.set((this.highlighted() - 1 + results.length) % results.length);
    }

    if (event.key === 'Enter') {
      event.preventDefault();
      const selected = results[this.highlighted()];
      if (selected) {
        this.openPatient(selected.id);
      }
    }
  }
}
