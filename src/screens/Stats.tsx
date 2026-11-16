import { useMemo, type CSSProperties } from "react";
import { ArrowRight, BarChart3, BookOpen, CalendarDays, Flame, PenLine, Target, TrendingUp } from "lucide-react";
import { useStore } from "../lib/store";
import {
  compactNumber,
  countWords,
  dayKey,
  daysBack,
  formatNumber,
  formatShortDate,
  timeAgo,
} from "../lib/text";
import { PageHeader } from "../components/PageHeader";

function levelFor(words: number, goal: number): number {
  if (words <= 0) return 0;
  if (words < goal * 0.25) return 1;
  if (words < goal * 0.6) return 2;
  if (words < goal) return 3;
  return 4;
}

export function StatsScreen() {
  const { data } = useStore();
  const goal = Math.max(1, data.settings.dailyGoal);
  const liveDocs = useMemo(() => data.docs.filter((doc) => doc.deletedAt === null), [data.docs]);
  const libraryWords = useMemo(
    () => liveDocs.reduce((total, doc) => total + countWords(doc.body), 0),
    [liveDocs],
  );
  const today = data.stats[dayKey()] ?? 0;
  const goalPercent = Math.min(100, Math.round((today / goal) * 100));

  const days = useMemo(() => daysBack(119), []);
  const dayValues = useMemo(() => days.map((day) => data.stats[day] ?? 0), [days, data.stats]);
  const maxDay = Math.max(...dayValues, 1);

  const week = useMemo(() => daysBack(7), []);
  const weekValues = useMemo(() => week.map((day) => data.stats[day] ?? 0), [week, data.stats]);
  const maxWeek = Math.max(...weekValues, 1);

  const { streak, best, activeDays, lifetime } = useMemo(() => {
    let run = 0;
    const cursor = new Date();
    cursor.setHours(12, 0, 0, 0);
    if (!data.stats[dayKey(cursor)]) cursor.setDate(cursor.getDate() - 1);
    while (data.stats[dayKey(cursor)] > 0 && run < 3700) {
      run += 1;
      cursor.setDate(cursor.getDate() - 1);
    }
    const entries = Object.entries(data.stats).filter(([, value]) => value > 0);
    const bestEntry = entries.sort((a, b) => b[1] - a[1])[0] ?? null;
    return {
      streak: run,
      best: bestEntry,
      activeDays: entries.length,
      lifetime: entries.reduce((total, [, value]) => total + value, 0),
    };
  }, [data.stats]);

  const recent = useMemo(
    () => [...liveDocs].sort((a, b) => b.updatedAt - a.updatedAt).slice(0, 5),
    [liveDocs],
  );

  const weekday = new Intl.DateTimeFormat("id-ID", { weekday: "short" });

  return (
    <div className="stats-screen">
      <PageHeader
        eyebrow="Ritme"
        title="Statistik menulis"
        subtitle="Angka yang tumbuh pelan-pelan. Dihitung dari kata yang benar-benar kamu tambahkan, bukan dari target yang belum terjadi."
        actions={
          <a className="btn" href="#/pengaturan">
            <Target size={16} />
            Atur target harian
          </a>
        }
      />

      <section className="stats-grid">
        <article className="stat-card stat-card--accent">
          <span className="stat-icon">
            <PenLine size={17} />
          </span>
          <span className="stat-label">Kata hari ini</span>
          <strong className="stat-value">{formatNumber(today)}</strong>
          <span className="stat-note">Target {formatNumber(goal)} kata</span>
          <span className="stat-bar" aria-hidden="true">
            <span style={{ width: `${goalPercent}%` }} />
          </span>
        </article>
        <article className="stat-card">
          <span className="stat-icon">
            <Flame size={17} />
          </span>
          <span className="stat-label">Runtutan</span>
          <strong className="stat-value">{formatNumber(streak)}</strong>
          <span className="stat-note">hari berturut-turut</span>
        </article>
        <article className="stat-card">
          <span className="stat-icon">
            <BookOpen size={17} />
          </span>
          <span className="stat-label">Kata di pustaka</span>
          <strong className="stat-value">{compactNumber(libraryWords)}</strong>
          <span className="stat-note">{formatNumber(liveDocs.length)} naskah aktif</span>
        </article>
        <article className="stat-card">
          <span className="stat-icon">
            <CalendarDays size={17} />
          </span>
          <span className="stat-label">Hari aktif</span>
          <strong className="stat-value">{formatNumber(activeDays)}</strong>
          <span className="stat-note">
            {best
              ? `Terbaik: ${formatNumber(best[1])} kata, ${formatShortDate(
                  new Date(`${best[0]}T12:00:00`).getTime(),
                )}`
              : "Belum ada rekor"}
          </span>
        </article>
      </section>

      <section className="card heatmap-card">
        <header className="heatmap-head">
          <div>
            <h2>Ritme 17 minggu</h2>
            <p className="hint">
              Setiap kotak satu hari. Warna mengikuti seberapa dekat harimu dengan target{" "}
              {formatNumber(goal)} kata. Total {formatNumber(lifetime)} kata tercatat.
            </p>
          </div>
          <div
            className="goal-ring"
            style={{ "--pct": `${goalPercent}%` } as CSSProperties}
            role="img"
            aria-label={`${goalPercent} persen dari target hari ini`}
          >
            <span>{goalPercent}%</span>
          </div>
        </header>
        <div className="heatmap-wrap">
          <div className="heatmap" role="group" aria-label="Peta panas aktivitas menulis">
            {days.map((day, index) => {
              const value = dayValues[index];
              return (
                <span
                  key={day}
                  className={`heat-cell${day === dayKey() ? " is-today" : ""}`}
                  data-level={levelFor(value, goal)}
                  title={`${day}: ${formatNumber(value)} kata`}
                  aria-label={`${day}: ${formatNumber(value)} kata`}
                  role="img"
                />
              );
            })}
          </div>
          <div className="heat-legend" aria-hidden="true">
            <span>Sedikit</span>
            {[0, 1, 2, 3, 4].map((level) => (
              <span key={level} className="heat-cell" data-level={level} />
            ))}
            <span>Banyak</span>
          </div>
        </div>
        <div className="heat-footer">
          <span className="hint">
            Hari paling produktif: {best ? `${formatNumber(best[1])} kata` : "belum ada"}
          </span>
          <span className="hint">Rata-rata per hari aktif: {activeDays ? formatNumber(Math.round(lifetime / activeDays)) : 0} kata</span>
        </div>
      </section>

      <div className="stats-lower">
        <section className="card week-card">
          <header className="card-head">
            <div>
              <h2>Tujuh hari terakhir</h2>
              <p className="hint">Termasuk hari ini.</p>
            </div>
            <TrendingUp size={18} aria-hidden="true" />
          </header>
          <div className="week-chart" style={{ "--max": maxDay } as CSSProperties}>
            {week.map((day, index) => {
              const value = weekValues[index];
              const height = value === 0 ? 3 : Math.max(6, Math.round((value / maxWeek) * 100));
              return (
                <div className="week-col" key={day}>
                  <span className="week-bar-track">
                    <span
                      className={`week-bar${value === 0 ? " is-empty" : ""}`}
                      style={{ height: `${height}%` }}
                      title={`${formatNumber(value)} kata`}
                    />
                  </span>
                  <span className="week-label">
                    {weekday.format(new Date(`${day}T12:00:00`))}
                  </span>
                  <span className="week-value">{value === 0 ? "–" : compactNumber(value)}</span>
                </div>
              );
            })}
          </div>
        </section>

        <section className="card recent-card">
          <header className="card-head">
            <div>
              <h2>Terakhir disentuh</h2>
              <p className="hint">Lima naskah yang paling baru diperbarui.</p>
            </div>
            <BarChart3 size={18} aria-hidden="true" />
          </header>
          {recent.length === 0 ? (
            <p className="hint">Belum ada naskah. Mulai dari pustaka.</p>
          ) : (
            <ul className="recent-list">
              {recent.map((doc) => {
                const book = data.books.find((item) => item.id === doc.bookId);
                return (
                  <li key={doc.id}>
                    <a href={`#/tulis/${doc.id}`}>
                      <span className="recent-title">{doc.title || "Tanpa judul"}</span>
                      <span className="recent-meta">
                        {book?.title ?? "Tanpa buku"} · {formatNumber(countWords(doc.body))} kata ·{" "}
                        {timeAgo(doc.updatedAt)}
                      </span>
                    </a>
                    <ArrowRight size={15} aria-hidden="true" />
                  </li>
                );
              })}
            </ul>
          )}
        </section>
      </div>
    </div>
  );
}
