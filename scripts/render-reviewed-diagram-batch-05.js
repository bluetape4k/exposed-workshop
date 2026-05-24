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
.label{font-family:"Architects Daughter","Comic Sans MS",cursive;font-size:17px;fill:#1e293b}
.panelTitle{font-family:"Architects Daughter","Comic Sans MS",cursive;font-size:12px;letter-spacing:.08em;fill:#94a3b8}
.card{stroke-width:1.7;filter:url(#shadow)}.panel{fill:#f8fafc;stroke:#d7e2ec;stroke-width:1.2}
.arrow{fill:none;stroke:#2563eb;stroke-width:2;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowBlue)}
.runtimeUse{fill:none;stroke:#7c3aed;stroke-width:1.8;stroke-dasharray:5 5;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowPurple)}
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
    file: `${outDir}/03-exposed-basic-exposed-sql-example-erd-01.svg`,
    title: "Exposed SQL DSL User Cities ERD",
    subtitle: "users.city_id is a nullable foreign key to cities.id; two sample users intentionally have no city",
    width: 1160,
    height: 740,
    body: sqlErd,
  },
  {
    file: `${outDir}/03-exposed-basic-exposed-sql-example-sequence-02.svg`,
    title: "Exposed SQL DSL Query Flow",
    subtitle: "Sync and coroutine examples share the same schema helper, sample data, and type-safe DSL operations",
    width: 1440,
    height: 820,
    body: sqlSequence,
  },
  {
    file: `${outDir}/03-exposed-basic-exposed-sql-example-class-03.svg`,
    title: "Exposed SQL DSL Domain Model",
    subtitle: "Plain Table objects define schema; test classes execute CRUD, joins, and aggregation through helpers",
    width: 1480,
    height: 860,
    body: sqlClass,
  },
  {
    file: `${outDir}/04-exposed-ddl-architecture-01.svg`,
    title: "Exposed DDL Chapter Architecture",
    subtitle: "Connection examples establish Database/transaction behavior; DDL examples render and execute schema definitions",
    width: 1480,
    height: 880,
    body: ddlArchitecture,
  },
];

writeGraphvizSketches();

function writeGraphvizSketches() {
  fs.mkdirSync(sketchDir, { recursive: true });
  const sketches = {
    "batch-05-sql-erd": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=1.8, ranksep=1.8, pad=0.2];
      node [shape=record, style="rounded,filled", fillcolor="#ffffff", color="#94a3b8"];
      users [label="{users|id VARCHAR(10) PK\\lname VARCHAR(50)\\lcity_id INT NULL FK}"];
      cities [label="{cities|id SERIAL PK\\lname VARCHAR(50)}"];
      users -> cities [label="city_id -> id"];
    }`,
    "batch-05-sql-sequence": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=0.8, ranksep=1.0, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      example -> helper -> tables -> sample_data -> dsl_ops -> assertions;
      suspended_example -> suspended_helper -> tables;
      dsl_ops -> tables;
    }`,
    "batch-05-sql-class": `digraph G {
      graph [rankdir=TB, splines=ortho, nodesep=0.8, ranksep=0.9, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      table -> city_table;
      table -> user_table;
      user_table -> city_table [label="nullable FK"];
      abstract_test -> sql_example;
      abstract_test -> suspended_example;
      schema -> city_table;
      schema -> user_table;
      schema -> helpers;
      helpers -> sql_example;
      helpers -> suspended_example;
    }`,
    "batch-05-ddl-architecture": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=0.85, ranksep=1.0, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      subgraph cluster_connection {
        label="01-connection";
        datasource -> database_connect -> transaction_manager -> metadata_tests;
        hikari -> database_connect;
        multidb -> transaction_manager;
      }
      subgraph cluster_ddl {
        label="02-ddl";
        table_declarations -> schema_utils -> ddl_statements -> dialect_dbs;
        indexes -> schema_utils;
        sequences_enums -> schema_utils;
      }
    }`,
  };

  for (const [name, dot] of Object.entries(sketches)) {
    const dotPath = `${sketchDir}/${name}.dot`;
    fs.writeFileSync(dotPath, dot);
    spawnSync(DOT, ["-Tsvg", "-o", `${sketchDir}/${name}.svg`, dotPath], { encoding: "utf8" });
    spawnSync(DOT, ["-Tplain", "-o", `${sketchDir}/${name}.plain`, dotPath], { encoding: "utf8" });
  }
}

function sqlErd() {
  let b = panel(76, 142, 420, 282, "Parent table");
  b += erdTable(124, 188, 322, "cities", ["id SERIAL PK", "name VARCHAR(50) NOT NULL"], 0, 132);

  b += panel(622, 142, 420, 326, "Child table");
  b += erdTable(670, 188, 322, "users", ["id VARCHAR(10) PK", "name VARCHAR(50) NOT NULL", "city_id INT NULL FK"], 2, 166);

  b += `<path d="M670,276 L446,276" class="fkArrow"/>
<rect x="506" y="258" width="112" height="24" rx="8" fill="#fff" stroke="#99f6e4"/>
<text x="562" y="275" class="tiny" text-anchor="middle">users.city_id</text>
<text x="458" y="268" class="tiny">1</text>
<text x="650" y="268" class="tiny">0..N</text>\n`;

  b += panel(142, 536, 876, 86, "Sample data");
  b += card(188, 552, 192, 58, "3 cities", 0, "Seoul Busan Da");
  b += card(484, 552, 220, 58, "5 users", 2, "debop jane john alex smth");
  b += card(784, 552, 190, 58, "2 null city refs", 6, "alex smth");
  b += note(142, 660, 876, "Source check: UserTable.cityId is optReference(\"city_id\", CityTable.id); insertSampleData inserts alex and smth with null cityId.");
  return b;
}

function sqlSequence() {
  const xs = [110, 340, 590, 850, 1110, 1310];
  const names = ["Test class", "Schema helper", "Transaction", "City/User tables", "DSL operation", "Assertion"];
  let b = "";
  names.forEach((name, i) => {
    b += card(xs[i] - 82, 128, 164, 54, name, i);
    b += `<path d="M${xs[i]},182 L${xs[i]},704" class="lifeline"/>\n`;
  });
  b += sequenceMsg(xs[0], xs[1], 230, "sync withCityUsers()");
  b += sequenceMsg(xs[0], xs[1], 286, "suspend withCityUsers()");
  b += sequenceMsg(xs[1], xs[2], 346, "withTables / suspending");
  b += sequenceMsg(xs[2], xs[3], 402, "create tables and seed rows");
  b += sequenceReturn(xs[3], xs[1], 458, "commit seeded rows");
  b += sequenceMsg(xs[1], xs[4], 520, "run update/delete/join/groupBy body");
  b += sequenceMsg(xs[4], xs[3], 576, "select/update/delete via DSL");
  b += sequenceReturn(xs[3], xs[4], 632, "ResultRow / affected count");
  b += sequenceMsg(xs[4], xs[5], 688, "assert result shape");
  b += note(100, 742, 1228, "Sequence check: return messages use dashed arrows; both sync and coroutine examples execute the same DSL scenarios against the same CityTable/UserTable schema.");
  return b;
}

function sqlClass() {
  let b = panel(48, 126, 650, 554, "Schema object and tables");
  b += umlClass(260, 166, 190, 82, "Table", ["Exposed base", "PrimaryKey support"], 7, "base");
  b += umlClass(100, 356, 230, 132, "CityTable", ["Table(\"cities\")", "id autoIncrement", "name varchar(50)", "PK_Cities_ID"], 0, "SQL DSL table");
  b += umlClass(404, 356, 230, 148, "UserTable", ["Table(\"users\")", "id varchar(10)", "name varchar(50)", "cityId optReference"], 2, "SQL DSL table");
  b += umlClass(238, 562, 222, 96, "Schema", ["withCityUsers()", "withSuspendedCityUsers()", "insertSampleData()"], 6, "object");
  b += inheritArrow(215, 356, 355, 248, 308);
  b += inheritArrow(519, 356, 355, 248, 308);
  b += `<path d="M404,430 L330,430" class="fkArrow"/>
<rect x="342" y="412" width="48" height="24" rx="8" fill="#fff" stroke="#99f6e4"/>
<text x="366" y="429" class="tiny" text-anchor="middle">FK</text>
<path d="M300,562 L300,526 L215,526 L215,488" class="runtimeUse"/>
<path d="M400,562 L400,526 L519,526 L519,504" class="runtimeUse"/>\n`;

  b += panel(782, 126, 650, 554, "Test classes");
  b += umlClass(988, 166, 250, 82, "AbstractExposedTest", ["ENABLE_DIALECTS_METHOD", "dialect test harness"], 0, "test base");
  b += umlClass(842, 356, 246, 150, "ExposedSQLExample", ["update/delete", "manual inner join", "leftJoin FK", "groupBy count"], 3, "sync test");
  b += umlClass(1136, 356, 246, 150, "ExposedSQLSuspendedExample", ["same DSL scenarios", "runSuspendIO", "withTablesSuspending"], 4, "coroutine test");
  b += inheritArrow(965, 356, 1113, 248, 312);
  b += inheritArrow(1259, 356, 1113, 248, 312);
  b += `<path d="M842,430 L736,430 L736,610 L460,610" class="runtimeUse"/>
<path d="M1136,430 L1112,430 L1112,704 L348,704 L348,658" class="runtimeUse"/>\n`;
  b += note(142, 738, 1190, "UML check: Table and AbstractExposedTest supertypes stay above concrete children; FK uses a separate child-to-parent arrow.");
  return b;
}

function ddlArchitecture() {
  let b = panel(48, 126, 656, 620, "01-connection");
  b += card(92, 178, 228, 70, "DataSource / JDBC URL", 0, "H2, HikariCP, stubs");
  b += card(386, 178, 228, 70, "Database.connect", 1, "register database");
  b += card(386, 318, 228, 70, "transaction(db)", 6, "isolation and retry");
  b += card(92, 318, 228, 70, "Connection metadata", 5, "columns / constraints");
  b += card(92, 508, 228, 70, "Multi Database", 4, "db1 / db2 switching");
  b += card(386, 508, 228, 70, "TransactionManager", 7, "defaultDatabase");
  b += `<path d="M320,213 L386,213" class="arrow"/>
<path d="M500,248 L500,318" class="arrow"/>
<path d="M386,353 L320,353" class="arrow"/>
<path d="M320,543 L386,543" class="arrow"/>
<path d="M500,508 L500,388" class="runtimeUse"/>\n`;

  b += panel(776, 126, 656, 620, "02-ddl");
  b += card(820, 178, 228, 70, "Table declarations", 0, "Table / IdTable / IntIdTable");
  b += card(1114, 178, 228, 70, "SchemaUtils", 1, "create/drop/missing");
  b += card(820, 318, 228, 70, "Constraints", 5, "PK / FK / unique");
  b += card(1114, 318, 228, 70, "Indexes", 6, "standard/hash/partial");
  b += card(820, 508, 228, 70, "Sequences / Enums", 4, "dialect-specific DDL");
  b += cylinder(1136, 500, 180, 116, "Dialect DBs", ["H2", "Postgres", "MySQL"], 3);
  b += `<path d="M1048,213 L1114,213" class="arrow"/>
<path d="M934,248 L934,318" class="runtimeUse"/>
<path d="M1048,353 L1114,353" class="arrow"/>
<path d="M934,388 L934,508" class="runtimeUse"/>
<path d="M1114,213 L1084,213 L1084,558 L1136,558" class="dbUse"/>
<path d="M1228,388 L1228,500" class="dbUse"/>\n`;
  b += note(152, 798, 1176, "Source check: connection tests exercise Database.connect, metadata, HikariCP, and multi-DB transactions; DDL tests use SchemaUtils with tables, constraints, indexes, sequences, and enums.");
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

function erdTable(x, y, w, title, rows, c, h = 170) {
  const [fill, stroke] = colors[c % colors.length];
  let out = `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="12" fill="#fff" stroke="${stroke}" class="card"/>
<path d="M${x},${y + 42} L${x},${y + 12} Q${x},${y} ${x + 12},${y} L${x + w - 12},${y} Q${x + w},${y} ${x + w},${y + 12} L${x + w},${y + 42} Z" fill="${fill}"/>
<text x="${x + w / 2}" y="${y + 28}" class="label" text-anchor="middle">${esc(title)}</text>\n`;
  rows.forEach((row, i) => (out += `<text x="${x + 18}" y="${y + 70 + i * 17}" class="tiny">${esc(row)}</text>\n`));
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
  const maxRows = Math.max(0, Math.floor((h - headerH - 12) / 15));
  rows.slice(0, maxRows).forEach((row, i) => (out += `<text x="${x + 14}" y="${y + headerH + 18 + i * 15}" class="tiny">${esc(row)}</text>\n`));
  return out;
}

function cylinder(x, y, w, h, title, rows, c) {
  const [fill, stroke] = colors[c % colors.length];
  const capH = 28;
  let out = `<path d="M${x},${y + capH / 2} C${x},${y - 4} ${x + w},${y - 4} ${x + w},${y + capH / 2} L${x + w},${y + h - capH / 2} C${x + w},${y + h + 4} ${x},${y + h + 4} ${x},${y + h - capH / 2} Z" fill="#fff" stroke="${stroke}" stroke-width="1.7" class="card"/>
<ellipse cx="${x + w / 2}" cy="${y + capH / 2}" rx="${w / 2}" ry="${capH / 2}" fill="${fill}" stroke="${stroke}" stroke-width="1.7"/>
<path d="M${x},${y + h - capH / 2} C${x},${y + h + 4} ${x + w},${y + h + 4} ${x + w},${y + h - capH / 2}" fill="none" stroke="${stroke}" stroke-width="1.7"/>
<text x="${x + w / 2}" y="${y + 24}" class="label" text-anchor="middle">${esc(title)}</text>\n`;
  rows.forEach((row, i) => (out += `<text x="${x + 18}" y="${y + 56 + i * 17}" class="tiny">${esc(row)}</text>\n`));
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
<rect x="${mid - 95}" y="${y - 28}" width="190" height="22" rx="7" fill="#fff" stroke="#bfdbfe"/>
<text x="${mid}" y="${y - 13}" class="tiny" text-anchor="middle">${esc(text)}</text>\n`;
}

function sequenceReturn(x1, x2, y, text) {
  const mid = (x1 + x2) / 2;
  return `<path d="M${x1},${y} L${x2},${y}" class="returnLine"/>
<rect x="${mid - 84}" y="${y - 28}" width="168" height="22" rx="7" fill="#fff" stroke="#cbd5e1"/>
<text x="${mid}" y="${y - 13}" class="tiny" text-anchor="middle">${esc(text)}</text>\n`;
}

function note(x, y, w, text) {
  return `<rect x="${x}" y="${y}" width="${w}" height="48" rx="10" fill="#ecfdf5" stroke="#86efac" stroke-width="1.3"/>
<text x="${x + w / 2}" y="${y + 30}" class="detail" text-anchor="middle">${esc(text)}</text>\n`;
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

for (const diagram of diagrams) {
  fs.writeFileSync(diagram.file, shell(diagram));
  const png = diagram.file.replace(/\.svg$/, ".png");
  const result = spawnSync(RSVG, ["-f", "png", "-o", png, diagram.file], { encoding: "utf8" });
  if (result.status !== 0) throw new Error(result.stderr || result.stdout);
  console.log(png);
}

validateBatch05Semantics();
validateConnectorGeometry();

function validateBatch05Semantics() {
  const erd = fs.readFileSync(diagrams[0].file, "utf8");
  const sequence = fs.readFileSync(diagrams[1].file, "utf8");
  const klass = fs.readFileSync(diagrams[2].file, "utf8");
  const ddl = fs.readFileSync(diagrams[3].file, "utf8");
  const failures = [];
  if (!erd.includes('M670,276 L446,276')) failures.push("SQL ERD must draw users.city_id -> cities.id directly");
  if (!sequence.includes('class="returnLine"')) failures.push("SQL query sequence must use dashed return messages");
  if (!klass.includes("Table") || !klass.includes("AbstractExposedTest")) failures.push("SQL class diagram must show supertypes");
  if (!ddl.includes("Database.connect") || !ddl.includes("SchemaUtils")) failures.push("DDL architecture must show connection and DDL tracks");
  if (failures.length) throw new Error(`batch05_semantic_validation_failed\n${failures.join("\n")}`);
  console.log("batch05_semantics=ok");
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
      const path = (match[1].match(/d="([^"]+)"/) || [])[1];
      if (!path) continue;
      const points = [...path.matchAll(/[ML]([\d.]+),([\d.]+)/g)].map((m) => ({ x: Number(m[1]), y: Number(m[2]) }));
      for (let i = 0; i < points.length - 1; i++) {
        for (const rect of cards) {
          const endpoint =
            (i === 0 && pointInside(rect, points[i])) ||
            (i === points.length - 2 && pointInside(rect, points[i + 1]));
          const crossing = segmentCrossesRect(rect, points[i], points[i + 1]);
          if (crossing && !endpoint) failures.push(`${diagram.file}: ${crossing} ${points[i].x},${points[i].y}->${points[i + 1].x},${points[i + 1].y}`);
        }
      }
    }
  }
  if (failures.length) throw new Error(`batch05_connector_validation_failed\n${failures.join("\n")}`);
  console.log("batch05_connectors=ok");
}

function attrNumbers(value) {
  const out = {};
  for (const match of value.matchAll(/\b(x|y|width|height)="([\d.]+)"/g)) out[match[1]] = Number(match[2]);
  return out;
}

function pointInside(rect, point) {
  return point.x > rect.x + 1 && point.x < rect.x + rect.width - 1 && point.y > rect.y + 1 && point.y < rect.y + rect.height - 1;
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
