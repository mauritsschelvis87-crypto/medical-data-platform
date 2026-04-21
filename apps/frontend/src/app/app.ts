import { Component, computed, inject, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AppHeaderComponent } from './shared/app-header.component';
import { DemoUserService } from './state/demo-user.service';
import { AppPreferencesService } from './state/app-preferences.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, AppHeaderComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  protected readonly preferences = inject(AppPreferencesService);
  private readonly demoUserService = inject(DemoUserService);

  protected readonly doctor = this.demoUserService.doctor;
  protected readonly notice = signal<string | null>(null);
  protected readonly title = computed(() => 'Medical Data Platform');

  protected showDemoLoginNotice(): void {
    this.notice.set('For the demo, this physician remains signed in.');
    window.setTimeout(() => this.notice.set(null), 3200);
  }
}
