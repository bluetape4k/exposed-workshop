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
.fkArrow{fill:none;stroke:#0f766e;stroke-width:2;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowTeal)}
.returnLine{fill:none;stroke:#64748b;stroke-width:1.8;stroke-dasharray:6 5;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrow)}
.lifeline{stroke:#cbd5e1;stroke-width:1.5;stroke-dasharray:5 6}
.inherit{fill:none;stroke:#475569;stroke-width:2.1;stroke-linecap:round;stroke-linejoin:round}
.inheritHead{fill:#fff;stroke:#475569;stroke-width:1.8;stroke-linejoin:round}
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
    file: `${outDir}/05-exposed-dml-architecture-01.svg`,
    title: "Exposed DML Chapter Architecture",
    subtitle: "Tests progress from DSL read/write operations to type mapping, SQL functions, transactions, and DAO entities",
    width: 1500,
    height: 900,
    body: dmlChapterArchitecture,
  },
  {
    file: `${outDir}/05-exposed-dml-01-dml-architecture-01.svg`,
    title: "Exposed DML DSL Execution Flow",
    subtitle: "Source tests build type-safe DSL statements, generate dialect SQL, and verify rows in shared DML tables",
    width: 1580,
    height: 900,
    body: dmlDslArchitecture,
  },
  {
    file: `${outDir}/05-exposed-dml-01-dml-sequence-02.svg`,
    title: "Exposed DML Test Operation Sequence",
    subtitle: "CRUD and advanced query tests share schema setup, sample data, DSL execution, and assertion phases",
    width: 1500,
    height: 880,
    body: dmlSequence,
  },
  {
    file: `${outDir}/05-exposed-dml-01-dml-erd-03.svg`,
    title: "Exposed DML Shared Test Data ERD",
    subtitle: "DML examples reuse Cities, Users, UserData, Sales, and SomeAmounts tables from shared test fixtures",
    width: 1260,
    height: 820,
    body: dmlErd,
  },
  {
    file: `${outDir}/05-exposed-dml-02-types-class-01.svg`,
    title: "Exposed DML Column Type Test Model",
    subtitle: "Column type tests extend the shared test base and exercise Table column factories across supported DB dialects",
    width: 1520,
    height: 900,
    body: typesClassModel,
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

validateBatch07Semantics();
validateConnectorGeometry();

function writeGraphvizSketches() {
  fs.mkdirSync(sketchDir, { recursive: true });
  const sketches = {
    "batch-07-dml-chapter": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=0.8, ranksep=1.0, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      dml -> types -> functions -> transactions -> entities;
      dml -> shared_tables -> dialect_dbs;
      types -> dialect_dbs;
      functions -> dialect_dbs;
      transactions -> dialect_dbs;
      entities -> shared_tables;
    }`,
    "batch-07-dml-dsl": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=0.75, ranksep=1.0, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      basic_dml -> exposed_dsl -> generated_sql -> database;
      advanced_select -> exposed_dsl;
      advanced_dml -> exposed_dsl;
      performance_extension -> exposed_dsl;
      shared_tables -> database;
    }`,
    "batch-07-dml-sequence": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=0.65, ranksep=0.9, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      test -> with_tables -> fixtures -> dsl -> db -> assertions;
      db -> dsl [style=dashed, label="ResultRow/count"];
    }`,
    "batch-07-dml-erd": `digraph G {
      graph [rankdir=TB, splines=ortho, nodesep=0.8, ranksep=1.0, pad=0.2];
      node [shape=record, style="rounded,filled", fillcolor="#ffffff", color="#94a3b8"];
      cities [label="{cities|city_id PK\\lname}"];
      users [label="{users|id PK\\lname\\lcity_id FK NULL\\lflags}"];
      userdata [label="{userdata|user_id FK\\lcomment\\lvalue}"];
      sales [label="{sales|year\\lmonth\\lproduct\\lamount}"];
      someamounts [label="{someamounts|amount}"];
      users -> cities [label="city_id -> city_id"];
      userdata -> users [label="user_id -> id"];
    }`,
    "batch-07-types-class": `digraph G {
      graph [rankdir=TB, splines=ortho, nodesep=0.75, ranksep=0.85, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      abstract_test -> basic_types;
      abstract_test -> array_types;
      abstract_test -> extended_types;
      table -> column_factories;
      basic_types -> column_factories;
      array_types -> column_factories;
      extended_types -> column_factories;
      column_factories -> dialects;
    }`,
  };
  for (const [name, dot] of Object.entries(sketches)) {
    const dotPath = `${sketchDir}/${name}.dot`;
    fs.writeFileSync(dotPath, dot);
    spawnSync(DOT, ["-Tsvg", "-o", `${sketchDir}/${name}.svg`, dotPath], { encoding: "utf8" });
    spawnSync(DOT, ["-Tplain", "-o", `${sketchDir}/${name}.plain`, dotPath], { encoding: "utf8" });
  }
}

function dmlChapterArchitecture() {
  let b = panel(58, 130, 342, 610, "Study path");
  b += card(98, 178, 238, 70, "01 DML", 0, "SELECT/INSERT/UPDATE");
  b += card(98, 290, 238, 70, "02 Types", 5, "Column mapping");
  b += card(98, 402, 238, 70, "03 Functions", 6, "SQL/window funcs");
  b += card(98, 514, 238, 70, "04 Transactions", 1, "isolation/nesting");
  b += card(98, 626, 238, 70, "05 Entities", 4, "DAO relations");

  b += panel(472, 130, 438, 610, "Shared fixtures and APIs");
  b += card(536, 190, 286, 78, "DMLTestData", 2, "Cities, Users, UserData");
  b += card(536, 330, 286, 78, "Exposed DSL", 1, "Query, Statement, ResultRow");
  b += card(536, 470, 286, 78, "Dialect checks", 7, "feature support tables");
  b += card(536, 610, 286, 78, "Assertions", 5, "rows, counts, rollback");

  b += panel(982, 130, 410, 610, "Execution target");
  b += card(1028, 196, 242, 70, "Generated SQL", 0, "DML and DDL fragments");
  b += cylinder(1058, 344, 182, 190, "Dialect DBs", ["H2", "PostgreSQL", "MySQL/MariaDB"], 3);
  b += card(1028, 614, 242, 70, "Behavior matrix", 6, "RETURNING, MERGE, arrays");

  b += path("M336,213 L536,229", "arrow", "M336,213 L436,213 L436,229 L536,229");
  b += path("M336,325 L536,369", "arrow", "M336,325 L436,325 L436,369 L536,369");
  b += path("M336,437 L536,369", "arrow", "M336,437 L436,437 L436,369 L536,369");
  b += path("M336,549 L536,649", "runtimeUse", "M336,549 L448,549 L448,649 L536,649");
  b += path("M336,661 L536,229", "runtimeUse", "M336,661 L420,661 L420,229 L536,229");
  b += path("M822,369 L1028,231", "arrow", "M822,369 L944,369 L944,231 L1028,231");
  b += path("M1270,231 L1310,231 L1310,439 L1240,439", "dbUse");
  b += path("M1240,439 L1310,439 L1310,649 L1270,649", "returnLine");
  b += note(132, 798, 1236, "Source check: chapter README orders modules from DML and types through functions, transactions, and DAO entities; examples verify dialect-specific behavior with tests.");
  return b;
}

function dmlDslArchitecture() {
  let b = panel(58, 130, 388, 610, "Example categories");
  b += card(96, 174, 286, 68, "Basic DML", 0, "Select/Insert/Update/Delete");
  b += card(96, 284, 286, 68, "Advanced SELECT", 5, "Join/GroupBy/OrderBy");
  b += card(96, 394, 286, 68, "Advanced DML", 4, "Upsert/Replace/Merge");
  b += card(96, 504, 286, 68, "Query extensions", 6, "Returning/Union/CTE");
  b += card(96, 614, 286, 68, "Shared tables", 2, "Cities, Users, Sales");

  b += panel(520, 130, 340, 610, "Exposed DSL");
  b += card(578, 196, 224, 72, "Query builder", 1, "select/where/join");
  b += card(578, 338, 224, 72, "Statement builder", 0, "insert/update/delete");
  b += card(578, 480, 224, 72, "Dialect gates", 7, "feature support");
  b += card(578, 622, 224, 72, "Result mapping", 5, "ResultRow/count");

  b += panel(934, 130, 338, 610, "Generated SQL");
  b += card(984, 174, 230, 68, "SELECT", 0, "where/join/group");
  b += card(984, 284, 230, 68, "INSERT / UPDATE", 2, "batch and returning");
  b += card(984, 394, 230, 68, "DELETE / UPSERT", 4, "conflict handling");
  b += card(984, 504, 230, 68, "CTE / UNION", 6, "advanced query SQL");
  b += card(984, 614, 230, 68, "EXPLAIN / paging", 7, "performance checks");

  b += panel(1332, 130, 170, 610, "Database");
  b += cylinder(1360, 282, 112, 250, "DB", ["H2", "Postgres", "MySQL", "MariaDB"], 3);

  b += path("M382,208 L578,232", "arrow", "M382,208 L480,208 L480,232 L578,232");
  b += path("M382,318 L578,232", "arrow", "M382,318 L496,318 L496,232 L578,232");
  b += path("M382,428 L578,374", "arrow", "M382,428 L480,428 L480,374 L578,374");
  b += path("M382,538 L578,516", "runtimeUse", "M382,538 L496,538 L496,516 L578,516");
  b += path("M382,648 L578,658", "runtimeUse", "M382,648 L480,648 L480,658 L578,658");

  b += path("M802,232 L984,208", "arrow", "M802,232 L902,232 L902,208 L984,208");
  b += path("M802,374 L984,318", "arrow", "M802,374 L902,374 L902,318 L984,318");
  b += path("M802,374 L984,428", "arrow", "M802,374 L884,374 L884,428 L984,428");
  b += path("M802,516 L984,538", "runtimeUse", "M802,516 L902,516 L902,538 L984,538");
  b += path("M802,658 L984,648", "returnLine", "M802,658 L902,658 L902,648 L984,648");

  b += path("M1214,208 L1360,344", "dbUse", "M1214,208 L1294,208 L1294,344 L1360,344");
  b += path("M1214,318 L1360,384", "dbUse", "M1214,318 L1294,318 L1294,384 L1360,384");
  b += path("M1214,428 L1360,424", "dbUse", "M1214,428 L1294,428 L1294,424 L1360,424");
  b += path("M1214,538 L1360,464", "dbUse", "M1214,538 L1294,538 L1294,464 L1360,464");
  b += path("M1214,648 L1360,504", "dbUse", "M1214,648 L1294,648 L1294,504 L1360,504");
  b += note(126, 798, 1328, "Source check: 01-dml tests cover Ex01..Ex05 basic DML, Ex06..Ex11 advanced SELECT, Ex12..Ex17 advanced DML, and CTE/EXPLAIN/query adjustment examples.");
  return b;
}

function dmlSequence() {
  const xs = [104, 326, 560, 802, 1040, 1280];
  const names = ["Test method", "withTables", "DMLTestData", "Exposed DSL", "Dialect DB", "Assertions"];
  let b = "";
  names.forEach((name, i) => {
    b += card(xs[i] - 82, 128, 164, 54, name, i);
    b += `<path d="M${xs[i]},182 L${xs[i]},732" class="lifeline"/>\n`;
  });
  b += sequenceMsg(xs[0], xs[1], 230, "1. start dialect test");
  b += sequenceMsg(xs[1], xs[2], 292, "2. create shared tables");
  b += sequenceMsg(xs[2], xs[4], 354, "3. seed Cities/Users/Sales");
  b += sequenceMsg(xs[0], xs[3], 428, "4. build DSL statement");
  b += sequenceMsg(xs[3], xs[4], 490, "5. execute generated SQL");
  b += sequenceReturn(xs[4], xs[3], 552, "6. ResultRow / update count");
  b += sequenceMsg(xs[3], xs[5], 614, "7. verify expected shape");
  b += sequenceReturn(xs[1], xs[0], 688, "8. drop tables / rollback");
  b += note(118, 776, 1264, "Sequence check: returns are dashed; CRUD, join/group, upsert/merge, returning, and CTE tests follow the same setup-execute-assert lifecycle.");
  return b;
}

function dmlErd() {
  let b = panel(64, 132, 744, 560, "City-user fixture");
  b += erdTable(128, 184, 290, "cities", ["PK city_id SERIAL", "name VARCHAR(50) NOT NULL"], 0, 138);
  b += erdTable(128, 402, 290, "users", ["PK id VARCHAR(10)", "name VARCHAR(50) NOT NULL", "FK city_id INT NULL", "flags INT DEFAULT 0"], 5, 176);
  b += erdTable(526, 402, 216, "userdata", ["FK user_id VARCHAR(10)", "comment VARCHAR(30)", "value INT"], 6, 154);
  b += path("M273,402 L273,322", "fkArrow");
  b += labelPill(186, 352, 174, "users.city_id -> cities.city_id", "#ccfbf1");
  b += path("M526,479 L418,479", "fkArrow");
  b += labelPill(430, 448, 84, "user_id -> id", "#ccfbf1");

  b += panel(874, 132, 322, 560, "Aggregation fixtures");
  b += erdTable(920, 220, 230, "sales", ["year INT", "month INT", "product VARCHAR(30) NULL", "amount DECIMAL(8,2)"], 2, 176);
  b += erdTable(920, 486, 230, "someamounts", ["amount DECIMAL(8,2)"], 4, 120);
  b += labelPill(932, 420, 206, "groupBy / sum / avg examples", "#fef3c7");
  b += note(120, 744, 1028, "ERD check: FK arrows point from child tables to parent tables; Sales and SomeAmounts are standalone aggregation fixtures.");
  return b;
}

function typesClassModel() {
  let b = panel(54, 128, 704, 622, "Test class hierarchy");
  b += umlClass(286, 166, 222, 82, "AbstractExposedTest", ["ENABLE_DIALECTS_METHOD", "dialect test harness"], 0, "test base");
  b += umlClass(100, 330, 210, 126, "Basic Type Tests", ["Ex01 Boolean", "Ex02 Char", "Ex03 Numeric", "Ex04 Double"], 2, "classes");
  b += umlClass(432, 330, 210, 126, "Array Type Tests", ["Ex05 Array", "Ex06 MultiArray"], 5, "classes");
  b += umlClass(100, 560, 210, 126, "Extended Type Tests", ["Ex07 Unsigned", "Ex08 Blob", "Ex09 Java UUID", "Ex10 Kotlin UUID"], 4, "classes");
  b += umlClass(432, 560, 210, 126, "Table Models", ["anonymous Table(\"tester\")", "IntIdTable(\"double_table\")", "UUID tables"], 6, "objects");
  b += inheritArrow(205, 330, 397, 248, 292);
  b += inheritArrow(537, 330, 397, 248, 292);
  b += inheritArrow(205, 560, 397, 248, 520);
  b += path("M310,623 L432,623", "runtimeUse");

  b += panel(822, 128, 614, 622, "Column factories and dialect behavior");
  b += umlClass(880, 182, 210, 112, "Table", ["bool(), char(), varchar()", "integer(), decimal()", "array(), blob(), uuid()"], 7, "schema API");
  b += umlClass(1160, 182, 210, 112, "Column<T>", ["typed value binding", "nullable/default/check"], 0, "generic");
  b += card(878, 370, 224, 72, "Basic columns", 2, "bool/char/numeric/double");
  b += card(1150, 370, 224, 72, "Special columns", 4, "array/blob/uuid/unsigned");
  b += cylinder(1068, 548, 140, 142, "DB Dialects", ["H2", "Postgres", "MySQL", "MariaDB"], 3);
  b += path("M1090,238 L1160,238", "arrow");
  b += path("M985,294 L985,370", "arrow");
  b += path("M1265,294 L1265,370", "arrow");
  b += path("M990,442 L1068,606", "dbUse", "M990,442 L1038,442 L1038,606 L1068,606");
  b += path("M1262,442 L1208,606", "dbUse", "M1262,442 L1232,442 L1232,606 L1208,606");
  b += path("M642,623 L878,406", "runtimeUse", "M642,623 L790,623 L790,406 L878,406");
  b += note(128, 804, 1260, "Source check: 02-types examples validate bool/char/numeric/double, arrays, multi-arrays, unsigned values, blobs, Java UUID, and Kotlin UUID across DB dialects.");
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
<marker id="arrowTeal" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto"><path d="M1,1 L8,4.5 L1,8 Z" fill="#0f766e"/></marker>
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

function umlClass(x, y, w, h, title, rows, c, stereotype = "") {
  const [fill, stroke] = colors[c % colors.length];
  const titleLines = wrapIdentifier(title, Math.max(10, Math.floor(w / 10)), 2);
  const headerH = titleLines.length > 1 ? 62 : 48;
  let out = `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="4" fill="#fff" stroke="${stroke}" class="card"/>
<path d="M${x},${y + headerH} L${x},${y + 4} Q${x},${y} ${x + 4},${y} L${x + w - 4},${y} Q${x + w},${y} ${x + w},${y + 4} L${x + w},${y + headerH} Z" fill="${fill}"/>
<text x="${x + w / 2}" y="${y + 18}" class="tiny" text-anchor="middle">&lt;&lt;${esc(stereotype)}&gt;&gt;</text>\n`;
  titleLines.forEach((line, i) => (out += `<text x="${x + w / 2}" y="${y + 38 + i * 17}" class="label" text-anchor="middle">${esc(line)}</text>\n`));
  out += `<path d="M${x},${y + headerH} H${x + w}" stroke="${stroke}" stroke-width="1.1"/>\n`;
  rows.slice(0, Math.max(0, Math.floor((h - headerH - 12) / 15))).forEach((row, i) => (out += `<text x="${x + 14}" y="${y + headerH + 18 + i * 15}" class="tiny">${esc(row)}</text>\n`));
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
  rows.forEach((row, i) => (out += `<text x="${x + 18}" y="${y + 70 + i * 17}" class="tiny">${esc(row)}</text>\n`));
  return out;
}

function inheritArrow(childX, childY, apexX, apexY, laneY) {
  const baseY = apexY + 16;
  return `<path d="M${childX},${childY} L${childX},${laneY} L${apexX},${laneY} L${apexX},${baseY}" class="inherit"/>
<path d="M${apexX},${apexY} L${apexX - 12},${baseY} L${apexX + 12},${baseY} Z" class="inheritHead"/>\n`;
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
<rect x="${mid - 92}" y="${y - 28}" width="184" height="22" rx="7" fill="#fff" stroke="#cbd5e1"/>
<text x="${mid}" y="${y - 13}" class="tiny" text-anchor="middle">${esc(text)}</text>\n`;
}

function path(defaultD, cls, explicitD = null) {
  return `<path d="${explicitD || defaultD}" class="${cls}"/>\n`;
}

function labelPill(x, y, w, text, fill) {
  return `<rect x="${x}" y="${y}" width="${w}" height="24" rx="8" fill="${fill}" stroke="#99f6e4"/>
<text x="${x + w / 2}" y="${y + 16}" class="tiny" text-anchor="middle">${esc(text)}</text>\n`;
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

function wrapIdentifier(value, max, lines) {
  const text = String(value);
  if (text.length <= max) return [text];
  const parts = text.split(/(?=[A-Z][a-z])/);
  const out = [];
  let line = "";
  for (const part of parts) {
    const next = line + part;
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

function esc(value) {
  return String(value).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

function validateBatch07Semantics() {
  const chapter = fs.readFileSync(diagrams[0].file, "utf8");
  const dml = fs.readFileSync(diagrams[1].file, "utf8");
  const sequence = fs.readFileSync(diagrams[2].file, "utf8");
  const erd = fs.readFileSync(diagrams[3].file, "utf8");
  const types = fs.readFileSync(diagrams[4].file, "utf8");
  const failures = [];
  for (const required of ["01 DML", "02 Types", "03 Functions", "04 Transactions", "05 Entities", "Dialect DBs"]) {
    if (!chapter.includes(required)) failures.push(`chapter architecture missing ${required}`);
  }
  const dmlLower = dml.toLowerCase();
  for (const required of ["basic dml", "advanced select", "advanced dml", "generated sql", "database"]) {
    if (!dmlLower.includes(required)) failures.push(`DML architecture missing ${required}`);
  }
  if (!sequence.includes('class="returnLine"')) failures.push("DML sequence must use dashed return arrows");
  if (!erd.includes("users.city_id -&gt; cities.city_id")) failures.push("DML ERD must show users -> cities FK");
  if (!erd.includes("user_id -&gt; id")) failures.push("DML ERD must show userdata -> users FK");
  for (const required of ["AbstractExposedTest", "Basic Type Tests", "Array Type Tests", "Extended Type Tests", "Column&lt;T&gt;", "DB Dialects"]) {
    if (!types.includes(required)) failures.push(`types class model missing ${required}`);
  }
  if (failures.length) throw new Error(`batch07_semantic_validation_failed\n${failures.join("\n")}`);
  console.log("batch07_semantics=ok");
}

function validateConnectorGeometry() {
  const connectorClasses = "(?:arrow|runtimeUse|dbUse|fkArrow|returnLine|inherit)";
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
          const endpoint =
            (i === 0 && pointOnBoundary(rect, a)) ||
            (i === points.length - 2 && pointOnBoundary(rect, b));
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
  if (failures.length) throw new Error(`batch07_connector_validation_failed\n${failures.join("\n")}`);
  console.log("batch07_connectors=ok");
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
