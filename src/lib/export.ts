import type { Book, Doc, ThemeId, Volume } from "./types";
import { renderMarkdown, escapeHtml } from "./markdown";
import {
  countWords,
  dateStamp,
  downloadBlob,
  downloadFile,
  formatDate,
  slugify,
  stripMarkdown,
} from "./text";

function docTitle(doc: Doc): string {
  return doc.title.trim() || "Tanpa judul";
}

export function docPlainText(doc: Doc): string {
  return `${docTitle(doc)}\n\n${doc.body}`.trimEnd();
}

function docBodyHtml(doc: Doc): string {
  if (doc.markdown) return renderMarkdown(doc.body);
  return doc.body
    .split(/\n{2,}/)
    .map((paragraph) => `<p>${escapeHtml(paragraph).replace(/\n/g, "<br />")}</p>`)
    .join("\n");
}

function standaloneHtml(doc: Doc, bookTitle: string, forPrint: boolean): string {
  const title = docTitle(doc);
  return `<!doctype html>
<html lang="id">
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1" />
<title>${escapeHtml(title)} — Tinta</title>
<style>
  :root { color-scheme: light; }
  * { box-sizing: border-box; }
  body {
    margin: 0;
    background: ${forPrint ? "#ffffff" : "#f4efe4"};
    color: #241f18;
    font-family: "Newsreader Variable", Georgia, "Times New Roman", serif;
    line-height: 1.75;
    font-size: 19px;
  }
  .page { max-width: 720px; margin: 0 auto; padding: 72px 28px 96px; }
  .meta { font-family: "Instrument Sans Variable", system-ui, sans-serif; font-size: 12px; letter-spacing: .08em; text-transform: uppercase; color: #6f6455; margin: 0 0 12px; }
  h1 { font-size: 40px; line-height: 1.15; letter-spacing: -.02em; margin: 0 0 10px; }
  .byline { font-family: "Instrument Sans Variable", system-ui, sans-serif; font-size: 13px; color: #6f6455; margin: 0 0 40px; }
  h1, h2, h3, h4 { line-height: 1.25; margin: 1.6em 0 .5em; letter-spacing: -.01em; }
  h2 { font-size: 28px; } h3 { font-size: 23px; }
  p { margin: 0 0 1em; }
  blockquote { margin: 1.6em 0; padding-left: 20px; border-left: 3px solid #1f6b5a; color: #4b4338; font-style: italic; }
  pre { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 15px; background: #ece5d8; padding: 16px; border-radius: 10px; overflow-x: auto; }
  code { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: .88em; background: #ece5d8; padding: .1em .35em; border-radius: 4px; }
  pre code { background: none; padding: 0; }
  img { max-width: 100%; border-radius: 8px; }
  a { color: #1f6b5a; }
  hr { border: none; border-top: 1px solid #d8cdbb; margin: 2em 0; }
  .footer { margin-top: 56px; padding-top: 16px; border-top: 1px solid #ddd2c0; font-family: "Instrument Sans Variable", system-ui, sans-serif; font-size: 12px; color: #7a6f60; }
  .print-bar { position: sticky; top: 0; background: #241f18; color: #f9f4e9; display: flex; gap: 12px; align-items: center; justify-content: space-between; padding: 12px 20px; font-family: "Instrument Sans Variable", system-ui, sans-serif; font-size: 13px; }
  .print-bar button { font: inherit; background: #f9f4e9; color: #241f18; border: 0; border-radius: 999px; padding: 8px 16px; cursor: pointer; }
  @media print {
    body { background: #fff; font-size: 12pt; }
    .page { max-width: none; padding: 0; }
    .print-bar { display: none; }
    .footer { color: #666; }
    @page { margin: 22mm 20mm; }
  }
</style>
</head>
<body>
${forPrint ? `<div class="print-bar"><span>Pratinjau cetak — pilih “Simpan sebagai PDF” di dialog cetak.</span><button onclick="window.print()">Cetak / simpan PDF</button></div>` : ""}
<main class="page">
  <p class="meta">${escapeHtml(bookTitle)}</p>
  <h1>${escapeHtml(title)}</h1>
  <p class="byline">${formatDate(doc.updatedAt)} · ${countWords(doc.body).toLocaleString("id-ID")} kata</p>
  <article>${docBodyHtml(doc)}</article>
  <p class="footer">Ditulis dengan Tinta — menulis tanpa kehilangan.</p>
</main>
</body>
</html>`;
}

export function exportDoc(doc: Doc, bookTitle: string, format: "txt" | "md" | "html"): void {
  const base = slugify(docTitle(doc));
  if (format === "txt") {
    downloadFile(`${base}.txt`, doc.markdown ? stripMarkdown(doc.body) : doc.body, "text/plain");
    return;
  }
  if (format === "md") {
    downloadFile(`${base}.md`, `# ${docTitle(doc)}\n\n${doc.body}\n`, "text/markdown");
    return;
  }
  downloadFile(`${base}.html`, standaloneHtml(doc, bookTitle, false), "text/html");
}

