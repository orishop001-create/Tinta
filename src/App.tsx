import { useEffect } from "react";
import { useRoute } from "./lib/router";
import { useStore } from "./lib/store";
import { Shell } from "./components/Shell";
import { Toasts } from "./components/Toasts";
import { LibraryScreen } from "./screens/Library";
import { BookView } from "./screens/BookView";
import { EditorScreen } from "./screens/Editor";
import { HistoryScreen } from "./screens/History";
import { SearchScreen } from "./screens/Search";
import { StatsScreen } from "./screens/Stats";
import { TrashScreen } from "./screens/Trash";
import { SettingsScreen } from "./screens/Settings";
import { NotFoundScreen } from "./screens/NotFound";

const TITLES: Record<string, string> = {
  library: "Pustaka",
  book: "Buku",
  history: "Riwayat",
  search: "Cari & ganti",
  stats: "Statistik",
  trash: "Sampah",
  settings: "Setelan",
  "not-found": "Tidak ditemukan",
};

export default function App() {
  const route = useRoute();
  const { data } = useStore();
  const settings = data.settings;

  useEffect(() => {
    const root = document.documentElement;
    root.dataset.theme = settings.theme;
    root.style.setProperty("--editor-size", `${settings.fontSize}px`);
    root.style.setProperty("--editor-leading", String(settings.lineHeight));
    root.style.setProperty("--editor-para", `${settings.paragraphSpacing}px`);
    root.style.setProperty("--page-width", `${settings.pageWidth}px`);
    root.style.setProperty(
      "--editor-font",
      settings.font === "serif"
        ? "var(--font-serif)"
        : settings.font === "sans"
          ? "var(--font-sans)"
          : "var(--font-mono)",
    );
    const meta = document.querySelector('meta[name="theme-color"]');
    meta?.setAttribute(
      "content",
      settings.theme === "night" || settings.theme === "graphite" ? "#121616" : "#f4efe4",
    );
  }, [settings]);

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: "auto" });
  }, [route]);

  const activeDoc =
    route.name === "editor" || route.name === "history"
      ? data.docs.find((doc) => doc.id === route.docId)
      : null;

  useEffect(() => {
    const page = activeDoc?.title || TITLES[route.name] || "Tinta";
    document.title = page === "Tinta" ? "Tinta — menulis tanpa kehilangan" : `${page} — Tinta`;
  }, [route.name, activeDoc?.title]);

  if (route.name === "editor") {
    return (
      <>
        <EditorScreen docId={route.docId} />
        <Toasts />
      </>
    );
  }

  if (route.name === "history") {
    return (
      <>
        <HistoryScreen docId={route.docId} />
        <Toasts />
      </>
    );
  }

  let screen;
  switch (route.name) {
    case "library":
      screen = <LibraryScreen />;
      break;
    case "book":
      screen = <BookView bookId={route.bookId} />;
      break;
    case "search":
      screen = <SearchScreen />;
      break;
    case "stats":
      screen = <StatsScreen />;
      break;
    case "trash":
      screen = <TrashScreen />;
      break;
    case "settings":
      screen = <SettingsScreen />;
      break;
    default:
      screen = <NotFoundScreen />;
  }

  return (
    <>
      <Shell route={route}>{screen}</Shell>
      <Toasts />
    </>
  );
}
