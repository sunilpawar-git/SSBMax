import { StudyMaterial, OIRQuestion, BatchDocument } from '../../types/testContent';

export interface IContentRepository {
  getStudyMaterials(): Promise<StudyMaterial[]>;
  getStudyMaterialById(id: string): Promise<StudyMaterial | null>;
  getOIRQuestions(batchIndex?: number): Promise<OIRQuestion[]>;
  getCappedBatch<T>(collectionName: string, batchIndex?: number, maxItems?: number): Promise<BatchDocument<T>>;
}
