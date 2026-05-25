#!/usr/bin/env node

const fs = require("fs");
const { spawnSync } = require("child_process");

const RSVG = process.env.RSVG_CONVERT || "rsvg-convert";
const DOT = process.env.DOT || "dot";
const outDir = "docs/images/readme-diagrams";
const sketchDir = ".omx/artifacts/diagram-sketches";

const style = `
.canvas{fill:#f6f9fc}.frame{fill:#fff;stroke:#d7e2ec;stroke-width:1.5}
.title{font-family:"Architects Daughter","Comic Sans MS",cursive;font-size:31px;fill:#1e293b}
.subtitle,.detail{font-family:"Comic Mono","Comic Sans MS","Comic Sans","Comic Neue",Arial,sans-serif;fill:#536476}
.subtitle{font-size:13px}.detail{font-size:12px}.tiny{font-family:"Comic Mono","Comic Sans MS","Comic Sans","Comic Neue",Arial,sans-serif;font-size:10px;fill:#64748b}
.label{font-family:"Architects Daughter","Comic Sans MS",cursive;font-size:16px;fill:#1e293b}
.tableName{font-family:"Architects Daughter","Comic Sans MS",cursive;font-size:18px;fill:#1e293b}
.panelTitle{font-family:"Architects Daughter","Comic Sans MS",cursive;font-size:12px;letter-spacing:.08em;fill:#94a3b8}
.card,.tableBox{stroke-width:1.7;filter:url(#shadow)}.panel{fill:#f8fafc;stroke:#d7e2ec;stroke-width:1.2}
.arrow{fill:none;stroke:#2563eb;stroke-width:2;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowBlue)}
.mapLine{fill:none;stroke:#16a34a;stroke-width:1.9;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowGreen)}
.dbUse{fill:none;stroke:#ea580c;stroke-width:2;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowOrange)}
.runtimeUse{fill:none;stroke:#7c3aed;stroke-width:1.9;stroke-dasharray:6 5;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowPurple)}
.returnLine{fill:none;stroke:#64748b;stroke-width:1.7;stroke-dasharray:7 5;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowGray)}
.lifeline{stroke:#cbd5e1;stroke-width:1.4;stroke-dasharray:5 6}
`;

const colors = [
  ["#dbeafe", "#3b82f6"],
  ["#ede9fe", "#7c3aed"],
  ["#dcfce7", "#16a34a"],
  ["#ffedd5", "#ea580c"],
  ["#fce7f3", "#db2777"],
  ["#ccfbf1", "#0f766e"],
  ["#fef3c7", "#d97706"],
  ["#e2e8f0", "#64748b"],
];

const diagrams = [
  {
    file: `${outDir}/08-coroutines-01-coroutines-basic-sequence-02.svg`,
    title: "newSuspendedTransaction Processing Sequence",
    subtitle: "Ex01_Coroutines coordinates withTablesSuspending, initial insert, async insert/update jobs, await, and final ordered assertions",
    width: 1500,
    height: 900,
    body: coroutineAsyncSequence,
  },
  {
    file: `${outDir}/08-coroutines-01-coroutines-basic-erd-03.svg`,
    title: "Coroutine Tester Tables ERD",
    subtitle: "Ex01_Coroutines defines independent Tester and TesterUnique tables; duplicate/update tests exercise the unique table",
    width: 1180,
    height: 720,
    body: coroutineTablesErd,
  },
  {
    file: `${outDir}/08-coroutines-02-virtualthreads-basic-sequence-01.svg`,
    title: "Virtual Thread Transaction Sequence",
    subtitle: "Ex01_VirtualThreads uses Java 21 gated tests, newVirtualThreadJdbcTransaction, async fan-out, regular transaction comparison, and exception cleanup",
    width: 1500,
    height: 940,
    body: virtualThreadSequence,
  },
  {
    file: `${outDir}/08-coroutines-02-virtualthreads-basic-architecture-02.svg`,
    title: "Virtual Thread Processing Model",
    subtitle: "VirtualThread JDBC APIs keep blocking-style code while VirtualFuture work runs through JVM virtual threads into normal Exposed transactions",
    width: 1500,
    height: 860,
    body: virtualThreadProcessingModel,
  },
  {
    file: `${outDir}/08-coroutines-02-virtualthreads-basic-architecture-03.svg`,
    title: "Virtual Thread vs Platform Thread Comparison",
    subtitle: "The example compares virtual-thread transaction APIs with regular transaction calls against the same JDBC pool and VTester table",
    width: 1500,
    height: 820,
    body: virtualVsPlatformComparison,
  },
];

