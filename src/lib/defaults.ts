import type { AppData, Settings } from "./types";

export const STORAGE_KEY = "tinta.v1";
export const SCHEMA_VERSION = 1;

export const DEFAULT_SETTINGS: Settings = {
  theme: "paper",
  font: "serif",
  fontSize: 19,
  lineHeight: 1.75,
  paragraphSpacing: 10,
  pageWidth: 720,
  typewriter: false,
  rulings: false,
  dailyGoal: 500,
  phrases: ["“", "”", "—", "…", "Tiba-tiba,", "Sementara itu,"],
  lastDocId: null,
};

export function emptyData(settings: Settings = DEFAULT_SETTINGS): AppData {
  return {
    schema: SCHEMA_VERSION,
    books: [],
    volumes: [],
    docs: [],
    snapshots: [],
    stats: {},
    settings: { ...settings, phrases: [...settings.phrases] },
  };
}
