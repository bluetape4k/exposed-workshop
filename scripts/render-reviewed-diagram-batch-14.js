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
    file: `${outDir}/06-advanced-architecture-01.svg`,
    title: "Advanced Exposed Extension Architecture",
    subtitle: "Advanced modules extend ColumnType, Table DSL, Entity IDs, JSON predicates, and encrypted persistence before values reach databases",
    width: 1560,
    height: 940,
    body: advancedArchitecture,
  },
  {
    file: `${outDir}/06-advanced-architecture-02.svg`,
    title: "Advanced Module Classification",
    subtitle: "Current advanced source directories group examples by value type, JSON serialization, encryption, and custom extension mechanisms",
    width: 1560,
    height: 780,
    body: advancedClassification,
  },
  {
    file: `${outDir}/07-jpa-architecture-01.svg`,
    title: "JPA to Exposed Migration Strategy",
    subtitle: "Basic and advanced JPA examples migrate annotation mappings, queries, relationships, inheritance, audit, and version behavior into Exposed tests",
    width: 1560,
    height: 940,
    body: jpaMigrationStrategy,
  },
  {
    file: `${outDir}/07-jpa-class-02.svg`,
    title: "JPA and Exposed Concept Mapping",
    subtitle: "Root JPA migration examples map JPA annotations and EntityManager behavior to Table DSL, DAO entities, Query DSL, and transaction blocks",
    width: 1320,
    height: 980,
    body: jpaConceptMapping,
  },
  {
    file: `${outDir}/07-jpa-architecture-03.svg`,
    title: "Migration Approach Comparison",
    subtitle: "Use source-backed equivalence tests to migrate from basic CRUD through relationships, inheritance strategies, auditing, and optimistic locking",
    width: 1560,
    height: 940,
    body: jpaApproachComparison,
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
validateBatch14Semantics();
validateConnectorGeometry();

function writeGraphvizSketches() {
  fs.mkdirSync(sketchDir, { recursive: true });
  const sketches = {
    "batch-14-advanced": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=0.55, ranksep=0.75, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      sensitive_data -> table_column_dsl -> ddl_columns -> db;
      structured_json -> column_type -> round_trip_values -> db;
      domain_values -> column_type;
      custom_identity -> entity_id_strategy -> encrypted_rows -> db;
      query_predicates -> path_queries -> db;
      entity_id_strategy -> path_queries;
      query_predicates -> encrypted_rows;
      { rank=same; sensitive_data; structured_json; domain_values; custom_identity; }
      { rank=same; table_column_dsl; column_type; entity_id_strategy; query_predicates; }
      { rank=same; ddl_columns; round_trip_values; path_queries; encrypted_rows; }
    }`,
    "batch-14-jpa-strategy": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=0.55, ranksep=0.75, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      entity_table -> basic_crud -> table_dsl -> db;
      entity_manager -> relations -> dao_entity -> db;
      jpql_criteria -> advanced_queries -> query_dsl -> db;
      transactional -> inheritance -> transaction_block -> assertions;
      version_audit -> advanced_queries;
      db -> assertions;
      { rank=same; entity_table; entity_manager; jpql_criteria; transactional; version_audit; }
      { rank=same; basic_crud; relations; advanced_queries; inheritance; }
      { rank=same; table_dsl; dao_entity; query_dsl; transaction_block; }
    }`,
    "batch-14-jpa-concepts": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=0.55, ranksep=0.75, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      entity_annotation -> simple_schema -> table_dao;
      column_fields -> blog_schema -> column_delegates;
      id_annotation -> book_schema -> id_tables;
      relationships -> blog_schema -> references;
      relationships -> person_schema -> references;
      lifecycle -> inheritance_tests -> explicit_tests;
      { rank=same; entity_annotation; column_fields; id_annotation; relationships; lifecycle; }
      { rank=same; simple_schema; blog_schema; person_schema; book_schema; inheritance_tests; }
      { rank=same; table_dao; column_delegates; id_tables; references; explicit_tests; }
    }`,
  };
  for (const [name, dot] of Object.entries(sketches)) {
    const dotPath = `${sketchDir}/${name}.dot`;
    fs.writeFileSync(dotPath, dot);
    spawnSync(DOT, ["-Tplain", "-o", `${sketchDir}/${name}.plain`, dotPath], { encoding: "utf8" });
    spawnSync(DOT, ["-Tsvg", "-o", `${sketchDir}/${name}.svg`, dotPath], { encoding: "utf8" });
  }
}

function advancedArchitecture() {
  let b = "";
  b += panel(58, 130, 282, 620, "Production concerns");
  b += card(94, 178, 210, 66, "Sensitive data", 4, "crypt + Tink");
  b += card(94, 304, 210, 66, "Structured JSON", 1, "json/jackson/fastjson");
  b += card(94, 430, 210, 66, "Domain values", 6, "time + money");
  b += card(94, 556, 210, 66, "Custom identity", 5, "KSUID/Snowflake/UUID");

  b += panel(420, 130, 328, 620, "Exposed extension points");
  b += card(470, 176, 228, 64, "Table column DSL", 0, "registerColumn()");
  b += card(470, 302, 228, 64, "ColumnType", 2, "serialize / parse");
  b += card(470, 428, 228, 64, "EntityID strategy", 6, "custom IdTable");
  b += card(470, 554, 228, 64, "Query predicates", 1, "extract/contains/exists");

  b += panel(828, 130, 326, 620, "Tested persistence");
  b += card(874, 176, 234, 64, "DDL columns", 3, "varchar/jsonb/binary");
  b += card(874, 302, 234, 64, "Round-trip values", 5, "DAO + DSL tests");
  b += card(874, 428, 234, 64, "Path queries", 1, "JSON predicates");
  b += card(874, 554, 234, 64, "Encrypted rows", 4, "search constraints");

  b += panel(1244, 130, 196, 620, "Databases");
  b += cylinder(1288, 286, 108, 224, "DB", ["H2", "Postgres", "MySQL", "MariaDB"], 3);

  b += path("M304,211 L470,208", "arrow", "M304,211 L386,211 L386,208 L470,208");
  b += path("M304,337 L470,334", "arrow", "M304,337 L386,337 L386,334 L470,334");
  b += path("M304,463 L470,334", "arrow", "M304,463 L386,463 L386,334 L470,334");
  b += path("M304,589 L470,460", "arrow", "M304,589 L386,589 L386,460 L470,460");
  b += path("M698,208 L874,208", "mapLine", "M698,208 L786,208 L786,208 L874,208");
  b += path("M698,334 L874,334", "mapLine");
  b += path("M698,460 L874,586", "mapLine", "M698,460 L780,460 L780,586 L874,586");
  b += path("M698,586 L874,460", "codecLine", "M698,586 L760,586 L760,520 L812,520 L812,460 L874,460");
  b += path("M1108,208 L1288,356", "dbUse", "M1108,208 L1196,208 L1196,356 L1288,356");
  b += path("M1108,334 L1288,394", "dbUse", "M1108,334 L1196,334 L1196,394 L1288,394");
  b += path("M1108,460 L1288,432", "dbUse", "M1108,460 L1196,460 L1196,432 L1288,432");
  b += path("M1108,586 L1288,470", "dbUse", "M1108,586 L1196,586 L1196,470 L1288,470");
  b += note(126, 824, 1208, "Source check: advanced chapter directories present modules 01-09, 11, and 12; stale README-only rows are not treated as source-owned modules.");
  return b;
}

function advancedClassification() {
  let b = "";
  b += panel(58, 130, 650, 500, "Problem domains");
  b += card(98, 182, 224, 70, "Protect data", 4, "01 crypt / 12 Tink");
  b += card(98, 326, 224, 70, "Represent values", 6, "02/03 time + 05 money");
  b += card(98, 470, 224, 70, "Store JSON", 1, "04/08/09/11 serializers");
  b += card(410, 182, 224, 70, "Extend columns", 5, "06 custom-columns");
  b += card(410, 326, 224, 70, "Extend IDs", 3, "07 custom-entities");
  b += card(410, 470, 224, 70, "Validate behavior", 7, "round-trip tests");

  b += panel(810, 130, 590, 500, "Implementation mechanisms");
  b += card(852, 190, 230, 72, "ColumnType adapters", 2, "conversion boundary");
  b += card(852, 334, 230, 72, "JSON operators", 1, "extract/contains/exists");
  b += card(852, 478, 230, 72, "Encryption wrappers", 4, "crypt/AEAD/DAEAD");
  b += card(1132, 262, 230, 72, "EntityID bases", 6, "KSUID/Snowflake/UUID");
  b += card(1132, 438, 230, 72, "DAO + DSL fixtures", 0, "source-backed tests");

  b += path("M322,217 L852,206", "codecLine", "M322,217 L360,217 L360,160 L820,160 L820,206 L852,206");
  b += path("M322,361 L852,246", "mapLine", "M322,361 L360,361 L360,278 L820,278 L820,246 L852,246");
  b += path("M322,505 L852,390", "arrow", "M322,505 L360,505 L360,420 L820,420 L820,390 L852,390");
  b += path("M634,217 L852,226", "mapLine", "M634,217 L750,217 L750,226 L852,226");
  b += path("M634,361 L1132,298", "arrow", "M634,361 L744,361 L744,298 L1132,298");
  b += path("M634,505 L1132,474", "runtimeUse", "M634,505 L744,505 L744,474 L1132,474");
  b += path("M1082,226 L1132,474", "runtimeUse", "M1082,226 L1104,226 L1104,474 L1132,474");
  b += path("M1082,370 L1132,474", "runtimeUse", "M1082,370 L1104,370 L1104,474 L1132,474");
  b += path("M1082,514 L1132,474", "runtimeUse", "M1082,514 L1104,514 L1104,474 L1132,474");
  b += note(142, 690, 1200, "Classification rule: group by current source directories and tested extension mechanisms, not by stale or missing chapter rows.");
  return b;
}

function jpaMigrationStrategy() {
  let b = "";
  b += panel(58, 130, 292, 640, "JPA surface");
  b += card(96, 174, 216, 62, "@Entity / @Table", 0, "class mapping");
  b += card(96, 284, 216, 62, "EntityManager", 1, "persist/find/remove");
  b += card(96, 394, 216, 62, "JPQL / Criteria", 6, "query model");
  b += card(96, 504, 216, 62, "@Transactional", 7, "unit of work");
  b += card(96, 614, 216, 62, "@Version / audit", 4, "consistency");

  b += panel(430, 130, 322, 640, "Migration slices");
  b += card(474, 178, 234, 66, "01 basic CRUD", 2, "SimpleSchema");
  b += card(474, 306, 234, 66, "Relations", 5, "Blog/Person/Book");
  b += card(474, 434, 234, 66, "02 advanced", 3, "joins/subquery/CTE");
  b += card(474, 562, 234, 66, "Inheritance", 1, "single/joined/table");

  b += panel(828, 130, 326, 640, "Exposed implementation");
  b += card(874, 178, 234, 66, "Table DSL", 0, "LongIdTable/IdTable");
  b += card(874, 306, 234, 66, "DAO Entity", 2, "LongEntity/IntEntity");
  b += card(874, 434, 234, 66, "Query DSL", 6, "select/join/exists");
  b += card(874, 562, 234, 66, "Transaction block", 7, "withTables fixtures");

  b += panel(1246, 130, 190, 640, "Regression target");
  b += cylinder(1288, 278, 108, 230, "DB", ["H2", "Postgres", "MySQL", "equivalent rows"], 3);
  b += card(1260, 588, 160, 60, "Assertions", 4, "same behavior");

  b += path("M312,205 L474,211", "arrow", "M312,205 L392,205 L392,211 L474,211");
  b += path("M312,315 L474,339", "arrow", "M312,315 L392,315 L392,339 L474,339");
  b += path("M312,425 L474,467", "arrow", "M312,425 L392,425 L392,467 L474,467");
  b += path("M312,535 L474,595", "runtimeUse", "M312,535 L386,535 L386,595 L474,595");
  b += path("M312,645 L474,496", "codecLine", "M312,645 L414,645 L414,496 L474,496");
  b += path("M708,211 L874,211", "mapLine");
  b += path("M708,339 L874,339", "mapLine");
  b += path("M708,467 L874,467", "mapLine");
  b += path("M708,595 L874,595", "runtimeUse");
  b += path("M1108,211 L1288,348", "dbUse", "M1108,211 L1196,211 L1196,348 L1288,348");
  b += path("M1108,339 L1288,386", "dbUse", "M1108,339 L1196,339 L1196,386 L1288,386");
  b += path("M1108,467 L1288,424", "dbUse", "M1108,467 L1196,467 L1196,424 L1288,424");
  b += path("M1108,595 L1260,618", "runtimeUse", "M1108,595 L1196,595 L1196,618 L1260,618");
  b += path("M1342,508 L1342,588", "runtimeUse");
  b += note(126, 838, 1208, "Source check: root JPA chapter delegates to 01 basic and 02 advanced modules; tests verify equivalent rows and behavior.");
  return b;
}

function jpaConceptMapping() {
  let b = "";
  b += panel(58, 130, 650, 700, "JPA concepts");
  const left = [
    [98, 178, "@Entity class", "SimpleEntity / Person", 0],
    [98, 294, "@Column fields", "name, address, version", 6],
    [98, 410, "@Id / @EmbeddedId", "Long id / composite id", 1],
    [98, 526, "Relationships", "OneToOne/ManyToMany", 5],
    [98, 642, "Lifecycle behavior", "audit / version / lazy", 4],
  ];
  left.forEach(([x, y, title, detail, c]) => (b += card(x, y, 230, 64, title, c, detail)));

  b += panel(838, 130, 424, 700, "Exposed counterpart");
  const right = [
    [920, 178, "Table + DAO", "LongIdTable + LongEntity", 0],
    [920, 294, "Column delegates", "var field by Table.col", 6],
    [920, 410, "IdTable variants", "LongIdTable / CompositeIdTable", 1],
    [920, 526, "Reference APIs", "referencedOn / referrersOn / via", 5],
    [920, 642, "Explicit tests", "transaction + EntityHook", 4],
  ];
  right.forEach(([x, y, title, detail, c]) => (b += card(x, y, 260, 64, title, c, detail)));

  b += panel(432, 200, 270, 560, "Source examples");
  b += card(468, 244, 196, 54, "SimpleSchema", 2, "CRUD");
  b += card(468, 354, 196, 54, "BlogSchema", 5, "relations");
  b += card(468, 464, 196, 54, "PersonSchema", 3, "many-to-one");
  b += card(468, 574, 196, 54, "BookSchema", 1, "composite PK");
  b += card(468, 684, 196, 54, "Inheritance tests", 7, "3 strategies");

  b += path("M328,210 L468,271", "arrow", "M328,210 L398,210 L398,271 L468,271");
  b += path("M328,326 L468,381", "arrow", "M328,326 L398,326 L398,381 L468,381");
  b += path("M328,442 L468,601", "arrow", "M328,442 L398,442 L398,601 L468,601");
  b += path("M328,558 L468,381", "arrow", "M328,558 L398,558 L398,381 L468,381");
  b += path("M328,548 L468,491", "mapLine", "M328,548 L418,548 L418,491 L468,491");
  b += path("M328,674 L468,711", "runtimeUse", "M328,674 L398,674 L398,711 L468,711");
  b += path("M664,271 L920,210", "mapLine", "M664,271 L780,271 L780,210 L920,210");
  b += path("M664,271 L920,326", "mapLine", "M664,271 L760,271 L760,326 L920,326");
  b += path("M664,601 L920,442", "mapLine", "M664,601 L760,601 L760,442 L920,442");
  b += path("M664,381 L920,558", "mapLine", "M664,381 L760,381 L760,558 L920,558");
  b += path("M664,491 L920,558", "mapLine", "M664,491 L790,491 L790,558 L920,558");
  b += path("M664,711 L920,674", "runtimeUse", "M664,711 L760,711 L760,674 L920,674");
  b += note(128, 884, 1064, "Concept rule: Exposed splits mapping into explicit table definitions, DAO classes, query DSL, and transaction-scoped verification.");
  return b;
}

function jpaApproachComparison() {
  let b = "";
  b += panel(58, 130, 412, 640, "Approach options");
  b += card(102, 182, 310, 70, "Big-bang rewrite", 4, "high blast radius");
  b += card(102, 344, 310, 70, "Parallel equivalence", 2, "compare old/new results");
  b += card(102, 506, 310, 70, "Incremental replacement", 0, "module-by-module");

  b += panel(562, 130, 388, 640, "Migration order");
  b += card(610, 166, 292, 60, "1. CRUD basics", 2, "SimpleTable + SimpleEntity");
  b += card(610, 278, 292, 60, "2. Relations", 5, "Blog/Person/Book schemas");
  b += card(610, 390, 292, 60, "3. Advanced queries", 6, "joins/subquery/CTE");
  b += card(610, 502, 292, 60, "4. Inheritance", 1, "single/joined/table per class");
  b += card(610, 614, 292, 60, "5. Consistency", 4, "audit + optimistic locking");

  b += panel(1046, 130, 330, 640, "Verification gates");
  b += card(1090, 196, 242, 64, "Row equivalence", 0, "same records");
  b += card(1090, 332, 242, 64, "Query count", 6, "N+1 guarded");
  b += card(1090, 468, 242, 64, "Transaction behavior", 7, "same boundary");
  b += card(1090, 604, 242, 64, "Lock conflict", 4, "version policy");
  b += cylinder(1418, 320, 104, 220, "DB", ["fixtures", "indexes", "constraints"], 3);

  b += path("M412,217 L610,196", "arrow", "M412,217 L500,217 L500,196 L610,196");
  b += path("M412,379 L610,308", "mapLine", "M412,379 L500,379 L500,308 L610,308");
  b += path("M412,541 L610,196", "arrow", "M412,541 L530,541 L530,196 L610,196");
  b += path("M902,196 L1090,228", "mapLine", "M902,196 L996,196 L996,228 L1090,228");
  b += path("M902,308 L1090,364", "mapLine", "M902,308 L996,308 L996,364 L1090,364");
  b += path("M902,420 L1090,364", "mapLine", "M902,420 L996,420 L996,364 L1090,364");
  b += path("M902,532 L1090,500", "runtimeUse", "M902,532 L996,532 L996,500 L1090,500");
  b += path("M902,644 L1090,636", "codecLine", "M902,644 L996,644 L996,636 L1090,636");
  b += path("M1332,228 L1418,392", "dbUse", "M1332,228 L1380,228 L1380,392 L1418,392");
  b += path("M1332,364 L1418,428", "dbUse", "M1332,364 L1380,364 L1380,428 L1418,428");
  b += path("M1332,500 L1418,464", "dbUse", "M1332,500 L1380,500 L1380,464 L1418,464");
  b += path("M1332,636 L1470,540", "dbUse", "M1332,636 L1380,636 L1380,568 L1470,568 L1470,540");
  b += note(128, 838, 1194, "Recommended route: keep JPA-facing behavior stable, migrate one feature slice at a time, and lock each slice with equivalence tests.");
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
  const titleLines = splitTitle(title, Math.max(9, Math.floor(w / 10)), 2);
  const shownDetail = titleLines.length > 1 && h < 62 ? "" : detail;
  const titleY = y + (shownDetail ? 25 : h / 2 - (titleLines.length - 1) * 10 + 6);
  let out = `<desc>${esc(title)}</desc><rect x="${x}" y="${y}" width="${w}" height="${h}" rx="10" fill="${fill}" stroke="${stroke}" class="card"/>\n`;
  titleLines.forEach((line, i) => (out += `<text x="${x + w / 2}" y="${titleY + i * 18}" class="label" text-anchor="middle">${esc(line)}</text>\n`));
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

function validateBatch14Semantics() {
  const [advancedArch, advancedClass, jpaStrategy, jpaConcepts, jpaApproach] = diagrams.map((d) => fs.readFileSync(d.file, "utf8"));
  const failures = [];
  for (const required of ["Sensitive data", "Structured JSON", "EntityID strategy", "Encrypted rows", "H2", "Postgres"]) {
    if (!advancedArch.includes(required)) failures.push(`advanced architecture missing ${required}`);
  }
  for (const required of ["01 crypt", "12 Tink", "04/08/09/11 serializers", "ColumnType adapters", "EntityID bases"]) {
    if (!advancedClass.includes(required)) failures.push(`advanced classification missing ${required}`);
  }
  for (const required of ["@Entity / @Table", "EntityManager", "01 basic CRUD", "Relations", "Inheritance", "Assertions"]) {
    if (!jpaStrategy.includes(required)) failures.push(`jpa strategy missing ${required}`);
  }
  for (const required of ["SimpleSchema", "BlogSchema", "PersonSchema", "BookSchema", "Table + DAO", "Reference APIs"]) {
    if (!jpaConcepts.includes(required)) failures.push(`jpa concepts missing ${required}`);
  }
  for (const required of ["Big-bang rewrite", "Parallel equivalence", "Incremental replacement", "Inheritance", "Lock conflict"]) {
    if (!jpaApproach.includes(required)) failures.push(`jpa approach missing ${required}`);
  }
  if (failures.length) throw new Error(`batch14_semantic_validation_failed\n${failures.join("\n")}`);
  console.log("batch14_semantics=ok");
}

function validateConnectorGeometry() {
  const connectorClasses = "(?:arrow|mapLine|codecLine|dbUse|runtimeUse|inheritStem)";
  const failures = [];
  for (const diagram of diagrams) {
    const svg = fs.readFileSync(diagram.file, "utf8");
    const segments = [];
    const cards = [...svg.matchAll(/<rect x="([\d.]+)" y="([\d.]+)" width="([\d.]+)" height="([\d.]+)" rx="10"[^>]*class="card"/g)].map((m) => ({
      x: Number(m[1]),
      y: Number(m[2]),
      w: Number(m[3]),
      h: Number(m[4]),
    }));
    for (const match of svg.matchAll(new RegExp(`<path\\b([^>]*class="(${connectorClasses})"[^>]*)`, "g"))) {
      const cls = match[2];
      const pathD = (match[1].match(/d="([^"]+)"/) || [])[1];
      if (!pathD) continue;
      const points = [...pathD.matchAll(/[ML]([\d.]+),([\d.]+)/g)].map((m) => ({ x: Number(m[1]), y: Number(m[2]) }));
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
  if (failures.length) throw new Error(`batch14_connector_validation_failed\n${failures.join("\n")}`);
  console.log("batch14_connectors=ok");
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
