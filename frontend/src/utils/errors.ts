import type { AxiosError } from 'axios';

interface ApiErrorData {
  message?: string;
  fieldErrors?: Record<string, string>;
}

export function getApiErrorMessage(error: unknown, fallback: string): string {
  const axiosError = error as AxiosError<ApiErrorData>;
  return axiosError?.response?.data?.message || fallback;
}
