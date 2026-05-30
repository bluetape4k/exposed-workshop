#!/usr/bin/env node

const fs = require("fs");
const path = require("path");
const { spawnSync } = require("child_process");

const RSVG = process.env.RSVG_CONVERT || "rsvg-convert";

const COLORS = [
  ["#dbeafe", "#3b82f6"],
  ["#ede9fe", "#7c3aed"],
  ["#dcfce7", "#16a34a"],
  ["#ffedd5", "#ea580c"],
  ["#fce7f3", "#db2777"],
  ["#ccfbf1", "#0f766e"],
  ["#fef3c7", "#d97706"],
  ["#e2e8f0", "#64748b"],
];

const STYLE = `
  .canvas{fill:#f6f9fc}
  .frame{fill:#fff;stroke:#d7e2ec;stroke-width:1.5}
  .panel{fill:#f8fafc;stroke:#d7e2ec;stroke-width:1.2}
  .card{stroke-width:1.7;filter:url(#shadow)}
  .title{font-family:"Architects Daughter","Comic Sans MS",cursive;font-size:32px;fill:#1e293b}
  .subtitle{font-family:"Comic Sans MS","Comic Sans","Comic Neue",Arial,sans-serif;font-size:13px;fill:#536476}
  .panelTitle{font-family:"Architects Daughter","Comic Sans MS",cursive;font-size:12px;letter-spacing:.08em;fill:#94a3b8}
  .label{font-family:"Architects Daughter","Comic Sans MS",cursive;font-size:18px;fill:#1e293b}
  .smallLabel{font-family:"Architects Daughter","Comic Sans MS",cursive;font-size:15px;fill:#1e293b}
  .detail{font-family:"Comic Sans MS","Comic Sans","Comic Neue",Arial,sans-serif;font-size:12px;fill:#536476}
  .tiny{font-family:"Comic Sans MS","Comic Sans","Comic Neue",Arial,sans-serif;font-size:10px;fill:#64748b}
  .arrow{fill:none;stroke:#64748b;stroke-width:2;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrow)}
  .softArrow{fill:none;stroke:#94a3b8;stroke-width:1.5;stroke-linecap:round;stroke-linejoin:round;stroke-dasharray:5 5;marker-end:url(#arrow)}
  .inherit{fill:none;stroke:#64748b;stroke-width:1.7;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#inherit)}
  .divider{stroke:#cbd5e1;stroke-width:1.2;stroke-dasharray:6 6}
`;

