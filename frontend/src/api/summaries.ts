import client from './client';
import type { DailySummary } from '../types/api';

export async function getAccountSummaries(
  accountId: number,
  from: string,
  to: string
): Promise<DailySummary[]> {
  const res = await client.get<DailySummary[]>(
    `/accounts/${accountId}/summaries`,
    { params: { from, to } }
  );
  return res.data;
}
