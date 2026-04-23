create table patient (
    id uuid primary key,
    created_at timestamp not null,
    updated_at timestamp not null,
    patient_number varchar(50) not null,
    source_patient_id varchar(100) not null,
    first_name varchar(100) not null,
    last_name varchar(100) not null,
    full_name varchar(250) not null,
    birth_date date not null,
    gender varchar(20) not null,
    deceased boolean not null,
    death_date date,
    marital_status varchar(50),
    race varchar(100),
    ethnicity varchar(100),
    constraint uk_patient_patient_number unique (patient_number),
    constraint uk_patient_source_patient_id unique (source_patient_id)
);

create index idx_patient_patient_number on patient (patient_number);
create index idx_patient_source_patient_id on patient (source_patient_id);
create index idx_patient_full_name on patient (full_name);
create index idx_patient_birth_date on patient (birth_date);

create table patient_address (
    id uuid primary key,
    created_at timestamp not null,
    updated_at timestamp not null,
    patient_id uuid not null,
    address_line varchar(255),
    city varchar(100),
    state varchar(100),
    county varchar(100),
    zip_code varchar(20),
    constraint uk_patient_address_patient_id unique (patient_id),
    constraint fk_patient_address_patient foreign key (patient_id) references patient (id)
);

create index idx_patient_address_patient_id on patient_address (patient_id);
create index idx_patient_address_city_state on patient_address (city, state);

create table dataset_imports (
    id uuid primary key,
    created_at timestamp not null,
    updated_at timestamp not null,
    source_name varchar(255) not null,
    dataset_type varchar(50) not null,
    imported_at timestamp not null,
    status varchar(30) not null,
    notes text
);

create index idx_dataset_import_source_name on dataset_imports (source_name);
create index idx_dataset_import_dataset_type on dataset_imports (dataset_type);
create index idx_dataset_import_status on dataset_imports (status);

create table medication_catalog (
    id uuid primary key,
    created_at timestamp not null,
    updated_at timestamp not null,
    code varchar(50) not null,
    dutch_name varchar(255) not null,
    latin_name varchar(255),
    default_dosage varchar(100),
    advice text,
    active boolean not null,
    constraint uk_medication_catalog_code unique (code)
);

create index idx_medication_catalog_code on medication_catalog (code);

create table vital_signs (
    id uuid primary key,
    created_at timestamp not null,
    updated_at timestamp not null,
    patient_id uuid not null,
    type varchar(50) not null,
    measurement_value numeric(12,4) not null,
    unit varchar(30) not null,
    measured_at timestamp not null,
    source_observation_code varchar(50),
    source_description varchar(255),
    source varchar(20) not null,
    constraint fk_vital_signs_patient foreign key (patient_id) references patient (id)
);

create index idx_vital_signs_patient_measured_at on vital_signs (patient_id, measured_at);
create index idx_vital_signs_patient_type on vital_signs (patient_id, type);

create table predictions (
    id uuid primary key,
    created_at timestamp not null,
    updated_at timestamp not null,
    patient_id uuid not null,
    prediction_type varchar(50) not null,
    risk_level varchar(20) not null,
    risk_score numeric(5,2) not null,
    confidence numeric(5,2) not null,
    explanation text,
    is_main_prediction boolean not null,
    triggered_by_reference_id uuid,
    prediction_timestamp timestamp not null,
    previous_risk_level varchar(20),
    risk_increased boolean not null,
    requires_confirmation boolean not null,
    is_confirmed boolean not null,
    confirmed_at timestamp,
    confirmed_by varchar(255),
    model_version varchar(50),
    threshold_triggered boolean not null,
    constraint fk_predictions_patient foreign key (patient_id) references patient (id)
);

create index idx_prediction_patient_created_at on predictions (patient_id, prediction_timestamp);
create index idx_prediction_patient_type on predictions (patient_id, prediction_type);

create table consult_notes (
    id uuid primary key,
    created_at timestamp not null,
    updated_at timestamp not null,
    patient_id uuid not null,
    status varchar(20) not null,
    created_by varchar(100) not null,
    current_version_id uuid,
    constraint fk_consult_notes_patient foreign key (patient_id) references patient (id)
);

create index idx_consult_note_patient on consult_notes (patient_id);

create table consult_note_versions (
    id uuid primary key,
    created_at timestamp not null,
    updated_at timestamp not null,
    consult_note_id uuid not null,
    version_number varchar(20) not null,
    subjective text,
    objective text,
    assessment text,
    plan text,
    created_by varchar(100) not null,
    change_reason text,
    constraint fk_consult_note_versions_consult_note foreign key (consult_note_id) references consult_notes (id)
);

create index idx_consult_note_version_consult_note on consult_note_versions (consult_note_id);

alter table consult_notes
    add constraint fk_consult_notes_current_version
    foreign key (current_version_id) references consult_note_versions (id);

create table patient_medications (
    id uuid primary key,
    created_at timestamp not null,
    updated_at timestamp not null,
    patient_id uuid not null,
    medication_catalog_id uuid not null,
    dosage varchar(100) not null,
    frequency varchar(100) not null,
    start_date date not null,
    end_date date,
    reason text,
    status varchar(20) not null,
    prescribed_by varchar(100),
    constraint fk_patient_medications_patient foreign key (patient_id) references patient (id),
    constraint fk_patient_medications_catalog foreign key (medication_catalog_id) references medication_catalog (id)
);

create index idx_patient_medication_patient on patient_medications (patient_id);
create index idx_patient_medication_catalog on patient_medications (medication_catalog_id);

create table timeline_events (
    id uuid primary key,
    created_at timestamp not null,
    updated_at timestamp not null,
    patient_id uuid not null,
    event_type varchar(50) not null,
    reference_id uuid,
    reference_type varchar(100),
    title varchar(255) not null,
    description text,
    event_timestamp timestamp not null,
    constraint fk_timeline_events_patient foreign key (patient_id) references patient (id)
);

create index idx_timeline_event_patient_timestamp on timeline_events (patient_id, event_timestamp);
