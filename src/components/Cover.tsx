import { Droplet } from "lucide-react";

export const COVERS = [
  { id: "clay", label: "Tanah liat" },
  { id: "ink", label: "Biru malam" },
  { id: "moss", label: "Lumut" },
  { id: "plum", label: "Prem" },
  { id: "sun", label: "Gandum" },
];

export function coverClass(color: string): string {
  return `cover--${COVERS.some((cover) => cover.id === color) ? color : "clay"}`;
}

interface BookCoverProps {
  title: string;
  color: string;
  size?: "sm" | "md";
}

export function BookCover({ title, color, size = "md" }: BookCoverProps) {
  return (
    <div className={`book-cover ${coverClass(color)} book-cover--${size}`} aria-hidden="true">
      <span className="book-cover-grain" />
      <span className="book-cover-kicker">BUKU</span>
      <span className="book-cover-title">{title || "Tanpa judul"}</span>
      <Droplet className="book-cover-mark" size={size === "sm" ? 14 : 18} strokeWidth={2} />
    </div>
  );
}
