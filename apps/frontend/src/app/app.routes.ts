import { Routes } from '@angular/router';
import { PatientDetailPageComponent } from './pages/patient-detail-page.component';
import { PatientSearchPageComponent } from './pages/patient-search-page.component';

export const routes: Routes = [
  {
    path: '',
    component: PatientSearchPageComponent
  },
  {
    path: 'patients/:patientId',
    component: PatientDetailPageComponent
  },
  {
    path: '**',
    redirectTo: ''
  }
];
