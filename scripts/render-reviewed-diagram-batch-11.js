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
    file: `${outDir}/06-advanced-04-exposed-json-erd-02.svg`,
    title: "Exposed JSON Table Storage Model",
    subtitle: "JSON examples define four IntIdTable objects with JSON/JSONB columns and no relational foreign keys",
    width: 1500,
    height: 900,
    body: jsonErd,
  },
  {
    file: `${outDir}/06-advanced-04-exposed-json-class-03.svg`,
    title: "Exposed JSON Class and Payload Model",
    subtitle: "JsonTestData keeps table objects, DAO entities, and kotlinx.serialization payload models as separate source concepts",
    width: 1560,
    height: 940,
    body: jsonClass,
  },
  {
    file: `${outDir}/06-advanced-05-exposed-money-erd-01.svg`,
    title: "Exposed Money Composite Column ERD",
    subtitle: "compositeMoney stores MonetaryAmount as amount and currency columns with defaults, nullable variants, and an index",
    width: 1500,
    height: 900,
    body: moneyErd,
  },
  {
    file: `${outDir}/06-advanced-05-exposed-money-class-02.svg`,
    title: "Exposed Money Class and Mapping Model",
    subtitle: "Money examples map compositeMoney tables to DAO entities and test direct DSL plus default-value behavior",
    width: 1560,
    height: 940,
    body: moneyClass,
  },
  {
    file: `${outDir}/06-advanced-06-custom-columns-architecture-01.svg`,
    title: "Custom Column Storage Pipelines",
    subtitle: "Custom column examples route raw bytes, serialized objects, and generated IDs through specific column builders",
    width: 1560,
    height: 940,
    body: customColumnsArchitecture,
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
validateBatch11Semantics();
validateConnectorGeometry();

function writeGraphvizSketches() {
  fs.mkdirSync(sketchDir, { recursive: true });
  const sketches = {
    "batch-11-json": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=0.55, ranksep=0.8, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      JsonTable -> DataHolder;
      JsonBTable -> DataHolder;
      JsonArrayTable -> UserGroup;
      JsonBArrayTable -> UserGroup;
      DataHolder -> User;
      UserGroup -> User;
    }`,
    "batch-11-money": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=0.6, ranksep=0.85, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      MonetaryAmount -> compositeMoney -> amount_currency_columns -> AccountTable -> DB;
      TableWithDBDefault -> DBDefault -> DB;
      tester -> DB;
    }`,
    "batch-11-custom-columns": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=0.55, ranksep=0.8, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      compression_tests -> compressedBinaryBlob -> compressors -> compressed_T1 -> db;
      serialization_tests -> binarySerialized -> serializers -> serialized_T1 -> db;
      client_default_tests -> generated_columns -> ClientGenerated -> db;
    }`,
  };
  for (const [name, dot] of Object.entries(sketches)) {
    const dotPath = `${sketchDir}/${name}.dot`;
    fs.writeFileSync(dotPath, dot);
    spawnSync(DOT, ["-Tsvg", "-o", `${sketchDir}/${name}.svg`, dotPath], { encoding: "utf8" });
    spawnSync(DOT, ["-Tplain", "-o", `${sketchDir}/${name}.plain`, dotPath], { encoding: "utf8" });
  }
}

function jsonErd() {
  let b = "";
  b += panel(58, 130, 870, 600, "Database tables");
  b += erdTable(106, 186, 300, "JsonTable", ["id PK", "j_column JSON NOT NULL", "builder: json<DataHolder>"], 0, 152);
  b += erdTable(520, 186, 300, "JsonBTable", ["id PK", "j_b_column JSONB NOT NULL", "builder: jsonb<DataHolder>"], 4, 152);
  b += erdTable(106, 476, 300, "JsonArrayTable", ["id PK", "groups JSON NOT NULL", "numbers JSON NOT NULL"], 2, 152);
  b += erdTable(520, 476, 300, "JsonBArrayTable", ["id PK", "groups JSONB NOT NULL", "numbers JSONB NOT NULL"], 3, 152);

  b += panel(1010, 130, 350, 600, "Serialized payloads");
  b += umlBox(1060, 196, 250, 126, "DataHolder", ["user: User", "logins: Int", "active: Boolean", "team: String?"], [], 2);
  b += umlBox(1060, 394, 250, 92, "User", ["name: String", "team: String?"], [], 0);
  b += umlBox(1060, 562, 250, 92, "UserGroup", ["users: List<User>"], [], 1);
  b += path("M406,262 L1060,244", "mapLine", "M406,262 L466,262 L466,154 L946,154 L946,244 L1060,244");
  b += path("M820,262 L1060,276", "mapLine", "M820,262 L946,262 L946,276 L1060,276");
  b += path("M406,552 L1060,608", "mapLine", "M406,552 L466,552 L466,690 L946,690 L946,608 L1060,608");
  b += path("M820,552 L1060,632", "mapLine", "M820,552 L946,552 L946,632 L1060,632");
  b += path("M1185,322 L1185,394", "runtimeUse");
  b += path("M1185,562 L1185,486", "runtimeUse");
  b += note(126, 794, 1188, "No FK lines are drawn: the relationships are JSON payload shape references, not relational constraints.");
  return b;
}

function jsonClass() {
  let b = "";
  b += panel(58, 130, 840, 520, "Table hierarchy");
  b += umlBox(372, 150, 210, 94, "IntIdTable", ["id: EntityID<Int>"], ["table base"], 1);
  b += umlBox(92, 380, 176, 114, "JsonArrayTable", ["groups: JSON", "numbers: JSON"], [], 2);
  b += umlBox(288, 380, 176, 114, "JsonBArrayTable", ["groups: JSONB", "numbers: JSONB"], [], 3);
  b += umlBox(484, 380, 176, 114, "JsonBTable", ["jsonBColumn", "jsonb<DataHolder>"], [], 4);
  b += umlBox(680, 380, 176, 114, "JsonTable", ["jsonColumn", "json<DataHolder>"], [], 0);
  b += generalizationTree(477, 244, 306, 380, [180, 376, 572, 768]);

  b += panel(930, 130, 340, 520, "DAO entities");
  b += umlBox(1000, 150, 200, 94, "IntEntity", ["EntityID<Int>"], ["DAO base"], 7);
  b += umlBox(950, 372, 154, 94, "JsonEntity", ["var jsonColumn"], [], 5);
  b += umlBox(1116, 372, 154, 94, "JsonBEntity", ["var jsonBColumn"], [], 6);
  b += generalizationTree(1100, 244, 306, 372, [1027, 1193]);
  b += path("M856,436 L950,419", "mapLine", "M856,436 L906,436 L906,419 L950,419");
  b += path("M572,494 L1116,419", "mapLine", "M572,494 L572,520 L1110,520 L1110,419 L1116,419");

  b += panel(1300, 130, 224, 520, "Payload model");
  b += umlBox(1320, 178, 180, 132, "DataHolder", ["user: User", "logins: Int", "active: Boolean", "team: String?"], [], 2);
  b += umlBox(1320, 380, 180, 92, "User", ["name: String", "team: String?"], [], 0);
  b += umlBox(1320, 548, 180, 92, "UserGroup", ["users: List<User>"], [], 1);
  b += path("M1500,244 L1514,244 L1514,426 L1500,426", "runtimeUse");
  b += path("M1410,548 L1410,472", "runtimeUse");
  b += path("M1104,419 L1320,244", "mapLine", "M1104,419 L1280,419 L1280,244 L1320,244");
  b += path("M1270,419 L1320,276", "mapLine", "M1270,419 L1290,419 L1290,276 L1320,276");
  b += note(150, 808, 1230, "Source check: JsonTestData has four IntIdTable objects, two IntEntity DAO classes, and three serializable payload data classes.");
  return b;
}

function moneyErd() {
  let b = "";
  b += panel(58, 130, 420, 600, "Composite money type");
  b += card(116, 190, 300, 72, "MonetaryAmount", 5, "javax.money");
  b += card(116, 328, 300, 72, "BigDecimal amount", 6, "DECIMAL scale");
  b += card(116, 466, 300, 72, "CurrencyUnit", 1, "VARCHAR(3)");
  b += path("M266,262 L266,328", "mapLine");
  b += path("M266,400 L266,466", "mapLine");

  b += panel(548, 130, 820, 600, "Tables");
  b += erdTable(600, 184, 300, "AccountTable", ["id PK", "composite_money DECIMAL(8,5)", "composite_money_C VARCHAR(3)", "index ix_money_amount"], 1, 180);
  b += erdTable(1010, 184, 300, "TableWithDBDefault", ["id PK", "field VARCHAR(100)", "t1 + t1_C DEFAULT USD", "t2 + t2_C NULL", "clientDefault INT"], 6, 198);
  b += erdTable(792, 480, 330, "tester", ["amount DECIMAL(8,5)", "currency VARCHAR(3)", "nullable_amount DECIMAL(8,5)", "nullable_currency VARCHAR(3)"], 2, 184);
  b += path("M416,364 L600,274", "mapLine", "M416,364 L512,364 L512,274 L600,274");
  b += path("M416,502 L600,306", "mapLine", "M416,502 L520,502 L520,306 L600,306");
  b += path("M416,364 L1010,282", "runtimeUse", "M416,364 L516,364 L516,420 L970,420 L970,282 L1010,282");
  b += path("M416,502 L792,572", "runtimeUse", "M416,502 L522,502 L522,572 L792,572");
  b += note(126, 794, 1188, "ERD shows physical columns. compositeMoney is not a separate table; it expands into amount and currency columns.");
  return b;
}

function moneyClass() {
  let b = "";
  b += panel(58, 130, 602, 560, "Tests");
  b += umlBox(244, 178, 210, 94, "AbstractExposedTest", ["withTables(testDB, ...)"], ["base fixture"], 7);
  b += umlBox(100, 386, 230, 118, "Ex01 Money Defaults", ["TableWithDBDefault", "DBDefault entity", "default/null tests"], [], 3);
  b += umlBox(386, 386, 230, 118, "Ex02 Money", ["AccountTable", "manual tester table", "search predicates"], [], 4);
  b += generalizationTree(349, 272, 328, 386, [215, 501]);

  b += panel(740, 130, 742, 300, "Table hierarchy");
  b += umlBox(874, 150, 198, 92, "IntIdTable", ["id: EntityID<Int>"], ["table base"], 1);
  b += umlBox(770, 332, 220, 90, "AccountTable", ["composite_money", "ix_money_amount"], [], 1);
  b += umlBox(1030, 332, 230, 90, "TableWithDBDefault", ["field", "t1 / t2", "clientDefault"], [], 6);
  b += umlBox(1304, 306, 132, 116, "tester", ["amount", "currency", "nullable_*", "local Table"], [], 3);
  b += generalizationTree(973, 242, 292, 332, [880, 1145]);

  b += panel(740, 486, 742, 204, "DAO entity hierarchy");
  b += umlBox(954, 496, 198, 88, "IntEntity", ["EntityID<Int>"], ["DAO base"], 7);
  b += umlBox(770, 636, 220, 58, "AccountEntity", ["money / amount / currency"], [], 5);
  b += umlBox(1030, 636, 230, 58, "DBDefault", ["field / t1 / t2 / clientDefault"], [], 2);
  b += generalizationTree(1053, 584, 616, 636, [880, 1145]);
  b += path("M880,422 L880,636", "mapLine", "M880,422 L880,486 L880,486 L880,636");
  b += path("M1260,377 L1260,665", "mapLine", "M1260,377 L1278,377 L1278,665 L1260,665");
  b += path("M616,444 L770,377", "runtimeUse", "M616,444 L700,444 L700,377 L770,377");
  b += path("M616,468 L1304,377", "runtimeUse", "M616,468 L700,468 L700,718 L1370,718 L1370,422 L1360,422");

  b += panel(188, 724, 1160, 92, "Composite access");
  b += card(246, 746, 250, 48, "MonetaryAmount?", 5, "entity property");
  b += card(640, 746, 250, 48, "amount: BigDecimal?", 6, "component column");
  b += card(1022, 746, 250, 48, "currency: CurrencyUnit?", 1, "component column");
  b += path("M496,770 L640,770", "mapLine");
  b += path("M890,770 L1022,770", "mapLine");
  return b;
}

function customColumnsArchitecture() {
  let b = "";
  b += panel(58, 130, 274, 640, "Test groups");
  b += card(96, 188, 198, 70, "Compression tests", 0, "ByteArray payloads");
  b += card(96, 386, 198, 70, "Serialization tests", 4, "Embeddable objects");
  b += card(96, 584, 198, 70, "Client default tests", 2, "generated values");

  b += panel(406, 130, 344, 640, "Column builders");
  b += card(460, 174, 236, 60, "compressedBinary", 0, "BINARY size 4096");
  b += card(460, 252, 236, 60, "compressedBlob", 3, "BLOB");
  b += card(460, 366, 236, 60, "binarySerializedBinary", 1, "BINARY + serializer");
  b += card(460, 444, 236, 60, "binarySerializedBlob", 4, "BLOB + serializer");
  b += card(460, 586, 236, 60, "timebased / snowflake", 5, "clientDefault");
  b += card(460, 664, 236, 60, "ksuid / ksuidMillis", 6, "clientDefault");

  b += panel(824, 130, 302, 640, "Runtime codecs");
  b += card(866, 198, 218, 72, "Compressors", 3, "LZ4 / Snappy / Zstd");
  b += card(866, 396, 218, 72, "BinarySerializers", 4, "Fory / Kryo + codecs");
  b += card(866, 604, 218, 72, "ID generators", 5, "UUID / Snowflake / KSUID");

  b += panel(1194, 130, 190, 640, "Tables");
  b += erdTable(1218, 176, 142, "T1", ["lz4_data", "snappy_data", "zstd_data"], 0, 132);
  b += erdTable(1218, 374, 142, "T1", ["name", "lz4_fory", "lz4_kryo", "zstd_*"], 4, 154);
  b += erdTable(1218, 592, 142, "ClientGenerated", ["timebased_uuid", "snowflake", "ksuid"], 2, 142);

  b += cylinder(1418, 330, 104, 218, "DB", ["BINARY", "BLOB", "generated ids"], 3);
  b += path("M294,223 L460,204", "arrow", "M294,223 L370,223 L370,204 L460,204");
  b += path("M294,421 L460,396", "arrow", "M294,421 L370,421 L370,396 L460,396");
  b += path("M294,619 L460,616", "arrow", "M294,619 L370,619 L370,616 L460,616");
  b += path("M696,204 L866,234", "codecLine", "M696,204 L782,204 L782,234 L866,234");
  b += path("M696,282 L866,234", "codecLine", "M696,282 L782,282 L782,234 L866,234");
  b += path("M696,396 L866,432", "codecLine", "M696,396 L782,396 L782,432 L866,432");
  b += path("M696,474 L866,432", "codecLine", "M696,474 L782,474 L782,432 L866,432");
  b += path("M696,616 L866,640", "runtimeUse", "M696,616 L782,616 L782,640 L866,640");
  b += path("M1084,234 L1218,242", "mapLine", "M1084,234 L1148,234 L1148,242 L1218,242");
  b += path("M1084,432 L1218,451", "mapLine", "M1084,432 L1148,432 L1148,451 L1218,451");
  b += path("M1084,640 L1218,663", "mapLine", "M1084,640 L1148,640 L1148,663 L1218,663");
  b += path("M1360,242 L1418,390", "dbUse", "M1360,242 L1390,242 L1390,390 L1418,390");
  b += path("M1360,451 L1418,438", "dbUse", "M1360,451 L1390,451 L1390,438 L1418,438");
  b += path("M1360,663 L1470,548", "dbUse", "M1360,663 L1390,663 L1390,600 L1470,600 L1470,548");
  b += note(128, 838, 1196, "Source check: compressedBinary/blob, binarySerializedBinary/blob, and generated client-default columns each have DSL plus DAO tests.");
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
  const fieldH = hasMethods ? Math.max(34, Math.floor(bodyH * 0.58)) : bodyH;
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
  let out = `<path d="M${parentX},${baseY} L${parentX},${busY}" class="inheritStem"/>
<polygon points="${parentX},${parentBottomY} ${parentX - 9},${baseY} ${parentX + 9},${baseY}" fill="#fff" stroke="#475569" stroke-width="1.8"/>\n`;
  out += `<path d="M${Math.min(...childXs)},${busY} L${Math.max(...childXs)},${busY}" class="inheritStem"/>\n`;
  for (const childX of childXs) out += `<path d="M${childX},${busY} L${childX},${childTopY}" class="inheritStem"/>\n`;
  return out;
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

function validateBatch11Semantics() {
  const [jsonErdSvg, jsonClassSvg, moneyErdSvg, moneyClassSvg, customSvg] = diagrams.map((d) => fs.readFileSync(d.file, "utf8"));
  const failures = [];
  for (const required of ["JsonTable", "JsonBTable", "JsonArrayTable", "JsonBArrayTable", "j_column JSON", "j_b_column JSONB", "No FK lines"]) {
    if (!jsonErdSvg.includes(required)) failures.push(`json erd missing ${required}`);
  }
  for (const required of ["IntIdTable", "IntEntity", "JsonEntity", "JsonBEntity", "DataHolder", "UserGroup", "kotlinx.serialization"]) {
    if (!jsonClassSvg.includes(required)) failures.push(`json class missing ${required}`);
  }
  for (const required of ["AccountTable", "TableWithDBDefault", "tester", "composite_money", "CurrencyUnit", "ix_money_amount"]) {
    if (!moneyErdSvg.includes(required)) failures.push(`money erd missing ${required}`);
  }
  for (const required of ["AbstractExposedTest", "Ex01 Money Defaults", "Ex02 Money", "AccountEntity", "DBDefault", "MonetaryAmount"]) {
    if (!moneyClassSvg.includes(required)) failures.push(`money class missing ${required}`);
  }
  for (const required of ["compressedBinary", "compressedBlob", "binarySerializedBinary", "binarySerializedBlob", "timebased / snowflake", "ksuid / ksuidMillis", "Compressors", "BinarySerializers", "ClientGenerated"]) {
    if (!customSvg.includes(required)) failures.push(`custom arch missing ${required}`);
  }
  if (failures.length) throw new Error(`batch11_semantic_validation_failed\n${failures.join("\n")}`);
  console.log("batch11_semantics=ok");
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
  if (failures.length) throw new Error(`batch11_connector_validation_failed\n${failures.join("\n")}`);
  console.log("batch11_connectors=ok");
}
