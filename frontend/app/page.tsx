"use client";

export default function Home() {
  return (
    <main className="min-h-screen bg-slate-950 text-white flex items-center justify-center p-8">
      <section className="max-w-4xl w-full">
        <p className="text-cyan-400 font-semibold tracking-widest">ARAMA / AI CAREER</p>
        <h1 className="text-5xl md:text-7xl font-bold mt-4">Kariyerinin bir sonraki adımını bul.</h1>
        <p className="text-slate-300 text-lg mt-6 max-w-2xl">CV&apos;ni analiz et, yeteneklerini keşfet ve sana en uygun iş fırsatlarını açıklanabilir yapay zekâ ile eşleştir.</p>
        <div className="flex gap-4 mt-8">
          <button className="rounded-xl bg-cyan-400 text-slate-950 px-6 py-3 font-bold">CV&apos;ni Analiz Et</button>
          <button className="rounded-xl border border-slate-700 px-6 py-3">İşleri Keşfet</button>
        </div>
      </section>
    </main>
  );
}
