import { auth } from '../config/firebase';

export interface QueuedSubmission {
  id: string;
  testType: 'OIR' | 'TAT' | 'WAT' | 'SRT' | 'SD' | 'PPDT';
  userId: string;
  payload: Record<string, unknown>;
  timestamp: number;
}

export class OfflineQueueService {
  private static DB_NAME = 'SSBMax_OfflineDB';
  private static STORE_NAME = 'pendingSubmissions';
  private static memoryFallback: QueuedSubmission[] = [];

  private async openDB(): Promise<IDBDatabase | null> {
    if (typeof window === 'undefined' || !window.indexedDB) {
      return null;
    }

    return new Promise((resolve) => {
      const request = indexedDB.open(OfflineQueueService.DB_NAME, 1);
      request.onupgradeneeded = () => {
        const db = request.result;
        if (!db.objectStoreNames.contains(OfflineQueueService.STORE_NAME)) {
          db.createObjectStore(OfflineQueueService.STORE_NAME, { keyPath: 'id' });
        }
      };
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => resolve(null);
    });
  }

  async enqueueSubmission(submission: Omit<QueuedSubmission, 'id' | 'timestamp'>): Promise<string> {
    const id = `sub_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`;
    const fullSubmission: QueuedSubmission = {
      ...submission,
      id,
      timestamp: Date.now()
    };

    const db = await this.openDB();
    if (!db) {
      OfflineQueueService.memoryFallback.push(fullSubmission);
      return id;
    }

    return new Promise((resolve) => {
      const tx = db.transaction(OfflineQueueService.STORE_NAME, 'readwrite');
      const store = tx.objectStore(OfflineQueueService.STORE_NAME);
      const req = store.add(fullSubmission);
      req.onsuccess = () => resolve(id);
      req.onerror = () => {
        OfflineQueueService.memoryFallback.push(fullSubmission);
        resolve(id);
      };
    });
  }

  async getQueuedSubmissions(): Promise<QueuedSubmission[]> {
    const db = await this.openDB();
    if (!db) {
      return [...OfflineQueueService.memoryFallback];
    }

    return new Promise((resolve) => {
      const tx = db.transaction(OfflineQueueService.STORE_NAME, 'readonly');
      const store = tx.objectStore(OfflineQueueService.STORE_NAME);
      const req = store.getAll();
      req.onsuccess = () => {
        const items = req.result || [];
        resolve([...items, ...OfflineQueueService.memoryFallback]);
      };
      req.onerror = () => resolve([...OfflineQueueService.memoryFallback]);
    });
  }

  async removeSubmission(id: string): Promise<void> {
    OfflineQueueService.memoryFallback = OfflineQueueService.memoryFallback.filter((s) => s.id !== id);

    const db = await this.openDB();
    if (!db) return;

    return new Promise((resolve) => {
      const tx = db.transaction(OfflineQueueService.STORE_NAME, 'readwrite');
      const store = tx.objectStore(OfflineQueueService.STORE_NAME);
      const req = store.delete(id);
      req.onsuccess = () => resolve();
      req.onerror = () => resolve();
    });
  }

  async syncPendingSubmissions(
    syncHandler: (submission: QueuedSubmission) => Promise<boolean>
  ): Promise<{ syncedCount: number; failedCount: number; authRequired: boolean }> {
    const currentUser = auth.currentUser;
    if (!currentUser) {
      return { syncedCount: 0, failedCount: 0, authRequired: true };
    }

    const pending = await this.getQueuedSubmissions();
    let syncedCount = 0;
    let failedCount = 0;

    for (const item of pending) {
      try {
        const success = await syncHandler(item);
        if (success) {
          await this.removeSubmission(item.id);
          syncedCount++;
        } else {
          failedCount++;
        }
      } catch (err) {
        failedCount++;
      }
    }

    return { syncedCount, failedCount, authRequired: false };
  }
}
