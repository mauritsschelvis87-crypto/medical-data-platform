import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AppLanguage, AppTheme, UserProfile } from '../models/medical.models';
import { AppPreferencesService } from '../state/app-preferences.service';

@Component({
  selector: 'app-header',
  imports: [CommonModule, RouterLink],
  templateUrl: './app-header.component.html',
  styleUrl: './app-header.component.scss',
})
export class AppHeaderComponent {
  private readonly preferences = inject(AppPreferencesService);

  @Input({ required: true }) doctor!: UserProfile;
  @Input({ required: true }) loggedIn!: boolean;
  @Input({ required: true }) language!: AppLanguage;
  @Input({ required: true }) theme!: AppTheme;
  @Input() notice: string | null = null;

  @Output() languageChange = new EventEmitter<AppLanguage>();
  @Output() themeChange = new EventEmitter<AppTheme>();
  @Output() sessionAction = new EventEmitter<'login' | 'logout'>();

  protected readonly settingsOpen = signal(false);
  protected readonly accountOpen = signal(false);
  protected readonly text = this.preferences;

  protected toggleSettings(): void {
    this.settingsOpen.update((open) => !open);
    this.accountOpen.set(false);
  }

  protected toggleAccount(): void {
    this.accountOpen.update((open) => !open);
    this.settingsOpen.set(false);
  }

  protected setSession(action: 'login' | 'logout'): void {
    this.sessionAction.emit(action);
    this.accountOpen.set(false);
  }

  protected setLanguage(language: AppLanguage): void {
    this.languageChange.emit(language);
  }

  protected setTheme(theme: AppTheme): void {
    this.themeChange.emit(theme);
  }
}
