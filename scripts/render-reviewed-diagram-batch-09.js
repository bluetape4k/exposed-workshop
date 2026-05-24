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
.fkLine{fill:none;stroke:#0f766e;stroke-width:1.9;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowTeal)}
.runtimeUse{fill:none;stroke:#7c3aed;stroke-width:1.9;stroke-dasharray:6 5;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowPurple)}
.dbUse{fill:none;stroke:#ea580c;stroke-width:2;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowOrange)}
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
    file: `${outDir}/05-exposed-dml-05-entities-class-01.svg`,
    title: "Exposed Entity Mapping Model",
    subtitle: "EntityTestData maps IdTable/IntIdTable columns to DAO entities while hooks and cache observe transaction changes",
    width: 1500,
    height: 900,
    body: entityMappingModel,
  },
  {
    file: `${outDir}/05-exposed-dml-05-entities-erd-02.svg`,
    title: "XEntity and YEntity Table ERD",
    subtitle: "EntityTestData.XTable stores an optional y1 foreign key to EntityTestData.YTable.uuid",
    width: 1200,
    height: 720,
    body: xyEntityErd,
  },
  {
    file: `${outDir}/05-exposed-dml-05-entities-class-03.svg`,
    title: "Exposed Entity and Table Base Hierarchy",
    subtitle: "Source examples use superclass-first DAO and table bases for Int, Long, UUID, manual, and composite key strategies",
    width: 1600,
    height: 940,
    body: entityHierarchy,
  },
  {
    file: `${outDir}/06-advanced-architecture-01.svg`,
    title: "Advanced Exposed Extension Architecture",
    subtitle: "Current 06-advanced modules extend Table columns, Entity IDs, serializers, and encryption adapters before SQL reaches databases",
    width: 1550,
    height: 900,
    body: advancedArchitecture,
  },
  {
    file: `${outDir}/06-advanced-architecture-02.svg`,
    title: "Advanced Module Classification",
    subtitle: "Existing source directories group advanced examples by security, value types, JSON serialization, and custom extension points",
    width: 1500,
    height: 900,
    body: advancedClassification,
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
validateBatch09Semantics();
validateConnectorGeometry();

function writeGraphvizSketches() {
  fs.mkdirSync(sketchDir, { recursive: true });
  const sketches = {
    "batch-09-entity-mapping": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=0.65, ranksep=0.9, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      ytable -> yentity;
      xtable -> xentity;
      xtable -> ytable;
      entityclass -> xentity;
      transaction -> entitycache -> db;
      transaction -> entityhook;
    }`,
    "batch-09-xy-erd": `digraph G {
      graph [rankdir=BT, splines=ortho, nodesep=0.8, ranksep=0.9, pad=0.2];
      node [shape=record, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      xtable -> ytable [label="y1 -> uuid"];
    }`,
    "batch-09-entity-hierarchy": `digraph G {
      graph [rankdir=BT, splines=ortho, nodesep=0.55, ranksep=0.85, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      xentity -> intentity -> entity;
      city -> longentity -> entity;
      uuid_person -> uuidentity -> entity;
      composite -> compositeentity -> entity;
      xtable -> intidtable -> idtable -> table;
      cities -> longidtable -> idtable;
      uuid_table -> uuidtable -> idtable;
      composite_table -> compositeidtable -> table;
    }`,
    "batch-09-advanced-architecture": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=0.55, ranksep=0.85, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      modules -> table_dsl -> column_type -> generated_sql -> database;
      serializers -> column_type;
      crypto -> column_type;
      custom_ids -> entity_api;
      entity_api -> table_dsl;
    }`,
    "batch-09-advanced-classification": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=0.55, ranksep=0.85, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      security -> encrypted_columns;
      temporal_value -> typed_columns;
      json -> serializer_columns;
      custom_extensions -> id_and_column_types;
    }`,
  };
  for (const [name, dot] of Object.entries(sketches)) {
    const dotPath = `${sketchDir}/${name}.dot`;
    fs.writeFileSync(dotPath, dot);
    spawnSync(DOT, ["-Tsvg", "-o", `${sketchDir}/${name}.svg`, dotPath], { encoding: "utf8" });
    spawnSync(DOT, ["-Tplain", "-o", `${sketchDir}/${name}.plain`, dotPath], { encoding: "utf8" });
  }
}

function entityMappingModel() {
  let b = "";
  b += panel(58, 130, 370, 610, "Table definitions");
  b += erdTable(92, 188, 292, "YTable", ["uuid PK varchar(24)", "x bool default true"], 0, 132);
  b += erdTable(92, 420, 292, "XTable", ["id PK serial", "b1 bool default true", "b2 bool default false", "y1 FK nullable"], 1, 154);
  b += card(116, 624, 244, 70, "SchemaUtils.withTables", 7, "create / drop fixtures");

  b += panel(492, 130, 430, 610, "DAO entity layer");
  b += umlBox(540, 178, 330, 152, "YEntity", ["EntityID<String>", "var x by YTable.x"], ["EntityClass<YEntity>(YTable)"], 2);
  b += umlBox(540, 430, 330, 172, "XEntity", ["EntityID<Int>", "var b1 by XTable.b1", "var b2 by XTable.b2"], ["IntEntityClass<XEntity>(XTable)"], 5);
  b += card(590, 650, 230, 58, "AEntity/BEntity", 6, "factory subclasses");

  b += panel(990, 130, 390, 610, "Transaction behavior");
  b += card(1040, 184, 260, 72, "EntityClass", 3, "new / find / all");
  b += card(1040, 314, 260, 72, "EntityHook", 4, "Created / Updated / Removed");
  b += card(1040, 444, 260, 72, "EntityCache", 1, "maxEntitiesToStore");
  b += cylinder(1096, 584, 150, 122, "DB", ["H2", "Postgres", "MySQL"], 3);

  b += path("M384,254 L540,254", "mapLine");
  b += path("M384,497 L540,516", "mapLine", "M384,497 L462,497 L462,516 L540,516");
  b += path("M238,420 L238,320", "fkLine");
  b += path("M870,254 L1040,220", "arrow", "M870,254 L948,254 L948,220 L1040,220");
  b += path("M870,516 L1040,220", "arrow", "M870,516 L950,516 L950,220 L1040,220");
  b += path("M1170,256 L1170,314", "runtimeUse");
  b += path("M1170,386 L1170,444", "runtimeUse");
  b += path("M1170,516 L1170,584", "dbUse");
  b += note(118, 796, 1264, "Source check: EntityTestData defines YTable/XTable, XEntity/YEntity mappings; Ex02 tracks EntityHook changes and Ex03 verifies EntityCache sizing.");
  return b;
}

function xyEntityErd() {
  let b = "";
  b += panel(86, 134, 1028, 430, "EntityTestData tables");
  b += erdTable(172, 190, 330, "YTable", ["uuid PK varchar(24)", "x bool default true", "primaryKey(uuid)"], 0, 166);
  b += erdTable(690, 342, 330, "XTable", ["id PK serial", "b1 bool default true", "b2 bool default false", "y1 FK nullable -> YTable.uuid"], 1, 184);
  b += path("M690,434 L502,278", "fkLine", "M690,434 L596,434 L596,278 L502,278");
  b += labelPill(552, 392, "optional FK y1 -> uuid", 142);
  b += card(172, 426, 330, 70, "YEntity", 2, "EntityID<String>");
  b += card(690, 218, 330, 70, "XEntity", 5, "IntEntity");
  b += path("M337,356 L337,426", "mapLine");
  b += path("M855,342 L855,288", "mapLine");
  b += note(112, 610, 976, "ERD rule: FK direction is child table to parent table. Entity mapping lines stay local and do not cross the FK relationship lane.");
  return b;
}

function entityHierarchy() {
  let b = "";
  b += panel(58, 130, 708, 690, "DAO entity bases");
  b += umlBox(310, 174, 210, 90, "Entity<ID>", ["id: EntityID<ID>"], ["base DAO entity"], 7);
  b += umlBox(88, 338, 154, 86, "IntEntity", ["EntityID<Int>"], [], 0);
  b += umlBox(258, 338, 154, 86, "LongEntity", ["EntityID<Long>"], [], 2);
  b += umlBox(428, 338, 154, 86, "UUIDEntity", ["EntityID<UUID>"], [], 4);
  b += umlBox(598, 338, 154, 86, "CompositeEntity", ["CompositeID"], [], 6);
  b += card(92, 674, 190, 66, "XEntity/TestEntity", 0, "Int examples");
  b += card(304, 674, 190, 66, "City/Person", 2, "Long examples");
  b += card(516, 674, 190, 66, "UUID Person", 4, "UUID example");

  b += panel(834, 130, 708, 690, "Table bases");
  b += umlBox(1084, 174, 210, 116, "Table", ["columns", "primaryKey", "foreign keys / indexes"], [], 7);
  b += umlBox(900, 348, 210, 90, "IdTable<ID>", ["id: Column<EntityID<ID>>"], [], 1);
  b += umlBox(1220, 348, 238, 90, "CompositeIdTable", ["addIdColumn", "entityId"], [], 6);
  b += umlBox(866, 560, 178, 86, "IntIdTable", ["serial id"], [], 0);
  b += umlBox(1062, 560, 178, 86, "LongIdTable", ["long id"], [], 2);
  b += umlBox(1258, 560, 178, 86, "UUIDTable", ["uuid id"], [], 4);

  b += generalizationTree(415, 264, 304, 338, [165, 335, 505, 675]);
  b += generalizationTree(1128, 290, 322, 348, [1005, 1339]);
  b += generalizationTree(1005, 438, 500, 560, [956, 1151, 1347]);
  b += note(156, 854, 1288, "UML rule: superclass boxes are above concrete examples, and hollow generalization triangles point upward to the parent boundary with vertical stems.");
  return b;
}

function advancedArchitecture() {
  let b = "";
  b += panel(58, 130, 286, 590, "Current modules");
  b += card(92, 182, 218, 58, "Security", 4, "01 crypt / 12 tink");
  b += card(92, 274, 218, 58, "Time and Money", 6, "02 / 03 / 05");
  b += card(92, 366, 218, 58, "JSON Serialization", 1, "04 / 08 / 09 / 11");
  b += card(92, 458, 218, 58, "Custom Extensions", 5, "06 columns / 07 ids");

  b += panel(420, 130, 338, 590, "Exposed extension point");
  b += card(468, 184, 242, 70, "Table column DSL", 0, "registerColumn()");
  b += card(468, 326, 242, 70, "ColumnType", 2, "serialize / parse");
  b += card(468, 468, 242, 70, "Entity ID strategy", 6, "KSUID / Snowflake / UUID");
  b += card(468, 606, 242, 58, "Entity API", 7, "custom IdTable");

  b += panel(828, 130, 330, 590, "Generated persistence");
  b += card(874, 194, 238, 64, "DDL columns", 3, "varchar / jsonb / binary");
  b += card(874, 326, 238, 64, "DML values", 5, "round-trip tests");
  b += card(874, 458, 238, 64, "Path queries", 1, "extract / contains");
  b += card(874, 590, 238, 64, "Default expressions", 6, "date/time/current");

  b += panel(1244, 130, 216, 590, "Database");
  b += cylinder(1288, 250, 128, 230, "DB", ["H2", "Postgres", "MySQL", "MariaDB"], 3);

  b += path("M310,211 L468,219", "arrow", "M310,211 L388,211 L388,219 L468,219");
  b += path("M310,303 L468,361", "arrow", "M310,303 L388,303 L388,361 L468,361");
  b += path("M310,395 L468,361", "arrow", "M310,395 L388,395 L388,361 L468,361");
  b += path("M310,487 L468,503", "arrow", "M310,487 L388,487 L388,503 L468,503");
  b += path("M710,219 L874,226", "mapLine", "M710,219 L790,219 L790,226 L874,226");
  b += path("M710,361 L874,358", "mapLine", "M710,361 L790,361 L790,358 L874,358");
  b += path("M710,503 L874,622", "mapLine", "M710,503 L790,503 L790,622 L874,622");
  b += path("M1112,226 L1288,326", "dbUse", "M1112,226 L1190,226 L1190,326 L1288,326");
  b += path("M1112,358 L1288,358", "dbUse");
  b += path("M1112,490 L1288,390", "dbUse", "M1112,490 L1190,490 L1190,390 L1288,390");
  b += note(108, 796, 1336, "Source check: module directories present are 01,02,03,04,05,06,07,08,09,11,12; stale README-only entries are not treated as source-owned modules.");
  return b;
}

function advancedClassification() {
  let b = "";
  b += panel(64, 132, 642, 608, "Problem domains");
  b += card(106, 190, 244, 78, "Protect sensitive data", 4, "encrypted columns");
  b += card(106, 326, 244, 78, "Represent value types", 6, "time / money");
  b += card(106, 462, 244, 78, "Persist structured data", 1, "JSON documents");
  b += card(106, 598, 244, 78, "Extend identity / columns", 5, "custom ID and types");
  b += card(414, 190, 244, 78, "01 exposed-crypt", 4, "encryptedVarchar/Binary");
  b += card(414, 290, 244, 78, "12 exposed-tink", 3, "AEAD/DAEAD");
  b += card(414, 410, 244, 78, "02/03 date-time", 6, "Java + Kotlin time");
  b += card(414, 510, 244, 78, "05 exposed-money", 2, "amount + currency");

  b += panel(794, 132, 642, 608, "Implementation mechanisms");
  b += card(836, 190, 244, 78, "04 exposed-json", 1, "json/jsonb paths");
  b += card(836, 290, 244, 78, "08/09/11 serializers", 0, "Jackson/Fastjson");
  b += card(836, 410, 244, 78, "06 custom-columns", 5, "binary/blob/string");
  b += card(836, 510, 244, 78, "07 custom-entities", 6, "KSUID/Snowflake/UUID");
  b += card(1150, 260, 220, 78, "ColumnType", 7, "database conversion");
  b += card(1150, 460, 220, 78, "EntityID", 7, "typed primary keys");

  b += path("M350,229 L414,229", "arrow");
  b += path("M350,365 L414,449", "arrow", "M350,365 L382,365 L382,449 L414,449");
  b += path("M350,501 L836,229", "arrow", "M350,501 L736,501 L736,229 L836,229");
  b += path("M350,637 L836,449", "arrow", "M350,637 L736,637 L736,449 L836,449");
  b += path("M658,229 L1150,299", "runtimeUse", "M658,229 L730,229 L730,299 L1150,299");
  b += path("M1080,229 L1150,299", "runtimeUse", "M1080,229 L1116,229 L1116,299 L1150,299");
  b += path("M1080,449 L1150,499", "runtimeUse", "M1080,449 L1116,449 L1116,499 L1150,499");
  b += note(116, 796, 1268, "Classification rule: modules are grouped by current source directories and tested extension mechanisms, not by stale or missing chapter rows.");
  return b;
}

function shell(diagram) {
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${diagram.width} ${diagram.height}" width="${diagram.width}" height="${diagram.height}" role="img" aria-label="${esc(diagram.title)}">
<defs>
<filter id="shadow" x="-10%" y="-10%" width="120%" height="130%"><feDropShadow dx="2" dy="4" stdDeviation="5" flood-color="#1f2937" flood-opacity="0.12"/></filter>
<marker id="arrowBlue" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto"><path d="M1,1 L8,4.5 L1,8 Z" fill="#2563eb"/></marker>
<marker id="arrowGreen" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto"><path d="M1,1 L8,4.5 L1,8 Z" fill="#16a34a"/></marker>
<marker id="arrowTeal" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto"><path d="M1,1 L8,4.5 L1,8 Z" fill="#0f766e"/></marker>
<marker id="arrowPurple" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto"><path d="M1,1 L8,4.5 L1,8 Z" fill="#7c3aed"/></marker>
<marker id="arrowOrange" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto"><path d="M1,1 L8,4.5 L1,8 Z" fill="#ea580c"/></marker>
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
  const fieldH = hasMethods ? Math.max(34, Math.floor(bodyH * 0.54)) : bodyH;
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
  rows.forEach((row, i) => (out += `<text x="${x + 18}" y="${y + 70 + i * 18}" class="tiny">${esc(row)}</text>\n`));
  return out;
}

function cylinder(x, y, w, h, title, rows, c) {
  const [fill, stroke] = colors[c % colors.length];
  const capH = 34;
  let out = `<path d="M${x},${y + capH / 2} C${x},${y - 4} ${x + w},${y - 4} ${x + w},${y + capH / 2} L${x + w},${y + h - capH / 2} C${x + w},${y + h + 4} ${x},${y + h + 4} ${x},${y + h - capH / 2} Z" fill="#fff" stroke="${stroke}" stroke-width="1.7" class="card"/>
<ellipse cx="${x + w / 2}" cy="${y + capH / 2}" rx="${w / 2}" ry="${capH / 2}" fill="${fill}" stroke="${stroke}" stroke-width="1.7"/>
<path d="M${x},${y + h - capH / 2} C${x},${y + h + 4} ${x + w},${y + h + 4} ${x + w},${y + h - capH / 2}" fill="none" stroke="${stroke}" stroke-width="1.7"/>
<text x="${x + w / 2}" y="${y + 48}" class="label" text-anchor="middle">${esc(title)}</text>\n`;
  rows.forEach((row, i) => (out += `<text x="${x + 16}" y="${y + 78 + i * 18}" class="tiny">${esc(row)}</text>\n`));
  return out;
}

function generalizeUp(childX, childTopY, parentX, parentBottomY) {
  const baseY = parentBottomY + 16;
  return `<path d="M${childX},${childTopY} L${childX},${baseY} L${parentX},${baseY}" class="inheritStem"/>
<polygon points="${parentX},${parentBottomY} ${parentX - 9},${baseY} ${parentX + 9},${baseY}" fill="#fff" stroke="#475569" stroke-width="1.8"/>\n`;
}

function generalizeUpRouted(childX, childTopY, parentX, parentBottomY) {
  const baseY = parentBottomY + 16;
  const laneY = childTopY - 22;
  return `<path d="M${childX},${childTopY} L${childX},${laneY} L${parentX},${laneY} L${parentX},${baseY}" class="inheritStem"/>
<polygon points="${parentX},${parentBottomY} ${parentX - 9},${baseY} ${parentX + 9},${baseY}" fill="#fff" stroke="#475569" stroke-width="1.8"/>\n`;
}

function generalizationTree(parentX, parentBottomY, busY, childTopY, childXs) {
  const baseY = parentBottomY + 16;
  let out = `<path d="M${parentX},${baseY} L${parentX},${busY}" class="inheritStem"/>
<polygon points="${parentX},${parentBottomY} ${parentX - 9},${baseY} ${parentX + 9},${baseY}" fill="#fff" stroke="#475569" stroke-width="1.8"/>\n`;
  out += `<path d="M${Math.min(...childXs)},${busY} L${Math.max(...childXs)},${busY}" class="inheritStem"/>\n`;
  for (const childX of childXs) {
    out += `<path d="M${childX},${busY} L${childX},${childTopY}" class="inheritStem"/>\n`;
  }
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

function validateBatch09Semantics() {
  const [mapping, erd, hierarchy, architecture, classification] = diagrams.map((d) => fs.readFileSync(d.file, "utf8"));
  const failures = [];
  for (const required of ["YTable", "XTable", "YEntity", "XEntity", "EntityHook", "EntityCache"]) {
    if (!mapping.includes(required)) failures.push(`entity mapping missing ${required}`);
  }
  for (const required of ["uuid PK", "y1 FK", "optional FK y1 -&gt; uuid"]) {
    if (!erd.includes(required)) failures.push(`ERD missing ${required}`);
  }
  for (const required of ["Entity&lt;ID&gt;", "IntEntity", "LongEntity", "UUIDEntity", "CompositeEntity", "IdTable&lt;ID&gt;", "CompositeIdTable"]) {
    if (!hierarchy.includes(required)) failures.push(`hierarchy missing ${required}`);
  }
  for (const required of ["Security", "Time and Money", "JSON Serialization", "Custom Extensions", "ColumnType", "Entity ID strategy", "DB"]) {
    if (!architecture.includes(required)) failures.push(`advanced architecture missing ${required}`);
  }
  for (const required of ["01 exposed-crypt", "12 exposed-tink", "02/03 date-time", "04 exposed-json", "08/09/11 serializers", "07 custom-entities"]) {
    if (!classification.includes(required)) failures.push(`advanced classification missing ${required}`);
  }
  if (failures.length) throw new Error(`batch09_semantic_validation_failed\n${failures.join("\n")}`);
  console.log("batch09_semantics=ok");
}

function validateConnectorGeometry() {
  const connectorClasses = "(?:arrow|mapLine|fkLine|runtimeUse|dbUse|inheritStem)";
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
  if (failures.length) throw new Error(`batch09_connector_validation_failed\n${failures.join("\n")}`);
  console.log("batch09_connectors=ok");
}
