import { collection, doc, getDoc, getDocs, query, limit } from 'firebase/firestore';
import { db } from '../config/firebase';
import { IContentRepository } from './interfaces/IContentRepository';
import { StudyMaterial, OIRQuestion, BatchDocument } from '../types/testContent';

export class ContentRepository implements IContentRepository {
  private static readonly MAX_BATCH_ITEMS = 50;

  async getStudyMaterials(): Promise<StudyMaterial[]> {
    try {
      const materialsRef = collection(db, 'studyMaterials');
      const q = query(materialsRef, limit(50));
      const querySnapshot = await getDocs(q);

      const materials: StudyMaterial[] = [];
      querySnapshot.forEach((docSnap) => {
        const data = docSnap.data();
        materials.push({
          id: docSnap.id,
          title: data.title || '',
          category: data.category || 'General',
          summary: data.summary || '',
          contentMarkdown: data.contentMarkdown || '',
          estimatedReadTimeMinutes: data.estimatedReadTimeMinutes || 5,
          tags: data.tags || [],
          createdAt: data.createdAt || new Date().toISOString()
        });
      });

      if (materials.length === 0) {
        return this.getFallbackStudyMaterials();
      }

      return materials;
    } catch (error) {
      console.warn('Failed to fetch study materials from Firestore, using offline fallback', error);
      return this.getFallbackStudyMaterials();
    }
  }

  async getStudyMaterialById(id: string): Promise<StudyMaterial | null> {
    try {
      const docRef = doc(db, 'studyMaterials', id);
      const docSnap = await getDoc(docRef);

      if (!docSnap.exists()) {
        const fallback = this.getFallbackStudyMaterials().find((m) => m.id === id);
        return fallback || null;
      }

      const data = docSnap.data();
      return {
        id: docSnap.id,
        title: data.title || '',
        category: data.category || 'General',
        summary: data.summary || '',
        contentMarkdown: data.contentMarkdown || '',
        estimatedReadTimeMinutes: data.estimatedReadTimeMinutes || 5,
        tags: data.tags || [],
        createdAt: data.createdAt || new Date().toISOString()
      };
    } catch (error) {
      console.warn(`Failed to fetch study material ${id}, checking offline fallback`, error);
      const fallback = this.getFallbackStudyMaterials().find((m) => m.id === id);
      return fallback || null;
    }
  }

  async getOIRQuestions(batchIndex = 0): Promise<OIRQuestion[]> {
    const batch = await this.getCappedBatch<OIRQuestion>('oirQuestions', batchIndex);
    return batch.items;
  }

  async getCappedBatch<T>(
    collectionName: string,
    batchIndex = 0,
    maxItems = ContentRepository.MAX_BATCH_ITEMS
  ): Promise<BatchDocument<T>> {
    try {
      const docId = `batch_${batchIndex}`;
      const docRef = doc(db, collectionName, docId);
      const docSnap = await getDoc(docRef);

      if (!docSnap.exists()) {
        return this.getFallbackBatch<T>(collectionName, batchIndex, maxItems);
      }

      const data = docSnap.data();
      const rawItems: T[] = data.items || [];
      const cappedItems = rawItems.slice(0, maxItems);

      return {
        id: docSnap.id,
        batchIndex: data.batchIndex ?? batchIndex,
        totalItems: cappedItems.length,
        items: cappedItems
      };
    } catch (error) {
      console.warn(`Failed to fetch batch ${batchIndex} for ${collectionName}, using fallback`, error);
      return this.getFallbackBatch<T>(collectionName, batchIndex, maxItems);
    }
  }

  private getFallbackStudyMaterials(): StudyMaterial[] {
    return [
      {
        id: 'ssb-overview-01',
        title: 'SSB Interview 5-Day Selection Process Overview',
        category: 'SSB Basics',
        summary: 'A complete breakdown of Day 1 to Day 5 at Services Selection Board.',
        contentMarkdown: '# SSB 5-Day Process\n\n- **Day 1**: Screening (OIR & PPDT)\n- **Day 2**: Psychology Tests (TAT, WAT, SRT, SD)\n- **Day 3 & 4**: GTO Tasks & Personal Interview\n- **Day 5**: Conference',
        estimatedReadTimeMinutes: 6,
        tags: ['SSB', 'Screening', 'Overview'],
        createdAt: '2026-01-01T00:00:00Z'
      },
      {
        id: 'oir-tips-02',
        title: 'Mastering OIR: Verbal & Non-Verbal Reasoning',
        category: 'OIR',
        summary: 'Essential strategies for achieving OIR Rating 1 in Stage-1 screening.',
        contentMarkdown: '# OIR Preparation Strategies\n\nSpeed and accuracy are crucial for OIR Rating 1.',
        estimatedReadTimeMinutes: 4,
        tags: ['OIR', 'Reasoning', 'Screening'],
        createdAt: '2026-01-02T00:00:00Z'
      }
    ];
  }

  private getFallbackBatch<T>(collectionName: string, batchIndex: number, maxItems: number): BatchDocument<T> {
    const fallbackItems: OIRQuestion[] = [];
    if (collectionName === 'oirQuestions') {
      for (let i = 1; i <= Math.min(50, maxItems); i++) {
        fallbackItems.push({
          id: `oir_${batchIndex}_${i}`,
          questionNumber: i,
          questionText: `Sample OIR Question #${i}: Find the odd one out.`,
          options: ['Option A', 'Option B', 'Option C', 'Option D'],
          type: i % 2 === 0 ? 'VERBAL' : 'NON_VERBAL'
        });
      }
    }
    return {
      id: `batch_${batchIndex}`,
      batchIndex,
      totalItems: fallbackItems.length,
      items: fallbackItems as unknown as T[]
    };
  }
}
