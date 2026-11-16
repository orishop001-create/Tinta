import {
  createContext,
  useContext,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";
import type {
  AppData,
  Book,
  Doc,
  Settings,
  Snapshot,
  SnapshotReason,
  ThemeId,
  Volume,
} from "./types";
import { countWords, dayKey, uid } from "./text";
import { seedData } from "./seed";
import { SCHEMA_VERSION, STORAGE_KEY, emptyData } from "./defaults";
import { toast } from "./toast";

const MAX_SNAPSHOTS_PER_DOC = 40;

export interface BookInput {
  title: string;
  author?: string;
  tags?: string[];
  color?: string;
}

export interface DocPatch {
  title?: string;
  body?: string;
  markdown?: boolean;
}

export interface Actions {
  createBook: (input: BookInput) => string;
  updateBook: (id: string, patch: Partial<Book>) => void;
  deleteBook: (id: string) => void;
  createVolume: (bookId: string, title: string) => string;
  renameVolume: (id: string, title: string) => void;
  deleteVolume: (id: string) => void;
  createDoc: (bookId: string, volumeId: string | null, title?: string) => string;
  updateDoc: (id: string, patch: DocPatch) => void;
  deleteDoc: (id: string) => void;
  restoreDoc: (id: string) => void;
  purgeDoc: (id: string) => void;
  emptyTrash: () => void;
  duplicateDoc: (id: string) => string | null;
  moveDoc: (id: string, bookId: string, volumeId: string | null) => void;
  placeDocs: (docIds: string[], bookId: string, volumeId: string | null) => void;
  reorderVolumes: (bookId: string, orderedIds: string[]) => void;
  snapshot: (docId: string, reason: SnapshotReason) => void;
  restoreSnapshot: (snapshotId: string) => void;
  updateSettings: (patch: Partial<Settings>) => void;
  importData: (raw: string) => { ok: true } | { ok: false; error: string };
  resetData: () => void;
}

interface StoreValue {
  data: AppData;
  actions: Actions;
}

const StoreContext = createContext<StoreValue | null>(null);

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

const THEMES: ThemeId[] = ["paper", "sepia", "night", "graphite"];

function clamp(value: number, min: number, max: number, fallback: number): number {
  if (!Number.isFinite(value)) return fallback;
  return Math.min(max, Math.max(min, value));
}

function normalize(input: unknown): AppData {
  const base = emptyData();
  if (!isRecord(input)) return base;

  const rawSettings = isRecord(input.settings) ? input.settings : {};
  const phrases = Array.isArray(rawSettings.phrases)
    ? rawSettings.phrases.filter((item): item is string => typeof item === "string").slice(0, 40)
    : base.settings.phrases;
  const settings: Settings = {
    ...base.settings,
    theme: THEMES.includes(rawSettings.theme as ThemeId)
      ? (rawSettings.theme as ThemeId)
      : base.settings.theme,
    font: ["serif", "sans", "mono"].includes(String(rawSettings.font))
      ? (rawSettings.font as Settings["font"])
      : base.settings.font,
    fontSize: clamp(Number(rawSettings.fontSize), 14, 30, base.settings.fontSize),
    lineHeight: clamp(Number(rawSettings.lineHeight), 1.3, 2.4, base.settings.lineHeight),
    paragraphSpacing: clamp(
      Number(rawSettings.paragraphSpacing),
      0,
      32,
      base.settings.paragraphSpacing,
    ),
    pageWidth: clamp(Number(rawSettings.pageWidth), 520, 960, base.settings.pageWidth),
    typewriter: Boolean(rawSettings.typewriter),
    rulings: Boolean(rawSettings.rulings),
    dailyGoal: clamp(Number(rawSettings.dailyGoal), 50, 10_000, base.settings.dailyGoal),
    phrases: phrases.length > 0 ? phrases : base.settings.phrases,
    lastDocId: typeof rawSettings.lastDocId === "string" ? rawSettings.lastDocId : null,
  };

  const books: Book[] = (Array.isArray(input.books) ? input.books : []).flatMap((item) => {
    if (!isRecord(item) || typeof item.id !== "string" || typeof item.title !== "string") return [];
    return [
      {
        id: item.id,
        title: item.title,
        author: typeof item.author === "string" ? item.author : "",
        color: typeof item.color === "string" ? item.color : "clay",
        tags: Array.isArray(item.tags)
          ? item.tags.filter((tag): tag is string => typeof tag === "string")
          : [],
        createdAt: Number(item.createdAt) || Date.now(),
        updatedAt: Number(item.updatedAt) || Date.now(),
      },
    ];
  });
  const bookIds = new Set(books.map((book) => book.id));

  const volumes: Volume[] = (Array.isArray(input.volumes) ? input.volumes : []).flatMap((item) => {
    if (
      !isRecord(item) ||
      typeof item.id !== "string" ||
      typeof item.bookId !== "string" ||
      !bookIds.has(item.bookId) ||
      typeof item.title !== "string"
    ) {
      return [];
    }
    return [
      {
        id: item.id,
        bookId: item.bookId,
        title: item.title,
        order: Number(item.order) || 0,
        createdAt: Number(item.createdAt) || Date.now(),
      },
    ];
  });
  const volumeIds = new Set(volumes.map((volume) => volume.id));

  const docs: Doc[] = (Array.isArray(input.docs) ? input.docs : []).flatMap((item) => {
    if (
      !isRecord(item) ||
      typeof item.id !== "string" ||
      typeof item.bookId !== "string" ||
      !bookIds.has(item.bookId) ||
      typeof item.title !== "string"
    ) {
      return [];
    }
    return [
      {
        id: item.id,
        bookId: item.bookId,
        volumeId:
          typeof item.volumeId === "string" && volumeIds.has(item.volumeId) ? item.volumeId : null,
        title: item.title,
        body: typeof item.body === "string" ? item.body : "",
        markdown: Boolean(item.markdown),
        order: Number(item.order) || 0,
        createdAt: Number(item.createdAt) || Date.now(),
        updatedAt: Number(item.updatedAt) || Date.now(),
        deletedAt: Number(item.deletedAt) || null,
      },
    ];
  });
  const docIds = new Set(docs.map((doc) => doc.id));

  const snapshots: Snapshot[] = (Array.isArray(input.snapshots) ? input.snapshots : []).flatMap(
    (item) => {
      if (
        !isRecord(item) ||
        typeof item.id !== "string" ||
        typeof item.docId !== "string" ||
        !docIds.has(item.docId) ||
        typeof item.body !== "string"
      ) {
        return [];
      }
      const reason: SnapshotReason = ["auto", "manual", "before-restore"].includes(
        String(item.reason),
      )
        ? (item.reason as SnapshotReason)
        : "auto";
      return [
        {
          id: item.id,
          docId: item.docId,
          title: typeof item.title === "string" ? item.title : "",
          body: item.body,
          markdown: Boolean(item.markdown),
          reason,
          createdAt: Number(item.createdAt) || Date.now(),
        },
      ];
    },
  );

  const stats: Record<string, number> = {};
  if (isRecord(input.stats)) {
    for (const [key, value] of Object.entries(input.stats)) {
      if (/^\d{4}-\d{2}-\d{2}$/.test(key) && typeof value === "number" && value > 0) {
        stats[key] = Math.round(value);
      }
    }
  }

  return {
    schema: SCHEMA_VERSION,
    books,
    volumes,
    docs,
    snapshots,
    stats,
    settings,
  };
}

let quotaWarnedAt = 0;

function persist(data: AppData): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
  } catch (error) {
    console.error("Tinta: gagal menyimpan", error);
    if (Date.now() - quotaWarnedAt > 30_000) {
      quotaWarnedAt = Date.now();
      toast("Penyimpanan peramban penuh. Ekspor cadangan sekarang.", "danger");
    }
  }
}

