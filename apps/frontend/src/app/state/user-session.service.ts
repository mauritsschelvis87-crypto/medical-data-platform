import { Injectable, signal } from '@angular/core';
import { UserProfile } from '../models/medical.models';

const DEMO_DOCTOR: UserProfile = {
  name: 'Dr. Jonathan Hyde',
  shortName: 'Dr. J. Hyde',
  specialty: 'Internal Medicine',
  department: 'Internal Medicine Department',
  accessLevel: 'Full clinical access',
  status: 'Demo session active'
};

@Injectable({
  providedIn: 'root'
})
export class UserSessionService {
  readonly doctor = signal<UserProfile>(DEMO_DOCTOR);
  readonly loggedIn = signal(true);

  login(): void {
    this.loggedIn.set(true);
  }

  logout(): void {
    this.loggedIn.set(false);
  }
}
