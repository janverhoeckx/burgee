import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { AuditEntry, AuditService } from './audit.service';

const sampleEntry: AuditEntry = {
  id: 'audit-1',
  flagId: 'flag-1',
  flagKey: 'new-checkout',
  action: 'TOGGLE',
  actor: 'alice',
  detail: 'enabled -> disabled',
  occurredAt: '2026-01-03T00:00:00Z',
};

describe('AuditService', () => {
  let service: AuditService;
  let httpMock: HttpTestingController;
  const base = '/api/admin/audit';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [AuditService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuditService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('lists all entries without query params when no flagId is given', () => {
    let result: AuditEntry[] | undefined;
    service.list().subscribe((entries) => (result = entries));

    const req = httpMock.expectOne(base);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.has('flagId')).toBe(false);
    req.flush([sampleEntry]);

    expect(result).toEqual([sampleEntry]);
  });

  it('passes flagId as a query param when provided', () => {
    service.list('flag-1').subscribe();

    const req = httpMock.expectOne((r) => r.url === base);
    expect(req.request.params.get('flagId')).toBe('flag-1');
    req.flush([]);
  });
});
