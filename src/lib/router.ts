import { useEffect, useState } from "react";

export type Route =
  | { name: "library" }
  | { name: "book"; bookId: string }
  | { name: "editor"; docId: string }
  | { name: "history"; docId: string }
  | { name: "search" }
  | { name: "stats" }
  | { name: "trash" }
  | { name: "settings" }
  | { name: "not-found" };

export function parseRoute(raw: string): Route {
  const clean = raw.replace(/^#\/?/, "");
  const [head, id] = clean.split("/");
  switch (head) {
    case "":
    case "pustaka":
      return { name: "library" };
    case "buku":
      return id ? { name: "book", bookId: decodeURIComponent(id) } : { name: "library" };
    case "tulis":
      return id ? { name: "editor", docId: decodeURIComponent(id) } : { name: "library" };
    case "riwayat":
      return id ? { name: "history", docId: decodeURIComponent(id) } : { name: "library" };
    case "cari":
      return { name: "search" };
    case "statistik":
      return { name: "stats" };
    case "sampah":
      return { name: "trash" };
    case "pengaturan":
      return { name: "settings" };
    default:
      return { name: "not-found" };
  }
}

export function navigate(path: string): void {
  if (window.location.hash === path) return;
  window.location.hash = path;
}

export function useRoute(): Route {
  const [route, setRoute] = useState<Route>(() => parseRoute(window.location.hash));

  useEffect(() => {
    const onChange = () => setRoute(parseRoute(window.location.hash));
    window.addEventListener("hashchange", onChange);
    if (!window.location.hash) {
      window.history.replaceState(null, "", "#/pustaka");
      setRoute({ name: "library" });
    }
    return () => window.removeEventListener("hashchange", onChange);
  }, []);

  return route;
}
