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
    file: `${outDir}/06-advanced-06-custom-columns-class-02.svg`,
    title: "Custom Column Class and Table Model",
    subtitle: "Custom column tests share Exposed test bases, T1 tables, IntEntity DAO classes, codecs, and embeddable payloads",
    width: 1560,
    height: 960,
    body: customColumnsClass,
  },
  {
    file: `${outDir}/06-advanced-07-custom-entities-class-01.svg`,
    title: "Custom ID Entity Strategy Model",
    subtitle: "Each custom ID example pairs a generated-ID table base with its matching entity base and source-local T1/E1 types",
    width: 1600,
    height: 980,
    body: customEntitiesClass,
  },
  {
    file: `${outDir}/06-advanced-07-custom-entities-architecture-02.svg`,
    title: "Custom ID Generation Flow",
    subtitle: "Custom entity tests exercise Timebased UUID, Base62, Snowflake, KSUID, and KSUID millis IDs through DSL and DAO paths",
    width: 1560,
    height: 940,
    body: customEntitiesArchitecture,
  },
  {
    file: `${outDir}/06-advanced-08-exposed-jackson-erd-01.svg`,
    title: "Jackson JSON Table Storage Model",
    subtitle: "JacksonSchema defines JSON/JSONB tables and array tables backed by Jackson serializers, without relational FKs",
    width: 1500,
    height: 900,
    body: jacksonErd,
  },
  {
    file: `${outDir}/06-advanced-08-exposed-jackson-architecture-02.svg`,
    title: "Jackson JSON Query Flow",
    subtitle: "Jackson and JacksonB tests serialize DataHolder models, persist JSON columns, and query nested data with extract/contains/exists",
    width: 1560,
    height: 940,
    body: jacksonArchitecture,
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
validateBatch12Semantics();
validateConnectorGeometry();

function writeGraphvizSketches() {
  fs.mkdirSync(sketchDir, { recursive: true });
  const sketches = {
    "batch-12-custom-columns-class": `digraph G {
      graph [rankdir=TB, splines=ortho, nodesep=0.55, ranksep=0.75, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      AbstractExposedTest -> tests;
      IntIdTable -> T1_tables;
      IntEntity -> E1_entities;
      T1_tables -> E1_entities;
      codecs -> T1_tables;
    }`,
    "batch-12-custom-entities": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=0.55, ranksep=0.75, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      id_strategy -> table_base -> T1 -> entity_base -> E1 -> db;
    }`,
    "batch-12-jackson": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=0.55, ranksep=0.75, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      DataHolder -> JacksonSerializer -> jackson_columns -> JacksonSchemaTables -> query_ops -> db;
    }`,
  };
  for (const [name, dot] of Object.entries(sketches)) {
    const dotPath = `${sketchDir}/${name}.dot`;
    fs.writeFileSync(dotPath, dot);
    spawnSync(DOT, ["-Tsvg", "-o", `${sketchDir}/${name}.svg`, dotPath], { encoding: "utf8" });
    spawnSync(DOT, ["-Tplain", "-o", `${sketchDir}/${name}.plain`, dotPath], { encoding: "utf8" });
  }
}

