import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class AppNoticeService {
  readonly notice = signal<string | null>(null);
  private timeoutId: number | null = null;

  show(message: string, durationMs = 3200): void {
    this.notice.set(message);

    if (this.timeoutId != null) {
      window.clearTimeout(this.timeoutId);
    }

    this.timeoutId = window.setTimeout(() => {
      this.notice.set(null);
      this.timeoutId = null;
    }, durationMs);
  }
}
