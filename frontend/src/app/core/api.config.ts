declare global {
  interface Window {
    __burgeeConfig?: { apiBaseUrl?: string };
  }
}

export const apiBaseUrl = (): string => {
  if (typeof window !== 'undefined' && window.__burgeeConfig?.apiBaseUrl) {
    return window.__burgeeConfig.apiBaseUrl.replace(/\/$/, '');
  }
  return '';
};
