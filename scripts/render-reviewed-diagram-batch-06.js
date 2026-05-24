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
.migration{fill:none;stroke:#db2777;stroke-width:1.9;stroke-dasharray:7 4;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowPink)}
.returnLine{fill:none;stroke:#64748b;stroke-width:1.8;stroke-dasharray:6 5;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrow)}
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
    file: `${outDir}/04-exposed-ddl-01-connection-architecture-01.svg`,
    title: "Exposed DDL Connection Management Flow",
    subtitle: "Database.connect registers JDBC URLs and DataSources; transactions choose DB handles, retry policy, and metadata access",
    width: 1540,
    height: 920,
    body: connectionArchitecture,
  },
  {
    file: `${outDir}/04-exposed-ddl-02-ddl-architecture-01.svg`,
    title: "Exposed DDL Schema Execution Flow",
    subtitle: "Table declarations feed SchemaUtils and MigrationUtils before dialect-specific SQL reaches the database",
    width: 1500,
    height: 900,
    body: ddlExecutionArchitecture,
  },
  {
    file: `${outDir}/04-exposed-ddl-02-ddl-class-02.svg`,
    title: "Exposed DDL Table Type Model",
    subtitle: "DDL examples combine the Exposed Table hierarchy with concrete schema objects and SchemaUtils operations",
    width: 1500,
    height: 920,
    body: ddlClassModel,
  },
  {
    file: `${outDir}/04-exposed-ddl-02-ddl-erd-03.svg`,
    title: "Exposed DDL Composite Foreign Keys ERD",
    subtitle: "Both examples define child1 foreign keys to parent1 composite primary keys with cascade delete and update",
    width: 1360,
    height: 820,
    body: ddlCompositeFkErd,
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

validateBatch06Semantics();
validateConnectorGeometry();

function writeGraphvizSketches() {
  fs.mkdirSync(sketchDir, { recursive: true });
  const sketches = {
    "batch-06-connection-architecture": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=0.7, ranksep=1.0, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      h2_url -> database_connect -> transaction_db -> h2_db;
      database_connect -> transaction_manager;
      transaction_db -> metadata -> people_parent_child;
      hikari_ds -> database_connect_hikari -> suspended_async -> hikari_db;
      wrapping_ds -> database_connect_wrapped -> max_attempts -> retry_result;
      db1_url -> database_connect_db1 -> transaction_db1 -> db1;
      db2_url -> database_connect_db2 -> transaction_db2 -> db2;
      transaction_db2 -> with_suspend_transaction -> db1;
    }`,
    "batch-06-ddl-architecture": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=0.75, ranksep=1.0, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      tables -> schema_utils -> create_drop -> ddl_statements -> dialect_db;
      constraints -> schema_utils;
      indexes -> schema_utils;
      sequences -> schema_utils;
      enums -> schema_utils;
      table_v1_v2 -> migration_utils -> alter_statements -> dialect_db;
    }`,
    "batch-06-ddl-class": `digraph G {
      graph [rankdir=TB, splines=ortho, nodesep=0.75, ranksep=0.85, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      table -> id_table;
      id_table -> int_id_table;
      id_table -> long_id_table;
      table -> book_table;
      table -> person_table;
      table -> developer;
      int_id_table -> enum_table;
      long_id_table -> people;
      schema_utils -> book_table;
      schema_utils -> enum_table;
      migration_utils -> tester_v1_v2;
    }`,
    "batch-06-ddl-erd": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=1.2, ranksep=1.5, pad=0.2];
      node [shape=record, style="rounded,filled", fillcolor="#ffffff", color="#94a3b8"];
      parent_same [label="{parent1|id_a INT PK\\lid_b INT PK}"];
      child_same [label="{child1|id_a INT FK\\lid_b INT FK}"];
      parent_pair [label="{parent1|pid_a INT PK\\lpid_b INT PK}"];
      child_pair [label="{child1|id_a INT FK\\lid_b INT FK}"];
      child_same -> parent_same [label="(id_a,id_b)"];
      child_pair -> parent_pair [label="id_a->pid_a, id_b->pid_b"];
    }`,
  };

  for (const [name, dot] of Object.entries(sketches)) {
    const dotPath = `${sketchDir}/${name}.dot`;
    fs.writeFileSync(dotPath, dot);
    spawnSync(DOT, ["-Tsvg", "-o", `${sketchDir}/${name}.svg`, dotPath], { encoding: "utf8" });
    spawnSync(DOT, ["-Tplain", "-o", `${sketchDir}/${name}.plain`, dotPath], { encoding: "utf8" });
  }
}

function connectionArchitecture() {
  let b = panel(58, 134, 1424, 618, "Source-derived connection scenarios");

  b += card(96, 184, 224, 72, "JDBC URL / DataSource", 0, "H2, TestDB, stubs");
  b += card(392, 184, 226, 72, "Database.connect", 1, "register Database");
  b += card(690, 184, 246, 72, "transaction(db)", 6, "explicit DB handle");
  b += card(1012, 184, 226, 72, "connection.metadata", 5, "columns, constraints");
  b += cylinder(1284, 174, 142, 104, "H2 DB", ["People", "parent", "child"], 3);

  b += card(96, 330, 224, 72, "HikariDataSource", 2, "maximumPoolSize = 10");
  b += card(392, 330, 226, 72, "hikariDB1", 0, "Database handle");
  b += card(690, 330, 246, 72, "suspendedTransactionAsync", 1, "2x pool + 1 tasks");
  b += cylinder(1262, 318, 186, 112, "HIKARI_TESTER", ["TestEntity rows", "connection reuse"], 2);

  b += card(96, 476, 224, 72, "WrappingDataSource", 4, "ConnectionSpy");
  b += card(392, 476, 226, 72, "Database.connect", 1, "wrapped connection");
  b += card(690, 476, 246, 72, "maxAttempts", 4, "3 or 5 retries");
  b += card(1012, 476, 226, 72, "commit / rollback", 7, "close observed");
  b += card(1262, 476, 186, 72, "Timeout source", 6, "getConnection fails");

  b += card(96, 622, 224, 72, "db1 / db2 URLs", 3, "jdbc:h2:mem");
  b += card(392, 622, 226, 72, "TransactionManager", 0, "db1, db2 handles");
  b += card(690, 622, 246, 72, "nested transactions", 1, "db1 -> db2 -> db1");
  b += card(1012, 622, 226, 72, "withSuspendTransaction", 5, "return to tr1");
  b += cylinder(1272, 604, 166, 116, "H2 db1/db2", ["separate Cities", "default DB checks"], 3);

  b += path("M320,220 L392,220", "arrow");
  b += path("M618,220 L690,220", "arrow");
  b += path("M936,220 L1012,220", "runtimeUse");
  b += path("M1238,220 L1284,220", "dbUse");

  b += path("M320,366 L392,366", "arrow");
  b += path("M618,366 L690,366", "arrow");
  b += path("M936,366 L1262,366", "dbUse");

  b += path("M320,512 L392,512", "arrow");
  b += path("M618,512 L690,512", "arrow");
  b += path("M936,512 L1012,512", "runtimeUse");
  b += path("M1238,512 L1262,512", "migration");

  b += path("M320,658 L392,658", "arrow");
  b += path("M618,658 L690,658", "arrow");
  b += path("M936,658 L1012,658", "runtimeUse");
  b += path("M1238,658 L1272,658", "dbUse");

  b += note(126, 804, 1288, "Source check: Ex01_Connection reads metadata; Hikari uses suspendedTransactionAsync; retry examples count failed connections; multi-DB tests switch db1/db2 and re-enter db1 with withSuspendTransaction.");
  return b;
}

function ddlExecutionArchitecture() {
  let b = panel(58, 130, 414, 608, "Declarations");
  b += card(96, 178, 286, 70, "Table / IdTable", 0, "Book, Person, Users2");
  b += card(96, 290, 286, 70, "Constraints", 5, "PK, FK, unique");
  b += card(96, 402, 286, 70, "Indexes", 6, "standard, HASH, partial");
  b += card(96, 514, 286, 70, "Sequences", 2, "my_sequence");
  b += card(96, 626, 286, 70, "Custom enums", 4, "StatusEnum, EnumTable");

  b += panel(544, 130, 370, 608, "DDL executors");
  b += card(608, 236, 242, 82, "SchemaUtils", 1, "create/drop/missing");
  b += card(608, 466, 242, 82, "MigrationUtils", 4, "statementsRequired");
  b += card(608, 626, 242, 70, "Dialect guards", 7, "Postgres/MySQL/H2");

  b += panel(986, 130, 456, 608, "Generated database shape");
  b += card(1028, 178, 258, 70, "CREATE TABLE", 0, "PK, FK, columns");
  b += card(1028, 290, 258, 70, "ALTER TABLE", 4, "unique, column drift");
  b += card(1028, 402, 258, 70, "CREATE INDEX", 6, "predicate/functions");
  b += card(1028, 514, 258, 70, "CREATE SEQUENCE", 2, "start/increment/cache");
  b += cylinder(1308, 248, 96, 300, "DB", ["H2", "Postgres", "MySQL"], 3);

  b += path("M382,213 L608,277", "arrow", "M382,213 L510,213 L510,277 L608,277");
  b += path("M382,325 L608,277", "arrow", "M382,325 L510,325 L510,277 L608,277");
  b += path("M382,437 L608,277", "arrow", "M382,437 L510,437 L510,277 L608,277");
  b += path("M382,549 L608,277", "arrow", "M382,549 L510,549 L510,277 L608,277");
  b += path("M382,661 L608,277", "arrow", "M382,661 L510,661 L510,277 L608,277");
  b += path("M382,325 L608,507", "migration", "M382,325 L520,325 L520,507 L608,507");

  b += path("M850,277 L1028,213", "arrow", "M850,277 L940,277 L940,213 L1028,213");
  b += path("M850,507 L1028,325", "migration", "M850,507 L940,507 L940,325 L1028,325");
  b += path("M850,277 L1028,437", "arrow", "M850,277 L958,277 L958,437 L1028,437");
  b += path("M850,277 L1028,549", "arrow", "M850,277 L976,277 L976,549 L1028,549");

  b += path("M1286,213 L1308,314", "dbUse", "M1286,213 L1298,213 L1298,314 L1308,314");
  b += path("M1286,325 L1308,354", "dbUse", "M1286,325 L1298,325 L1298,354 L1308,354");
  b += path("M1286,437 L1308,394", "dbUse", "M1286,437 L1298,437 L1298,394 L1308,394");
  b += path("M1286,549 L1308,434", "dbUse", "M1286,549 L1298,549 L1298,434 L1308,434");

  b += note(122, 794, 1256, "Source check: Ex02 creates tables and composite FKs; Ex03 runs MigrationUtils; Ex05 creates index variants; Ex06 creates sequences; Ex07 maps custom enum SQL types and references.");
  return b;
}

function ddlClassModel() {
  let b = panel(52, 128, 704, 638, "Exposed table hierarchy");
  b += umlClass(300, 166, 176, 82, "Table", ["PrimaryKey", "Column definitions"], 7, "base");
  b += umlClass(292, 306, 192, 82, "IdTable<T>", ["id: Column<EntityID<T>>"], 0, "abstract table");
  b += umlClass(146, 470, 194, 96, "IntIdTable", ["integer id", "auto increment"], 2, "id table");
  b += umlClass(438, 470, 194, 96, "LongIdTable", ["long id", "BIGSERIAL/BIGINT"], 3, "id table");
  b += umlClass(78, 636, 172, 82, "EnumTable", ["IntIdTable(\"enum_table\")", "status customEnumeration"], 4, "object");
  b += umlClass(286, 636, 172, 82, "IDTable", ["IntIdTable(\"IntIdTable\")"], 6, "object");
  b += umlClass(494, 636, 172, 82, "People", ["LongIdTable()", "metadata example"], 5, "object");

  b += inheritArrow(388, 306, 388, 248, 276);
  b += inheritArrow(243, 470, 388, 388, 426);
  b += inheritArrow(535, 470, 388, 388, 426);
  b += inheritArrow(164, 636, 243, 566, 598);
  b += inheritArrow(372, 636, 243, 566, 598);
  b += inheritArrow(580, 636, 535, 566, 598);

  b += panel(806, 128, 642, 638, "Concrete DDL examples");
  b += umlClass(844, 170, 190, 110, "BookTable", ["Table(\"book\")", "id autoIncrement", "PK_Book_ID"], 0, "object");
  b += umlClass(1108, 170, 210, 110, "PersonTable", ["Table(\"person\")", "id1, id2", "composite PK"], 5, "object");
  b += umlClass(844, 334, 190, 126, "Developer", ["Table(\"developer\")", "id, name", "composite PK", "Sequence value"], 2, "object");
  b += umlClass(1108, 334, 210, 126, "tester", ["IntIdTable(\"tester\")", "index variants", "partial/functional"], 6, "anonymous table");
  b += umlClass(844, 534, 210, 112, "SchemaUtils", ["create/drop", "createSequence/dropSequence", "createMissingTablesAndColumns"], 1, "jdbc utility");
  b += umlClass(1108, 534, 210, 112, "MigrationUtils", ["statementsRequiredForDatabaseMigration"], 4, "migration utility");

  b += labelPill(882, 292, 114, "extends Table", "#dbeafe");
  b += labelPill(1154, 292, 124, "extends Table", "#dbeafe");
  b += labelPill(882, 474, 114, "extends Table", "#dbeafe");
  b += labelPill(1130, 474, 158, "extends IntIdTable", "#dcfce7");
  b += path("M844,590 L812,590 L812,397 L844,397", "runtimeUse");
  b += path("M1054,590 L1082,590 L1082,397 L1108,397", "runtimeUse");
  b += path("M1318,590 L1356,590 L1356,397 L1318,397", "migration");
  b += note(138, 822, 1224, "UML check: supertypes stay above children, hollow generalization triangles are explicit, and every stem meets the triangle base vertically.");
  return b;
}

function ddlCompositeFkErd() {
  let b = panel(70, 134, 560, 490, "Example 01: child columns match parent key names");
  b += erdTable(126, 202, 362, "parent1", ["PK id_a INT", "PK id_b INT", "PrimaryKey(idA, idB)"], 0, 150);
  b += erdTable(126, 430, 362, "child1", ["FK id_a INT NOT NULL", "FK id_b INT NOT NULL", "MyForeignKey1"], 5, 168);
  b += path("M307,430 L307,352", "fkArrow");
  b += labelPill(213, 384, 188, "id_a,id_b -> id_a,id_b", "#ccfbf1");
  b += labelPill(228, 610, 160, "CASCADE delete/update", "#fef3c7");

  b += panel(730, 134, 560, 490, "Example 02: child columns map to differently named parent keys");
  b += erdTable(786, 202, 362, "parent1", ["PK pid_a INT", "PK pid_b INT", "PrimaryKey(pidA, pidB)"], 0, 150);
  b += erdTable(786, 430, 362, "child1", ["FK id_a INT NOT NULL", "FK id_b INT NOT NULL", "MyForeignKey1"], 5, 168);
  b += path("M967,430 L967,352", "fkArrow");
  b += labelPill(860, 384, 214, "id_a -> pid_a, id_b -> pid_b", "#ccfbf1");
  b += labelPill(888, 610, 160, "CASCADE delete/update", "#fef3c7");

  b += note(134, 700, 1092, "ERD check: FK arrows point from child1 to parent1; endpoints hit table boundaries at 90 degrees and stay visually distinct from UML inheritance triangles.");
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

function validateBatch06Semantics() {
  const connection = fs.readFileSync(diagrams[0].file, "utf8");
  const architecture = fs.readFileSync(diagrams[1].file, "utf8");
  const klass = fs.readFileSync(diagrams[2].file, "utf8");
  const erd = fs.readFileSync(diagrams[3].file, "utf8");
  const failures = [];

  for (const required of ["Database.connect", "TransactionManager", "HikariDataSource", "suspendedTransactionAsync", "WrappingDataSource", "maxAttempts", "db1 / db2", "withSuspendTransaction"]) {
    if (!connection.includes(required)) failures.push(`connection diagram missing ${required}`);
  }
  for (const required of ["SchemaUtils", "MigrationUtils", "CREATE INDEX", "CREATE SEQUENCE", "Custom enums", "Dialect guards"]) {
    if (!architecture.includes(required)) failures.push(`DDL architecture missing ${required}`);
  }
  for (const required of ["Table", "IdTable&lt;T&gt;", "IntIdTable", "LongIdTable", "SchemaUtils", "MigrationUtils"]) {
    if (!klass.includes(required)) failures.push(`DDL class model missing ${required}`);
  }
  if (!erd.includes("id_a,id_b -&gt; id_a,id_b")) failures.push("ERD must show same-name composite FK mapping");
  if (!erd.includes("id_a -&gt; pid_a, id_b -&gt; pid_b")) failures.push("ERD must show explicit pair composite FK mapping");
  if (!erd.includes("CASCADE delete/update")) failures.push("ERD must show cascade options");
  if (!erd.includes('d="M307,430 L307,352" class="fkArrow"') || !erd.includes('d="M967,430 L967,352" class="fkArrow"')) {
    failures.push("ERD FK lines must leave child table top and enter parent table bottom at 90 degrees");
  }
  if (klass.includes("LongEntityClass")) failures.push("DDL class model must not show unrelated LongEntityClass");

  if (failures.length) throw new Error(`batch06_semantic_validation_failed\n${failures.join("\n")}`);
  console.log("batch06_semantics=ok");
}

function validateConnectorGeometry() {
  const connectorClasses = "(?:arrow|runtimeUse|dbUse|fkArrow|returnLine|inherit|migration)";
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
  if (failures.length) throw new Error(`batch06_connector_validation_failed\n${failures.join("\n")}`);
  console.log("batch06_connectors=ok");
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
