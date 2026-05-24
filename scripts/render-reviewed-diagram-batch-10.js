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
.cryptoLine{fill:none;stroke:#db2777;stroke-width:1.9;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowPink)}
.dbUse{fill:none;stroke:#ea580c;stroke-width:2;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowOrange)}
.runtimeUse{fill:none;stroke:#7c3aed;stroke-width:1.9;stroke-dasharray:6 5;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowPurple)}
.inheritStem{fill:none;stroke:#475569;stroke-width:1.8;stroke-linecap:round;stroke-linejoin:round}
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
    file: `${outDir}/06-advanced-01-exposed-crypt-architecture-01.svg`,
    title: "Exposed Crypt Column Flow",
    subtitle: "Encrypted column tests route plain values through encryptedVarchar/encryptedBinary and Exposed column types before DB storage",
    width: 1560,
    height: 900,
    body: cryptArchitecture,
  },
  {
    file: `${outDir}/06-advanced-01-exposed-crypt-class-02.svg`,
    title: "Exposed Crypt Class and Table Model",
    subtitle: "Crypt examples combine AbstractExposedTest, IntIdTable, encrypted columns, and an IntEntity mapping",
    width: 1500,
    height: 900,
    body: cryptClassModel,
  },
  {
    file: `${outDir}/06-advanced-02-exposed-javatime-class-01.svg`,
    title: "Java Time Column Type Mapping",
    subtitle: "Java Time examples map LocalDate, LocalTime, LocalDateTime, Instant, and OffsetDateTime to Exposed column builders",
    width: 1540,
    height: 900,
    body: javaTimeMapping,
  },
  {
    file: `${outDir}/06-advanced-03-exposed-kotlin-datetime-class-01.svg`,
    title: "Kotlinx DateTime Column Type Mapping",
    subtitle: "Kotlin datetime examples mirror Java time coverage with kotlinx.datetime types and default expressions",
    width: 1540,
    height: 900,
    body: kotlinDateTimeMapping,
  },
  {
    file: `${outDir}/06-advanced-04-exposed-json-architecture-01.svg`,
    title: "Exposed JSON Column Query Flow",
    subtitle: "JSON examples serialize DataHolder models into json/jsonb columns and query nested data with extract, contains, and exists",
    width: 1560,
    height: 900,
    body: jsonArchitecture,
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
validateBatch10Semantics();
validateConnectorGeometry();

function writeGraphvizSketches() {
  fs.mkdirSync(sketchDir, { recursive: true });
  const sketches = {
    "batch-10-crypt-flow": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=0.65, ranksep=0.9, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      tests -> encrypted_columns -> algorithms -> exposed_column_type -> tables -> db;
      entity -> tables;
    }`,
    "batch-10-crypt-class": `digraph G {
      graph [rankdir=TB, splines=ortho, nodesep=0.65, ranksep=0.9, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      Ex01 -> AbstractExposedTest;
      Ex02 -> AbstractExposedTest;
      TestTable -> IntIdTable;
      ETest -> IntEntity;
      ETest -> TestTable;
    }`,
    "batch-10-time-mapping": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=0.6, ranksep=0.8, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      language_types -> column_builders -> table_columns -> defaults -> database;
    }`,
    "batch-10-json-flow": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=0.6, ranksep=0.8, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      data_models -> kotlinx_json -> json_columns -> tables -> path_queries -> db;
    }`,
  };
  for (const [name, dot] of Object.entries(sketches)) {
    const dotPath = `${sketchDir}/${name}.dot`;
    fs.writeFileSync(dotPath, dot);
    spawnSync(DOT, ["-Tsvg", "-o", `${sketchDir}/${name}.svg`, dotPath], { encoding: "utf8" });
    spawnSync(DOT, ["-Tplain", "-o", `${sketchDir}/${name}.plain`, dotPath], { encoding: "utf8" });
  }
}

function cryptArchitecture() {
  let b = "";
  b += panel(58, 130, 300, 608, "Test entry points");
  b += card(96, 194, 224, 70, "Ex01 DSL test", 0, "StringTable");
  b += card(96, 332, 224, 70, "Ex02 Entity test", 2, "TestTable + ETest");
  b += card(96, 470, 224, 70, "Plain values", 7, "name / city / address");
  b += card(96, 608, 224, 64, "Assertions", 5, "decrypted round-trip");

  b += panel(430, 130, 344, 608, "Encrypted columns");
  b += card(482, 196, 240, 74, "encryptedVarchar", 4, "name / address / age");
  b += card(482, 350, 240, 74, "encryptedBinary", 1, "city ByteArray");
  b += card(482, 504, 240, 74, "Exposed Column<T>", 6, "value conversion");

  b += panel(840, 130, 330, 608, "Crypto algorithms");
  b += card(884, 184, 242, 64, "AES-256 PBE GCM", 4, "varchar tests");
  b += card(884, 298, 242, 64, "AES-256 PBE CBC", 1, "binary tests");
  b += card(884, 412, 242, 64, "Blowfish", 3, "address");
  b += card(884, 526, 242, 64, "Triple DES", 6, "age");

  b += panel(1234, 130, 234, 608, "Database");
  b += erdTable(1272, 204, 158, "StringTable", ["id PK", "name encrypted", "city encrypted", "address encrypted", "age encrypted"], 0, 168);
  b += erdTable(1272, 438, 158, "TestTable", ["id PK", "encrypted: varchar", "encryptedBinary"], 2, 142);
  b += cylinder(1288, 604, 126, 118, "DB", ["H2", "Postgres"], 3);

  b += path("M320,229 L482,233", "arrow", "M320,229 L400,229 L400,233 L482,233");
  b += path("M320,367 L482,387", "arrow", "M320,367 L400,367 L400,387 L482,387");
  b += path("M320,505 L482,233", "runtimeUse", "M320,505 L396,505 L396,233 L482,233");
  b += path("M722,233 L884,216", "cryptoLine", "M722,233 L802,233 L802,216 L884,216");
  b += path("M722,387 L884,330", "cryptoLine", "M722,387 L802,387 L802,330 L884,330");
  b += path("M722,541 L1272,288", "mapLine", "M722,541 L796,541 L796,680 L1210,680 L1210,288 L1272,288");
  b += path("M1126,216 L1272,288", "dbUse", "M1126,216 L1198,216 L1198,288 L1272,288");
  b += path("M1126,330 L1272,510", "dbUse", "M1126,330 L1198,330 L1198,510 L1272,510");
  b += path("M1430,288 L1414,664", "dbUse", "M1430,288 L1450,288 L1450,664 L1414,664");
  b += path("M1351,580 L1351,604", "dbUse");
  b += note(118, 796, 1264, "Source check: Ex01 uses encryptedVarchar/encryptedBinary with AES/GCM, AES/CBC, Blowfish, Triple DES; Ex02 maps encrypted columns through IntEntity.");
  return b;
}

function cryptClassModel() {
  let b = "";
  b += panel(70, 130, 642, 608, "Test classes");
  b += umlBox(286, 178, 220, 96, "AbstractExposedTest", ["withTables(testDB, ...)"], ["base test fixture"], 7);
  b += umlBox(128, 386, 230, 108, "Ex01 Crypt Column", ["Ex01_EncryptedColumn", "DSL insert/update", "StringTable local object"], [], 0);
  b += umlBox(410, 386, 280, 108, "Ex02 Entity Crypt", ["Ex02_EncryptedColumnWithEntity", "Entity.new", "ETest mapping"], [], 2);

  b += panel(812, 130, 612, 608, "Table and entity model");
  b += umlBox(860, 178, 230, 94, "IntIdTable", ["id: EntityID<Int>"], ["table base"], 1);
  b += umlBox(860, 386, 230, 130, "TestTable", ["encrypted: varchar(255)", "encryptedBinary: binary(255)"], ["encrypted columns"], 4);
  b += umlBox(1148, 178, 230, 94, "IntEntity", ["EntityID<Int>"], ["entity base"], 7);
  b += umlBox(1148, 386, 230, 130, "ETest", ["var encrypted", "var encryptedBinary"], ["IntEntityClass<ETest>"], 5);

  b += generalizationTree(396, 274, 328, 386, [243, 550]);
  b += generalizationTree(975, 272, 328, 386, [975]);
  b += generalizationTree(1263, 272, 328, 386, [1263]);
  b += path("M1090,451 L1148,451", "mapLine");
  b += path("M690,440 L860,451", "runtimeUse", "M690,440 L760,440 L760,451 L860,451");
  b += labelPill(1020, 534, "delegated columns", 122);
  b += note(136, 796, 1228, "UML rule: superclasses stay above concrete examples; entity-to-table mapping is a dependency line, not inheritance.");
  return b;
}

function javaTimeMapping() {
  return timeMapping({
    title: "Java time",
    leftPanel: "Java type",
    sourceNote: "Ex01_JavaTime, Ex02_Defaults, Ex03_DateTimeLiteral, and Ex04_MiscTable cover Java Time columns, defaults, literals, and nullable variants.",
    typeRows: [
      ["LocalDate", "date(\"d\")", "DATE", 0],
      ["LocalTime", "time(\"t\")", "TIME", 5],
      ["LocalDateTime", "datetime(\"dt\")", "DATETIME", 2],
      ["Instant", "timestamp(\"ts\")", "TIMESTAMP", 1],
      ["OffsetDateTime", "timestampWithTimeZone", "TIMESTAMPTZ", 4],
    ],
    defaults: ["CurrentDate", "CurrentDateTime", "CurrentTimestamp", "clientDefault { now }"],
    dbRows: ["H2", "Postgres", "MySQL caveat", "MariaDB caveat"],
  });
}

function kotlinDateTimeMapping() {
  return timeMapping({
    title: "Kotlin datetime",
    leftPanel: "kotlinx.datetime type",
    sourceNote: "Ex01_KotlinDateTime, Ex02_Defaults, Ex03_DateTimeLiteral, KotlinDateTimeSupports, and KotlinTimeAssertions mirror the Java Time test surface.",
    typeRows: [
      ["LocalDate", "date(\"date\")", "DATE", 0],
      ["LocalTime", "time(\"time\")", "TIME", 5],
      ["LocalDateTime", "datetime(\"datetime\")", "DATETIME", 2],
      ["Instant", "timestamp(\"ts\")", "TIMESTAMP", 1],
      ["Instant + zone", "timestampWithTimeZone", "TIMESTAMPTZ", 4],
    ],
    defaults: ["CurrentDate", "CurrentDateTime", "CurrentTimestamp", "CurrentTimestampWithTimeZone"],
    dbRows: ["H2", "Postgres", "dialect checks"],
  });
}

function timeMapping(config) {
  let b = "";
  b += panel(58, 130, 330, 608, config.leftPanel);
  b += panel(454, 130, 354, 608, "Exposed column builder");
  b += panel(874, 130, 314, 608, "Table columns and defaults");
  b += panel(1250, 130, 210, 608, "Database");
  config.typeRows.forEach(([type, builder, sql, c], i) => {
    const y = 178 + i * 96;
    b += card(96, y, 240, 62, type, c, config.title);
    b += card(506, y, 250, 62, builder, c, sql);
    b += path(`M336,${y + 31} L506,${y + 31}`, "mapLine");
  });
  b += erdTable(918, 184, 226, "Misc / test tables", ["nullable variants", "literal tables", "assertion fixtures"], 7, 128);
  b += erdTable(918, 380, 226, "TableWithDBDefault", config.defaults, 6, 164);
  b += cylinder(1292, 294, 126, 250, "DB", config.dbRows, 3);
  b += path("M756,209 L918,248", "arrow", "M756,209 L836,209 L836,248 L918,248");
  b += path("M756,305 L918,248", "arrow", "M756,305 L836,305 L836,248 L918,248");
  b += path("M756,401 L918,462", "arrow", "M756,401 L836,401 L836,462 L918,462");
  b += path("M756,497 L918,462", "arrow", "M756,497 L836,497 L836,462 L918,462");
  b += path("M1144,248 L1292,378", "dbUse", "M1144,248 L1218,248 L1218,378 L1292,378");
  b += path("M1144,462 L1292,420", "dbUse", "M1144,462 L1218,462 L1218,420 L1292,420");
  b += note(118, 796, 1264, `Source check: ${config.sourceNote}`);
  return b;
}

function jsonArchitecture() {
  let b = "";
  b += panel(58, 130, 324, 608, "Domain objects");
  b += umlBox(96, 184, 244, 108, "DataHolder", ["user: User", "logins: Int", "active: Boolean"], [], 2);
  b += umlBox(96, 350, 244, 92, "User", ["name", "team: UserGroup"], [], 0);
  b += umlBox(96, 500, 244, 92, "UserGroup", ["name"], [], 1);

  b += panel(454, 130, 334, 608, "Serialization and columns");
  b += card(500, 194, 242, 68, "Json.Default", 6, "kotlinx.serialization");
  b += card(500, 334, 242, 68, "json<DataHolder>", 0, "JsonTable.j_column");
  b += card(500, 474, 242, 68, "jsonb<DataHolder>", 4, "JsonBTable.j_b_column");
  b += card(500, 612, 242, 58, "array variants", 5, "j_arrays / j_b_arrays");

  b += panel(858, 130, 320, 608, "Query DSL");
  b += card(902, 184, 232, 66, "insert / update", 2, "object as-is");
  b += card(902, 316, 232, 66, "extract(path)", 1, "$.user.name / $.logins");
  b += card(902, 448, 232, 66, "contains", 4, "JSONB @>");
  b += card(902, 580, 232, 66, "exists(path)", 6, "jsonb_path_exists");

  b += panel(1250, 130, 210, 608, "Database");
  b += erdTable(1276, 188, 156, "JsonTable", ["id PK", "j_column JSON"], 0, 118);
  b += erdTable(1276, 360, 156, "JsonBTable", ["id PK", "j_b_column JSONB"], 4, 118);
  b += cylinder(1292, 566, 126, 128, "DB", ["Postgres JSONB", "H2 JSON"], 3);

  b += path("M340,238 L500,228", "mapLine", "M340,238 L420,238 L420,228 L500,228");
  b += path("M742,228 L902,217", "arrow", "M742,228 L822,228 L822,217 L902,217");
  b += path("M742,368 L902,349", "arrow", "M742,368 L822,368 L822,349 L902,349");
  b += path("M742,508 L902,481", "arrow", "M742,508 L822,508 L822,481 L902,481");
  b += path("M1134,217 L1276,247", "dbUse", "M1134,217 L1206,217 L1206,247 L1276,247");
  b += path("M1134,349 L1276,419", "dbUse", "M1134,349 L1206,349 L1206,419 L1276,419");
  b += path("M1134,481 L1276,419", "dbUse", "M1134,481 L1206,481 L1206,419 L1276,419");
  b += path("M1134,613 L1292,640", "dbUse", "M1134,613 L1206,613 L1206,640 L1292,640");
  b += path("M1432,247 L1418,630", "dbUse", "M1432,247 L1452,247 L1452,630 L1418,630");
  b += path("M1354,478 L1354,566", "dbUse");
  b += note(118, 796, 1264, "Source check: JsonTestData defines DataHolder/User/UserGroup plus JsonTable, JsonBTable, JsonArrayTable, and JsonBArrayTable; tests call extract, contains, and exists.");
  return b;
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

function umlBox(x, y, w, h, title, fields, methods, c) {
  const [fill, stroke] = colors[c % colors.length];
  const headerH = 40;
  const split1 = y + headerH;
  const bodyH = h - headerH;
  const hasMethods = methods.length > 0;
  const fieldH = hasMethods ? Math.max(34, Math.floor(bodyH * 0.56)) : bodyH;
  const methodH = hasMethods ? bodyH - fieldH : 0;
  const split2 = split1 + fieldH;
  let out = `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="4" fill="#fff" stroke="${stroke}" class="card"/>
<path d="M${x},${split1} L${x},${y + 4} Q${x},${y} ${x + 4},${y} L${x + w - 4},${y} Q${x + w},${y} ${x + w},${y + 4} L${x + w},${split1} Z" fill="${fill}"/>
<line x1="${x}" y1="${split1}" x2="${x + w}" y2="${split1}" stroke="${stroke}" stroke-width="1.2"/>
<text x="${x + w / 2}" y="${y + 26}" class="label" text-anchor="middle">${esc(title)}</text>\n`;
  if (hasMethods) out += `<line x1="${x}" y1="${split2}" x2="${x + w}" y2="${split2}" stroke="${stroke}" stroke-width="1.2"/>\n`;
  fields.forEach((row, i) => (out += `<text x="${x + 16}" y="${centeredRowY(split1, fieldH, fields.length, i)}" class="tiny">${esc(row)}</text>\n`));
  methods.forEach((row, i) => (out += `<text x="${x + 16}" y="${centeredRowY(split2, methodH, methods.length, i)}" class="tiny">${esc(row)}</text>\n`));
  return out;
}

function centeredRowY(top, height, count, index) {
  const step = 18;
  const blockH = count * step;
  return top + (height - blockH) / 2 + 13 + index * step;
}

function erdTable(x, y, w, title, rows, c, h = 170) {
  const [fill, stroke] = colors[c % colors.length];
  let out = `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="10" fill="#fff" stroke="${stroke}" class="card"/>
<path d="M${x},${y + 42} L${x},${y + 10} Q${x},${y} ${x + 10},${y} L${x + w - 10},${y} Q${x + w},${y} ${x + w},${y + 10} L${x + w},${y + 42} Z" fill="${fill}"/>
<text x="${x + w / 2}" y="${y + 28}" class="label" text-anchor="middle">${esc(title)}</text>\n`;
  rows.forEach((row, i) => (out += `<text x="${x + 16}" y="${y + 70 + i * 17}" class="tiny">${esc(row)}</text>\n`));
  return out;
}

function cylinder(x, y, w, h, title, rows, c) {
  const [fill, stroke] = colors[c % colors.length];
  const capH = 34;
  let out = `<path d="M${x},${y + capH / 2} C${x},${y - 4} ${x + w},${y - 4} ${x + w},${y + capH / 2} L${x + w},${y + h - capH / 2} C${x + w},${y + h + 4} ${x},${y + h + 4} ${x},${y + h - capH / 2} Z" fill="#fff" stroke="${stroke}" stroke-width="1.7" class="card"/>
<ellipse cx="${x + w / 2}" cy="${y + capH / 2}" rx="${w / 2}" ry="${capH / 2}" fill="${fill}" stroke="${stroke}" stroke-width="1.7"/>
<path d="M${x},${y + h - capH / 2} C${x},${y + h + 4} ${x + w},${y + h + 4} ${x + w},${y + h - capH / 2}" fill="none" stroke="${stroke}" stroke-width="1.7"/>
<text x="${x + w / 2}" y="${y + 48}" class="label" text-anchor="middle">${esc(title)}</text>\n`;
  rows.forEach((row, i) => (out += `<text x="${x + 14}" y="${y + 78 + i * 18}" class="tiny">${esc(row)}</text>\n`));
  return out;
}

function generalizationTree(parentX, parentBottomY, busY, childTopY, childXs) {
  const baseY = parentBottomY + 16;
  let out = `<path d="M${parentX},${baseY} L${parentX},${busY}" class="inheritStem"/>
<polygon points="${parentX},${parentBottomY} ${parentX - 9},${baseY} ${parentX + 9},${baseY}" fill="#fff" stroke="#475569" stroke-width="1.8"/>\n`;
  out += `<path d="M${Math.min(...childXs)},${busY} L${Math.max(...childXs)},${busY}" class="inheritStem"/>\n`;
  for (const childX of childXs) out += `<path d="M${childX},${busY} L${childX},${childTopY}" class="inheritStem"/>\n`;
  return out;
}

function labelPill(x, y, text, w) {
  return `<rect x="${x}" y="${y}" width="${w}" height="24" rx="8" fill="#fff" stroke="#cbd5e1"/><text x="${x + w / 2}" y="${y + 16}" class="tiny" text-anchor="middle">${esc(text)}</text>\n`;
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

function validateBatch10Semantics() {
  const [cryptFlow, cryptClass, javaTime, kotlinTime, jsonFlow] = diagrams.map((d) => fs.readFileSync(d.file, "utf8"));
  const failures = [];
  for (const required of ["encryptedVarchar", "encryptedBinary", "AES-256 PBE GCM", "Blowfish", "Triple DES", "StringTable", "TestTable"]) {
    if (!cryptFlow.includes(required)) failures.push(`crypt flow missing ${required}`);
  }
  for (const required of ["AbstractExposedTest", "IntIdTable", "IntEntity", "TestTable", "ETest", "delegated columns"]) {
    if (!cryptClass.includes(required)) failures.push(`crypt class missing ${required}`);
  }
  for (const required of ["LocalDate", "LocalTime", "LocalDateTime", "Instant", "OffsetDateTime", "timestampWithTimeZone", "CurrentTimestamp"]) {
    if (!javaTime.includes(required)) failures.push(`java time missing ${required}`);
  }
  for (const required of ["kotlinx.datetime", "LocalDate", "LocalTime", "LocalDateTime", "Instant", "CurrentTimestampWithTimeZone"]) {
    if (!kotlinTime.includes(required)) failures.push(`kotlin datetime missing ${required}`);
  }
  for (const required of ["DataHolder", "UserGroup", "Json.Default", "json&lt;DataHolder&gt;", "jsonb&lt;DataHolder&gt;", "extract(path)", "contains", "exists(path)", "JsonBTable"]) {
    if (!jsonFlow.includes(required)) failures.push(`json flow missing ${required}`);
  }
  if (failures.length) throw new Error(`batch10_semantic_validation_failed\n${failures.join("\n")}`);
  console.log("batch10_semantics=ok");
}

function validateConnectorGeometry() {
  const connectorClasses = "(?:arrow|mapLine|cryptoLine|dbUse|runtimeUse|inheritStem)";
  const failures = [];
  for (const diagram of diagrams) {
    const svg = fs.readFileSync(diagram.file, "utf8");
    for (const match of svg.matchAll(new RegExp(`<path\\b([^>]*class="${connectorClasses}"[^>]*)`, "g"))) {
      const pathD = (match[1].match(/d="([^"]+)"/) || [])[1];
      if (!pathD) continue;
      const points = [...pathD.matchAll(/[ML]([\d.]+),([\d.]+)/g)].map((m) => ({ x: Number(m[1]), y: Number(m[2]) }));
      if (points.length < 2) continue;
      for (let i = 0; i < points.length - 1; i++) {
        const a = points[i];
        const b = points[i + 1];
        if (a.x !== b.x && a.y !== b.y) failures.push(`${diagram.file}: diagonal connector ${a.x},${a.y}->${b.x},${b.y}`);
      }
    }
  }
  if (failures.length) throw new Error(`batch10_connector_validation_failed\n${failures.join("\n")}`);
  console.log("batch10_connectors=ok");
}
