export function uid(): string {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }
  return `id-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
}

export function countWords(text: string): number {
  const cleaned = text.replace(/[*_`#>~[\]()]/g, " ").trim();
  if (!cleaned) return 0;
  let words = 0;
  for (const token of cleaned.split(/\s+/)) {
    const cjk = token.match(/[\u3040-\u30ff\u3400-\u4dbf\u4e00-\u9fff\uac00-\ud7af]/g);
    if (cjk) {
      words += cjk.length;
    } else if (/[\p{L}\p{N}]/u.test(token)) {
      words += 1;
    }
  }
  return words;
}

export function countChars(text: string): number {
  return text.replace(/\s/g, "").length;
}

export function excerpt(text: string, max = 120): string {
  const flat = text.replace(/\s+/g, " ").trim();
  if (!flat) return "";
  return flat.length > max ? `${flat.slice(0, max).trimEnd()}…` : flat;
}

export function formatNumber(value: number): string {
  return new Intl.NumberFormat("id-ID").format(value);
}

export function compactNumber(value: number): string {
  if (value >= 1_000_000) {
    const n = value / 1_000_000;
    return `${n >= 10 ? Math.round(n) : n.toFixed(1).replace(".", ",")} jt`;
  }
  if (value >= 1_000) {
    const n = value / 1_000;
    return `${n >= 10 ? Math.round(n) : n.toFixed(1).replace(".", ",")} rb`;
  }
  return formatNumber(value);
}

export function formatDate(ts: number): string {
  return new Intl.DateTimeFormat("id-ID", {
    day: "numeric",
    month: "long",
    year: "numeric",
  }).format(ts);
}

export function formatShortDate(ts: number): string {
  return new Intl.DateTimeFormat("id-ID", {
    day: "numeric",
    month: "short",
    year: "numeric",
  }).format(ts);
}

export function formatTime(ts: number): string {
  return new Intl.DateTimeFormat("id-ID", {
    hour: "2-digit",
    minute: "2-digit",
  }).format(ts);
}

export function formatDateTime(ts: number): string {
  return `${formatShortDate(ts)} · ${formatTime(ts)}`;
}

export function timeAgo(ts: number): string {
  const minutes = Math.max(0, Math.round((Date.now() - ts) / 60_000));
  if (minutes < 1) return "baru saja";
  if (minutes < 60) return `${minutes} menit lalu`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours} jam lalu`;
  const days = Math.round(hours / 24);
  if (days === 1) return "kemarin";
  if (days < 7) return `${days} hari lalu`;
  return formatShortDate(ts);
}

export function dayKey(date: Date | number = new Date()): string {
  const d = new Date(date);
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

export function daysBack(count: number): string[] {
  const days: string[] = [];
  const today = new Date();
  today.setHours(12, 0, 0, 0);
  for (let i = count - 1; i >= 0; i -= 1) {
    const d = new Date(today);
    d.setDate(today.getDate() - i);
    days.push(dayKey(d));
  }
  return days;
}

export function dateStamp(ts = Date.now()): string {
  const d = new Date(ts);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(
    d.getDate(),
  ).padStart(2, "0")}`;
}

export function slugify(value: string): string {
  return (
    value
      .normalize("NFD")
      .replace(/\p{Diacritic}/gu, "")
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, "-")
      .replace(/^-+|-+$/g, "")
      .slice(0, 60) || "tanpa-judul"
  );
}

export function stripMarkdown(source: string): string {
  return source
    .replace(/```[\s\S]*?```/g, (block) => block.replace(/```[\w-]*\s*/g, ""))
    .replace(/!\[([^\]]*)\]\([^)]*\)/g, "$1")
    .replace(/\[([^\]]+)\]\([^)]*\)/g, "$1")
    .replace(/^#{1,6}\s+/gm, "")
    .replace(/^\s*>\s?/gm, "")
    .replace(/^(\s*)([-*+]|\d+[.)])\s+/gm, "$1")
    .replace(/[*_~`]/g, "");
}

export function downloadBlob(filename: string, blob: Blob): void {
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  setTimeout(() => URL.revokeObjectURL(url), 4000);
}

export function downloadFile(filename: string, content: string, mime: string): void {
  downloadBlob(filename, new Blob([content], { type: `${mime};charset=utf-8` }));
}

export async function copyText(text: string): Promise<boolean> {
  try {
    await navigator.clipboard.writeText(text);
    return true;
  } catch {
    return false;
  }
}
