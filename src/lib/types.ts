export type ThemeId = "paper" | "sepia" | "night" | "graphite";
export type FontId = "serif" | "sans" | "mono";
export type SnapshotReason = "auto" | "manual" | "before-restore";

export interface Book {
  id: string;
  title: string;
  author: string;
  color: string;
  tags: string[];
  createdAt: number;
  updatedAt: number;
}

export interface Volume {
  id: string;
  bookId: string;
  title: string;
  order: number;
  createdAt: number;
}

export interface Doc {
  id: string;
  bookId: string;
  volumeId: string | null;
  title: string;
  body: string;
  markdown: boolean;
  order: number;
  createdAt: number;
  updatedAt: number;
  deletedAt: number | null;
}

export interface Snapshot {
  id: string;
  docId: string;
  title: string;
  body: string;
  markdown: boolean;
  reason: SnapshotReason;
  createdAt: number;
}

export interface Settings {
  theme: ThemeId;
  font: FontId;
  fontSize: number;
  lineHeight: number;
  paragraphSpacing: number;
  pageWidth: number;
  typewriter: boolean;
  rulings: boolean;
  dailyGoal: number;
  phrases: string[];
  lastDocId: string | null;
}

export interface AppData {
  schema: number;
  books: Book[];
  volumes: Volume[];
  docs: Doc[];
  snapshots: Snapshot[];
  stats: Record<string, number>;
  settings: Settings;
}