function load(): AppData {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return seedData();
    return normalize(JSON.parse(raw));
  } catch (error) {
    console.error("Tinta: data rusak, memuat data contoh", error);
    return seedData();
  }
}

function pruneSnapshots(snapshots: Snapshot[]): Snapshot[] {
  const sorted = [...snapshots].sort((a, b) => b.createdAt - a.createdAt);
  const counts = new Map<string, number>();
  return sorted.filter((snapshot) => {
    const count = counts.get(snapshot.docId) ?? 0;
    if (count >= MAX_SNAPSHOTS_PER_DOC) return false;
    counts.set(snapshot.docId, count + 1);
    return true;
  });
}

export function StoreProvider({ children }: { children: ReactNode }) {
  const [data, setData] = useState<AppData>(load);
  const dataRef = useRef(data);
  dataRef.current = data;
  const baselineRef = useRef(JSON.stringify(data));

  // A tab that has no local edits must not overwrite newer data written by
  // another tab. If it finds a newer payload, it adopts it instead.
  const safePersist = useCallback((value: AppData) => {
    const serialized = JSON.stringify(value);
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw && raw !== serialized && serialized === baselineRef.current) {
      try {
        const next = normalize(JSON.parse(raw));
        baselineRef.current = JSON.stringify(next);
        setData(next);
        return;
      } catch (error) {
        console.error("Tinta: data terbaru dari tab lain tidak bisa dibaca", error);
      }
    }
    persist(value);
    baselineRef.current = serialized;
  }, []);

  useEffect(() => {
    const timer = window.setTimeout(() => safePersist(data), 350);
    return () => window.clearTimeout(timer);
  }, [data, safePersist]);

  useEffect(() => {
    const flush = () => safePersist(dataRef.current);
    const onVisibility = () => {
      if (document.visibilityState === "hidden") flush();
    };
    window.addEventListener("pagehide", flush);
    window.addEventListener("beforeunload", flush);
    document.addEventListener("visibilitychange", onVisibility);
    return () => {
      window.removeEventListener("pagehide", flush);
      window.removeEventListener("beforeunload", flush);
      document.removeEventListener("visibilitychange", onVisibility);
    };
  }, [safePersist]);

  // Keep already-open tabs reasonably fresh. A tab that is actively writing is
  // never overwritten mid-keystroke; it gets a hint to reload instead.
  useEffect(() => {
    const onStorage = (event: StorageEvent) => {
      if (event.key !== STORAGE_KEY || !event.newValue) return;
      try {
        const next = normalize(JSON.parse(event.newValue));
        if (window.location.hash.startsWith("#/tulis/")) {
          toast("Data berubah di tab lain. Muat ulang untuk menyegarkan tampilan.", "info");
          return;
        }
        baselineRef.current = JSON.stringify(next);
        setData(next);
      } catch (error) {
        console.error("Tinta: perubahan dari tab lain tidak bisa dibaca", error);
      }
    };
    window.addEventListener("storage", onStorage);
    return () => window.removeEventListener("storage", onStorage);
  }, []);

  const actions = useMemo<Actions>(() => {
    const createBook: Actions["createBook"] = (input) => {
      const id = uid();
      const now = Date.now();
      setData((prev) => ({
        ...prev,
        books: [
          {
            id,
            title: input.title.trim() || "Tanpa judul",
            author: input.author?.trim() ?? "",
            color: input.color ?? "clay",
            tags: (input.tags ?? []).map((tag) => tag.trim()).filter(Boolean).slice(0, 6),
            createdAt: now,
            updatedAt: now,
          },
          ...prev.books,
        ],
      }));
      return id;
    };

    const updateBook: Actions["updateBook"] = (id, patch) => {
      setData((prev) => ({
        ...prev,
        books: prev.books.map((book) =>
          book.id === id ? { ...book, ...patch, updatedAt: Date.now() } : book,
        ),
      }));
    };

    const deleteBook: Actions["deleteBook"] = (id) => {
      setData((prev) => {
        const removedDocs = new Set(prev.docs.filter((doc) => doc.bookId === id).map((doc) => doc.id));
        return {
          ...prev,
          books: prev.books.filter((book) => book.id !== id),
          volumes: prev.volumes.filter((volume) => volume.bookId !== id),
          docs: prev.docs.filter((doc) => doc.bookId !== id),
          snapshots: prev.snapshots.filter((snapshot) => !removedDocs.has(snapshot.docId)),
          settings: {
            ...prev.settings,
            lastDocId:
              prev.settings.lastDocId && removedDocs.has(prev.settings.lastDocId)
                ? null
                : prev.settings.lastDocId,
          },
        };
      });
    };

    const createVolume: Actions["createVolume"] = (bookId, title) => {
      const id = uid();
      setData((prev) => {
        const order =
          Math.max(-1, ...prev.volumes.filter((v) => v.bookId === bookId).map((v) => v.order)) + 1;
        return {
          ...prev,
          volumes: [
            ...prev.volumes,
            { id, bookId, title: title.trim() || "Bagian baru", order, createdAt: Date.now() },
          ],
        };
      });
      return id;
    };

    const renameVolume: Actions["renameVolume"] = (id, title) => {
      setData((prev) => ({
        ...prev,
        volumes: prev.volumes.map((volume) =>
          volume.id === id ? { ...volume, title: title.trim() || volume.title } : volume,
        ),
      }));
    };

    const deleteVolume: Actions["deleteVolume"] = (id) => {
      setData((prev) => ({
        ...prev,
        volumes: prev.volumes.filter((volume) => volume.id !== id),
        docs: prev.docs.map((doc) => (doc.volumeId === id ? { ...doc, volumeId: null } : doc)),
      }));
    };

    const createDoc: Actions["createDoc"] = (bookId, volumeId, title) => {
      const id = uid();
      const now = Date.now();
      setData((prev) => {
        const order =
          Math.max(
            -1,
            ...prev.docs
              .filter(
                (doc) =>
                  doc.bookId === bookId && doc.volumeId === volumeId && doc.deletedAt === null,
              )
              .map((doc) => doc.order),
          ) + 1;
        return {
          ...prev,
          docs: [
            ...prev.docs,
            {
              id,
              bookId,
              volumeId,
              title: title?.trim() || "Tanpa judul",
              body: "",
              markdown: false,
              order,
              createdAt: now,
              updatedAt: now,
              deletedAt: null,
            },
          ],
          books: prev.books.map((book) =>
            book.id === bookId ? { ...book, updatedAt: now } : book,
          ),
        };
      });
      return id;
    };

    const updateDoc: Actions["updateDoc"] = (id, patch) => {
      setData((prev) => {
        const doc = prev.docs.find((item) => item.id === id);
        if (!doc) return prev;
        const now = Date.now();
        const delta = countWords(patch.body ?? doc.body) - countWords(doc.body);
        const stats = { ...prev.stats };
        if (delta > 0) {
          const key = dayKey();
          stats[key] = (stats[key] ?? 0) + delta;
        }
        return {
          ...prev,
          stats,
          docs: prev.docs.map((item) =>
            item.id === id ? { ...item, ...patch, updatedAt: now } : item,
          ),
          books: prev.books.map((book) =>
            book.id === doc.bookId ? { ...book, updatedAt: now } : book,
          ),
        };
      });
    };

    const deleteDoc: Actions["deleteDoc"] = (id) => {
      setData((prev) => ({
        ...prev,
        docs: prev.docs.map((doc) =>
          doc.id === id ? { ...doc, deletedAt: Date.now() } : doc,
        ),
        settings: {
          ...prev.settings,
          lastDocId: prev.settings.lastDocId === id ? null : prev.settings.lastDocId,
        },
      }));
    };

    const restoreDoc: Actions["restoreDoc"] = (id) => {
      setData((prev) => ({
        ...prev,
        docs: prev.docs.map((doc) =>
          doc.id === id ? { ...doc, deletedAt: null, updatedAt: Date.now() } : doc,
        ),
      }));
    };

    const purgeDoc: Actions["purgeDoc"] = (id) => {
      setData((prev) => ({
        ...prev,
        docs: prev.docs.filter((doc) => doc.id !== id),
        snapshots: prev.snapshots.filter((snapshot) => snapshot.docId !== id),
      }));
    };

    const emptyTrash: Actions["emptyTrash"] = () => {
      setData((prev) => {
        const trashed = new Set(
          prev.docs.filter((doc) => doc.deletedAt !== null).map((doc) => doc.id),
        );
        return {
          ...prev,
          docs: prev.docs.filter((doc) => doc.deletedAt === null),
          snapshots: prev.snapshots.filter((snapshot) => !trashed.has(snapshot.docId)),
        };
      });
    };

    const duplicateDoc: Actions["duplicateDoc"] = (id) => {
      const source = dataRef.current.docs.find((doc) => doc.id === id);
      if (!source) return null;
      const newId = uid();
      const now = Date.now();
      setData((prev) => ({
        ...prev,
        docs: [
          ...prev.docs,
          {
            ...source,
            id: newId,
            title: `${source.title || "Tanpa judul"} (salinan)`,
            order: source.order + 0.5,
            createdAt: now,
            updatedAt: now,
            deletedAt: null,
          },
        ],
        books: prev.books.map((book) =>
          book.id === source.bookId ? { ...book, updatedAt: now } : book,
        ),
      }));
      return newId;
    };

    const moveDoc: Actions["moveDoc"] = (id, bookId, volumeId) => {
      setData((prev) => {
        const order =
          Math.max(
            -1,
            ...prev.docs
              .filter(
                (doc) =>
                  doc.bookId === bookId && doc.volumeId === volumeId && doc.deletedAt === null,
              )
              .map((doc) => doc.order),
          ) + 1;
        return {
          ...prev,
          docs: prev.docs.map((doc) =>
            doc.id === id ? { ...doc, bookId, volumeId, order, updatedAt: Date.now() } : doc,
          ),
        };
      });
    };

    const placeDocs: Actions["placeDocs"] = (docIds, bookId, volumeId) => {
      setData((prev) => {
        const orderMap = new Map(docIds.map((id, index) => [id, index]));
        return {
          ...prev,
          docs: prev.docs.map((doc) => {
            const order = orderMap.get(doc.id);
            if (order === undefined) return doc;
            return { ...doc, bookId, volumeId, order };
          }),
        };
      });
    };

    const reorderVolumes: Actions["reorderVolumes"] = (bookId, orderedIds) => {
      setData((prev) => {
        const orderMap = new Map(orderedIds.map((id, index) => [id, index]));
        return {
          ...prev,
          volumes: prev.volumes.map((volume) => {
            if (volume.bookId !== bookId) return volume;
            const order = orderMap.get(volume.id);
            return order === undefined ? volume : { ...volume, order };
          }),
        };
      });
    };

    const snapshot: Actions["snapshot"] = (docId, reason) => {
      setData((prev) => {
        const doc = prev.docs.find((item) => item.id === docId);
        if (!doc) return prev;
        const latest = prev.snapshots
          .filter((item) => item.docId === docId)
          .sort((a, b) => b.createdAt - a.createdAt)[0];
        if (
          reason !== "manual" &&
          latest &&
          latest.body === doc.body &&
          latest.title === doc.title
        ) {
          return prev;
        }
        const entry: Snapshot = {
          id: uid(),
          docId,
          title: doc.title,
          body: doc.body,
          markdown: doc.markdown,
          reason,
          createdAt: Date.now(),
        };
        return { ...prev, snapshots: pruneSnapshots([entry, ...prev.snapshots]) };
      });
    };

    const restoreSnapshot: Actions["restoreSnapshot"] = (snapshotId) => {
      setData((prev) => {
        const target = prev.snapshots.find((item) => item.id === snapshotId);
        if (!target) return prev;
        const doc = prev.docs.find((item) => item.id === target.docId);
        if (!doc) return prev;
        const safety: Snapshot = {
          id: uid(),
          docId: doc.id,
          title: doc.title,
          body: doc.body,
          markdown: doc.markdown,
          reason: "before-restore",
          createdAt: Date.now(),
        };
        return {
          ...prev,
          docs: prev.docs.map((item) =>
            item.id === doc.id
              ? {
                  ...item,
                  title: target.title,
                  body: target.body,
                  markdown: target.markdown,
                  updatedAt: Date.now(),
                }
              : item,
          ),
          snapshots: pruneSnapshots([safety, ...prev.snapshots]),
        };
      });
    };

    const updateSettings: Actions["updateSettings"] = (patch) => {
      setData((prev) => ({ ...prev, settings: { ...prev.settings, ...patch } }));
    };

    const importData: Actions["importData"] = (raw) => {
      try {
        const parsed: unknown = JSON.parse(raw);
        if (!isRecord(parsed) || !Array.isArray(parsed.books) || !Array.isArray(parsed.docs)) {
          return { ok: false, error: "Berkas ini bukan cadangan Tinta." };
        }
        const next = normalize(parsed);
        if (next.books.length === 0 && next.docs.length === 0) {
          return { ok: false, error: "Cadangan tidak berisi buku atau naskah." };
        }
        setData(next);
        return { ok: true };
      } catch {
        return { ok: false, error: "Berkas cadangan tidak bisa dibaca." };
      }
    };

    const resetData: Actions["resetData"] = () => {
      setData((prev) => {
        const next = emptyData({ ...prev.settings, lastDocId: null });
        return next;
      });
    };

    return {
      createBook,
      updateBook,
      deleteBook,
      createVolume,
      renameVolume,
      deleteVolume,
      createDoc,
      updateDoc,
      deleteDoc,
      restoreDoc,
      purgeDoc,
      emptyTrash,
      duplicateDoc,
      moveDoc,
      placeDocs,
      reorderVolumes,
      snapshot,
      restoreSnapshot,
      updateSettings,
      importData,
      resetData,
    };
  }, []);

  const value = useMemo<StoreValue>(() => ({ data, actions }), [data, actions]);

  return <StoreContext.Provider value={value}>{children}</StoreContext.Provider>;
}

export function useStore(): StoreValue {
  const context = useContext(StoreContext);
  if (!context) throw new Error("useStore harus dipakai di dalam StoreProvider");
  return context;
}
