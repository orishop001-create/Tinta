import type { ReactNode } from "react";
import {
  BarChart3,
  Droplet,
  Library,
  Search,
  Settings,
  ShieldCheck,
  Sparkles,
  Trash2,
} from "lucide-react";
import type { Route } from "../lib/router";
import { useStore } from "../lib/store";

export const NAV_ITEMS = [
  { path: "#/pustaka", label: "Pustaka", icon: Library, match: ["library", "book"] },
  { path: "#/cari", label: "Cari", icon: Search, match: ["search"] },
  { path: "#/statistik", label: "Statistik", icon: BarChart3, match: ["stats"] },
  { path: "#/sampah", label: "Sampah", icon: Trash2, match: ["trash"] },
  { path: "#/pengaturan", label: "Setelan", icon: Settings, match: ["settings"] },
];

function isActive(route: Route, matches: string[]): boolean {
  return matches.includes(route.name);
}

export function Brand({ compact = false }: { compact?: boolean }) {
  return (
    <a className={`brand${compact ? " brand--compact" : ""}`} href="#/pustaka" aria-label="Tinta, ke pustaka">
      <span className="brand-mark" aria-hidden="true">
        <Droplet size={19} strokeWidth={2.3} />
      </span>
      <span className="brand-text">
        <span className="brand-name">tinta</span>
        {compact ? null : <span className="brand-tag">ruang menulis</span>}
      </span>
    </a>
  );
}

function FreeCard() {
  return (
    <div className="free-card">
      <div className="free-card-head">
        <Sparkles size={15} aria-hidden="true" />
        <strong>Semua terbuka</strong>
      </div>
      <p>Tanpa akun, tanpa iklan, tanpa versi Pro. Fitur yang biasanya dikunci semuanya ada di sini.</p>
      <span className="free-badge">100% gratis</span>
    </div>
  );
}

export function Sidebar({ route }: { route: Route }) {
  const { data } = useStore();
  const trashCount = data.docs.filter((doc) => doc.deletedAt !== null).length;

  return (
    <aside className="sidebar">
      <Brand />
      <nav className="nav" aria-label="Navigasi utama">
        <span className="nav-heading">Ruang kerja</span>
        {NAV_ITEMS.map((item) => {
          const Icon = item.icon;
          const active = isActive(route, item.match);
          return (
            <a
              key={item.path}
              href={item.path}
              className={`nav-link${active ? " is-active" : ""}`}
              aria-current={active ? "page" : undefined}
            >
              <Icon size={18} aria-hidden="true" />
              <span>{item.label}</span>
              {item.label === "Sampah" && trashCount > 0 ? (
                <span className="nav-count">{trashCount}</span>
              ) : null}
            </a>
          );
        })}
      </nav>

      <div className="sidebar-spacer" />
      <FreeCard />
      <p className="sidebar-foot">
        <ShieldCheck size={13} aria-hidden="true" />
        Data tinggal di peramban ini
      </p>
    </aside>
  );
}

export function MobileTopBar() {
  return (
    <div className="mobile-top">
      <Brand compact />
      <a className="icon-btn" href="#/cari" aria-label="Cari di semua naskah">
        <Search size={19} />
      </a>
    </div>
  );
}

export function MobileNav({ route }: { route: Route }) {
  const { data } = useStore();
  const trashCount = data.docs.filter((doc) => doc.deletedAt !== null).length;

  return (
    <nav className="bottom-nav" aria-label="Navigasi bawah">
      {NAV_ITEMS.map((item) => {
        const Icon = item.icon;
        const active = isActive(route, item.match);
        return (
          <a
            key={item.path}
            href={item.path}
            className={`bottom-link${active ? " is-active" : ""}`}
            aria-current={active ? "page" : undefined}
          >
            <span className="bottom-icon">
              <Icon size={20} aria-hidden="true" />
              {item.label === "Sampah" && trashCount > 0 ? (
                <span className="bottom-dot" aria-label={`${trashCount} naskah di sampah`} />
              ) : null}
            </span>
            <span>{item.label}</span>
          </a>
        );
      })}
    </nav>
  );
}

export function Shell({ route, children }: { route: Route; children: ReactNode }) {
  return (
    <div className="app">
      <Sidebar route={route} />
      <div className="main">
        <MobileTopBar />
        <main className="main-scroll" id="main">
          {children}
        </main>
      </div>
      <MobileNav route={route} />
    </div>
  );
}
