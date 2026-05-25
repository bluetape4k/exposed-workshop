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
.mapLine{fill:none;stroke:#16a34a;stroke-width:1.9;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowGreen)}
.codecLine{fill:none;stroke:#db2777;stroke-width:1.9;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowPink)}
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
    file: `${outDir}/08-coroutines-architecture-01.svg`,
    title: "Coroutines vs Virtual Threads Processing Flow",
    subtitle: "Coroutine and Virtual Thread examples run equivalent Exposed JDBC transactions with different scheduling and cancellation models",
    width: 1500,
    height: 780,
    body: processingFlow,
  },
  {
    file: `${outDir}/08-coroutines-architecture-02.svg`,
    title: "Thread Model Structure Comparison",
    subtitle: "Coroutines multiplex suspend work on Dispatchers.IO while Virtual Threads keep blocking-style code on JVM-managed lightweight threads",
    width: 1500,
    height: 820,
    body: threadModelComparison,
  },
  {
    file: `${outDir}/08-coroutines-architecture-03.svg`,
    title: "Coroutine Transaction Flow",
    subtitle: "Ex01_Coroutines uses withTablesSuspending, newSuspendedTransaction, suspendedTransactionAsync, and withSuspendTransaction against Tester tables",
    width: 1500,
    height: 900,
    body: coroutineTransactionFlow,
  },
  {
    file: `${outDir}/08-coroutines-architecture-04.svg`,
    title: "Virtual Thread Transaction Flow",
    subtitle: "Ex01_VirtualThreads runs Java 21-only JDBC transactions through newVirtualThreadJdbcTransaction and virtualThreadJdbcTransactionAsync",
    width: 1500,
    height: 900,
    body: virtualThreadTransactionFlow,
  },
  {
    file: `${outDir}/08-coroutines-01-coroutines-basic-sequence-01.svg`,
    title: "Coroutine Transaction Sequence",
    subtitle: "Coroutine tests create tables, enter suspended transactions, use nested withSuspendTransaction lookup, and verify DB state",
    width: 1420,
    height: 860,
    body: coroutineSequence,
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
validateBatch15Semantics();
validateConnectorGeometry();

function writeGraphvizSketches() {
  fs.mkdirSync(sketchDir, { recursive: true });
  const sketches = {
    "batch-15-processing-flow": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=0.7, ranksep=0.9, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      coroutine_tests -> new_suspended_transaction -> dispatcher_io -> jdbc_transaction -> tester_tables -> db -> assertions;
      coroutine_tests -> suspended_transaction_async -> dispatcher_io;
      virtual_thread_tests -> new_virtual_thread_jdbc_transaction -> virtual_thread_scheduler -> jdbc_transaction;
      virtual_thread_tests -> virtual_thread_jdbc_transaction_async -> virtual_thread_scheduler;
      { rank=same; coroutine_tests; virtual_thread_tests; }
      { rank=same; new_suspended_transaction; suspended_transaction_async; new_virtual_thread_jdbc_transaction; virtual_thread_jdbc_transaction_async; }
    }`,
    "batch-15-thread-model": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=0.65, ranksep=0.9, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      coroutine_jobs -> dispatcher_io_pool -> jdbc_connection_pool -> db;
      cancellation_job -> coroutine_jobs;
      virtual_tasks -> virtual_threads -> carrier_threads -> jdbc_connection_pool;
      interrupt_cancel -> virtual_threads;
      { rank=same; coroutine_jobs; virtual_tasks; }
      { rank=same; dispatcher_io_pool; virtual_threads; }
      { rank=same; jdbc_connection_pool; carrier_threads; }
    }`,
    "batch-15-coroutine-flow": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=0.7, ranksep=0.9, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      run_suspend_io -> with_tables_suspending -> new_suspended_transaction -> tester_table -> db -> assertions;
      with_tables_suspending -> suspended_transaction_async -> await_all -> assertions;
      new_suspended_transaction -> with_suspend_transaction -> tester_table;
      duplicate_id -> exposed_sql_exception -> cleanup_assertion;
    }`,
    "batch-15-virtual-flow": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=0.7, ranksep=0.9, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      java21_gate -> with_tables -> new_virtual_thread_jdbc_transaction -> vtester_table -> db -> assertions;
      with_tables -> virtual_thread_jdbc_transaction_async -> await_all -> assertions;
      regular_transaction -> vtester_table;
      duplicate_id -> execution_exception -> cleanup_assertion;
    }`,
    "batch-15-coroutine-sequence": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=0.6, ranksep=0.8, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      test -> fixture -> coroutine_api -> transaction -> tester_table -> assertions;
      transaction -> db;
      db -> transaction [style=dashed];
      transaction -> assertions [style=dashed];
    }`,
  };
  for (const [name, dot] of Object.entries(sketches)) {
    const dotPath = `${sketchDir}/${name}.dot`;
    fs.writeFileSync(dotPath, dot);
    spawnSync(DOT, ["-Tplain", "-o", `${sketchDir}/${name}.plain`, dotPath], { encoding: "utf8" });
    spawnSync(DOT, ["-Tsvg", "-o", `${sketchDir}/${name}.svg`, dotPath], { encoding: "utf8" });
  }
}

function processingFlow() {
  let b = "";
  b += panel(58, 130, 584, 480, "Coroutine path");
  b += card(96, 176, 218, 64, "runSuspendIO tests", 0, "Ex01_Coroutines");
  b += card(96, 302, 218, 64, "Coroutine APIs", 1, "newSuspended + async");
  b += card(96, 428, 218, 64, "Dispatcher.IO", 7, "I/O scheduling");
  b += card(380, 238, 214, 64, "JdbcTransaction", 5, "suspend boundary");
  b += card(380, 428, 214, 64, "Cancellation", 4, "Job + cleanup");

  b += panel(858, 130, 584, 480, "Virtual thread path");
  b += card(896, 176, 218, 64, "Java 21 tests", 0, "@EnabledForJreRange");
  b += card(896, 302, 218, 64, "Virtual APIs", 1, "VT transaction + async");
  b += card(896, 428, 218, 64, "JVM scheduler", 7, "mount/unmount");
  b += card(1180, 238, 214, 64, "JdbcTransaction", 5, "blocking style");
  b += card(1180, 428, 214, 64, "Interrupt cancel", 4, "Future/Thread");

  b += panel(660, 230, 180, 350, "Shared target");
  b += card(690, 270, 120, 58, "Tables", 2, "Tester/VTester");
  b += cylinder(690, 370, 120, 150, "DB", ["H2", "Postgres", "MySQL"], 3);

  b += path("M314,208 L380,250", "arrow", "M314,208 L356,208 L356,250 L380,250");
  b += path("M314,334 L380,270", "arrow", "M314,334 L340,334 L340,270 L380,270");
  b += path("M314,460 L380,290", "runtimeUse", "M314,460 L324,460 L324,290 L380,290");
  b += path("M594,270 L690,299", "mapLine", "M594,270 L642,270 L642,299 L690,299");
  b += path("M487,428 L487,302", "runtimeUse");
  b += path("M750,328 L750,370", "dbUse");
  b += path("M1114,208 L1180,250", "arrow", "M1114,208 L1156,208 L1156,250 L1180,250");
  b += path("M1114,334 L1180,270", "arrow", "M1114,334 L1140,334 L1140,270 L1180,270");
  b += path("M1114,460 L1180,290", "runtimeUse", "M1114,460 L1124,460 L1124,290 L1180,290");
  b += path("M1180,292 L810,299", "mapLine", "M1180,292 L1130,292 L1130,299 L810,299");
  b += path("M1287,428 L1287,302", "runtimeUse");
  b += note(150, 700, 1200, "Source check: both modules verify insert/select, parallel async work, duplicate-key exceptions, connection cleanup, and equivalent row counts.");
  return b;
}

function threadModelComparison() {
  let b = "";
  b += panel(58, 130, 1384, 250, "Coroutine structure");
  b += card(96, 190, 210, 64, "Suspend callers", 0, "runSuspendIO");
  b += card(384, 190, 230, 64, "Coroutine jobs", 1, "Deferred + awaitAll");
  b += card(692, 190, 230, 64, "Dispatchers.IO", 7, "worker pool");
  b += card(1000, 190, 230, 64, "Connection pool", 5, "Exposed JDBC");
  b += cylinder(1300, 160, 104, 150, "DB", ["Tester", "unique"], 3);

  b += panel(58, 440, 1384, 250, "Virtual thread structure");
  b += card(96, 500, 210, 64, "Blocking callers", 0, "plain test code");
  b += card(384, 500, 230, 64, "VirtualFuture", 1, "awaitAll");
  b += card(692, 500, 230, 64, "Virtual Threads", 6, "JVM lightweight");
  b += card(1000, 500, 230, 64, "Carrier + JDBC", 7, "blocking I/O");
  b += cylinder(1300, 470, 104, 150, "DB", ["VTester", "unique"], 3);

  b += path("M306,222 L384,222", "arrow");
  b += path("M614,222 L692,222", "arrow");
  b += path("M922,222 L1000,222", "mapLine");
  b += path("M1230,222 L1300,235", "dbUse", "M1230,222 L1262,222 L1262,235 L1300,235");
  b += path("M306,532 L384,532", "arrow");
  b += path("M614,532 L692,532", "arrow");
  b += path("M922,532 L1000,532", "mapLine");
  b += path("M1230,532 L1300,545", "dbUse", "M1230,532 L1262,532 L1262,545 L1300,545");
  b += note(150, 738, 1200, "Model rule: coroutines require suspend-aware transaction boundaries; virtual threads preserve blocking-style transaction code but still share the same JDBC connection bottleneck.");
  return b;
}

function coroutineTransactionFlow() {
  let b = "";
  b += panel(58, 130, 320, 590, "Test harness");
  b += card(98, 178, 220, 62, "runSuspendIO", 0, "coroutine test scope");
  b += card(98, 298, 220, 62, "withTablesSuspending", 2, "create/drop tables");
  b += card(98, 418, 220, 62, "single dispatcher", 7, "ordered work");
  b += card(98, 538, 220, 62, "assertions", 4, "count + exceptions");

  b += panel(452, 130, 350, 590, "Coroutine transaction APIs");
  b += card(506, 176, 236, 64, "newSuspendedTransaction", 1, "Dispatchers.IO / db");
  b += card(506, 302, 236, 64, "withSuspendTransaction", 5, "nested lookup");
  b += card(506, 428, 236, 64, "suspended Tx async", 6, "10 parallel tasks");
  b += card(506, 554, 236, 64, "awaitAll", 7, "join results");

  b += panel(878, 130, 310, 590, "Exposed schema");
  b += card(922, 198, 222, 70, "Tester", 2, "IntIdTable coroutines_tester");
  b += card(922, 384, 222, 70, "TesterUnique", 4, "Table + unique PK");
  b += card(922, 570, 222, 70, "TesterEntity", 1, "IntEntity duplicate test");

  b += panel(1260, 130, 180, 590, "Database");
  b += cylinder(1292, 292, 112, 210, "DB", ["H2", "Postgres", "MySQL"], 3);

  b += path("M318,209 L506,208", "arrow", "M318,209 L410,209 L410,208 L506,208");
  b += path("M318,329 L506,208", "arrow", "M318,329 L410,329 L410,208 L506,208");
  b += path("M318,449 L506,334", "runtimeUse", "M318,449 L410,449 L410,334 L506,334");
  b += path("M742,208 L922,218", "mapLine", "M742,208 L836,208 L836,218 L922,218");
  b += path("M742,334 L922,238", "mapLine", "M742,334 L852,334 L852,238 L922,238");
  b += path("M742,460 L922,258", "arrow", "M742,460 L812,460 L812,258 L922,258");
  b += path("M742,586 L922,605", "runtimeUse", "M742,586 L836,586 L836,605 L922,605");
  b += path("M1144,233 L1292,360", "dbUse", "M1144,233 L1220,233 L1220,360 L1292,360");
  b += path("M1144,419 L1292,420", "dbUse", "M1144,419 L1220,419 L1220,420 L1292,420");
  b += path("M1144,605 L1292,480", "dbUse", "M1144,605 L1220,605 L1220,480 L1292,480");
  b += note(132, 780, 1236, "Source check: Ex01_Coroutines inserts/selects Tester rows, uses TesterUnique for duplicate-key failures, and confirms nested connection cleanup after ExposedSQLException.");
  return b;
}

function virtualThreadTransactionFlow() {
  let b = "";
  b += panel(58, 130, 320, 590, "Test harness");
  b += card(98, 178, 220, 62, "Java 21 gate", 0, "@EnabledForJreRange");
  b += card(98, 298, 220, 62, "withTables", 2, "create/drop tables");
  b += card(98, 418, 220, 62, "regular transaction", 7, "mixed comparison");
  b += card(98, 538, 220, 62, "assertions", 4, "ExecutionException");

  b += panel(452, 130, 350, 590, "Virtual Thread APIs");
  b += card(506, 176, 236, 64, "newVirtualThread Tx", 1, "JDBC transaction");
  b += card(506, 302, 236, 64, "VT transaction async", 6, "10 parallel tasks");
  b += card(506, 428, 236, 64, "VirtualFuture.awaitAll", 7, "join results");
  b += card(506, 554, 236, 64, "commit / cleanup", 5, "close inner connection");

  b += panel(878, 130, 310, 590, "Exposed schema");
  b += card(922, 198, 222, 70, "VTester", 2, "IntIdTable virtualthreads_table");
  b += card(922, 384, 222, 70, "VTesterUnique", 4, "Table + unique PK");
  b += card(922, 570, 222, 70, "TesterEntity", 1, "IntEntity duplicate test");

  b += panel(1260, 130, 180, 590, "Database");
  b += cylinder(1292, 292, 112, 210, "DB", ["H2", "Postgres", "MySQL"], 3);

  b += path("M318,209 L506,208", "arrow", "M318,209 L410,209 L410,208 L506,208");
  b += path("M318,329 L506,208", "arrow", "M318,329 L410,329 L410,208 L506,208");
  b += path("M318,449 L506,586", "runtimeUse", "M318,449 L410,449 L410,586 L506,586");
  b += path("M742,208 L922,218", "mapLine", "M742,208 L836,208 L836,218 L922,218");
  b += path("M742,334 L922,238", "arrow", "M742,334 L818,334 L818,238 L922,238");
  b += path("M742,460 L922,258", "arrow", "M742,460 L802,460 L802,258 L922,258");
  b += path("M742,586 L922,605", "runtimeUse", "M742,586 L836,586 L836,605 L922,605");
  b += path("M1144,233 L1292,360", "dbUse", "M1144,233 L1220,233 L1220,360 L1292,360");
  b += path("M1144,419 L1292,420", "dbUse", "M1144,419 L1220,419 L1220,420 L1292,420");
  b += path("M1144,605 L1292,480", "dbUse", "M1144,605 L1220,605 L1220,480 L1292,480");
  b += note(132, 780, 1236, "Source check: Ex01_VirtualThreads verifies Java 21 virtual-thread transactions, async fan-out, regular transaction mixing, and inner connection cleanup after duplicate ID failure.");
  return b;
}

function coroutineSequence() {
  let b = "";
  const participants = [
    [110, "Test"],
    [330, "Fixture"],
    [570, "Coroutine API"],
    [820, "JdbcTransaction"],
    [1070, "Tester table"],
    [1290, "Assertions"],
  ];
  for (const [x, label] of participants) {
    b += card(x - 72, 130, 144, 54, label, 0, "");
    b += `<line x1="${x}" y1="184" x2="${x}" y2="720" class="lifeline"/>\n`;
  }
  b += seqBand(86, 222, 246, "1", "withTablesSuspending creates Tester table", 2);
  b += seqArrow(110, 330, 276, "arrow");
  b += seqBand(306, 300, 288, "2", "newSuspendedTransaction opens suspend boundary", 1);
  b += seqArrow(330, 570, 354, "arrow");
  b += seqBand(546, 378, 300, "3", "insert/select runs inside JdbcTransaction", 5);
  b += seqArrow(570, 820, 432, "arrow");
  b += seqBand(796, 456, 298, "4", "Tester.insert and selectAll touch table", 6);
  b += seqArrow(820, 1070, 510, "dbUse");
  b += seqBand(546, 534, 548, "5", "withSuspendTransaction nested lookup returns ResultRow or null", 7);
  b += seqArrow(570, 820, 610, "runtimeUse");
  b += seqArrow(820, 1070, 638, "dbUse");
  b += seqArrow(1070, 820, 666, "returnLine");
  b += seqArrow(820, 1290, 666, "returnLine");
  b += seqBand(1046, 690, 268, "6", "assert count, null, exception cleanup", 4);
  b += note(110, 780, 1200, "Sequence rule: calls are solid, returns are dashed, and cancellation/duplicate-key cleanup is verified through assertions rather than hidden in the DB box.");
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
<marker id="arrowPink" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto"><path d="M1,1 L8,4.5 L1,8 Z" fill="#db2777"/></marker>
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

function validateBatch15Semantics() {
  const svgs = diagrams.map((d) => fs.readFileSync(d.file, "utf8"));
  const failures = [];
  for (const required of ["newSuspended", "Virtual APIs", "Dispatcher.IO", "JVM scheduler", "Tester/VTester"]) {
    if (!svgs[0].includes(required)) failures.push(`processing flow missing ${required}`);
  }
  for (const required of ["Coroutine jobs", "Dispatchers.IO", "Virtual Threads", "Carrier + JDBC", "Connection pool"]) {
    if (!svgs[1].includes(required)) failures.push(`thread model missing ${required}`);
  }
  for (const required of ["withTablesSuspending", "newSuspendedTransaction", "suspendedTransactionAsync", "TesterUnique", "TesterEntity"]) {
    if (!svgs[2].includes(required)) failures.push(`coroutine flow missing ${required}`);
  }
  for (const required of ["Java 21 gate", "newVirtualThreadJdbcTransaction", "virtualThreadJdbcTransactionAsync", "VTesterUnique", "ExecutionException"]) {
    if (!svgs[3].includes(required)) failures.push(`virtual flow missing ${required}`);
  }
  for (const required of ["withTablesSuspending", "newSuspendedTransaction", "withSuspendTransaction", "solid, returns are dashed"]) {
    if (!svgs[4].includes(required)) failures.push(`sequence missing ${required}`);
  }
  if (failures.length) throw new Error(`batch15_semantic_validation_failed\n${failures.join("\n")}`);
  console.log("batch15_semantics=ok");
}

function validateConnectorGeometry() {
  const connectorClasses = "(?:arrow|mapLine|codecLine|dbUse|runtimeUse|returnLine)";
  const failures = [];
  for (const diagram of diagrams) {
    const svg = fs.readFileSync(diagram.file, "utf8");
    const cards = [...svg.matchAll(/<rect x="([\d.]+)" y="([\d.]+)" width="([\d.]+)" height="([\d.]+)" rx="10"[^>]*class="card"/g)].map((m) => ({
      x: Number(m[1]),
      y: Number(m[2]),
      w: Number(m[3]),
      h: Number(m[4]),
    }));
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
        for (const rect of cards) {
          if (segmentCrossesCardInterior(a, b, rect)) failures.push(`${diagram.file}: connector crosses card interior ${cls} ${a.x},${a.y}->${b.x},${b.y}`);
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
  if (failures.length) throw new Error(`batch15_connector_validation_failed\n${failures.join("\n")}`);
  console.log("batch15_connectors=ok");
}

function segmentCrossesCardInterior(a, b, rect) {
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
