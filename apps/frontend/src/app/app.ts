import { Component, computed, inject, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AppHeaderComponent } from './shared/app-header.component';
import { AppPreferencesService } from './state/app-preferences.service';
import { UserSessionService } from './state/user-session.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, AppHeaderComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected readonly preferences = inject(AppPreferencesService);
  private readonly userSessionService = inject(UserSessionService);

  protected readonly doctor = this.userSessionService.doctor;
  protected readonly loggedIn = this.userSessionService.loggedIn;
  protected readonly notice = signal<string | null>(null);
  protected readonly title = computed(() => 'Medical Data Platform');

  protected handleSessionAction(action: 'login' | 'logout'): void {
    if (action === 'login') {
      this.userSessionService.login();
    } else {
      this.userSessionService.logout();
    }

    this.showSessionNotice();
  }

  protected showSessionNotice(): void {
    this.notice.set(this.preferences.t('sessionNotice'));
    window.setTimeout(() => this.notice.set(null), 3200);
  }
}