export function openPrintView(doc: Doc, bookTitle: string): void {
  const html = standaloneHtml(doc, bookTitle, true);
  const blob = new Blob([html], { type: "text/html;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const opened = window.open(url, "_blank");
  if (!opened) {
    URL.revokeObjectURL(url);
    downloadFile(`${slugify(docTitle(doc))}-cetak.html`, html, "text/html");
  }
  window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
}

export function libraryMarkdown(books: Book[], volumes: Volume[], docs: Doc[]): string {
  const lines: string[] = [
    `# Pustaka Tinta`,
    "",
    `Diekspor ${formatDate(Date.now())}.`,
    "",
  ];
  const sortedBooks = [...books].sort((a, b) => a.createdAt - b.createdAt);
  for (const book of sortedBooks) {
    lines.push(`## ${book.title}`, "");
    if (book.author) lines.push(`*${book.author}*`, "");
    const bookVolumes = volumes
      .filter((volume) => volume.bookId === book.id)
      .sort((a, b) => a.order - b.order);
    const loose = docs
      .filter((doc) => doc.bookId === book.id && doc.deletedAt === null && !doc.volumeId)
      .sort((a, b) => a.order - b.order);
    for (const doc of loose) {
      lines.push(`### ${docTitle(doc)}`, "", doc.body, "");
    }
    for (const volume of bookVolumes) {
      lines.push(`### ${volume.title}`, "");
      const items = docs
        .filter((doc) => doc.volumeId === volume.id && doc.deletedAt === null)
        .sort((a, b) => a.order - b.order);
      for (const doc of items) {
        lines.push(`#### ${docTitle(doc)}`, "", doc.body, "");
      }
    }
  }
  return lines.join("\n");
}

export function exportLibrary(books: Book[], volumes: Volume[], docs: Doc[]): void {
  downloadFile(
    `tinta-pustaka-${dateStamp()}.md`,
    libraryMarkdown(books, volumes, docs),
    "text/markdown",
  );
}

interface ImageOptions {
  doc: Doc;
  bookTitle: string;
  theme: ThemeId;
}

function readColor(name: string, fallback: string): string {
  const value = getComputedStyle(document.documentElement).getPropertyValue(name).trim();
  return value || fallback;
}

function wrapText(
  context: CanvasRenderingContext2D,
  text: string,
  maxWidth: number,
): string[] {
  if (!text) return [""];
  const words = text.split(/\s+/);
  const lines: string[] = [];
  let current = "";
  for (const word of words) {
    const candidate = current ? `${current} ${word}` : word;
    if (context.measureText(candidate).width > maxWidth && current) {
      lines.push(current);
      current = word;
    } else {
      current = candidate;
    }
  }
  if (current) lines.push(current);
  return lines.length > 0 ? lines : [""];
}

export async function exportDocImage({ doc, bookTitle }: ImageOptions): Promise<void> {
  await document.fonts.ready;

  const canvas = document.createElement("canvas");
  const context = canvas.getContext("2d");
  if (!context) throw new Error("Peramban ini tidak mendukung ekspor gambar.");

  const width = 1080;
  const padding = 96;
  const contentWidth = width - padding * 2;
  const titleFont = '600 46px "Newsreader Variable", Georgia, serif';
  const metaFont = '500 23px "Instrument Sans Variable", system-ui, sans-serif';
  const bodyFont = '400 30px "Newsreader Variable", Georgia, serif';
  const footerFont = '500 21px "Instrument Sans Variable", system-ui, sans-serif';

  context.font = titleFont;
  const title = docTitle(doc);
  const titleLines = wrapText(context, title, contentWidth);

  const bodySource = doc.markdown ? stripMarkdown(doc.body) : doc.body;
  const rawLines = bodySource.replace(/\r\n/g, "\n").split("\n");
  context.font = bodyFont;
  const bodyLines: string[] = [];
  for (const line of rawLines) {
    if (!line.trim()) {
      bodyLines.push("");
      continue;
    }
    bodyLines.push(...wrapText(context, line.trim(), contentWidth));
  }

  const titleBlock = titleLines.length * 60;
  const bodyBlock = bodyLines.length * 46;
  const height = padding + 16 + 40 + titleBlock + 20 + 34 + 44 + bodyBlock + 72 + padding;

  if (height > 15000) {
    throw new Error("Naskah terlalu panjang untuk satu gambar. Coba ekspor PDF atau TXT.");
  }

  const ratio = Math.min(2, 32000 / height);
  canvas.width = Math.round(width * ratio);
  canvas.height = Math.round(height * ratio);
  context.scale(ratio, ratio);

  const background = readColor("--editor-bg", "#fbf7ee");
  const ink = readColor("--text", "#241f18");
  const muted = readColor("--text-muted", "#6f6455");
  const accent = readColor("--accent", "#1f6b5a");
  const border = readColor("--border", "#e2d8c6");

  context.fillStyle = background;
  context.fillRect(0, 0, width, height);

  context.fillStyle = accent;
  context.fillRect(padding, padding, 84, 10);

  let y = padding + 16 + 40;
  context.fillStyle = ink;
  context.font = titleFont;
  context.textBaseline = "top";
  for (const line of titleLines) {
    context.fillText(line, padding, y);
    y += 60;
  }

  context.font = metaFont;
  context.fillStyle = muted;
  y += 20;
  context.fillText(
    `${bookTitle.toUpperCase()} · ${countWords(doc.body).toLocaleString("id-ID")} KATA`,
    padding,
    y,
  );
  y += 34 + 22;

  context.strokeStyle = border;
  context.lineWidth = 1;
  context.beginPath();
  context.moveTo(padding, y);
  context.lineTo(width - padding, y);
  context.stroke();
  y += 44;

  context.fillStyle = ink;
  context.font = bodyFont;
  for (const line of bodyLines) {
    if (line) context.fillText(line, padding, y);
    y += 46;
  }

  context.font = footerFont;
  context.fillStyle = muted;
  context.fillText("tinta — menulis tanpa kehilangan", padding, height - padding + 8);

  const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, "image/png"));
  if (!blob) throw new Error("Gambar gagal dibuat.");
  downloadBlob(`${slugify(title)}-${dateStamp()}.png`, blob);
}
