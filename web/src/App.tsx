import { FC } from 'react';
import { Header } from './components/common/Header';
import { strings } from './constants/strings';

export const App: FC = () => {
  return (
    <div className="min-h-screen bg-slate-900 text-slate-50 flex flex-col">
      <Header />
      <main className="flex-1 flex flex-col items-center justify-center p-6 text-center">
        <div className="max-w-xl space-y-4">
          <h2 className="text-3xl font-extrabold tracking-tight text-sky-400 sm:text-4xl">
            {strings.common.appName}
          </h2>
          <p className="text-lg text-slate-300">
            {strings.common.appTagline}
          </p>
        </div>
      </main>
    </div>
  );
};

export default App;
