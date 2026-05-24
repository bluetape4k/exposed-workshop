#!/usr/bin/env node

const fs = require("fs");
const path = require("path");
const { spawnSync } = require("child_process");

const ROOT = process.cwd();
const RSVG = process.env.RSVG_CONVERT || "rsvg-convert";

const PALETTE = [
  { fill: "#f7d9df", stroke: "#d9899b" },
  { fill: "#d9ecff", stroke: "#7eaad3" },
  { fill: "#dcf3e4", stroke: "#7fb895" },
  { fill: "#fff0c9", stroke: "#d0a84e" },
  { fill: "#eadff8", stroke: "#a287c8" },
  { fill: "#dff4f1", stroke: "#75aaa3" },
  { fill: "#ffe1cc", stroke: "#d5905f" },
  { fill: "#e9edf5", stroke: "#9ca9bd" },
];

const STYLE = `
  .canvas{fill:#f7f9fc}
  .frame{fill:#fff;stroke:#d8e0ea;stroke-width:1.5}
  .title{font-family:"Architects Daughter","Comic Sans MS","Comic Sans",cursive;font-size:42px;font-weight:400;fill:#102033}
  .subtitle{font-family:"Comic Sans MS","Comic Sans","Comic Neue",Arial,sans-serif;font-size:15px;font-weight:400;fill:#536273}
  .label{font-family:"Architects Daughter","Comic Sans MS","Comic Sans",cursive;font-size:24px;font-weight:400;fill:#102033}
  .smallLabel{font-family:"Architects Daughter","Comic Sans MS","Comic Sans",cursive;font-size:20px;font-weight:400;fill:#102033}
  .mono{font-family:"Comic Sans MS","Comic Sans","Comic Neue",Arial,sans-serif;font-size:12px;font-weight:400;fill:#102033}
  .small{font-family:"Comic Sans MS","Comic Sans","Comic Neue",Arial,sans-serif;font-size:12px;font-weight:400;fill:#536273}
  .strong{font-family:"Comic Sans MS","Comic Sans","Comic Neue",Arial,sans-serif;font-size:13px;font-weight:400;fill:#102033}
  .card{stroke-width:2;filter:url(#shadow)}
  .line{fill:none;stroke:#758297;stroke-width:2.1;stroke-linecap:round;stroke-linejoin:round}
  .dash{fill:none;stroke:#9aa6b8;stroke-width:1.6;stroke-linecap:round;stroke-dasharray:6 8}
  .note{fill:#ffffff;stroke:#d8e0ea;stroke-width:1.2}
`;

function walk(dir, out = []) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    if ([".git", ".gradle", "build", ".worktrees"].includes(entry.name)) continue;
    const file = path.join(dir, entry.name);
    if (entry.isDirectory()) walk(file, out);
    else out.push(file);
  }
  return out;
}

function readmeFiles() {
  return walk(".").filter((file) => /^README(\..+)?\.md$/i.test(path.basename(file)));
}

