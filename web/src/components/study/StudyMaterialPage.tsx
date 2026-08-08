import { FC, useEffect, useState } from 'react';
import { BookOpen, Clock, CheckCircle, Search, ChevronRight, X } from 'lucide-react';
import { strings } from '../../constants/strings';
import { StudyMaterialViewModel } from '../../viewmodels/StudyMaterialViewModel';
import { ContentRepository } from '../../repositories/ContentRepository';
import { StudyMaterial } from '../../types/testContent';

export interface StudyMaterialPageProps {
  viewModel?: StudyMaterialViewModel;
  onSelectMaterial?: (material: StudyMaterial) => void;
}

export const StudyMaterialPage: FC<StudyMaterialPageProps> = ({ viewModel, onSelectMaterial }) => {
  const [vm] = useState<StudyMaterialViewModel>(() => viewModel || new StudyMaterialViewModel(new ContentRepository()));
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('All');
  const [selectedMaterial, setSelectedMaterial] = useState<StudyMaterial | null>(null);
  const [, setRefreshState] = useState(0);

  useEffect(() => {
    vm.loadMaterials().then(() => setRefreshState((prev) => prev + 1));
  }, [vm]);

  const categories = vm.getCategories();
  const rawMaterials = vm.getMaterials();

  const filteredMaterials = rawMaterials.filter((material) => {
    const matchesCategory = selectedCategory === 'All' || material.category.toLowerCase() === selectedCategory.toLowerCase();
    const matchesSearch =
      material.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      material.summary.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesCategory && matchesSearch;
  });

  const handleCategoryChange = (cat: string) => {
    setSelectedCategory(cat);
    vm.setCategoryFilter(cat);
  };

  const handleToggleComplete = (id: string, e: React.MouseEvent) => {
    e.stopPropagation();
    vm.markAsCompleted(id);
    setRefreshState((prev) => prev + 1);
  };

  const openMaterial = (material: StudyMaterial) => {
    setSelectedMaterial(material);
    onSelectMaterial?.(material);
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-300" data-testid="study-material-page">
      {/* Header Banner */}
      <div className="bg-slate-800/80 border border-slate-700/80 rounded-2xl p-6 shadow-lg backdrop-blur-sm">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 text-xs font-bold text-sky-400 uppercase tracking-widest mb-1">
              <BookOpen className="w-4 h-4" />
              <span>{strings.nav.study}</span>
            </div>
            <h1 className="text-2xl font-black text-white tracking-tight">{strings.studyMaterial.title}</h1>
            <p className="text-xs text-slate-400 mt-1 max-w-2xl">{strings.dashboard.subtitle}</p>
          </div>

          <div className="relative w-full md:w-64">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder={strings.practice.searchPlaceholder}
              className="w-full bg-slate-900/80 border border-slate-700/80 rounded-xl pl-9 pr-4 py-2 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-sky-500 transition-colors"
              data-testid="search-input"
            />
          </div>
        </div>

        {/* Category Tabs */}
        <div className="flex items-center gap-2 overflow-x-auto mt-6 pt-4 border-t border-slate-700/60 pb-1">
          {categories.map((cat) => (
            <button
              key={cat}
              onClick={() => handleCategoryChange(cat)}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition-colors ${
                selectedCategory === cat
                  ? 'bg-sky-600 text-white shadow-sm'
                  : 'bg-slate-900/60 hover:bg-slate-700 text-slate-300 border border-slate-800'
              }`}
              data-testid={`category-tab-${cat.toLowerCase()}`}
            >
              {cat === 'All' ? strings.studyMaterial.allCategories : cat}
            </button>
          ))}
        </div>
      </div>

      {/* Grid of Materials */}
      {vm.getIsLoading() ? (
        <div className="text-center py-12 text-slate-400 text-xs">{strings.common.loading}</div>
      ) : filteredMaterials.length === 0 ? (
        <div className="text-center py-12 text-slate-400 text-xs bg-slate-800/80 border border-slate-700/80 rounded-2xl">
          {strings.studyMaterial.noMaterials}
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filteredMaterials.map((material) => {
            const isDone = vm.isCompleted(material.id);
            return (
              <div
                key={material.id}
                onClick={() => openMaterial(material)}
                className="bg-slate-800/80 border border-slate-700/80 rounded-xl p-5 flex flex-col justify-between hover:border-slate-600 transition-all cursor-pointer shadow-md group"
                data-testid={`material-card-${material.id}`}
              >
                <div>
                  <div className="flex items-center justify-between mb-3">
                    <span className="px-2 py-0.5 rounded bg-sky-500/20 text-sky-400 border border-sky-500/30 text-[10px] font-bold uppercase tracking-wider">
                      {material.category}
                    </span>
                    <span className="text-[11px] text-slate-400 flex items-center gap-1 font-mono">
                      <Clock className="w-3 h-3 text-slate-500" />
                      {strings.studyMaterial.readTime.replace('{min}', String(material.estimatedReadTimeMinutes))}
                    </span>
                  </div>

                  <h3 className="text-sm font-bold text-white group-hover:text-sky-300 transition-colors mb-2 leading-snug">
                    {material.title}
                  </h3>
                  <p className="text-xs text-slate-400 line-clamp-3 leading-relaxed mb-4">{material.summary}</p>
                </div>

                <div className="flex items-center justify-between pt-3 border-t border-slate-700/60 text-xs">
                  <button
                    onClick={(e) => handleToggleComplete(material.id, e)}
                    className={`flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-[11px] font-semibold transition-colors ${
                      isDone
                        ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'
                        : 'bg-slate-900/60 hover:bg-slate-700 text-slate-400 border border-slate-800'
                    }`}
                    data-testid={`mark-read-btn-${material.id}`}
                  >
                    <CheckCircle className="w-3.5 h-3.5" />
                    <span>{isDone ? strings.studyMaterial.completed : strings.studyMaterial.markAsRead}</span>
                  </button>

                  <span className="text-sky-400 flex items-center gap-1 font-semibold group-hover:translate-x-1 transition-transform">
                    <span>Read</span>
                    <ChevronRight className="w-4 h-4" />
                  </span>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Material Detail Reader Modal */}
      {selectedMaterial && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-md flex items-center justify-center p-4" data-testid="material-modal">
          <div className="bg-slate-900 border border-slate-700 rounded-2xl max-w-2xl w-full max-h-[85vh] flex flex-col shadow-2xl overflow-hidden animate-in zoom-in-95 duration-200">
            <div className="p-5 border-b border-slate-800 flex items-center justify-between bg-slate-900/90">
              <div className="flex items-center gap-2">
                <span className="px-2 py-0.5 rounded bg-sky-500/20 text-sky-400 border border-sky-500/30 text-[10px] font-bold uppercase">
                  {selectedMaterial.category}
                </span>
                <span className="text-xs text-slate-400">{strings.studyMaterial.offlineNotice}</span>
              </div>
              <button
                onClick={() => setSelectedMaterial(null)}
                className="p-1 rounded-lg hover:bg-slate-800 text-slate-400 hover:text-white transition-colors"
                data-testid="close-material-modal"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 overflow-y-auto space-y-4 text-slate-200 text-xs leading-relaxed">
              <h2 className="text-lg font-bold text-white mb-2">{selectedMaterial.title}</h2>
              <div className="bg-slate-800/60 p-4 rounded-xl border border-slate-700/60 text-slate-300 font-mono text-[11px] whitespace-pre-wrap">
                {selectedMaterial.contentMarkdown || selectedMaterial.summary}
              </div>
            </div>

            <div className="p-4 border-t border-slate-800 bg-slate-900/90 flex justify-end">
              <button
                onClick={() => setSelectedMaterial(null)}
                className="px-4 py-2 bg-sky-600 hover:bg-sky-500 text-white rounded-xl text-xs font-bold transition-colors"
              >
                {strings.common.close}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default StudyMaterialPage;
