import { Injectable, signal } from '@angular/core';
import { DemoDoctor } from '../models/medical.models';

@Injectable({
  providedIn: 'root'
})
export class DemoUserService {
  readonly doctor = signal<DemoDoctor>({
    name: 'Dr. Jonathan Hyde',
    shortName: 'Dr. J. Hyde',
    specialty: 'Internal Medicine',
    department: 'Internal Medicine Department',
    accessLevel: 'Full clinical access',
    status: 'Demo session active'
  });
}