writeGraphvizSketches();
for (const diagram of diagrams) {
  fs.writeFileSync(diagram.file, shell(diagram));
  const png = diagram.file.replace(/\.svg$/, ".png");
  const result = spawnSync(RSVG, ["-f", "png", "-o", png, diagram.file], { encoding: "utf8" });
  if (result.status !== 0) throw new Error(result.stderr || result.stdout);
  console.log(png);
}
validateBatch16Semantics();
validateConnectorGeometry();

function writeGraphvizSketches() {
  fs.mkdirSync(sketchDir, { recursive: true });
  const sketches = {
    "batch-16-coroutine-async-sequence": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=0.6, ranksep=0.85, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      test -> withTablesSuspending -> newSuspendedTransaction -> TesterUnique;
      newSuspendedTransaction -> insertJob -> TesterUnique;
      newSuspendedTransaction -> updateJob -> TesterUnique;
      insertJob -> await; updateJob -> await; await -> finalAssertions;
    }`,
    "batch-16-coroutine-erd": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=1.0, ranksep=1.0, pad=0.2];
      node [shape=record, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      Tester [label="{coroutines_tester|id SERIAL PK}"];
      TesterUnique [label="{coroutines_tester_unique|id INT PK UNIQUE}"];
      fixture [shape=box, label="withTablesSuspending"];
      fixture -> Tester; fixture -> TesterUnique;
    }`,
    "batch-16-virtual-sequence": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=0.6, ranksep=0.85, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      java21Test -> withTables -> newVirtualThreadJdbcTransaction -> VTester;
      newVirtualThreadJdbcTransaction -> getTesterById -> VTester;
      java21Test -> virtualThreadJdbcTransactionAsync -> awaitAll -> VTester;
      java21Test -> regularTransaction -> VTester;
      java21Test -> duplicateId -> ExecutionException -> cleanupAssertion;
    }`,
    "batch-16-virtual-model": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=0.75, ranksep=0.9, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      test -> virtualThreadApi -> virtualFuture -> jvmVirtualThread -> jdbcTransaction -> vtester -> db;
      duplicateId -> executionException -> cleanupAssertion;
      regularTransaction -> jdbcTransaction;
    }`,
    "batch-16-vt-vs-platform": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=0.8, ranksep=0.9, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      virtualPath -> virtualThreadJdbcTransactionAsync -> virtualThreads -> jdbcPool -> db;
      platformPath -> transaction -> platformThread -> jdbcPool;
      db -> vtester [dir=back];
    }`,
  };
  for (const [name, dot] of Object.entries(sketches)) {
    const dotPath = `${sketchDir}/${name}.dot`;
    fs.writeFileSync(dotPath, dot);
    spawnSync(DOT, ["-Tplain", "-o", `${sketchDir}/${name}.plain`, dotPath], { encoding: "utf8" });
    spawnSync(DOT, ["-Tsvg", "-o", `${sketchDir}/${name}.svg`, dotPath], { encoding: "utf8" });
  }
}

function coroutineAsyncSequence() {
  let b = "";
  const participants = [
    [110, "Test"],
    [315, "Fixture"],
    [535, "Coroutine API"],
    [760, "Insert job"],
    [980, "Update job"],
    [1200, "TesterUnique"],
    [1390, "Assertions"],
  ];
  for (const [x, label] of participants) {
    b += card(x - 72, 130, 144, 54, label, 0, "");
    b += `<line x1="${x}" y1="184" x2="${x}" y2="750" class="lifeline"/>\n`;
  }
  b += seqBand(86, 222, 246, "1", "withTablesSuspending creates unique table", 2);
  b += seqArrow(110, 315, 276, "arrow");
  b += seqBand(300, 300, 300, "2", "newSuspendedTransaction inserts origin id=1", 1);
  b += seqArrow(315, 535, 354, "arrow");
  b += seqArrow(535, 1200, 382, "dbUse");
  b += seqBand(612, 418, 300, "3", "insertJob retries id=1 after update", 6);
  b += seqArrow(535, 760, 472, "arrow");
  b += seqArrow(760, 1200, 500, "dbUse");
  b += seqBand(842, 534, 312, "4", "updateJob moves id 1 to 99", 7);
  b += seqArrow(535, 980, 588, "arrow");
  b += seqArrow(980, 1200, 616, "dbUse");
  b += seqBand(1126, 654, 286, "5", "await jobs; assert ids [1, 99]", 4);
  b += seqArrow(1200, 1390, 708, "returnLine");
  b += note(126, 808, 1248, "Source check: TesterUnique has id INT primary key + unique index; insert/update jobs use maxAttempts=20 and final ordered ids are [1, 99].");
  return b;
}

function coroutineTablesErd() {
  let b = "";
  b += panel(68, 130, 470, 380, "Coroutine fixture tables");
  b += table(112, 188, 380, "coroutines_tester", ["PK id: EntityID<Int>", "id column: SERIAL / IntIdTable"], ["Used by Tester", "Selected by getTesterById", "Backs TesterEntity duplicate test"], 2);
  b += panel(642, 130, 470, 380, "Unique constraint test table");
  b += table(686, 188, 380, "coroutines_tester_unique", ["PK id: INT", "UNIQUE id"], ["Used by TesterUnique", "Async insert/update contention", "Expected ordered ids: 1, 99"], 4);
  b += card(430, 568, 320, 62, "No FK relationship", 7, "independent test fixtures");
  b += note(118, 648, 944, "ERD rule: no relationship line is drawn between these tables because the source defines no foreign key; the unique table exists for duplicate and retry scenarios.");
  return b;
}

function virtualThreadSequence() {
  let b = "";
  const participants = [
    [105, "Java 21 Test"],
    [315, "Fixture"],
    [545, "VT API"],
    [775, "JdbcTransaction"],
    [1010, "VTester"],
    [1215, "Regular Tx"],
    [1390, "Assertions"],
  ];
  for (const [x, label] of participants) {
    b += card(x - 76, 130, 152, 54, label, 0, "");
    b += `<line x1="${x}" y1="184" x2="${x}" y2="790" class="lifeline"/>\n`;
  }
  b += seqBand(82, 222, 248, "1", "@EnabledForJreRange gates Java 21", 7);
  b += seqArrow(105, 315, 276, "arrow");
  b += seqBand(292, 304, 300, "2", "withTables creates VTester fixture", 2);
  b += seqArrow(315, 545, 358, "arrow");
  b += seqBand(520, 386, 304, "3", "newVirtualThread Tx inserts and commits", 1);
  b += seqArrow(545, 775, 440, "arrow");
  b += seqArrow(775, 1010, 468, "dbUse");
  b += seqBand(520, 502, 330, "4", "VT async fan-out inserts/selects", 6);
  b += seqArrow(545, 775, 556, "arrow");
  b += seqArrow(775, 1010, 584, "dbUse");
  b += seqBand(1030, 618, 280, "5", "regular transaction also selects", 7);
  b += seqArrow(1215, 1010, 672, "dbUse");
  b += seqBand(1112, 730, 278, "6", "duplicate id wraps ExposedSQLException", 4);
  b += seqArrow(775, 1390, 702, "returnLine");
  b += seqArrow(1010, 1390, 812, "returnLine");
  b += note(126, 850, 1248, "Sequence rule: Virtual Thread calls are solid; verification/exception results return as dashed arrows. The regular transaction comparison is source-backed and uses the same VTester table.");
  return b;
}

function virtualThreadProcessingModel() {
  let b = "";
  b += panel(58, 130, 300, 560, "Test entry");
  b += card(96, 180, 220, 62, "Java 21 gate", 0, "@EnabledForJreRange");
  b += card(96, 306, 220, 62, "withTables", 2, "VTester fixture");
  b += card(96, 432, 220, 62, "test scenarios", 4, "insert/select/exception");

  b += panel(430, 130, 380, 560, "Virtual Thread API");
  b += card(486, 178, 268, 64, "newVirtualThread Tx", 1, "awaits VirtualFuture");
  b += card(486, 310, 268, 64, "virtualThread Tx async", 6, "returns VirtualFuture");
  b += card(486, 442, 268, 64, "awaitAll", 7, "join 10 tasks");

  b += panel(882, 130, 300, 560, "Exposed JDBC");
  b += card(928, 188, 208, 64, "JVM virtual thread", 6, "blocking style");
  b += card(928, 330, 208, 64, "JdbcTransaction", 5, "transaction(db)");
  b += card(928, 472, 208, 64, "cleanup checks", 4, "closed inner conn");

  b += panel(1254, 130, 188, 560, "Database");
  b += card(1288, 202, 120, 58, "VTester", 2, "id, name");
  b += cylinder(1288, 344, 120, 190, "DB", ["H2", "Postgres", "MySQL"], 3);

  b += path("M316,211 L486,210", "arrow", "M316,211 L400,211 L400,210 L486,210");
  b += path("M316,337 L486,210", "arrow", "M316,337 L392,337 L392,210 L486,210");
  b += path("M316,463 L486,342", "runtimeUse", "M316,463 L392,463 L392,342 L486,342");
  b += path("M754,210 L928,220", "arrow", "M754,210 L838,210 L838,220 L928,220");
  b += path("M754,342 L928,362", "arrow", "M754,342 L838,342 L838,362 L928,362");
  b += path("M754,474 L928,504", "runtimeUse", "M754,474 L838,474 L838,504 L928,504");
  b += path("M1136,362 L1288,231", "mapLine", "M1136,362 L1210,362 L1210,231 L1288,231");
  b += path("M1136,504 L1288,430", "runtimeUse", "M1136,504 L1210,504 L1210,430 L1288,430");
  b += path("M1348,260 L1348,344", "dbUse");
  b += note(130, 758, 1240, "Source check: newVirtualThreadJdbcTransaction awaits VirtualFuture; async work runs transaction(db), and duplicate-id cleanup closes the inner connection.");
  return b;
}

function virtualVsPlatformComparison() {
  let b = "";
  b += panel(60, 130, 600, 430, "Virtual thread path");
  b += card(96, 198, 210, 64, "VT transaction async", 1, "VirtualFuture");
  b += card(390, 198, 210, 64, "Virtual Threads", 6, "lightweight tasks");
  b += card(96, 388, 210, 64, "newVirtualThread Tx", 5, "blocking style API");
  b += card(390, 388, 210, 64, "many inserts/selects", 2, "awaitAll fan-out");

  b += panel(60, 590, 600, 130, "Platform thread comparison");
  b += card(96, 624, 210, 58, "regular transaction", 7, "transaction { }");
  b += card(390, 624, 210, 58, "platform thread", 7, "baseline check");

  b += panel(760, 130, 330, 590, "Shared Exposed layer");
  b += card(820, 222, 210, 64, "JdbcTransaction", 5, "same Exposed API");
  b += card(820, 392, 210, 64, "Connection pool", 0, "shared bottleneck");
  b += card(820, 562, 210, 64, "Assertions", 4, "both paths pass");

  b += panel(1190, 130, 250, 590, "Database");
  b += card(1230, 202, 170, 58, "VTester", 2, "id + nullable name");
  b += cylinder(1258, 360, 114, 190, "DB", ["H2", "Postgres", "MySQL"], 3);

  b += path("M306,230 L390,230", "arrow");
  b += path("M600,230 L820,254", "arrow", "M600,230 L710,230 L710,254 L820,254");
  b += path("M306,420 L390,420", "arrow");
  b += path("M600,420 L820,424", "mapLine", "M600,420 L710,420 L710,424 L820,424");
  b += path("M306,653 L390,653", "runtimeUse");
  b += path("M600,653 L820,594", "runtimeUse", "M600,653 L710,653 L710,594 L820,594");
  b += path("M1030,254 L1230,231", "mapLine", "M1030,254 L1130,254 L1130,231 L1230,231");
  b += path("M1030,424 L1258,444", "dbUse", "M1030,424 L1144,424 L1144,444 L1258,444");
  b += path("M1315,260 L1315,360", "dbUse");
  b += note(142, 760, 1216, "Decision guide: virtual threads minimize code changes for Java 21 blocking-style code, but both paths still converge on the same JDBC transaction and database capacity.");
  return b;
}

function seqBand(x, y, w, n, label, c) {
  const [fill, stroke] = colors[c % colors.length];
  return `<rect x="${x}" y="${y}" width="${w}" height="46" rx="10" fill="${fill}" stroke="${stroke}" class="card"/>