function referencedDiagramSvgs() {
  const refs = new Set();
  const imageRe = /!\[[^\]]*]\(([^)]+)\)|<img\s+[^>]*src=["']([^"']+)["']/gi;
  for (const readme of readmeFiles()) {
    const body = fs.readFileSync(readme, "utf8");
    let match;
    while ((match = imageRe.exec(body))) {
      const raw = (match[1] || match[2] || "").split(/[?#]/)[0];
      if (/^https?:/i.test(raw)) continue;
      if (!/\.(png|svg|jpg|jpeg|webp)$/i.test(raw)) continue;
      if (!/docs\/(assets|images)\/readme-diagrams\//.test(raw)) continue;
      const abs = path.normalize(path.join(path.dirname(readme), raw));
      if (/\.svg$/i.test(abs)) refs.add(abs);
      if (/\.png$/i.test(abs)) refs.add(abs.replace(/\.png$/i, ".svg"));
    }
  }
  return [...refs].filter((file) => fs.existsSync(file)).sort();
}

function decodeXml(text) {
  return text
    .replace(/<!\[CDATA\[([\s\S]*?)]]>/g, "$1")
    .replace(/<br\s*\/?>/gi, " ")
    .replace(/<\/(div|p|span|tspan)>/gi, " ")
    .replace(/<[^>]+>/g, " ")
    .replace(/&nbsp;/g, " ")
    .replace(/&amp;/g, "&")
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/\s+/g, " ")
    .trim();
}

function extractLabels(svg) {
  const values = [];
  for (const re of [
    /<text\b[^>]*>([\s\S]*?)<\/text>/gi,
    /<span\b[^>]*>([\s\S]*?)<\/span>/gi,
    /<div\b[^>]*>([\s\S]*?)<\/div>/gi,
    /<p\b[^>]*>([\s\S]*?)<\/p>/gi,
  ]) {
    let match;
    while ((match = re.exec(svg))) {
      values.push(decodeXml(match[1]));
    }
  }
  return unique(
    values
      .flatMap((value) => value.split(/\s{2,}|\n/))
      .map(cleanLabel)
      .filter((value) => value.length > 0)
      .filter((value) => !/^(classDiagram|flowchart|sequenceDiagram|erDiagram)$/i.test(value))
      .filter((value) => !/Mermaid/i.test(value))
      .filter((value) => !/^Grouped architecture from the original source$/i.test(value))
      .filter((value) => !/^(UML classes|ERD tables and relationships|Sequence messages) from the original source$/i.test(value)),
  );
}

function cleanLabel(value) {
  return value
    .replace(/\bPostgre SQL\b/g, "PostgreSQL")
    .replace(/\bMy SQL\b/g, "MySQL")
    .replace(/\bMaria DB\b/g, "MariaDB")
    .replace(/\bHikari CP\b/g, "HikariCP")
    .replace(/\s+\/\s+/g, " / ")
    .replace(/\s+/g, " ")
    .trim();
}

function unique(values) {
  const seen = new Set();
  const out = [];
  for (const value of values) {
    const key = value.toLowerCase();
    if (seen.has(key)) continue;
    seen.add(key);
    out.push(value);
  }
  return out;
}

function isBadSvg(svg) {
  return (
    /class="flowchart"|aria-roledescription="flowchart|trebuchet ms|mermaid/i.test(svg) ||
    !/Architects Daughter/i.test(svg)
  );
}

function diagramType(file) {
  for (const type of ["architecture", "class", "sequence", "erd"]) {
    if (file.includes(`-${type}-`)) return type;
  }
  return "architecture";
}

function titleFromFile(file) {
  const stem = path.basename(file, ".svg").replace(/-\d+$/, "");
  return stem
    .replace(/-(architecture|class|sequence|erd)$/i, "")
    .split("-")
    .filter((part) => !/^\d+$/.test(part))
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ")
    .replace(/\bDml\b/g, "DML")
    .replace(/\bDdl\b/g, "DDL")
    .replace(/\bJpa\b/g, "JPA")
    .replace(/\bJdbc\b/g, "JDBC")
    .replace(/\bR2dbc\b/g, "R2DBC")
    .replace(/\bSql\b/g, "SQL")
    .replace(/\bJson\b/g, "JSON")
    .replace(/\bHttp\b/g, "HTTP")
    .replace(/\bKtor\b/g, "Ktor");
}

function classify(file, labels) {
  const type = diagramType(file);
  const title = looksLikeTitle(labels[0]) ? labels[0] : titleFromFile(file);
  const body = labels.filter((label) => label !== title).slice(0, 64);
  return { type, title, body };
}

function looksLikeTitle(value) {
  return Boolean(value && value.length <= 48 && !/[{}();=]/.test(value) && !/^\d+\./.test(value));
}

function esc(value) {
  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function wrapText(value, maxChars = 22, maxLines = 3) {
  const words = String(value)
    .split(/\s+/)
    .filter(Boolean)
    .flatMap((word) => splitLongWord(word, maxChars));
  const lines = [];
  let line = "";
  for (const word of words) {
    const next = line ? `${line} ${word}` : word;
    if (next.length > maxChars && line) {
      lines.push(line);
      line = word;
    } else {
      line = next;
    }
  }
  if (line) lines.push(line);
  const clipped = lines.slice(0, maxLines);
  if (lines.length > maxLines) clipped[maxLines - 1] = `${clipped[maxLines - 1].replace(/[. ]+$/, "")}...`;
  return clipped;
}

function splitLongWord(word, maxChars) {
  if (word.length <= maxChars) return [word];
  const normalized = word
    .replace(/([a-z])([A-Z])/g, "$1 $2")
    .replace(/([/_-])/g, "$1 ");
  if (normalized !== word) {
    return normalized.split(/\s+/).filter(Boolean).flatMap((part) => splitLongWord(part, maxChars));
  }
  const chunks = [];
  for (let i = 0; i < word.length; i += maxChars - 1) {
    chunks.push(word.slice(i, i + maxChars - 1));
  }
  return chunks;
}

function textLines(lines, x, y, cls, lineHeight = 18, anchor = "middle") {
  return lines
    .map((line, index) => `<text class="${cls}" x="${x}" y="${y + index * lineHeight}" text-anchor="${anchor}">${esc(line)}</text>`)
    .join("\n");
}

function svgShell(width, height, label, body) {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="${esc(label)}">
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%">
    <feDropShadow dx="0" dy="8" stdDeviation="9" flood-color="#1f2937" flood-opacity="0.10"/>
  </filter>
  <marker id="openArrow" markerWidth="12" markerHeight="10" refX="10" refY="5" orient="auto" markerUnits="strokeWidth">
    <path d="M 1 1 L 10 5 L 1 9" fill="none" stroke="#758297" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
  </marker>
  <style>${STYLE}</style>
</defs>
${body}
</svg>
`;
}

function headerSpec(title, width) {
  const maxChars = Math.max(18, Math.floor((width - 110) / 20));
  const lines = wrapText(title, maxChars, 2);
  return {
    lines,
    height: lines.length > 1 ? 166 : 124,
  };
}

function renderHeader(title, subtitle, width, height) {
  const header = headerSpec(title, width);
  const titleLines = header.lines
    .map((line, index) => `<text class="title" x="54" y="${72 + index * 44}">${esc(line)}</text>`)
    .join("\n");
  return `
<rect class="canvas" x="0" y="0" width="${width}" height="100%"/>
<rect class="frame" x="20" y="20" width="${width - 40}" height="${height - 40}" rx="18"/>
${titleLines}
<text class="subtitle" x="56" y="${header.lines.length > 1 ? 144 : 100}">${esc(subtitle)}</text>`;
}

function renderArchitecture(title, labels) {
  const nodes = labels.length ? labels : ["Application", "Exposed", "Database"];
  const cardW = 246;
  const cardH = 118;
  const cols = nodes.length <= 4 ? 2 : 3;
  const rows = Math.ceil(nodes.length / cols);
  const width = Math.max(860, 80 + cols * cardW + (cols - 1) * 52);
  const header = headerSpec(title, width);
  const top = header.height + 18;
  const height = top + rows * cardH + (rows - 1) * 38 + 44;
  const x0 = (width - (cols * cardW + (cols - 1) * 44)) / 2;
  let body = renderHeader(title, "Pastel architecture overview", width, height);
  const centers = [];
  nodes.forEach((node, index) => {
    const col = index % cols;
    const row = Math.floor(index / cols);
    const x = x0 + col * (cardW + 44);
    const y = top + row * (cardH + 38);
    const color = PALETTE[index % PALETTE.length];
    centers.push([x + cardW / 2, y + cardH / 2]);
    body += `\n<rect class="card" x="${x}" y="${y}" width="${cardW}" height="${cardH}" rx="16" fill="${color.fill}" stroke="${color.stroke}"/>`;
    body += "\n" + textLines(wrapText(node, 19, 3), x + cardW / 2, y + 46, "label", 25);
  });
  for (let index = 0; index < nodes.length; index += 1) {
    const col = index % cols;
    const row = Math.floor(index / cols);
    const x = x0 + col * (cardW + 44);
    const y = top + row * (cardH + 38);
    if (col < cols - 1 && index + 1 < nodes.length && Math.floor((index + 1) / cols) === row) {
      body += `\n<path class="line" d="M ${x + cardW + 8} ${y + cardH / 2} L ${x + cardW + 36} ${y + cardH / 2}" marker-end="url(#openArrow)"/>`;
    }
    if (row < rows - 1 && index + cols < nodes.length) {
      body += `\n<path class="line" d="M ${x + cardW / 2} ${y + cardH + 8} L ${x + cardW / 2} ${y + cardH + 30}" marker-end="url(#openArrow)"/>`;
    }
  }
  return svgShell(width, height, title, body);
}

function renderClass(title, labels) {
  const classes = bucketClassLabels(labels);
  const cardW = 280;
  const cardH = 138;
  const cols = classes.length <= 3 ? 2 : 3;
  const rows = Math.ceil(classes.length / cols);
  const width = Math.max(820, 86 + cols * cardW + (cols - 1) * 36);
  const header = headerSpec(title, width);
  const top = header.height + 18;
  const height = top + rows * cardH + (rows - 1) * 34 + 46;
  const x0 = (width - (cols * cardW + (cols - 1) * 36)) / 2;
  let body = renderHeader(title, "Class and component responsibilities", width, height);
  classes.forEach((item, index) => {
    const col = index % cols;
    const row = Math.floor(index / cols);
    const x = x0 + col * (cardW + 36);
    const y = top + row * (cardH + 34);
    const color = PALETTE[index % PALETTE.length];
    body += `\n<rect class="card" x="${x}" y="${y}" width="${cardW}" height="${cardH}" rx="14" fill="${color.fill}" stroke="${color.stroke}"/>`;
    body += "\n" + textLines(wrapText(item.name, 24, 2), x + cardW / 2, y + 36, "smallLabel", 23);
    body += `\n<line class="dash" x1="${x + 18}" y1="${y + 62}" x2="${x + cardW - 18}" y2="${y + 62}"/>`;
    item.members.slice(0, 4).forEach((member, m) => {
      body += `\n<text class="mono" x="${x + 24}" y="${y + 86 + m * 17}">+ ${esc(trimMiddle(member, 34))}</text>`;
    });
  });
  return svgShell(width, height, title, body);
}

function bucketClassLabels(labels) {
  const result = [];
  let current = null;
  for (const label of labels) {
    if (/^«.*»$/.test(label) || result.length === 0 || /^[A-Z][A-Za-z0-9]+$/.test(label)) {
      current = { name: label, members: [] };
      result.push(current);
    } else if (current) {
      current.members.push(label);
    }
  }
  if (result.length < 2) {
    return labels.slice(0, 12).map((label) => ({ name: label, members: [] }));
  }
  return result.slice(0, 18);
}

function renderErd(title, labels) {
  const tables = bucketTables(labels);
  const tableW = 244;
  const tableH = 154;
  const cols = tables.length <= 4 ? 2 : 3;
  const rows = Math.ceil(tables.length / cols);
  const width = Math.max(800, 90 + cols * tableW + (cols - 1) * 48);
  const header = headerSpec(title, width);
  const top = header.height + 18;
  const height = top + rows * tableH + (rows - 1) * 42 + 48;
  const x0 = (width - (cols * tableW + (cols - 1) * 48)) / 2;
  let body = renderHeader(title, "Entity relationship view", width, height);
  const centers = [];
  tables.forEach((table, index) => {
    const col = index % cols;
    const row = Math.floor(index / cols);
    const x = x0 + col * (tableW + 48);
    const y = top + row * (tableH + 42);
    const color = PALETTE[index % PALETTE.length];
    centers.push([x + tableW / 2, y + tableH / 2]);
    body += `\n<rect class="card" x="${x}" y="${y}" width="${tableW}" height="${tableH}" rx="14" fill="#ffffff" stroke="${color.stroke}"/>`;
    body += `\n<rect x="${x}" y="${y}" width="${tableW}" height="42" rx="14" fill="${color.fill}" stroke="${color.stroke}" stroke-width="0"/>`;
    body += "\n" + textLines(wrapText(table.name, 22, 1), x + tableW / 2, y + 28, "smallLabel", 22);
    table.columns.slice(0, 6).forEach((column, c) => {
      body += `\n<text class="mono" x="${x + 18}" y="${y + 66 + c * 15}">${esc(trimMiddle(column, 32))}</text>`;
    });
  });
  for (let index = 0; index < tables.length; index += 1) {
    const col = index % cols;
    const row = Math.floor(index / cols);
    const x = x0 + col * (tableW + 48);
    const y = top + row * (tableH + 42);
    if (col < cols - 1 && index + 1 < tables.length && Math.floor((index + 1) / cols) === row) {
      body += `\n<path class="line" d="M ${x + tableW + 8} ${y + tableH / 2} L ${x + tableW + 40} ${y + tableH / 2}" marker-end="url(#openArrow)"/>`;
    }
    if (row < rows - 1 && index + cols < tables.length) {
      body += `\n<path class="line" d="M ${x + tableW / 2} ${y + tableH + 8} L ${x + tableW / 2} ${y + tableH + 34}" marker-end="url(#openArrow)"/>`;
    }
  }
  return svgShell(width, height, title, body);
}

function bucketTables(labels) {
  const result = [];
  let current = null;
  for (const label of labels) {
    const isColumn = /\b(PK|FK|id|varchar|text|date|timestamp|bigint|int|boolean|decimal|json|uuid)\b/i.test(label);
    if (!isColumn || !current) {
      current = { name: label, columns: [] };
      result.push(current);
    } else {
      current.columns.push(label);
    }
  }
  if (result.length < 2) {
    return labels.slice(0, 10).map((label) => ({ name: label, columns: [] }));
  }
  return result.slice(0, 18);
}

function renderSequence(title, labels) {
  const splitAt = labels.findIndex((label) => /^\d+\./.test(label));
  const participants = (splitAt > 0 ? labels.slice(0, splitAt) : labels.slice(0, Math.min(5, labels.length))).slice(0, 7);
  const messages = (splitAt > 0 ? labels.slice(splitAt) : labels.slice(participants.length)).slice(0, 22);
  const lanes = participants.length || 3;
  const laneGap = 210;
  const width = Math.max(900, 100 + (lanes - 1) * laneGap + 220);
  const header = headerSpec(title, width);
  const top = header.height + 18;
  const height = top + 92 + Math.max(messages.length, 6) * 54 + 80;
  const x0 = (width - (lanes - 1) * laneGap) / 2;
  let body = renderHeader(title, "Request and data flow", width, height);
  participants.forEach((participant, index) => {
    const x = x0 + index * laneGap;
    const color = PALETTE[index % PALETTE.length];
    body += `\n<rect class="card" x="${x - 88}" y="${top}" width="176" height="58" rx="16" fill="${color.fill}" stroke="${color.stroke}"/>`;
    body += "\n" + textLines(wrapText(participant, 16, 2), x, top + 24, "smallLabel", 20);
    body += `\n<path class="dash" d="M ${x} ${top + 66} L ${x} ${height - 54}"/>`;
  });
  messages.forEach((message, index) => {
    const y = top + 106 + index * 54;
    const from = index % lanes;
    const to = (index + 1) % lanes;
    const x1 = x0 + from * laneGap;
    const x2 = x0 + to * laneGap;
    const left = Math.min(x1, x2);
    const right = Math.max(x1, x2);
    const labelX = (x1 + x2) / 2;
    const direction = x1 <= x2 ? `M ${x1 + 12} ${y} L ${x2 - 14} ${y}` : `M ${x1 - 12} ${y} L ${x2 + 14} ${y}`;
    body += `\n<path class="line" d="${direction}" marker-end="url(#openArrow)"/>`;
    body += `\n<rect class="note" x="${labelX - 132}" y="${y - 32}" width="264" height="24" rx="8"/>`;
    body += `\n<text class="small" x="${labelX}" y="${y - 15}" text-anchor="middle">${esc(trimMiddle(message, 42))}</text>`;
    if (right - left < 40) body += `\n<path class="line" d="M ${x1} ${y} c 58 0 58 36 0 36" marker-end="url(#openArrow)"/>`;
  });
  return svgShell(width, height, title, body);
}

function trimMiddle(value, max) {
  if (value.length <= max) return value;
  const keep = Math.max(8, Math.floor((max - 3) / 2));
  return `${value.slice(0, keep)}...${value.slice(value.length - keep)}`;
}

function renderDiagram(file, labels) {
  const spec = classify(file, labels);
  if (spec.type === "class") return renderClass(spec.title, spec.body);
  if (spec.type === "erd") return renderErd(spec.title, spec.body);
  if (spec.type === "sequence") return renderSequence(spec.title, spec.body);
  return renderArchitecture(spec.title, spec.body);
}

function originalSvg(file) {
  const result = spawnSync("git", ["show", `HEAD:${file}`], { encoding: "utf8" });
  return result.status === 0 ? result.stdout : null;
}

function convertToPng(svgFile) {
  const pngFile = svgFile.replace(/\.svg$/i, ".png");
  const result = spawnSync(RSVG, ["-f", "png", "-o", pngFile, svgFile], { encoding: "utf8" });
  if (result.status !== 0) {
    throw new Error(`rsvg-convert failed for ${svgFile}: ${result.stderr || result.stdout}`);
  }
}

function main() {
  const forceAll = process.argv.includes("--all");
  const svgs = referencedDiagramSvgs();
  const targets = [];
  for (const file of svgs) {
    const svg = fs.readFileSync(file, "utf8");
    if (!forceAll && !isBadSvg(svg)) continue;
    const labels = extractLabels(originalSvg(file) || svg);
    const next = renderDiagram(file, labels);
    fs.writeFileSync(file, next);
    convertToPng(file);
    targets.push(file);
  }
  const byType = targets.reduce((acc, file) => {
    const type = diagramType(file);
    acc[type] = (acc[type] || 0) + 1;
    return acc;
  }, {});
  console.log(JSON.stringify({ regenerated: targets.length, byType }, null, 2));
}

main();
