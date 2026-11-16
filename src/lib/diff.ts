export interface DiffRow {
  kind: "same" | "add" | "remove";
  text: string;
  oldLine?: number;
  newLine?: number;
}

function simpleDiff(before: string[], after: string[]): DiffRow[] {
  return [
    ...before.map((text) => ({ kind: "remove" as const, text })),
    ...after.map((text) => ({ kind: "add" as const, text })),
  ];
}

export function diffLines(beforeText: string, afterText: string): DiffRow[] {
  const before = beforeText.replace(/\r\n/g, "\n").split("\n");
  const after = afterText.replace(/\r\n/g, "\n").split("\n");

  let prefix = 0;
  while (prefix < before.length && prefix < after.length && before[prefix] === after[prefix]) {
    prefix += 1;
  }
  let suffix = 0;
  while (
    suffix < before.length - prefix &&
    suffix < after.length - prefix &&
    before[before.length - 1 - suffix] === after[after.length - 1 - suffix]
  ) {
    suffix += 1;
  }

  const a = before.slice(prefix, before.length - suffix);
  const b = after.slice(prefix, after.length - suffix);
  const rows: DiffRow[] = [];

  for (let i = 0; i < prefix; i += 1) {
    rows.push({ kind: "same", text: before[i], oldLine: i + 1, newLine: i + 1 });
  }

  if (a.length > 0 || b.length > 0) {
    if (a.length * b.length > 1_600_000) {
      rows.push(...simpleDiff(a, b));
    } else {
      const table: Uint32Array[] = Array.from(
        { length: a.length + 1 },
        () => new Uint32Array(b.length + 1),
      );
      for (let i = a.length - 1; i >= 0; i -= 1) {
        for (let j = b.length - 1; j >= 0; j -= 1) {
          table[i][j] =
            a[i] === b[j]
              ? table[i + 1][j + 1] + 1
              : Math.max(table[i + 1][j], table[i][j + 1]);
        }
      }
      let i = 0;
      let j = 0;
      while (i < a.length && j < b.length) {
        if (a[i] === b[j]) {
          rows.push({ kind: "same", text: a[i], oldLine: prefix + i + 1, newLine: prefix + j + 1 });
          i += 1;
          j += 1;
        } else if (table[i + 1][j] >= table[i][j + 1]) {
          rows.push({ kind: "remove", text: a[i], oldLine: prefix + i + 1 });
          i += 1;
        } else {
          rows.push({ kind: "add", text: b[j], newLine: prefix + j + 1 });
          j += 1;
        }
      }
      while (i < a.length) {
        rows.push({ kind: "remove", text: a[i], oldLine: prefix + i + 1 });
        i += 1;
      }
      while (j < b.length) {
        rows.push({ kind: "add", text: b[j], newLine: prefix + j + 1 });
        j += 1;
      }
    }
  }

  for (let k = 0; k < suffix; k += 1) {
    const oldIndex = before.length - suffix + k;
    const newIndex = after.length - suffix + k;
    rows.push({
      kind: "same",
      text: before[oldIndex],
      oldLine: oldIndex + 1,
      newLine: newIndex + 1,
    });
  }

  return rows;
}

export function diffStats(rows: DiffRow[]): { added: number; removed: number } {
  let added = 0;
  let removed = 0;
  for (const row of rows) {
    if (row.kind === "add") added += 1;
    if (row.kind === "remove") removed += 1;
  }
  return { added, removed };
}
