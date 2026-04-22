import { Component, computed, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AppHeaderComponent } from './shared/app-header.component';
import { AppNoticeService } from './state/app-notice.service';
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
  private readonly noticeService = inject(AppNoticeService);

  protected readonly doctor = this.userSessionService.doctor;
  protected readonly loggedIn = this.userSessionService.loggedIn;
  protected readonly notice = this.noticeService.notice;
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
    this.noticeService.show(this.preferences.t('sessionNotice'));
  }
}