function customColumnsClass() {
  let b = "";
  b += panel(58, 130, 618, 740, "Test hierarchy");
  b += umlBox(246, 156, 230, 94, "AbstractExposedTest", ["withTables(testDB, ...)"], ["fixture base"], 7);
  const tests = [
    [80, 360, "CompressedBinaryColumnTypeTest", ["compressedBinary", "BINARY(4096)"], 0],
    [332, 360, "CompressedBlobColumnTypeTest", ["compressedBlob", "BLOB"], 3],
    [80, 510, "BinarySerializedBinaryTest", ["binarySerializedBinary", "LZ4/Zstd serializers"], 4],
    [332, 510, "BinarySerializedBlobTest", ["binarySerializedBlob", "Fory/Kryo payloads"], 1],
    [206, 666, "CustomClientDefaultTest", ["timebased / snowflake", "ksuid / ksuidMillis"], 2],
  ];
  b += hollowTriangle(361, 250);
  b += path("M361,266 L361,306", "inheritStem");
  b += path("M68,306 L660,306", "inheritStem");
  b += path("M200,306 L200,360", "inheritStem");
  b += path("M452,306 L452,360", "inheritStem");
  b += path("M68,306 L68,490 L200,490 L200,510", "inheritStem");
  b += path("M660,306 L660,490 L452,490 L452,510", "inheritStem");
  b += path("M660,306 L660,646 L326,646 L326,666", "inheritStem");
  tests.forEach(([x, y, title, fields, c]) => (b += umlBox(x, y, 240, 96, title, fields, [], c)));

  b += panel(744, 130, 744, 740, "Tables, DAO, and payloads");
  b += umlBox(844, 156, 210, 94, "IntIdTable", ["id: EntityID<Int>"], ["table base"], 1);
  b += umlBox(770, 326, 210, 112, "Compression T1", ["lz4", "snappy", "zstd"], ["binary/blob columns"], 0);
  b += umlBox(1010, 326, 210, 112, "Serialized T1", ["name", "lz4Fory / lz4Kryo", "zstdFory / zstdKryo"], [], 4);
  b += umlBox(1250, 326, 210, 112, "ClientGenerated", ["timebasedUuid", "snowflake", "ksuid"], [], 2);
  b += generalizationTree(949, 250, 294, 326, [875, 1115, 1355]);

  b += umlBox(1010, 510, 210, 94, "IntEntity", ["EntityID<Int>"], ["DAO base"], 7);
  b += umlBox(770, 664, 210, 86, "E1", ["lz4Data", "snappyData", "zstdData"], [], 5);
  b += umlBox(1010, 664, 210, 86, "E1", ["name", "serialized fields"], [], 6);
  b += umlBox(1250, 664, 210, 86, "ClientGeneratedEntity", ["generated id fields"], [], 5);
  b += generalizationTree(1115, 604, 632, 664, [875, 1115, 1355]);
  b += path("M875,438 L845,438 L845,644 L875,644 L875,664", "mapLine");
  b += path("M1115,438 L1115,480 L990,480 L990,644 L1115,644 L1115,664", "mapLine");
  b += path("M1355,438 L1385,438 L1385,644 L1355,644 L1355,664", "mapLine");
  b += umlBox(800, 786, 260, 64, "Embeddable", ["name, age, address"], [], 6);
  b += umlBox(1130, 786, 260, 64, "Embeddable2", ["name, age, address, zipcode"], [], 3);
  b += path("M1115,438 L930,786", "runtimeUse", "M1115,438 L1240,438 L1240,764 L930,764 L930,786");
  b += path("M1115,438 L1260,786", "runtimeUse", "M1115,438 L1240,438 L1240,764 L1260,764 L1260,786");
  b += note(148, 884, 1260, "Source check: compression, binary serialization, and generated client-default tests each define T1 tables and DAO entities.");
  return b;
}

