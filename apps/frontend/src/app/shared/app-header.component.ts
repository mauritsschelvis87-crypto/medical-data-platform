import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AppLanguage, AppTheme, DemoDoctor } from '../models/medical.models';
import { AppPreferencesService } from '../state/app-preferences.service';

@Component({
  selector: 'app-header',
  imports: [CommonModule, RouterLink],
  templateUrl: './app-header.component.html',
  styleUrl: './app-header.component.scss'
})
export class AppHeaderComponent {
  private readonly preferences = inject(AppPreferencesService);

  @Input({ required: true }) doctor!: DemoDoctor;
  @Input({ required: true }) language!: AppLanguage;
  @Input({ required: true }) theme!: AppTheme;
  @Input() notice: string | null = null;

  @Output() languageChange = new EventEmitter<AppLanguage>();
  @Output() themeChange = new EventEmitter<AppTheme>();
  @Output() demoLoginClick = new EventEmitter<void>();

  protected readonly settingsOpen = signal(false);

  protected readonly text = this.preferences;

  protected toggleSettings(): void {
    this.settingsOpen.update((open) => !open);
  }

  protected setLanguage(language: AppLanguage): void {
    this.languageChange.emit(language);
  }

  protected setTheme(theme: AppTheme): void {
    this.themeChange.emit(theme);
  }
}
