export default function MetricsSkeleton() {
  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-6 animate-pulse">
      {[1, 2, 3].map((id) => (
        <div key={id} className="bg-slate-800 p-6 rounded-xl border border-slate-700 h-36 flex flex-col justify-between">
          <div className="h-4 bg-slate-700 rounded w-1/3"></div>
          <div className="h-8 bg-slate-700 rounded w-1/2"></div>
          <div className="h-3 bg-slate-700 rounded w-2/3"></div>
        </div>
      ))}
    </div>
  );
}  