function customEntitiesClass() {
  let b = "";
  b += panel(58, 130, 1484, 670, "Generated ID strategies");
  b += umlBox(116, 160, 240, 82, "AbstractExposedTest", ["base fixture"], [], 7);
  b += umlBox(116, 304, 240, 82, "AbstractCustomIdTableTest", ["GET_TESTDB_AND_ENTITY_COUNT"], [], 0);
  b += generalizationTree(236, 242, 272, 304, [236]);

  const rows = [
    [386, "Timebased UUID", "TimebasedUUIDTable", "t_timebased_uuid", "TimebasedUUIDEntity", "TimebasedUUIDEntityID", 0],
    [610, "UUID Base62", "TimebasedUUIDBase62Table", "t_timebased_uuid_base62", "TimebasedUUIDBase62Entity", "Base62EntityID", 1],
    [834, "Snowflake", "SnowflakeIdTable", "t_snowflake", "SnowflakeIdEntity", "SnowflakeIdEntityID", 2],
    [1058, "KSUID", "KsuidTable", "t_ksuid", "KsuidEntity", "KsuidEntityID", 3],
    [1282, "KSUID millis", "KsuidMillisTable", "t_ksuid_millis", "KsuidMillisEntity", "EntityID<String>", 4],
  ];
  for (const [x, label, tableBase, tableName, entityBase, idType, c] of rows) {
    b += umlBox(x, 156, 204, 108, tableBase, [idType], ["table base"], c);
    b += umlBox(x, 336, 204, 104, "T1", [tableName, "name", "age"], [], c);
    b += generalizationTree(x + 102, 264, 300, 336, [x + 102]);
    b += umlBox(x, 514, 204, 110, entityBase, [idType], ["entity base"], (c + 5) % 8);
    b += umlBox(x, 690, 204, 94, "E1", ["name", "age"], [], (c + 5) % 8);
    b += generalizationTree(x + 102, 624, 660, 690, [x + 102]);
  }
  b += umlBox(116, 498, 240, 126, "Test methods", ["DSL insert/select", "DAO new/find", "batch insert", "concurrent DAO"], [], 6);
  b += path("M356,561 L386,388", "runtimeUse", "M356,561 L370,561 L370,388 L386,388");
  b += note(148, 860, 1260, "Every strategy has the same local source shape: test class -> T1 custom ID table -> E1 custom ID entity -> Record(name, age).");
  return b;
}

function customEntitiesArchitecture() {
  let b = "";
  b += panel(58, 130, 310, 640, "ID strategy");
  const strategies = [
    [106, 180, "Timebased UUID", "UUID", 0],
    [106, 278, "UUID Base62", "24-char string", 1],
    [106, 376, "Snowflake", "Long", 2],
    [106, 474, "KSUID", "27-char string", 3],
    [106, 572, "KSUID millis", "time sortable", 4],
  ];
  strategies.forEach(([x, y, title, detail, c]) => (b += card(x, y, 214, 58, title, c, detail)));

  b += panel(444, 130, 306, 640, "Tests");
  b += card(492, 178, 210, 66, "DSL path", 0, "T1 insert/select");
  b += card(492, 306, 210, 66, "DAO path", 5, "E1 new/find");
  b += card(492, 434, 210, 66, "Batch insert", 6, "insertIgnore");
  b += card(492, 562, 210, 66, "Concurrent DAO", 1, "async new/find");

  b += panel(824, 130, 310, 640, "Custom tables");
  b += erdTable(862, 178, 232, "T1 tables", ["id custom generated", "name VARCHAR(255)", "age INT", "five table variants"], 2, 154);
  b += erdTable(862, 474, 232, "Record", ["name: String", "age: Int"], 6, 110);

  b += panel(1204, 130, 184, 640, "DAO entities");
  b += umlBox(1226, 230, 140, 116, "E1", ["custom id", "name", "age"], [], 5);
  b += umlBox(1226, 500, 140, 116, "EntityClass", ["matching table"], [], 7);
  b += cylinder(1420, 360, 104, 190, "DB", ["custom PK", "name", "age"], 3);

  strategies.forEach(([, y]) => (b += path(`M320,${y + 29} L492,211`, "arrow", `M320,${y + 29} L404,${y + 29} L404,211 L492,211`)));
  b += path("M702,211 L862,255", "mapLine", "M702,211 L782,211 L782,255 L862,255");
  b += path("M702,339 L862,255", "mapLine", "M702,339 L782,339 L782,255 L862,255");
  b += path("M702,467 L862,530", "mapLine", "M702,467 L782,467 L782,530 L862,530");
  b += path("M1094,255 L1226,288", "mapLine", "M1094,255 L1160,255 L1160,288 L1226,288");
  b += path("M1094,530 L1226,558", "runtimeUse", "M1094,530 L1160,530 L1160,558 L1226,558");
  b += path("M1366,288 L1420,420", "dbUse", "M1366,288 L1394,288 L1394,420 L1420,420");
  b += path("M1366,558 L1472,550", "dbUse", "M1366,558 L1394,558 L1394,586 L1472,586 L1472,550");
  b += note(128, 838, 1196, "Source check: five custom ID table tests share DSL, DAO, batch, and concurrent creation paths over T1/E1 records.");
  return b;
}

