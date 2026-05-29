import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { CreateUserPayload, UpdateUserPayload, User, UserService } from './user.service';

const sampleUser: User = {
  id: 'user-1',
  subject: 'jane',
  email: 'jane@example.com',
  displayName: 'Jane Doe',
  role: 'ADMIN',
  provider: 'BASIC',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-02T00:00:00Z',
};

describe('UserService', () => {
  let service: UserService;
  let httpMock: HttpTestingController;
  const base = '/api/admin/users';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [UserService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(UserService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('lists users with a GET request', () => {
    let result: User[] | undefined;
    service.list().subscribe((users) => (result = users));

    const req = httpMock.expectOne(base);
    expect(req.request.method).toBe('GET');
    req.flush([sampleUser]);

    expect(result).toEqual([sampleUser]);
  });

  it('gets a single user by id', () => {
    let result: User | undefined;
    service.get('user-1').subscribe((user) => (result = user));

    const req = httpMock.expectOne(`${base}/user-1`);
    expect(req.request.method).toBe('GET');
    req.flush(sampleUser);

    expect(result).toEqual(sampleUser);
  });

  it('creates a user with a POST request carrying the payload', () => {
    const payload: CreateUserPayload = {
      subject: 'jane',
      email: 'jane@example.com',
      displayName: 'Jane Doe',
      role: 'ADMIN',
      password: 'secret',
    };
    service.create(payload).subscribe();

    const req = httpMock.expectOne(base);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush(sampleUser);
  });

  it('updates a user with a PUT request', () => {
    const payload: UpdateUserPayload = {
      email: 'jane@example.com',
      displayName: 'Jane Doe',
      role: 'USER',
      password: null,
    };
    service.update('user-1', payload).subscribe();

    const req = httpMock.expectOne(`${base}/user-1`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(payload);
    req.flush(sampleUser);
  });

  it('deletes a user with a DELETE request', () => {
    service.delete('user-1').subscribe();

    const req = httpMock.expectOne(`${base}/user-1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