<circle cx="${x + 23}" cy="${y + 23}" r="14" fill="#fff" stroke="${stroke}" stroke-width="1.4"/>
<text x="${x + 23}" y="${y + 28}" class="detail" text-anchor="middle">${esc(n)}</text>
<text x="${x + 48}" y="${y + 29}" class="detail">${esc(label)}</text>\n`;
}

function seqArrow(x1, x2, y, cls) {
  return path(`M${x1},${y} L${x2},${y}`, cls);
}

function shell(diagram) {
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${diagram.width} ${diagram.height}" width="${diagram.width}" height="${diagram.height}" role="img" aria-label="${esc(diagram.title)}">
<defs>
<filter id="shadow" x="-10%" y="-10%" width="120%" height="130%"><feDropShadow dx="2" dy="4" stdDeviation="5" flood-color="#1f2937" flood-opacity="0.12"/></filter>
<marker id="arrowBlue" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto"><path d="M1,1 L8,4.5 L1,8 Z" fill="#2563eb"/></marker>
<marker id="arrowGreen" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto"><path d="M1,1 L8,4.5 L1,8 Z" fill="#16a34a"/></marker>
<marker id="arrowOrange" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto"><path d="M1,1 L8,4.5 L1,8 Z" fill="#ea580c"/></marker>
<marker id="arrowPurple" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto"><path d="M1,1 L8,4.5 L1,8 Z" fill="#7c3aed"/></marker>
<marker id="arrowGray" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto"><path d="M1,1 L8,4.5 L1,8 Z" fill="#64748b"/></marker>
<style>${style}</style>
</defs>
<rect width="${diagram.width}" height="${diagram.height}" class="canvas"/>
<rect x="20" y="20" width="${diagram.width - 40}" height="${diagram.height - 40}" rx="16" class="frame"/>
<text x="48" y="58" class="title">${esc(diagram.title)}</text>
<text x="48" y="80" class="subtitle">${esc(diagram.subtitle)}</text>
${diagram.body()}</svg>\n`;
}

