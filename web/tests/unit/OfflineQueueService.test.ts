import { describe, it, expect, vi } from 'vitest';
import { OfflineQueueService } from '../../src/services/OfflineQueueService';

vi.mock('../../src/config/firebase', () => ({
  auth: {
    currentUser: { uid: 'test_user_123', email: 'test@ssbmax.in' }
  }
}));

describe('OfflineQueueService Unit Tests', () => {
  const queueService = new OfflineQueueService();

  it('should enqueue and retrieve offline submission', async () => {
    const id = await queueService.enqueueSubmission({
      testType: 'OIR',
      userId: 'test_user_123',
      payload: { answers: [1, 2, 3] }
    });

    expect(id).toBeDefined();
    const queued = await queueService.getQueuedSubmissions();
    expect(queued.some((item) => item.id === id)).toBe(true);
  });

  it('should sync pending submissions using handler when user is authenticated', async () => {
    await queueService.enqueueSubmission({
      testType: 'TAT',
      userId: 'test_user_123',
      payload: { stories: ['story1'] }
    });

    const handler = vi.fn().mockResolvedValue(true);
    const result = await queueService.syncPendingSubmissions(handler);

    expect(result.authRequired).toBe(false);
    expect(result.syncedCount).toBeGreaterThan(0);
    expect(handler).toHaveBeenCalled();
  });

  it('should remove submission after successful sync', async () => {
    const id = await queueService.enqueueSubmission({
      testType: 'WAT',
      userId: 'test_user_123',
      payload: { words: ['bravery'] }
    });

    await queueService.removeSubmission(id);
    const queued = await queueService.getQueuedSubmissions();
    expect(queued.some((item) => item.id === id)).toBe(false);
  });
});
