import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { CreateFlagPayload, FeatureFlag, FlagService, UpdateFlagPayload } from './flag.service';

const sampleFlag: FeatureFlag = {
  id: 'flag-1',
  key: 'new-checkout',
  name: 'New Checkout',
  description: 'Enables the redesigned checkout',
  enabled: true,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-02T00:00:00Z',
};

describe('FlagService', () => {
  let service: FlagService;
  let httpMock: HttpTestingController;
  const base = '/api/admin/flags';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [FlagService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(FlagService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('lists flags with a GET request', () => {
    let result: FeatureFlag[] | undefined;
    service.list().subscribe((flags) => (result = flags));

    const req = httpMock.expectOne(base);
    expect(req.request.method).toBe('GET');
    req.flush([sampleFlag]);

    expect(result).toEqual([sampleFlag]);
  });

  it('gets a single flag by id', () => {
    let result: FeatureFlag | undefined;
    service.get('flag-1').subscribe((flag) => (result = flag));

    const req = httpMock.expectOne(`${base}/flag-1`);
    expect(req.request.method).toBe('GET');
    req.flush(sampleFlag);

    expect(result).toEqual(sampleFlag);
  });

  it('creates a flag with a POST request carrying the payload', () => {
    const payload: CreateFlagPayload = {
      key: 'new-checkout',
      name: 'New Checkout',
      description: null,
      enabled: false,
    };
    service.create(payload).subscribe();

    const req = httpMock.expectOne(base);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush(sampleFlag);
  });

  it('updates a flag with a PUT request', () => {
    const payload: UpdateFlagPayload = {
      name: 'Renamed',
      description: 'updated',
      enabled: true,
    };
    service.update('flag-1', payload).subscribe();

    const req = httpMock.expectOne(`${base}/flag-1`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(payload);
    req.flush(sampleFlag);
  });

  it('toggles a flag with an empty POST body', () => {
    service.toggle('flag-1').subscribe();

    const req = httpMock.expectOne(`${base}/flag-1/toggle`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});
    req.flush(sampleFlag);
  });

  it('deletes a flag with a DELETE request', () => {
    service.delete('flag-1').subscribe();

    const req = httpMock.expectOne(`${base}/flag-1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