function walk(dir, out = []) {
  if (!fs.existsSync(dir)) return out;
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

function referencedDiagrams() {
  const refs = new Map();
  const imageRe = /!\[[^\]]*]\(([^)]+)\)|<img\s+[^>]*src=["']([^"']+)["']/gi;
  for (const readme of readmeFiles()) {
    const body = fs.readFileSync(readme, "utf8");
    let match;
    while ((match = imageRe.exec(body))) {
      const raw = (match[1] || match[2] || "").split(/[?#]/)[0];
      if (/^https?:/i.test(raw) || !/\.(png|svg|jpg|jpeg|webp)$/i.test(raw)) continue;
      if (!/docs\/(assets|images)\/readme-diagrams\//.test(raw)) continue;
      const abs = path.normalize(path.join(path.dirname(readme), raw)).replace(/\.(png|jpg|jpeg|webp)$/i, ".svg");
      if (!fs.existsSync(abs)) continue;
      const current = refs.get(abs) || { svg: abs, readmes: [] };
      current.readmes.push(readme);
      refs.set(abs, current);
    }
  }
  return [...refs.values()].sort((a, b) => a.svg.localeCompare(b.svg));
}

function diagramType(file) {
  for (const type of ["architecture", "class", "sequence", "erd"]) {
    if (file.includes(`-${type}-`)) return type;
  }
  return "architecture";
}

function moduleDirFor(ref) {
  const dirs = ref.readmes.map((readme) => path.dirname(readme)).filter((dir) => dir !== ".");
  if (dirs.length === 0) return ".";
  dirs.sort((a, b) => b.length - a.length);
  return dirs[0];
}

function readText(file) {
  return fs.existsSync(file) ? fs.readFileSync(file, "utf8") : "";
}

function stripMarkdown(text) {
  return text
    .replace(/```[\s\S]*?```/g, " ")
    .replace(/!\[[^\]]*]\([^)]+\)/g, " ")
    .replace(/\[[^\]]+]\([^)]+\)/g, " ")
    .replace(/[#>*`|_-]+/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

function sourceFiles(moduleDir) {
  if (moduleDir === ".") return [];
  const roots = sourceRoots(moduleDir);
  const files = unique(roots.flatMap((root) => walk(root).filter((file) => file.endsWith(".kt"))));
  return files.slice(0, moduleDir === "." ? 260 : 140);
}

function sourceRoots(moduleDir) {
  const mainRoot = path.join(moduleDir, "src/main/kotlin");
  const testRoot = path.join(moduleDir, "src/test/kotlin");
  const roots = [mainRoot].filter((root) => fs.existsSync(root));
  const directMainCount = roots.flatMap((root) => walk(root).filter((file) => file.endsWith(".kt"))).length;
  if (directMainCount === 0 && fs.existsSync(testRoot)) roots.push(testRoot);
  if (directMainCount >= 3 && moduleDir !== ".") return roots;

  const nested = walk(moduleDir)
    .filter((file) => path.basename(file) === "build.gradle.kts")
    .map((file) => path.dirname(file))
    .filter((dir) => dir !== moduleDir && !dir.includes(`${path.sep}buildSrc`))
    .flatMap((dir) => {
      const main = path.join(dir, "src/main/kotlin");
      const test = path.join(dir, "src/test/kotlin");
      return fs.existsSync(main) ? [main] : [test];
    })
    .filter((root) => fs.existsSync(root));

  return unique([...roots, ...nested]);
}

function parseSources(moduleDir) {
  const files = sourceFiles(moduleDir);
  const symbols = [];
  const functions = [];
  const tables = [];
  for (const file of files) {
    const source = readText(file);
    const lines = source.split(/\r?\n/);
    const symbolRe = /^\s*(interface|abstract\s+class|open\s+class|data\s+class|class|object|enum\s+class)\s+([A-Z][A-Za-z0-9_]*)\s*([^{]*)/;
    const functionRe = /^\s*(?:@(?:Get|Post|Put|Delete|Patch)Mapping\(([^)]*)\)\s*)?(?:suspend\s+)?fun\s+([A-Za-z_][A-Za-z0-9_]*)\s*\(/;

    for (const line of lines) {
      const match = symbolRe.exec(line);
      if (!match) continue;
      const kind = match[1].replace(/\s+/g, " ");
      const name = match[2];
      const tail = match[3] || "";
      const supers = parseSupers(tail);
      if (validSymbolName(name)) {
        const role = roleFor(file, name, kind);
        symbols.push({ file, kind, name, supers, role });
      }
    }
    for (const line of lines) {
      const match = functionRe.exec(line);
      if (match) functions.push({ file, route: match[1] || "", name: match[2], role: roleFor(file, match[2], "function") });
    }
    tables.push(...parseTables(file, source));
  }
  return { files, symbols: uniqueBy(symbols, (s) => `${s.file}:${s.name}`), functions, tables: uniqueBy(tables, (t) => t.name) };
}

function validSymbolName(name) {
  if (!name || name.length < 3) return false;
  if (/^(String|Int|Long|Double|Float|Boolean|List|Set|Map|Pair|Triple|Record)$/.test(name)) return false;
  return true;
}

function parseSupers(tail) {
  const colon = tail.indexOf(":");
  if (colon < 0) return [];
  return unique(
    tail
      .slice(colon + 1)
      .split(",")
      .map((part) => beforeAny(part, ["(", "<"]).trim())
      .filter((part) => /^[A-Z][A-Za-z0-9_]+$/.test(part))
      .filter(validSymbolName),
  );
}

function beforeAny(value, delimiters) {
  let end = value.length;
  for (const delimiter of delimiters) {
    const index = value.indexOf(delimiter);
    if (index >= 0 && index < end) end = index;
  }
  return value.slice(0, end);
}

function parseTables(file, source) {
  const out = [];
  const tableRe = /\b(object|class)\s+([A-Z][A-Za-z0-9_]*)\s*:[^\n{]*(?:Table|IdTable|LongIdTable|IntIdTable)[^\n{]*\{/g;
  let match;
  while ((match = tableRe.exec(source))) {
    const body = blockBody(source, tableRe.lastIndex - 1);
    const columns = [];
    const refs = [];
    let col;
    const columnRe = /(?:val\s+)?([A-Za-z_][A-Za-z0-9_]*)\s*(?::[^=]+)?=\s*([A-Za-z_][A-Za-z0-9_]*)\("([^"]+)"/g;
    while ((col = columnRe.exec(body))) columns.push(`${col[3]} ${columnType(col[2], col[1])}`);
    const referenceRe = /(?:val\s+)?[A-Za-z_][A-Za-z0-9_]*\s*(?::[^=]+)?=\s*(?:optReference|reference)\("([^"]+)",\s*([A-Z][A-Za-z0-9_]*)/g;
    while ((col = referenceRe.exec(body))) refs.push(col[2]);
    if (/IdTable/i.test(match[0])) columns.unshift("id PK");
    out.push({ name: match[2], file, columns: unique(columns.length ? columns : ["id PK"]), refs: unique(refs) });
  }
  return out;
}

function blockBody(source, openBraceIndex) {
  let depth = 0;
  for (let i = openBraceIndex; i < source.length; i += 1) {
    if (source[i] === "{") depth += 1;
    if (source[i] === "}") {
      depth -= 1;
      if (depth === 0) return source.slice(openBraceIndex + 1, i);
    }
  }
  return source.slice(openBraceIndex + 1);
}

function columnType(builder, property) {
  if (/reference|optReference/i.test(builder)) return "FK";
  if (/id/i.test(property)) return "PK";
  return builder.replace(/Column$/i, "");
}

function roleFor(file, name, kind) {
  const value = `${file}/${name}/${kind}`;
  if (/\b(controller|route|handler|endpoint|api|web)\b/i.test(value)) return "entry";
  if (/\b(service|usecase|workflow|job|scheduler)\b/i.test(value)) return "service";
  if (/\b(repository|dao|persistence)\b/i.test(value)) return "repository";
  if (/\b(table|entity|model|domain|record|dto)\b/i.test(value)) return "model";
  if (/\b(config|configuration|plugin|security|tenant|context|filter|cache|datasource|outbox|transaction)\b/i.test(value)) return "infra";
  if (/\b(application|module)\b/i.test(value)) return "app";
  return "component";
}

function buildModel(ref) {
  const moduleDir = moduleDirFor(ref);
  const readme = englishReadme(ref.readmes) || path.join(moduleDir, "README.md");
  const readmeText = readText(readme);
  const title = titleFromReadme(readmeText) || titleFromFile(ref.svg);
  const source = parseSources(moduleDir);
  enrichFromReadme(source, readmeText);
  return {
    ref,
    moduleDir,
    readme,
    title,
    summary: stripMarkdown(readmeText).slice(0, 500),
    source,
    type: diagramType(ref.svg),
  };
}

function englishReadme(readmes) {
  return [...readmes].sort((a, b) => readmeRank(a) - readmeRank(b) || a.localeCompare(b))[0];
}

function readmeRank(file) {
  const base = path.basename(file).toLowerCase();
  if (base === "readme.md") return 0;
  if (/^readme\.[a-z-]+\.md$/.test(base)) return 2;
  return 1;
}

function enrichFromReadme(source, readmeText) {
  if (source.symbols.length >= 4) return;
  const concepts = readmeConcepts(readmeText);
  const existing = new Set(source.symbols.map((symbol) => symbol.name.toLowerCase()));
  for (const concept of concepts) {
    if (existing.has(concept.name.toLowerCase())) continue;
    existing.add(concept.name.toLowerCase());
    source.symbols.push({
      file: "README.md",
      kind: concept.kind,
      name: concept.name,
      supers: [],
      role: roleFor("README.md", concept.name, concept.kind),
    });
  }
}

function readmeConcepts(text) {
  const out = [];
  const add = (name, kind = "class") => {
    const clean = name.replace(/^[./:-]+|[./:-]+$/g, "").replace(/[-_/]+/g, " ");
    if (!/^[A-Z][A-Za-z0-9_ ]{2,}$/.test(clean)) return;
    if (/^(README|HTTP|GET|POST|PUT|DELETE|H2|SQL|JPA|DDL|DML)$/.test(clean)) return;
    if (/^[A-Z0-9_]+$/.test(clean)) return;
    out.push({ name: clean, kind });
  };
  for (const match of text.matchAll(/`([A-Za-z][A-Za-z0-9_./:-]+)`/g)) {
    const raw = match[1].split(/[/:]/).filter(Boolean).pop() || "";
    const token = raw.includes("-") ? cleanTitle(raw.replace(/-/g, " ")) : raw.split(/[.-]/).filter(Boolean).pop() || "";
    add(token, /Repository|Controller|Table|Entity|Record|Config|Application$/.test(token) ? "class" : "object");
  }
  for (const line of text.split(/\r?\n/)) {
    if (!line.startsWith("|")) continue;
    const first = line.split("|")[1]?.replace(/[*`]/g, "").trim();
    if (first && !/^-+$|module|class|controller|profile|path|http/i.test(first)) add(first);
  }
  return uniqueBy(out, (item) => item.name).slice(0, 16);
}

function titleFromReadme(text) {
  const match = text.match(/^#\s+(.+)$/m);
  return match ? cleanTitle(match[1]) : "";
}

function titleFromFile(file) {
  return cleanTitle(
    path
      .basename(file, ".svg")
      .replace(/-\d+$/, "")
      .replace(/-(architecture|class|sequence|erd)$/i, "")
      .split("-")
      .filter((part) => !/^\d+$/.test(part))
      .join(" "),
  );
}

function cleanTitle(title) {
  return title
    .replace(/\s*\[[^\]]+]\([^)]+\)\s*/g, " ")
    .replace(/\bDml\b/gi, "DML")
    .replace(/\bDdl\b/gi, "DDL")
    .replace(/\bJpa\b/gi, "JPA")
    .replace(/\bJdbc\b/gi, "JDBC")
    .replace(/\bR2dbc\b/gi, "R2DBC")
    .replace(/\bSql\b/gi, "SQL")
    .replace(/\bJson\b/gi, "JSON")
    .replace(/\bHttp\b/gi, "HTTP")
    .replace(/(^|\s)([a-z])/g, (m) => m.toUpperCase())
    .replace(/\s+/g, " ")
    .trim();
}

function shell(width, height, title, body) {
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${width} ${height}" width="${width}" height="${height}" role="img" aria-label="${esc(title)}">
  <defs>
    <filter id="shadow" x="-10%" y="-10%" width="120%" height="130%">
      <feDropShadow dx="2" dy="4" stdDeviation="5" flood-color="#1f2937" flood-opacity="0.12"/>
    </filter>
    <marker id="arrow" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto">
      <path d="M1,1 L8,4.5 L1,8 Z" fill="#64748b"/>
    </marker>
    <marker id="inherit" markerWidth="11" markerHeight="10" refX="9.5" refY="5" orient="auto">
      <path d="M0.5,1 L9.5,5 L0.5,9 Z" fill="#fff" stroke="#64748b" stroke-width="1.5"/>
    </marker>
    <style>${STYLE}</style>
  </defs>
  <rect width="${width}" height="${height}" class="canvas"/>
  <rect x="20" y="20" width="${width - 40}" height="${height - 40}" rx="16" class="frame"/>
${body}
</svg>
`;
}

function header(title, subtitle) {
  return `<text x="48" y="58" class="title">${esc(trim(title, 82))}</text>
  <text x="48" y="80" class="subtitle">${esc(subtitle)}</text>`;
}

function renderArchitecture(model) {
  const width = 1378;
  const height = 650;
  const groups = architectureGroups(model);
  let body = header(model.title, "Source-derived component flow · README plus Kotlin implementation");

  body += `\n${panel(36, 112, 270, 350, "Entry points")}`;
  groups.entry.slice(0, 4).forEach((item, i) => (body += `\n${card(56, 132 + i * 76, 230, 58, item, i, detailFor(item))}`));

  body += `\n${panel(390, 112, 360, 350, "Application / domain")}`;
  groups.app.slice(0, 3).forEach((item, i) => (body += `\n${card(420, 136 + i * 112, 300, 76, item, i + 1, detailFor(item))}`));

  body += `\n${panel(842, 112, 250, 350, "Persistence")}`;
  groups.persistence.slice(0, 4).forEach((item, i) => (body += `\n${card(864, 138 + i * 74, 206, 56, item, i + 3, detailFor(item))}`));

  body += `\n${panel(1120, 112, 222, 350, "Runtime / infrastructure")}`;
  groups.infra.slice(0, 4).forEach((item, i) => (body += `\n${card(1138, 138 + i * 74, 186, 56, item, i + 5, detailFor(item))}`));

  body += `\n<path d="M286,176 C330,176 366,174 420,174" class="arrow"/>`;
  body += `\n<path d="M720,288 C774,288 812,236 864,236" class="arrow"/>`;
  body += `\n<path d="M720,400 C790,400 1050,404 1138,404" class="softArrow"/>`;
  body += `\n<path d="M1070,240 C1098,240 1108,240 1138,240" class="softArrow"/>`;

  body += `\n<rect x="36" y="510" width="1306" height="58" rx="10" fill="#ecfdf5" stroke="#86efac" stroke-width="1.3"/>`;
  body += `\n<text x="689" y="535" class="smallLabel" text-anchor="middle">${esc(outcomeFor(model))}</text>`;
  body += `\n<text x="689" y="554" class="detail" text-anchor="middle">Source packages decide the panels; arrows show only the important runtime or dependency flow.</text>`;
  return shell(width, height, model.title, body);
}

function architectureGroups(model) {
  const byRole = (role) => model.source.symbols.filter((symbol) => symbol.role === role).map((symbol) => symbol.name);
  const entry = unique([...byRole("entry"), ...model.source.functions.filter((f) => f.role === "entry").map((f) => f.name)]);
  const service = unique([...byRole("service"), ...byRole("app"), ...byRole("component")]);
  const persistence = unique([...byRole("repository"), ...byRole("model"), ...model.source.tables.map((table) => table.name)]);
  const infra = unique(byRole("infra"));
  return {
    entry: fallback(entry, ["README User", "HTTP Request", "Workshop Example"]),
    app: fallback(service, [model.title, "Service Boundary", "Use Case Flow"]),
    persistence: fallback(persistence, ["Repository", "Exposed Table", "Database"]),
    infra: fallback(infra, ["Runtime", "Configuration", "Test Fixture"]),
  };
}

function fallback(values, defaults) {
  return values.filter(Boolean).slice(0, 10).length ? values.filter(Boolean).slice(0, 10) : defaults;
}

function detailFor(name) {
  if (/controller|route|handler|api|http/i.test(name)) return "entry";
  if (/service|job|workflow/i.test(name)) return "use case";
  if (/repository|dao/i.test(name)) return "persistence";
  if (/table|entity|items|movies|actors|orders|users/i.test(name)) return "table/model";
  if (/tenant|security|context|filter|transaction/i.test(name)) return "cross-cutting";
  if (/config|plugin|cache|datasource|outbox|redis/i.test(name)) return "infrastructure";
  return "";
}

function outcomeFor(model) {
  const text = `${model.title} ${model.summary}`;
  if (/tenant/i.test(text)) return "Tenant context is resolved before service, repository, and Exposed table access.";
  if (/cache/i.test(text)) return "Cache policy is explicit between request handling, service logic, and repository fallback.";
  if (/routing datasource|datasource routing/i.test(text)) return "Routing context selects the datasource before repository code executes.";
  if (/coroutine|suspend/i.test(text)) return "Suspending boundaries are visible between request handlers and Exposed transactions.";
  if (/virtual thread/i.test(text)) return "Blocking JDBC work is isolated behind the virtual-thread runtime boundary.";
  return "The diagram follows source packages from entry point to application logic, persistence, and runtime support.";
}

function renderClass(model) {
  const hierarchy = classHierarchy(model);
  const bottomCols = 3;
  const topCols = Math.min(5, Math.max(3, hierarchy.top.length));
  const topW = Math.floor((1128 - (topCols - 1) * 28) / topCols);
  const bottomW = 320;
  const topRows = Math.max(1, Math.ceil(hierarchy.top.length / topCols));
  const bottomRows = Math.max(1, Math.ceil(hierarchy.bottom.length / bottomCols));
  const topH = 34 + topRows * 96;
  const bottomH = 42 + bottomRows * 108;
  const width = 1220;
  const height = 180 + topH + bottomH + 106;
  let body = header(model.title, "Source-derived hierarchy · supertypes and abstractions above concrete implementations");
  body += `\n${panel(46, 120, 1128, topH, "Abstractions / supertypes")}`;
  const topPos = placeGrid(hierarchy.top, 74, 146, topW, 72, topCols, 0, 28);
  topPos.parts.forEach((part) => (body += `\n${part}`));
  body += `\n${panel(46, 172 + topH, 1128, bottomH, "Concrete implementations / data types")}`;
  const bottomPos = placeGrid(hierarchy.bottom, 74, 198 + topH, bottomW, 84, bottomCols, 3, 46);
  bottomPos.parts.forEach((part) => (body += `\n${part}`));
  hierarchy.edges.slice(0, 14).forEach((edge) => {
    const parent = topPos.positions.get(edge.parent);
    const child = bottomPos.positions.get(edge.child);
    if (parent && child) {
      const laneY = 152 + topH;
      body += `\n<path d="M${child.x},${child.y - 42} L${child.x},${laneY} L${parent.x},${laneY} L${parent.x},${parent.y + 42}" class="inherit"/>`;
    }
  });
  const noteY = height - 74;
  body += `\n<rect x="46" y="${noteY}" width="1128" height="44" rx="10" fill="#f8fafc" stroke="#d7e2ec"/>`;
  body += `\n<text x="610" y="${noteY + 27}" class="detail" text-anchor="middle">Interfaces, abstract/open classes, shared contracts, and base tables stay above concrete examples.</text>`;
  return shell(width, height, model.title, body);
}

function classHierarchy(model) {
  const symbols = model.source.symbols.filter((s) => s.name.length <= 48);
  const topNames = new Set();
  for (const symbol of symbols) {
    if (/interface|abstract|open/.test(symbol.kind) || /Base|Abstract|Support|Contract/.test(symbol.name)) topNames.add(symbol.name);
    symbol.supers.forEach((superName) => topNames.add(superName));
  }
  const top = unique([...topNames]).filter((name) => symbols.some((s) => s.name === name) || /^[A-Z]/.test(name)).slice(0, 9);
  const edges = [];
  for (const symbol of symbols) {
    for (const parent of symbol.supers) {
      if (top.includes(parent)) edges.push({ parent, child: symbol.name });
    }
  }
  const preferred = new Map(edges.map((edge) => [edge.child, top.indexOf(edge.parent) % 3]));
  const bottom = symbols
    .map((s, index) => ({ name: s.name, index }))
    .filter((item) => !top.includes(item.name))
    .sort((a, b) => (preferred.get(a.name) ?? 99) - (preferred.get(b.name) ?? 99) || a.index - b.index)
    .map((item) => item.name)
    .slice(0, 15);
  return {
    top: fallback(top, symbols.slice(0, 3).map((s) => s.name)),
    bottom: fallback(bottom, symbols.slice(3, 12).map((s) => s.name)),
    edges,
  };
}

function placeGrid(items, x, y, w, h, cols, colorOffset, gap = 46) {
  const parts = [];
  const positions = new Map();
  items.forEach((item, index) => {
    const col = index % cols;
    const row = Math.floor(index / cols);
    const cx = x + col * (w + gap);
    const cy = y + row * (h + 24);
    parts.push(card(cx, cy, w, h, item, index + colorOffset, detailFor(item)));
    positions.set(item, { x: cx + w / 2, y: cy + h / 2 });
  });
  return { parts, positions };
}

function renderErd(model) {
  const tables = model.source.tables.length ? model.source.tables : inferredTables(model);
  const cols = tables.length <= 4 ? 2 : 3;
  const tableW = 286;
  const tableH = 154;
  const rows = Math.ceil(tables.length / cols);
  const width = Math.max(980, 92 + cols * tableW + (cols - 1) * 76);
  const height = 170 + rows * (tableH + 58) + 76;
  let body = header(model.title, "Source-derived ERD · Exposed table objects and column builders");
  const x0 = (width - (cols * tableW + (cols - 1) * 76)) / 2;
  const centers = [];
  const positions = new Map();
  tables.slice(0, 12).forEach((table, index) => {
    const col = index % cols;
    const row = Math.floor(index / cols);
    const x = x0 + col * (tableW + 76);
    const y = 126 + row * (tableH + 58);
    centers.push([x + tableW / 2, y + tableH / 2]);
    positions.set(table.name, { x, y, cx: x + tableW / 2, cy: y + tableH / 2, w: tableW, h: tableH });
    const [fill, stroke] = COLORS[index % COLORS.length];
    body += `\n<rect x="${x}" y="${y}" width="${tableW}" height="${tableH}" rx="12" fill="#fff" stroke="${stroke}" class="card"/>`;
    body += `\n<path d="M${x},${y + 38} L${x},${y + 12} Q${x},${y} ${x + 12},${y} L${x + tableW - 12},${y} Q${x + tableW},${y} ${x + tableW},${y + 12} L${x + tableW},${y + 38} Z" fill="${fill}"/>`;
    body += `\n${textBlock(wrap(table.name, 27, 1), x + tableW / 2, y + 25, "smallLabel", 18)}`;
    table.columns.slice(0, 6).forEach((column, c) => {
      body += `\n<text x="${x + 18}" y="${y + 62 + c * 15}" class="tiny">${esc(trim(column, 40))}</text>`;
    });
  });
  const relations = tables.flatMap((table) => (table.refs || []).map((parent) => ({ child: table.name, parent })));
  for (const relation of relations.slice(0, 14)) {
    const child = positions.get(relation.child);
    const parent = positions.get(relation.parent);
    if (!child || !parent) continue;
    const startX = child.cy > parent.cy ? child.cx : child.x + child.w;
    const startY = child.cy > parent.cy ? child.y : child.cy;
    const endX = child.cy > parent.cy ? parent.cx : parent.x;
    const endY = child.cy > parent.cy ? parent.y + parent.h : parent.cy;
    const midY = child.cy > parent.cy ? (startY + endY) / 2 : startY;
    body += `\n<path d="M${startX},${startY} L${startX},${midY} L${endX},${midY} L${endX},${endY}" class="softArrow"/>`;
  }
  body += `\n<text x="${width / 2}" y="110" class="panelTitle" text-anchor="middle">TABLES AND RELATIONSHIPS</text>`;
  return shell(width, height, model.title, body);
}

function inferredTables(model) {
  return model.source.symbols
    .filter((s) => s.role === "model")
    .slice(0, 8)
    .map((symbol) => ({ name: symbol.name, columns: ["id PK", "domain fields"] }));
}

function renderSequence(model) {
  const participants = sequenceParticipants(model);
  const messages = sequenceMessages(model, participants);
  const width = Math.max(1120, participants.length * 180 + 160);
  const height = 220 + messages.length * 54 + 88;
  const gap = (width - 180) / Math.max(1, participants.length - 1);
  let body = header(model.title, "Source-derived sequence · request path through implementation roles");
  participants.forEach((participant, index) => {
    const x = 90 + index * gap;
    body += `\n${card(x - 78, 118, 156, 54, participant, index, "")}`;
    body += `\n<path d="M${x},182 L${x},${height - 64}" class="softArrow"/>`;
  });
  messages.forEach((message, index) => {
    const from = Math.min(index, participants.length - 2);
    const to = from + 1;
    const x1 = 90 + from * gap;
    const x2 = 90 + to * gap;
    const y = 220 + index * 54;
    body += `\n<path d="M${x1 + 18},${y} L${x2 - 18},${y}" class="arrow"/>`;
    body += `\n<rect x="${(x1 + x2) / 2 - 142}" y="${y - 30}" width="284" height="24" rx="8" fill="#fff" stroke="#d7e2ec" class="messageBand"/>`;
    body += `\n<text x="${(x1 + x2) / 2}" y="${y - 13}" class="tiny" text-anchor="middle">${esc(trim(message, 48))}</text>`;
  });
  return shell(width, height, model.title, body);
}

function sequenceParticipants(model) {
  const groups = architectureGroups(model);
  return unique([
    "Client",
    groups.entry[0],
    groups.app[0],
    groups.app[1],
    groups.persistence[0],
    groups.persistence[1] || "DB",
  ]).slice(0, 7);
}

function sequenceMessages(model, participants) {
  const verbs = model.source.functions.filter((f) => f.role === "entry").map((f) => f.name).slice(0, 2);
  const main = verbs.length ? verbs : ["handle request"];
  return [
    `${main[0]}()`,
    `validate input and resolve context`,
    `call ${participants[2] || "service"}`,
    `execute use case boundary`,
    `delegate to ${participants[4] || "repository"}`,
    `run Exposed transaction`,
    `map rows to response model`,
    `return HTTP response`,
  ];
}

function renderDiagram(model) {
  if (model.type === "class") return renderClass(model);
  if (model.type === "erd") return renderErd(model);
  if (model.type === "sequence") return renderSequence(model);
  return renderArchitecture(model);
}

function panel(x, y, w, h, title) {
  return `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="12" class="panel"/>
  <text x="${x + w / 2}" y="${y - 6}" class="panelTitle" text-anchor="middle">${esc(title.toUpperCase())}</text>`;
}

function card(x, y, w, h, label, colorIndex, detail = "") {
  const [fill, stroke] = COLORS[colorIndex % COLORS.length];
  const lines = wrap(label, Math.max(12, Math.floor(w / 11)), detail && h < 70 ? 1 : h >= 54 ? 2 : 1);
  const y0 = y + (detail ? 29 : h / 2 - (lines.length - 1) * 10 + 6);
  return `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="10" fill="${fill}" stroke="${stroke}" class="card"/>
${textBlock(lines, x + w / 2, y0, "smallLabel", 18)}
${detail ? `<text x="${x + w / 2}" y="${y + h - 16}" class="detail" text-anchor="middle">${esc(trim(detail, 30))}</text>` : ""}`;
}

function wrap(value, max = 24, lines = 2) {
  const words = String(value)
    .replace(/([a-z])([A-Z])/g, "$1 $2")
    .replace(/([/_-])/g, "$1 ")
    .split(/\s+/)
    .filter(Boolean);
  const out = [];
  let line = "";
  for (const word of words) {
    const next = line ? `${line} ${word}` : word;
    if (next.length > max && line) {
      out.push(line);
      line = word;
    } else {
      line = next;
    }
  }
  if (line) out.push(line);
  const clipped = out.slice(0, lines);
  if (out.length > lines) clipped[lines - 1] = `${clipped[lines - 1].replace(/[. ]+$/, "")}...`;
  return clipped;
}

function textBlock(lines, x, y, cls = "label", step = 20, anchor = "middle") {
  return lines.map((line, index) => `<text x="${x}" y="${y + index * step}" class="${cls}" text-anchor="${anchor}">${esc(line)}</text>`).join("\n");
}

function trim(value, max) {
  return value.length <= max ? value : `${value.slice(0, max - 3)}...`;
}

function esc(value) {
  return String(value).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

function unique(values) {
  const out = [];
  const seen = new Set();
  for (const value of values) {
    if (!value) continue;
    const key = value.toLowerCase();
    if (seen.has(key)) continue;
    seen.add(key);
    out.push(value);
  }
  return out;
}

function uniqueBy(values, keyFn) {
  const out = [];
  const seen = new Set();
  for (const value of values) {
    const key = keyFn(value);
    if (seen.has(key)) continue;
    seen.add(key);
    out.push(value);
  }
  return out;
}

function convertToPng(svgFile) {
  const pngFile = svgFile.replace(/\.svg$/i, ".png");
  const result = spawnSync(RSVG, ["-f", "png", "-o", pngFile, svgFile], { encoding: "utf8" });
  if (result.status !== 0) throw new Error(`rsvg-convert failed for ${svgFile}: ${result.stderr || result.stdout}`);
}

function main() {
  const refs = referencedDiagrams();
  const targets = [];
  for (const ref of refs) {
    const model = buildModel(ref);
    fs.writeFileSync(ref.svg, renderDiagram(model));
    convertToPng(ref.svg);
    targets.push(ref.svg);
  }
  const byType = targets.reduce((acc, file) => {
    const type = diagramType(file);
    acc[type] = (acc[type] || 0) + 1;
    return acc;
  }, {});
  console.log(JSON.stringify({ regenerated: targets.length, byType }, null, 2));
}

main();