function jacksonErd() {
  let b = "";
  b += panel(58, 130, 870, 600, "JacksonSchema tables");
  b += erdTable(106, 186, 300, "JacksonTable", ["id PK", "jackson_column JSON NOT NULL", "builder: jackson<DataHolder>"], 0, 152);
  b += erdTable(520, 186, 300, "JacksonBTable", ["id PK", "jackson_b_column JSONB", "builder: jacksonb<DataHolder>"], 4, 152);
  b += erdTable(106, 476, 300, "JacksonArrayTable", ["id PK", "groups JSON NOT NULL", "numbers JSON NOT NULL"], 2, 152);
  b += erdTable(520, 476, 300, "JacksonBArrayTable", ["id PK", "groups JSONB NOT NULL", "numbers JSONB NOT NULL"], 3, 152);
  b += panel(1010, 130, 350, 600, "Payloads");
  b += umlBox(1060, 196, 250, 126, "DataHolder", ["user: User", "logins: Int", "active: Boolean", "team: String?"], [], 2);
  b += umlBox(1060, 394, 250, 92, "User", ["name: String", "team: String?"], [], 0);
  b += umlBox(1060, 562, 250, 92, "UserGroup", ["users: List<User>"], [], 1);
  b += path("M406,262 L1060,244", "mapLine", "M406,262 L466,262 L466,154 L946,154 L946,244 L1060,244");
  b += path("M820,262 L1060,276", "mapLine", "M820,262 L946,262 L946,276 L1060,276");
  b += path("M406,552 L1060,608", "mapLine", "M406,552 L466,552 L466,690 L946,690 L946,608 L1060,608");
  b += path("M820,552 L1060,632", "mapLine", "M820,552 L946,552 L946,632 L1060,632");
  b += path("M1185,322 L1185,394", "runtimeUse");
  b += path("M1185,562 L1185,486", "runtimeUse");
  b += note(126, 794, 1188, "No FK lines are drawn: Jackson tables store object graphs as JSON/JSONB payloads, not relational rows.");
  return b;
}