function panel(x, y, w, h, title) {
  return `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="12" class="panel"/><text x="${x + w / 2}" y="${y - 8}" class="panelTitle" text-anchor="middle">${esc(title.toUpperCase())}</text>\n`;
}

function card(x, y, w, h, title, c, detail = "") {
  const [fill, stroke] = colors[c % colors.length];
  const lines = splitTitle(title, Math.max(9, Math.floor(w / 10)), 2);
  const shownDetail = lines.length > 1 && h < 62 ? "" : detail;
  const titleY = y + (shownDetail ? 25 : h / 2 - (lines.length - 1) * 10 + 6);
  let out = `<desc>${esc(title)}</desc><rect x="${x}" y="${y}" width="${w}" height="${h}" rx="10" fill="${fill}" stroke="${stroke}" class="card"/>\n`;
  lines.forEach((line, i) => (out += `<text x="${x + w / 2}" y="${titleY + i * 18}" class="label" text-anchor="middle">${esc(line)}</text>\n`));
  if (shownDetail) out += `<text x="${x + w / 2}" y="${y + h - 14}" class="detail" text-anchor="middle">${esc(shownDetail)}</text>\n`;
  return out;
}

function table(x, y, w, name, keys, rows, c) {
  const [fill, stroke] = colors[c % colors.length];
  const keyStart = y + 82;
  const rowStart = y + 164;
  let out = `<rect x="${x}" y="${y}" width="${w}" height="282" rx="6" fill="#fff" stroke="${stroke}" class="tableBox"/>
<rect x="${x}" y="${y}" width="${w}" height="58" rx="6" fill="${fill}" stroke="${stroke}" stroke-width="1.7"/>
<line x1="${x}" y1="${y + 58}" x2="${x + w}" y2="${y + 58}" stroke="${stroke}" stroke-width="1.3"/>
<line x1="${x}" y1="${y + 142}" x2="${x + w}" y2="${y + 142}" stroke="${stroke}" stroke-width="1.3"/>
<text x="${x + w / 2}" y="${y + 37}" class="tableName" text-anchor="middle">${esc(name)}</text>\n`;
  keys.forEach((key, i) => (out += `<text x="${x + 24}" y="${keyStart + i * 26}" class="detail">${esc(key)}</text>\n`));
  rows.forEach((row, i) => (out += `<text x="${x + 24}" y="${rowStart + i * 26}" class="detail">${esc(row)}</text>\n`));
  return out;
}

