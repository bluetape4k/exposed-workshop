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
.panelTitle{font-family:"Architects Daughter","Comic Sans MS",cursive;font-size:12px;letter-spacing:.08em;fill:#94a3b8}
.card{stroke-width:1.7;filter:url(#shadow)}.panel{fill:#f8fafc;stroke:#d7e2ec;stroke-width:1.2}
.arrow{fill:none;stroke:#2563eb;stroke-width:2;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowBlue)}
.runtimeUse{fill:none;stroke:#7c3aed;stroke-width:1.9;stroke-dasharray:5 5;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowPurple)}
.dbUse{fill:none;stroke:#ea580c;stroke-width:2;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowOrange)}
.savepoint{fill:none;stroke:#db2777;stroke-width:1.9;stroke-dasharray:7 4;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowPink)}
.returnLine{fill:none;stroke:#64748b;stroke-width:1.8;stroke-dasharray:6 5;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrow)}
.lifeline{stroke:#cbd5e1;stroke-width:1.5;stroke-dasharray:5 6}
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
    file: `${outDir}/05-exposed-dml-03-functions-architecture-01.svg`,
    title: "Exposed SQL Function Classification",
    subtitle: "Function tests group expression helpers by SQL intent and verify generated SQL against dialect behavior",
    width: 1560,
    height: 900,
    body: functionsClassification,
  },
  {
    file: `${outDir}/05-exposed-dml-03-functions-architecture-02.svg`,
    title: "Exposed Window Function Structure",
    subtitle: "Window functions compose function call, OVER clause, partition/order/frame, Sales rows, and dialect assertions",
    width: 1540,
    height: 900,
    body: windowFunctionStructure,
  },
  {
    file: `${outDir}/05-exposed-dml-04-transactions-architecture-01.svg`,
    title: "Exposed Nested Transaction Savepoint Flow",
    subtitle: "Nested transaction tests keep outer rows while rolling back nested work through useNestedTransactions and savepoints",
    width: 1500,
    height: 900,
    body: nestedTransactionFlow,
  },
  {
    file: `${outDir}/05-exposed-dml-04-transactions-sequence-02.svg`,
    title: "Exposed Coroutine Transaction Flow",
    subtitle: "Coroutine tests choose current savepoint or new suspended transaction, then return to the outer transaction context",
    width: 1500,
    height: 880,
    body: coroutineTransactionSequence,
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
validateBatch08Semantics();
validateConnectorGeometry();

function writeGraphvizSketches() {
  fs.mkdirSync(sketchDir, { recursive: true });
  const sketches = {
    "batch-08-functions-classification": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=0.75, ranksep=1.0, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      string_math_aggregate_window -> sql_function_dsl -> generated_sql -> db;
      fake_dual -> sql_function_dsl;
      sales -> aggregate_window;
      db -> assertions;
    }`,
    "batch-08-window-structure": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=0.75, ranksep=1.0, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      function_call -> over -> partition_order -> frame -> sales_select -> db -> expected_values;
    }`,
    "batch-08-nested-transaction": `digraph G {
      graph [rankdir=TB, splines=ortho, nodesep=0.8, ranksep=0.9, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      database_config -> outer_tx -> city1;
      outer_tx -> nested1 -> city2;
      nested1 -> nested2 -> city3;
      nested1 -> rollback_savepoint -> final_city1;
      outer_tx -> db;
    }`,
    "batch-08-coroutine-transaction": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=0.65, ranksep=0.9, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      test -> with_tables_suspending -> transaction_manager -> run_with_savepoint_or_new -> savepoint -> db -> assertions;
      run_with_savepoint_or_new -> new_suspended_transaction;
    }`,
  };
  for (const [name, dot] of Object.entries(sketches)) {
    const dotPath = `${sketchDir}/${name}.dot`;
    fs.writeFileSync(dotPath, dot);
    spawnSync(DOT, ["-Tsvg", "-o", `${sketchDir}/${name}.svg`, dotPath], { encoding: "utf8" });
    spawnSync(DOT, ["-Tplain", "-o", `${sketchDir}/${name}.plain`, dotPath], { encoding: "utf8" });
  }
}

function functionsClassification() {
  let b = panel(58, 130, 404, 610, "Function categories");
  b += card(96, 178, 290, 62, "String / Conditional", 0, "trim, lower, case, coalesce");
  b += card(96, 286, 290, 62, "Math / Trig", 6, "round, abs, sin, atan2");
  b += card(96, 394, 290, 62, "Aggregate / Statistics", 2, "count, sum, avg, stdDev");
  b += card(96, 502, 290, 62, "Window", 4, "rowNumber, rank, lead, lag");
  b += card(96, 610, 290, 62, "Bitwise / Regex", 5, "bitwiseAnd, regexp");

  b += panel(536, 130, 332, 610, "Exposed expression DSL");
  b += card(590, 202, 224, 74, "SqlFunction<T>", 1, "typed Function<T>");
  b += card(590, 346, 224, 74, "Table.Dual", 7, "scalar expressions");
  b += card(590, 490, 224, 74, "DMLTestData.Sales", 2, "aggregate/window rows");
  b += card(590, 634, 224, 62, "Assertions", 5, "expected values");

  b += panel(942, 130, 344, 610, "Generated SQL");
  b += card(990, 190, 232, 68, "SELECT expression", 0, "scalar functions");
  b += card(990, 330, 232, 68, "GROUP BY / HAVING", 2, "aggregate results");
  b += card(990, 470, 232, 68, "OVER clause", 4, "window results");
  b += card(990, 610, 232, 68, "Dialect support", 7, "feature assumptions");

  b += panel(1348, 130, 140, 610, "Database");
  b += cylinder(1370, 298, 96, 230, "DB", ["H2", "Postgres", "MySQL", "MariaDB"], 3);

  b += path("M386,209 L590,239", "arrow", "M386,209 L488,209 L488,239 L590,239");
  b += path("M386,317 L590,239", "arrow", "M386,317 L504,317 L504,239 L590,239");
  b += path("M386,425 L590,527", "arrow", "M386,425 L488,425 L488,527 L590,527");
  b += path("M386,533 L590,527", "arrow", "M386,533 L504,533 L504,527 L590,527");
  b += path("M386,641 L590,239", "runtimeUse", "M386,641 L472,641 L472,239 L590,239");
  b += path("M814,239 L990,224", "arrow", "M814,239 L904,239 L904,224 L990,224");
  b += path("M814,527 L990,364", "arrow", "M814,527 L904,527 L904,364 L990,364");
  b += path("M814,527 L990,504", "arrow", "M814,527 L920,527 L920,504 L990,504");
  b += path("M814,665 L990,644", "returnLine", "M814,665 L920,665 L920,644 L990,644");
  b += path("M1222,224 L1370,356", "dbUse", "M1222,224 L1310,224 L1310,356 L1370,356");
  b += path("M1222,364 L1370,396", "dbUse", "M1222,364 L1310,364 L1310,396 L1370,396");
  b += path("M1222,504 L1370,436", "dbUse", "M1222,504 L1310,504 L1310,436 L1370,436");
  b += note(126, 796, 1308, "Source check: Ex01..Ex05 cover string/conditional, math, statistics, trigonometric, and window functions through typed Exposed Function<T> expressions.");
  return b;
}

function windowFunctionStructure() {
  let b = panel(58, 130, 356, 610, "Function call");
  b += card(104, 186, 238, 66, "Ranking", 0, "rowNumber/rank/denseRank");
  b += card(104, 302, 238, 66, "Navigation", 5, "lead/lag/first/last");
  b += card(104, 418, 238, 66, "Distribution", 6, "percentRank/cumeDist/ntile");
  b += card(104, 534, 238, 66, "Aggregate window", 2, "sum/avg/count/min/max");

  b += panel(490, 130, 396, 610, "OVER definition");
  b += card(548, 190, 248, 70, "over()", 1, "WindowFunctionDefinition");
  b += card(548, 332, 248, 70, "partitionBy", 2, "year, product");
  b += card(548, 474, 248, 70, "orderBy", 6, "amount ASC");
  b += card(548, 616, 248, 70, "frame options", 7, "ROWS/RANGE bounds");

  b += panel(962, 130, 324, 610, "Sales query");
  b += erdTable(1010, 218, 226, "sales", ["year INT", "month INT", "product VARCHAR(30)", "amount DECIMAL(8,2)"], 2, 166);
  b += card(1010, 486, 226, 70, "select(windowFn)", 0, "expected list");

  b += panel(1350, 130, 130, 610, "Database");
  b += cylinder(1372, 308, 86, 208, "DB", ["H2", "Postgres", "MySQL"], 3);

  b += path("M342,219 L548,225", "arrow", "M342,219 L444,219 L444,225 L548,225");
  b += path("M342,335 L548,225", "arrow", "M342,335 L458,335 L458,225 L548,225");
  b += path("M342,451 L548,225", "arrow", "M342,451 L472,451 L472,225 L548,225");
  b += path("M342,567 L548,225", "arrow", "M342,567 L486,567 L486,225 L548,225");
  b += path("M672,260 L672,332", "arrow");
  b += path("M672,402 L672,474", "arrow");
  b += path("M672,544 L672,616", "runtimeUse");
  b += path("M796,367 L1010,301", "arrow", "M796,367 L920,367 L920,301 L1010,301");
  b += path("M796,509 L1010,521", "arrow", "M796,509 L920,509 L920,521 L1010,521");
  b += path("M1236,301 L1372,368", "dbUse", "M1236,301 L1310,301 L1310,368 L1372,368");
  b += path("M1236,521 L1372,456", "dbUse", "M1236,521 L1310,521 L1310,456 L1372,456");
  b += note(132, 796, 1276, "Source check: Ex05_WindowFunction composes function.over().partitionBy(sales.year, sales.product).orderBy(sales.amount) and asserts expected ranked values.");
  return b;
}

function nestedTransactionFlow() {
  let b = panel(58, 130, 356, 610, "Configuration");
  b += card(98, 192, 238, 78, "DatabaseConfig", 1, "useNestedTransactions=true");
  b += card(98, 352, 238, 78, "TransactionManager", 7, "current transaction");
  b += card(98, 512, 238, 78, "DMLTestData.Cities", 2, "city rows");

  b += panel(492, 130, 520, 610, "Nested transaction path");
  b += card(550, 184, 214, 70, "Outer transaction", 0, "insert city1");
  b += card(550, 344, 214, 70, "Nested tx 1", 4, "insert city2");
  b += card(550, 504, 214, 70, "Nested tx 2", 5, "insert city3");
  b += card(816, 344, 142, 70, "rollback()", 6, "savepoint1");
  b += card(816, 504, 142, 70, "commit", 2, "savepoint2");

  b += panel(1088, 130, 330, 610, "Observed result");
  b += cylinder(1170, 206, 140, 176, "H2 DB", ["Cities", "city1 only", "after rollback"], 3);
  b += card(1136, 500, 206, 70, "Assertion", 5, "names == [city1]");

  b += path("M336,231 L550,219", "arrow", "M336,231 L446,231 L446,219 L550,219");
  b += path("M336,551 L550,219", "runtimeUse", "M336,551 L430,551 L430,219 L550,219");
  b += path("M657,254 L657,344", "arrow");
  b += path("M657,414 L657,504", "arrow");
  b += path("M764,379 L816,379", "savepoint");
  b += path("M764,539 L816,539", "arrow");
  b += path("M958,379 L1170,294", "savepoint", "M958,379 L1060,379 L1060,294 L1170,294");
  b += path("M958,539 L1136,535", "arrow", "M958,539 L1044,539 L1044,535 L1136,535");
  b += path("M1310,294 L1378,294 L1378,535 L1342,535", "returnLine");
  b += note(126, 796, 1244, "Source check: Ex05_NestedTransactions inserts city1, city2, city3; rolling back nested tx 1 removes nested work and leaves the outer city1 row.");
  return b;
}

function coroutineTransactionSequence() {
  const xs = [108, 324, 548, 786, 1028, 1266];
  const names = ["Test", "withTablesSuspending", "TransactionManager", "Savepoint helper", "H2 Cities", "Assertions"];
  let b = "";
  names.forEach((name, i) => {
    b += card(xs[i] - 82, 128, 164, 54, name, i);
    b += `<path d="M${xs[i]},182 L${xs[i]},730" class="lifeline"/>\n`;
  });
  b += sequenceMsg(xs[0], xs[1], 230, "1. runSuspendIO");
  b += sequenceMsg(xs[1], xs[2], 292, "2. currentOrNull()");
  b += sequenceMsg(xs[2], xs[3], 354, "3. current tx exists");
  b += sequenceMsg(xs[3], xs[4], 416, "4. setSavepoint");
  b += sequenceMsg(xs[3], xs[4], 478, "5. insert city2/city3");
  b += sequenceReturn(xs[4], xs[3], 540, "6. rollback savepoint1");
  b += sequenceMsg(xs[0], xs[4], 602, "7. select remaining rows");
  b += sequenceMsg(xs[0], xs[5], 646, "8. assert [city1]");
  b += sequenceReturn(xs[2], xs[0], 704, "fallback: newSuspendedTransaction");
  b += note(116, 774, 1268, "Sequence check: return paths are dashed; helper chooses runWithSavepoint when a TransactionManager context exists, otherwise starts newSuspendedTransaction.");
  return b;
}

function shell(diagram) {
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${diagram.width} ${diagram.height}" width="${diagram.width}" height="${diagram.height}" role="img" aria-label="${esc(diagram.title)}">
<defs>
<filter id="shadow" x="-10%" y="-10%" width="120%" height="130%"><feDropShadow dx="2" dy="4" stdDeviation="5" flood-color="#1f2937" flood-opacity="0.12"/></filter>
<marker id="arrow" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto"><path d="M1,1 L8,4.5 L1,8 Z" fill="#64748b"/></marker>
<marker id="arrowBlue" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto"><path d="M1,1 L8,4.5 L1,8 Z" fill="#2563eb"/></marker>
<marker id="arrowPurple" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto"><path d="M1,1 L8,4.5 L1,8 Z" fill="#7c3aed"/></marker>
<marker id="arrowOrange" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto"><path d="M1,1 L8,4.5 L1,8 Z" fill="#ea580c"/></marker>
<marker id="arrowPink" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto"><path d="M1,1 L8,4.5 L1,8 Z" fill="#db2777"/></marker>
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
  const titleLines = wrap(title, Math.max(9, Math.floor(w / 10)), 2);
  const shownDetail = titleLines.length > 1 && h < 68 ? "" : detail;
  const titleY = y + (shownDetail ? 25 : h / 2 - (titleLines.length - 1) * 10 + 6);
  let out = `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="10" fill="${fill}" stroke="${stroke}" class="card"/>\n`;
  titleLines.forEach((line, i) => (out += `<text x="${x + w / 2}" y="${titleY + i * 18}" class="label" text-anchor="middle">${esc(line)}</text>\n`));
  if (shownDetail) out += `<text x="${x + w / 2}" y="${y + h - 14}" class="detail" text-anchor="middle">${esc(shownDetail)}</text>\n`;
  return out;
}

function erdTable(x, y, w, title, rows, c, h = 170) {
  const [fill, stroke] = colors[c % colors.length];
  let out = `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="12" fill="#fff" stroke="${stroke}" class="card"/>
<path d="M${x},${y + 42} L${x},${y + 12} Q${x},${y} ${x + 12},${y} L${x + w - 12},${y} Q${x + w},${y} ${x + w},${y + 12} L${x + w},${y + 42} Z" fill="${fill}"/>
<text x="${x + w / 2}" y="${y + 28}" class="label" text-anchor="middle">${esc(title)}</text>\n`;
  rows.forEach((row, i) => (out += `<text x="${x + 18}" y="${y + 70 + i * 17}" class="tiny">${esc(row)}</text>\n`));
  return out;
}

function cylinder(x, y, w, h, title, rows, c) {
  const [fill, stroke] = colors[c % colors.length];
  const capH = 32;
  let out = `<path d="M${x},${y + capH / 2} C${x},${y - 4} ${x + w},${y - 4} ${x + w},${y + capH / 2} L${x + w},${y + h - capH / 2} C${x + w},${y + h + 4} ${x},${y + h + 4} ${x},${y + h - capH / 2} Z" fill="#fff" stroke="${stroke}" stroke-width="1.7" class="card"/>
<ellipse cx="${x + w / 2}" cy="${y + capH / 2}" rx="${w / 2}" ry="${capH / 2}" fill="${fill}" stroke="${stroke}" stroke-width="1.7"/>
<path d="M${x},${y + h - capH / 2} C${x},${y + h + 4} ${x + w},${y + h + 4} ${x + w},${y + h - capH / 2}" fill="none" stroke="${stroke}" stroke-width="1.7"/>
<text x="${x + w / 2}" y="${y + 44}" class="label" text-anchor="middle">${esc(title)}</text>\n`;
  rows.forEach((row, i) => (out += `<text x="${x + 16}" y="${y + 70 + i * 17}" class="tiny">${esc(row)}</text>\n`));
  return out;
}

function sequenceMsg(x1, x2, y, text) {
  const mid = (x1 + x2) / 2;
  return `<path d="M${x1},${y} L${x2},${y}" class="arrow"/>
<rect x="${mid - 92}" y="${y - 28}" width="184" height="22" rx="7" fill="#fff" stroke="#bfdbfe"/>
<text x="${mid}" y="${y - 13}" class="tiny" text-anchor="middle">${esc(text)}</text>\n`;
}

function sequenceReturn(x1, x2, y, text) {
  const mid = (x1 + x2) / 2;
  return `<path d="M${x1},${y} L${x2},${y}" class="returnLine"/>
<rect x="${mid - 104}" y="${y - 28}" width="208" height="22" rx="7" fill="#fff" stroke="#cbd5e1"/>
<text x="${mid}" y="${y - 13}" class="tiny" text-anchor="middle">${esc(text)}</text>\n`;
}

function path(defaultD, cls, explicitD = null) {
  return `<path d="${explicitD || defaultD}" class="${cls}"/>\n`;
}

function note(x, y, w, text) {
  return `<rect x="${x}" y="${y}" width="${w}" height="52" rx="10" fill="#ecfdf5" stroke="#86efac" stroke-width="1.3"/>
<text x="${x + w / 2}" y="${y + 31}" class="detail" text-anchor="middle">${esc(text)}</text>\n`;
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

function validateBatch08Semantics() {
  const functions = fs.readFileSync(diagrams[0].file, "utf8");
  const windows = fs.readFileSync(diagrams[1].file, "utf8");
  const nested = fs.readFileSync(diagrams[2].file, "utf8");
  const coroutine = fs.readFileSync(diagrams[3].file, "utf8");
  const failures = [];
  for (const required of ["String / Conditional", "Math / Trig", "Aggregate / Statistics", "Window", "SqlFunction&lt;T&gt;", "Table.Dual", "DMLTestData.Sales"]) {
    if (!functions.includes(required)) failures.push(`functions diagram missing ${required}`);
  }
  for (const required of ["Ranking", "Navigation", "Distribution", "over()", "partitionBy", "orderBy", "sales"]) {
    if (!windows.includes(required)) failures.push(`window diagram missing ${required}`);
  }
  for (const required of ["useNestedTransactions=true", "Outer transaction", "Nested tx 1", "rollback()", "city1 only"]) {
    if (!nested.includes(required)) failures.push(`nested transaction diagram missing ${required}`);
  }
  if (!coroutine.includes("newSuspendedTransaction") || !coroutine.includes("setSavepoint") || !coroutine.includes('class="returnLine"')) {
    failures.push("coroutine transaction sequence must show newSuspendedTransaction, savepoint, and dashed returns");
  }
  if (failures.length) throw new Error(`batch08_semantic_validation_failed\n${failures.join("\n")}`);
  console.log("batch08_semantics=ok");
}

function validateConnectorGeometry() {
  const connectorClasses = "(?:arrow|runtimeUse|dbUse|savepoint|returnLine)";
  const failures = [];
  for (const diagram of diagrams) {
    const svg = fs.readFileSync(diagram.file, "utf8");
    const cards = [...svg.matchAll(/<rect\b([^>]*class="card"[^>]*)>/g)]
      .map((m) => attrNumbers(m[1]))
      .filter((r) => r.x !== undefined);
    for (const match of svg.matchAll(new RegExp(`<path\\b([^>]*class="${connectorClasses}"[^>]*)`, "g"))) {
      const pathD = (match[1].match(/d="([^"]+)"/) || [])[1];
      if (!pathD) continue;
      const points = [...pathD.matchAll(/[ML]([\d.]+),([\d.]+)/g)].map((m) => ({ x: Number(m[1]), y: Number(m[2]) }));
      if (points.length < 2) continue;
      for (let i = 0; i < points.length - 1; i++) {
        const a = points[i];
        const b = points[i + 1];
        if (a.x !== b.x && a.y !== b.y) failures.push(`${diagram.file}: diagonal ${a.x},${a.y}->${b.x},${b.y}`);
        for (const rect of cards) {
          const endpoint = (i === 0 && pointOnBoundary(rect, a)) || (i === points.length - 2 && pointOnBoundary(rect, b));
          const crossing = segmentCrossesRect(rect, a, b);
          if (crossing && !endpoint) failures.push(`${diagram.file}: ${crossing} ${a.x},${a.y}->${b.x},${b.y}`);
        }
      }
      const first = points[0];
      const last = points[points.length - 1];
      if (cards.some((rect) => pointInside(rect, first))) failures.push(`${diagram.file}: connector starts inside card at ${first.x},${first.y}`);
      if (cards.some((rect) => pointInside(rect, last))) failures.push(`${diagram.file}: connector ends inside card at ${last.x},${last.y}`);
    }
  }
  if (failures.length) throw new Error(`batch08_connector_validation_failed\n${failures.join("\n")}`);
  console.log("batch08_connectors=ok");
}

function attrNumbers(value) {
  const out = {};
  for (const match of value.matchAll(/\b(x|y|width|height)="([\d.]+)"/g)) out[match[1]] = Number(match[2]);
  return out;
}

function pointInside(rect, point) {
  return point.x > rect.x + 1 && point.x < rect.x + rect.width - 1 && point.y > rect.y + 1 && point.y < rect.y + rect.height - 1;
}

function pointOnBoundary(rect, point) {
  const onX = point.x >= rect.x - 1 && point.x <= rect.x + rect.width + 1;
  const onY = point.y >= rect.y - 1 && point.y <= rect.y + rect.height + 1;
  const verticalEdge = (Math.abs(point.x - rect.x) <= 1 || Math.abs(point.x - (rect.x + rect.width)) <= 1) && onY;
  const horizontalEdge = (Math.abs(point.y - rect.y) <= 1 || Math.abs(point.y - (rect.y + rect.height)) <= 1) && onX;
  return verticalEdge || horizontalEdge;
}

function segmentCrossesRect(rect, a, b) {
  if (a.x !== b.x && a.y !== b.y) return "diagonal";
  if (a.x === b.x) {
    const y1 = Math.min(a.y, b.y);
    const y2 = Math.max(a.y, b.y);
    if (a.x > rect.x + 1 && a.x < rect.x + rect.width - 1 && Math.max(y1, rect.y + 1) < Math.min(y2, rect.y + rect.height - 1)) return "cross";
  }
  if (a.y === b.y) {
    const x1 = Math.min(a.x, b.x);
    const x2 = Math.max(a.x, b.x);
    if (a.y > rect.y + 1 && a.y < rect.y + rect.height - 1 && Math.max(x1, rect.x + 1) < Math.min(x2, rect.x + rect.width - 1)) return "cross";
  }
  return null;
}