function jacksonArchitecture() {
  let b = "";
  b += panel(58, 130, 300, 640, "Tests");
  b += card(96, 184, 224, 70, "JacksonColumnTest", 0, "JSON column");
  b += card(96, 352, 224, 70, "JacksonBColumnTest", 4, "JSONB column");
  b += card(96, 520, 224, 70, "JacksonSchema", 7, "shared fixtures");

  b += panel(430, 130, 318, 640, "Serialization");
  b += card(474, 194, 230, 66, "DataHolder", 2, "User + state");
  b += card(474, 332, 230, 66, "DefaultJacksonSerializer", 6, "serialize / deserialize");
  b += card(474, 470, 230, 66, "jackson / jacksonb", 1, "ColumnType builders");
  b += card(474, 608, 230, 66, "transform/default", 5, "nullable / generated");

  b += panel(824, 130, 300, 640, "Tables");
  b += erdTable(864, 176, 220, "JacksonTable", ["jackson_column JSON", "JacksonEntity"], 0, 126);
  b += erdTable(864, 346, 220, "JacksonBTable", ["jackson_b_column JSONB", "JacksonBEntity"], 4, 126);
  b += erdTable(864, 536, 220, "Array tables", ["groups JSON/JSONB", "numbers JSON/JSONB"], 2, 132);

  b += panel(1194, 130, 204, 640, "Query DSL");
  b += card(1224, 184, 144, 58, "insert/update", 2, "round trip");
  b += card(1224, 312, 144, 58, "extract", 1, "path slice");
  b += card(1224, 440, 144, 58, "contains", 4, "JSON predicate");
  b += card(1224, 568, 144, 58, "exists", 6, "path exists");
  b += cylinder(1428, 348, 104, 218, "DB", ["Postgres", "MySQL V8", "H2 JSON"], 3);

  b += path("M320,219 L474,227", "arrow", "M320,219 L398,219 L398,227 L474,227");
  b += path("M320,387 L474,365", "arrow", "M320,387 L398,387 L398,365 L474,365");
  b += path("M704,227 L864,239", "mapLine", "M704,227 L784,227 L784,239 L864,239");
  b += path("M704,365 L864,409", "mapLine", "M704,365 L784,365 L784,409 L864,409");
  b += path("M704,503 L864,602", "mapLine", "M704,503 L784,503 L784,602 L864,602");
  b += path("M1084,239 L1224,213", "arrow", "M1084,239 L1160,239 L1160,213 L1224,213");
  b += path("M1084,409 L1224,341", "arrow", "M1084,409 L1160,409 L1160,341 L1224,341");
  b += path("M1084,409 L1224,469", "codecLine", "M1084,409 L1160,409 L1160,469 L1224,469");
  b += path("M1084,602 L1224,597", "runtimeUse", "M1084,602 L1160,602 L1160,597 L1224,597");
  b += path("M1368,213 L1428,414", "dbUse", "M1368,213 L1404,213 L1404,414 L1428,414");
  b += path("M1368,341 L1428,448", "dbUse", "M1368,341 L1404,341 L1404,448 L1428,448");
  b += path("M1368,469 L1428,482", "dbUse", "M1368,469 L1404,469 L1404,482 L1428,482");
  b += path("M1368,597 L1480,566", "dbUse", "M1368,597 L1404,597 L1404,604 L1480,604 L1480,566");
  b += note(128, 838, 1196, "Source check: JacksonColumnTest and JacksonBColumnTest cover insert/update, extract, contains, exists, arrays, defaults, upsert, and transform.");
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
  const titleLines = splitTitle(title, Math.max(10, Math.floor(w / 9)), 2);
  const headerH = titleLines.length > 1 ? 56 : 40;
  const split1 = y + headerH;
  const bodyH = h - headerH;
  const hasMethods = methods.length > 0;
  const fieldH = hasMethods ? Math.max(34, Math.floor(bodyH * 0.58)) : bodyH;
  const methodH = hasMethods ? bodyH - fieldH : 0;
  const split2 = split1 + fieldH;
  let out = `<desc>${esc(title)}</desc>
<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="4" fill="#fff" stroke="${stroke}" class="card"/>
<path d="M${x},${split1} L${x},${y + 4} Q${x},${y} ${x + 4},${y} L${x + w - 4},${y} Q${x + w},${y} ${x + w},${y + 4} L${x + w},${split1} Z" fill="${fill}"/>
<line x1="${x}" y1="${split1}" x2="${x + w}" y2="${split1}" stroke="${stroke}" stroke-width="1.2"/>
`;
  const firstTitleY = titleLines.length > 1 ? y + 22 : y + 26;
  titleLines.forEach((line, i) => (out += `<text x="${x + w / 2}" y="${firstTitleY + i * 18}" class="label" text-anchor="middle">${esc(line)}</text>\n`));
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
  rows.forEach((row, i) => (out += `<text x="${x + 16}" y="${y + 68 + i * 18}" class="tiny">${esc(row)}</text>\n`));
  return out;
}