function cylinder(x, y, w, h, title, rows, c) {
  const [fill, stroke] = colors[c % colors.length];
  const capH = 34;
  let out = `<path d="M${x},${y + capH / 2} C${x},${y - 4} ${x + w},${y - 4} ${x + w},${y + capH / 2} L${x + w},${y + h - capH / 2} C${x + w},${y + h + 4} ${x},${y + h + 4} ${x},${y + h - capH / 2} Z" fill="#fff" stroke="${stroke}" stroke-width="1.7" class="card"/>
<ellipse cx="${x + w / 2}" cy="${y + capH / 2}" rx="${w / 2}" ry="${capH / 2}" fill="${fill}" stroke="${stroke}" stroke-width="1.7"/>
<path d="M${x},${y + h - capH / 2} C${x},${y + h + 4} ${x + w},${y + h + 4} ${x + w},${y + h - capH / 2}" fill="none" stroke="${stroke}" stroke-width="1.7"/>
<text x="${x + w / 2}" y="${y + 52}" class="label" text-anchor="middle">${esc(title)}</text>\n`;
  rows.forEach((row, i) => (out += `<text x="${x + 14}" y="${y + 86 + i * 18}" class="tiny">${esc(row)}</text>\n`));
  return out;
}

function path(defaultD, cls, explicitD = null) {
  return `<path d="${explicitD || defaultD}" class="${cls}"/>\n`;
}

