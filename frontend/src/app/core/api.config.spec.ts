import { apiBaseUrl } from './api.config';

describe('apiBaseUrl', () => {
  afterEach(() => {
    delete window.__burgeeConfig;
  });

  it('returns an empty string when no config is present', () => {
    delete window.__burgeeConfig;
    expect(apiBaseUrl()).toBe('');
  });

  it('returns the configured base url', () => {
    window.__burgeeConfig = { apiBaseUrl: 'https://api.example.com' };
    expect(apiBaseUrl()).toBe('https://api.example.com');
  });

  it('strips a trailing slash from the configured base url', () => {
    window.__burgeeConfig = { apiBaseUrl: 'https://api.example.com/' };
    expect(apiBaseUrl()).toBe('https://api.example.com');
  });

  it('returns an empty string when apiBaseUrl is missing on the config object', () => {
    window.__burgeeConfig = {};
    expect(apiBaseUrl()).toBe('');
  });
});