function cylinder(x, y, w, h, title, rows, c) {
  const [fill, stroke] = colors[c % colors.length];
  const capH = 34;
  let out = `<path d="M${x},${y + capH / 2} C${x},${y - 4} ${x + w},${y - 4} ${x + w},${y + capH / 2} L${x + w},${y + h - capH / 2} C${x + w},${y + h + 4} ${x},${y + h + 4} ${x},${y + h - capH / 2} Z" fill="#fff" stroke="${stroke}" stroke-width="1.7" class="card"/>
<ellipse cx="${x + w / 2}" cy="${y + capH / 2}" rx="${w / 2}" ry="${capH / 2}" fill="${fill}" stroke="${stroke}" stroke-width="1.7"/>
<path d="M${x},${y + h - capH / 2} C${x},${y + h + 4} ${x + w},${y + h + 4} ${x + w},${y + h - capH / 2}" fill="none" stroke="${stroke}" stroke-width="1.7"/>
<text x="${x + w / 2}" y="${y + 48}" class="label" text-anchor="middle">${esc(title)}</text>\n`;
  rows.forEach((row, i) => (out += `<text x="${x + 14}" y="${y + 80 + i * 18}" class="tiny">${esc(row)}</text>\n`));
  return out;
}

function generalizationTree(parentX, parentBottomY, busY, childTopY, childXs) {
  const baseY = parentBottomY + 16;
  let out = `${hollowTriangle(parentX, parentBottomY)}<path d="M${parentX},${baseY} L${parentX},${busY}" class="inheritStem"/>\n`;
  out += `<path d="M${Math.min(...childXs)},${busY} L${Math.max(...childXs)},${busY}" class="inheritStem"/>\n`;
  for (const childX of childXs) out += `<path d="M${childX},${busY} L${childX},${childTopY}" class="inheritStem"/>\n`;
  return out;
}

function hollowTriangle(parentX, parentBottomY) {
  const baseY = parentBottomY + 16;
  return `<polygon points="${parentX},${parentBottomY} ${parentX - 9},${baseY} ${parentX + 9},${baseY}" fill="#fff" stroke="#475569" stroke-width="1.8"/>\n`;
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

function esc(value) {
  return String(value).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

function validateBatch12Semantics() {
  const [customColumns, customEntitiesClassSvg, customEntitiesArch, jacksonErdSvg, jacksonArch] = diagrams.map((d) => fs.readFileSync(d.file, "utf8"));
  const failures = [];
  for (const required of ["CompressedBinaryColumnTypeTest", "CompressedBlobColumnTypeTest", "BinarySerializedBinaryTest", "ClientGeneratedEntity", "Embeddable2"]) {
    if (!customColumns.includes(required)) failures.push(`custom columns missing ${required}`);
  }
  for (const required of ["TimebasedUUIDTable", "SnowflakeIdTable", "KsuidTable", "KsuidMillisTable", "T1", "E1", "Record(name, age)"]) {
    if (!customEntitiesClassSvg.includes(required)) failures.push(`custom entities class missing ${required}`);
  }
  for (const required of ["Timebased UUID", "UUID Base62", "Snowflake", "KSUID millis", "custom PK"]) {
    if (!customEntitiesArch.includes(required)) failures.push(`custom entities arch missing ${required}`);
  }
  for (const required of ["JacksonTable", "JacksonBTable", "JacksonArrayTable", "JacksonBArrayTable", "jackson&lt;DataHolder&gt;", "jacksonb&lt;DataHolder&gt;"]) {
    if (!jacksonErdSvg.includes(required)) failures.push(`jackson erd missing ${required}`);
  }
  for (const required of ["JacksonColumnTest", "JacksonBColumnTest", "DefaultJacksonSerializer", "extract", "contains", "exists", "Postgres", "MySQL V8"]) {
    if (!jacksonArch.includes(required)) failures.push(`jackson arch missing ${required}`);
  }
  if (failures.length) throw new Error(`batch12_semantic_validation_failed\n${failures.join("\n")}`);
  console.log("batch12_semantics=ok");
}

function validateConnectorGeometry() {
  const connectorClasses = "(?:arrow|mapLine|codecLine|dbUse|runtimeUse|inheritStem)";
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
  if (failures.length) throw new Error(`batch12_connector_validation_failed\n${failures.join("\n")}`);
  console.log("batch12_connectors=ok");
}