function note(x, y, w, text) {
  return `<rect x="${x}" y="${y}" width="${w}" height="52" rx="10" fill="#ecfdf5" stroke="#86efac" stroke-width="1.3"/>
<text x="${x + w / 2}" y="${y + 31}" class="detail" text-anchor="middle">${esc(text)}</text>\n`;
}

function splitTitle(value, max, lines) {
  const text = String(value);
  if (text.includes(" ")) return wrap(text, max, lines);
  if (text.length <= max) return [text];
  const parts = text.match(/[A-Z]+(?=[A-Z][a-z]|\d|$)|[A-Z]?[a-z]+|\d+/g) || [text];
  const out = [];
  let line = "";
  for (const part of parts) {
    const next = `${line}${part}`;
    if (next.length > max && line) {
      out.push(line);
      line = part;
    } else {
      line = next;
    }
  }
  if (line) out.push(line);
  return out.slice(0, lines);
}

function wrap(value, max, lines) {
  const words = String(value).split(/\s+/);
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
  return out.slice(0, lines);
}

function esc(value) {
  return String(value).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

function validateBatch16Semantics() {
  const svgs = diagrams.map((d) => fs.readFileSync(d.file, "utf8"));
  const failures = [];
  for (const required of ["withTablesSuspending", "newSuspendedTransaction", "TesterUnique", "maxAttempts=20", "[1, 99]"]) {
    if (!svgs[0].includes(required)) failures.push(`coroutine async sequence missing ${required}`);
  }
  for (const required of ["coroutines_tester", "coroutines_tester_unique", "No FK relationship", "UNIQUE id"]) {
    if (!svgs[1].includes(required)) failures.push(`coroutine ERD missing ${required}`);
  }
  for (const required of ["newVirtualThread", "VT async fan-out", "regular transaction", "ExposedSQLException"]) {
    if (!svgs[2].includes(required)) failures.push(`virtual sequence missing ${required}`);
  }
  for (const required of ["VirtualFuture", "JVM virtual thread", "JdbcTransaction", "closed inner conn"]) {
    if (!svgs[3].includes(required)) failures.push(`virtual model missing ${required}`);
  }
  for (const required of ["VT transaction async", "regular transaction", "Connection pool", "VTester"]) {
    if (!svgs[4].includes(required)) failures.push(`comparison missing ${required}`);
  }
  if (failures.length) throw new Error(`batch16_semantic_validation_failed\n${failures.join("\n")}`);
  console.log("batch16_semantics=ok");
}

function validateConnectorGeometry() {
  const connectorClasses = "(?:arrow|mapLine|dbUse|runtimeUse|returnLine)";
  const failures = [];
  for (const diagram of diagrams) {
    const svg = fs.readFileSync(diagram.file, "utf8");
    const blockers = [
      ...svg.matchAll(/<rect x="([\d.]+)" y="([\d.]+)" width="([\d.]+)" height="([\d.]+)" rx="10"[^>]*class="card"/g),
      ...svg.matchAll(/<rect x="([\d.]+)" y="([\d.]+)" width="([\d.]+)" height="282" rx="6"[^>]*class="tableBox"/g),
    ].map((m) => ({ x: Number(m[1]), y: Number(m[2]), w: Number(m[3]), h: Number(m[4]) }));
    const segments = [];
    for (const match of svg.matchAll(new RegExp(`<path\\b([^>]*class="(${connectorClasses})"[^>]*)`, "g"))) {
      const cls = match[2];
      const d = (match[1].match(/d="([^"]+)"/) || [])[1];
      if (!d) continue;
      const points = [...d.matchAll(/[ML]([\d.]+),([\d.]+)/g)].map((m) => ({ x: Number(m[1]), y: Number(m[2]) }));
      if (points.length < 2) continue;
      for (let i = 0; i < points.length - 1; i++) {
        const a = points[i];
        const b = points[i + 1];
        if (a.x !== b.x && a.y !== b.y) failures.push(`${diagram.file}: diagonal connector ${a.x},${a.y}->${b.x},${b.y}`);
        for (const rect of blockers) {
          if (segmentCrossesInterior(a, b, rect)) failures.push(`${diagram.file}: connector crosses component interior ${cls} ${a.x},${a.y}->${b.x},${b.y}`);
        }
        segments.push({ cls, a, b });
      }
    }
    for (let i = 0; i < segments.length; i++) {
      for (let j = i + 1; j < segments.length; j++) {
        const overlap = overlapLength(segments[i], segments[j]);
        if (overlap > 8 && segments[i].cls !== segments[j].cls) {
          failures.push(`${diagram.file}: mixed connector lane overlap ${segments[i].cls}/${segments[j].cls} length=${overlap}`);
        }
      }
    }
  }
  if (failures.length) throw new Error(`batch16_connector_validation_failed\n${failures.join("\n")}`);
  console.log("batch16_connectors=ok");
}

function segmentCrossesInterior(a, b, rect) {
  const left = rect.x + 2;
  const right = rect.x + rect.w - 2;
  const top = rect.y + 2;
  const bottom = rect.y + rect.h - 2;
  if (a.y === b.y) {
    if (a.y <= top || a.y >= bottom) return false;
    return rangeOverlap(a.x, b.x, left, right) > 8;
  }
  if (a.x === b.x) {
    if (a.x <= left || a.x >= right) return false;
    return rangeOverlap(a.y, b.y, top, bottom) > 8;
  }
  return false;
}

function overlapLength(left, right) {
  const lVertical = left.a.x === left.b.x;
  const rVertical = right.a.x === right.b.x;
  if (lVertical !== rVertical) return 0;
  if (lVertical) {
    if (left.a.x !== right.a.x) return 0;
    return rangeOverlap(left.a.y, left.b.y, right.a.y, right.b.y);
  }
  if (left.a.y !== right.a.y) return 0;
  return rangeOverlap(left.a.x, left.b.x, right.a.x, right.b.x);
}

function rangeOverlap(a1, a2, b1, b2) {
  const minA = Math.min(a1, a2);
  const maxA = Math.max(a1, a2);
  const minB = Math.min(b1, b2);
  const maxB = Math.max(b1, b2);
  return Math.max(0, Math.min(maxA, maxB) - Math.max(minA, minB));
}
