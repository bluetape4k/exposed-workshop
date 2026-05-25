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
.className{font-family:"Architects Daughter","Comic Sans MS",cursive;font-size:17px;fill:#1e293b}
.panelTitle{font-family:"Architects Daughter","Comic Sans MS",cursive;font-size:12px;letter-spacing:.08em;fill:#94a3b8}
.card,.uml{stroke-width:1.7;filter:url(#shadow)}.panel{fill:#f8fafc;stroke:#d7e2ec;stroke-width:1.2}
.arrow{fill:none;stroke:#2563eb;stroke-width:2;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowBlue)}
.mapLine{fill:none;stroke:#16a34a;stroke-width:1.9;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowGreen)}
.dbUse{fill:none;stroke:#ea580c;stroke-width:2;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowOrange)}
.runtimeUse{fill:none;stroke:#7c3aed;stroke-width:1.9;stroke-dasharray:6 5;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowPurple)}
.returnLine{fill:none;stroke:#64748b;stroke-width:1.7;stroke-dasharray:7 5;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowGray)}
.inherit{fill:none;stroke:#334155;stroke-width:1.8;stroke-linecap:round;stroke-linejoin:round}
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

let graphvizRouting = new Map();

const diagrams = [
  {
    file: `${outDir}/09-spring-architecture-01.svg`,
    title: "Spring Integration Architecture Flow",
    subtitle: "Chapter 09 progresses from Spring Boot Exposed auto-configuration to transaction boundaries, repositories, coroutines, and cache decorators",
    width: 1500,
    height: 860,
    body: springArchitecture,
  },
  {
    file: `${outDir}/09-spring-01-springboot-autoconfigure-class-01.svg`,
    title: "Spring Boot Auto-Configuration Class Structure",
    subtitle: "Application, custom DatabaseConfig, DatabaseInitializer, TestTable/TestEntity, and async service relationships from the auto-configuration tests",
    width: 1500,
    height: 980,
    body: autoconfigureClass,
  },
  {
    file: `${outDir}/09-spring-01-springboot-autoconfigure-sequence-02.svg`,
    title: "Auto-Registered Bean Flow",
    subtitle: "Spring Boot creates Exposed transaction beans, conditionally runs DatabaseInitializer, and verifies TestTable through sync, async, and coroutine paths",
    width: 1500,
    height: 920,
    body: autoconfigureSequence,
  },
  {
    file: `${outDir}/09-spring-02-transactiontemplate-class-01.svg`,
    title: "TransactionTemplate Class Structure",
    subtitle: "TransactionTemplateConfig wires SpringTransactionManager and TransactionOperations into BookService over BookSchema tables and DAO entities",
    width: 1500,
    height: 980,
    body: transactionTemplateClass,
  },
  {
    file: `${outDir}/09-spring-02-transactiontemplate-sequence-02.svg`,
    title: "TransactionTemplate Execution Flow",
    subtitle: "TransactionTemplateTest exercises Spring TransactionTemplate, Exposed transaction blocks, no-Spring-transaction operations, and @Transactional inheritance",
    width: 1500,
    height: 940,
    body: transactionTemplateSequence,
  },
];

graphvizRouting = writeGraphvizSketches();
for (const diagram of diagrams) {
  fs.writeFileSync(diagram.file, shell(diagram));
  const png = diagram.file.replace(/\.svg$/, ".png");
  const result = spawnSync(RSVG, ["-f", "png", "-o", png, diagram.file], { encoding: "utf8" });
  if (result.status !== 0) throw new Error(result.stderr || result.stdout);
  console.log(png);
}
validateBatch17Semantics();
validateConnectorGeometry();

function writeGraphvizSketches() {
  fs.mkdirSync(sketchDir, { recursive: true });
  const sketches = {
    "batch-17-spring-root": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=0.65, ranksep=0.9, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      autoConfigure -> syncRepository;
      transactionTemplate -> coroutineRepository;
      transactional -> transactionPolicy;
      syncRepository -> springCache;
      coroutineRepository -> suspendedCache;
      transactionPolicy -> database;
      springCache -> database;
      suspendedCache -> database;
    }`,
    "batch-17-autoconfig-class": `digraph G {
      graph [rankdir=TB, splines=ortho, nodesep=0.65, ranksep=0.8, pad=0.2];
      node [shape=record, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      IntIdTable -> TestTable; IntEntity -> TestEntity;
      Application -> DatabaseConfig;
      CustomDatabaseConfigConfiguration -> DatabaseConfig;
      ExposedAutoConfiguration -> SpringTransactionManager;
      ExposedAutoConfiguration -> DatabaseInitializer -> TestTable;
      AsyncExposedService -> TestEntity; TestEntity -> TestTable;
    }`,
    "batch-17-autoconfig-sequence": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=0.65, ranksep=0.85, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      SpringBootTest -> Application -> ExposedAutoConfiguration -> SpringTransactionManager;
      ExposedAutoConfiguration -> DatabaseInitializer -> TestTable -> H2;
      SpringBootTest -> AsyncExposedService -> TestEntity -> TestTable;
      SpringBootTest -> newSuspendedTransaction -> TestEntity;
    }`,
    "batch-17-transaction-class": `digraph G {
      graph [rankdir=TB, splines=ortho, nodesep=0.65, ranksep=0.8, pad=0.2];
      node [shape=record, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      LongIdTable -> AuthorTable; LongIdTable -> BookTable; Table -> BookAuthorTable;
      LongEntity -> Author; LongEntity -> Book;
      TransactionTemplateConfig -> TransactionTemplate;
      BookService -> TransactionTemplate;
      BookService -> TransactionOperations;
      TransactionTemplateTest -> BookService;
      BookService -> AuthorTable; BookService -> Book;
      BookAuthorTable -> AuthorTable; BookAuthorTable -> BookTable;
    }`,
    "batch-17-transaction-sequence": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=0.65, ranksep=0.85, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      TransactionTemplateTest -> BookService -> TransactionTemplate -> SpringTransactionManager -> AuthorTable;
      BookService -> ExposedTransaction -> BookTable;
      BookService -> WithoutTransactionOperations -> ExposedTransaction -> AuthorTable;
      BookService -> TransactionalAnnotation -> AuthorTable;
      JdbcTemplate -> AuthorTable;
    }`,
  };
  const routing = new Map();
  const evidence = {};
  for (const [name, dot] of Object.entries(sketches)) {
    const dotPath = `${sketchDir}/${name}.dot`;
    fs.writeFileSync(dotPath, dot);
    spawnSync(DOT, ["-Tplain", "-o", `${sketchDir}/${name}.plain`, dotPath], { encoding: "utf8" });
    spawnSync(DOT, ["-Tsvg", "-o", `${sketchDir}/${name}.svg`, dotPath], { encoding: "utf8" });
    const routes = parseGraphvizPlain(fs.readFileSync(`${sketchDir}/${name}.plain`, "utf8"));
    routing.set(name, routes);
    evidence[name] = routes.map((route) => ({
      from: route.from,
      to: route.to,
      points: route.points,
      firstLeg: route.firstLeg,
      lastLeg: route.lastLeg,
    }));
  }
  fs.writeFileSync(`${sketchDir}/batch-17-routing-evidence.json`, `${JSON.stringify(evidence, null, 2)}\n`);
  return routing;
}

function springArchitecture() {
  let b = "";
  b += panel(60, 130, 315, 560, "Bootstrapping");
  b += card(104, 188, 226, 64, "01 AutoConfigure", 0, "SpringTransactionManager");
  b += card(104, 338, 226, 64, "02 TransactionTemplate", 1, "programmatic boundary");
  b += card(104, 488, 226, 64, "03 @Transactional", 7, "declarative boundary");

  b += panel(450, 130, 390, 560, "Repository patterns");
  b += card(500, 188, 288, 64, "04 Sync Repository", 2, "Spring MVC + JDBC");
  b += card(500, 338, 288, 64, "05 Coroutine Repository", 5, "WebFlux + suspend");
  b += card(500, 488, 288, 64, "Transaction policy", 6, "Spring vs Exposed");

  b += panel(914, 130, 300, 560, "Cache integration");
  b += card(958, 220, 212, 64, "06 Spring Cache", 4, "@Cacheable / Redis");
  b += card(958, 420, 212, 64, "07 Suspended Cache", 5, "LettuceSuspendedCache");

  b += panel(1288, 130, 154, 560, "Database");
  b += cylinder(1310, 300, 110, 210, "DB", ["H2", "Postgres", "Redis"], 3);

  b += gpath("batch-17-spring-root", "autoConfigure", "syncRepository", "M330,220 L500,220", "arrow");
  b += gpath("batch-17-spring-root", "transactionTemplate", "coroutineRepository", "M330,370 L500,370", "arrow");
  b += gpath("batch-17-spring-root", "transactional", "transactionPolicy", "M330,520 L500,520", "runtimeUse");
  b += gpath("batch-17-spring-root", "syncRepository", "springCache", "M788,220 L958,252", "mapLine", "M788,220 L870,220 L870,252 L958,252");
  b += gpath("batch-17-spring-root", "coroutineRepository", "suspendedCache", "M788,370 L958,452", "mapLine", "M788,370 L870,370 L870,452 L958,452");
  b += gpath("batch-17-spring-root", "transactionPolicy", "database", "M788,520 L1310,500", "dbUse", "M788,520 L1248,520 L1248,500 L1310,500");
  b += gpath("batch-17-spring-root", "springCache", "database", "M1170,252 L1310,360", "dbUse", "M1170,252 L1240,252 L1240,360 L1310,360");
  b += gpath("batch-17-spring-root", "suspendedCache", "database", "M1170,452 L1310,480", "dbUse", "M1170,452 L1240,452 L1240,480 L1310,480");
  b += note(140, 760, 1220, "Source check: chapter modules progress from ExposedAutoConfiguration to TransactionTemplate, @Transactional, sync/coroutine repositories, and Redis-backed cache decorators.");
  return b;
}

function autoconfigureClass() {
  let b = "";
  b += panel(58, 130, 360, 730, "Spring boot context");
  b += uml(106, 186, 264, 110, "Application", ["@EnableAsync", "@SpringBootApplication"], ["runApplication<Application>()"], 0);
  b += uml(106, 388, 264, 130, "CustomDatabaseConfigConfiguration", ["DEFAULT_EXPOSED_DATABASE_CONFIG"], ["customDatabaseConfig(): DatabaseConfig"], 6);
  b += uml(106, 610, 264, 112, "ExposedAutoConfiguration", ["spring.exposed.generate-ddl", "spring.exposed.show-sql"], ["registers Exposed beans"], 1);

  b += panel(486, 130, 430, 730, "Generated beans and services");
  b += uml(552, 176, 300, 140, "DatabaseConfig", ["maxEntitiesToStoreInCachePerEntity=100"], ["overrides default config"], 6);
  b += uml(552, 378, 300, 142, "SpringTransactionManager", ["DataSource-backed manager"], ["transaction boundary bean"], 5);
  b += uml(552, 600, 300, 160, "DatabaseInitializer", ["scans Table objects", "excludedPackages support"], ["SchemaUtils.create() when enabled"], 4);

  b += panel(984, 130, 460, 730, "Test table and async access");
  b += uml(1036, 164, 140, 58, "IntIdTable", [], [], 7);
  b += uml(1254, 164, 140, 58, "IntEntity", [], [], 7);
  b += uml(1018, 304, 176, 150, "TestTable", ["name: varchar(100)", "createdAt: datetime"], ["IntIdTable(\"test_table\")"], 2);
  b += uml(1236, 304, 176, 150, "TestEntity", ["name", "createdAt"], ["wrapRows(query)"], 2);
  b += uml(1018, 586, 394, 162, "AsyncExposedService", ["@Async @Service"], ["allTestDataAsync()", "allTestDataVirtualThreads()"], 1);

  b += inheritFrom("batch-17-autoconfig-class", "IntIdTable", "TestTable", 1088, 222, 1088, 304);
  b += inheritFrom("batch-17-autoconfig-class", "IntEntity", "TestEntity", 1324, 222, 1324, 304);
  b += gpath("batch-17-autoconfig-class", "Application", "DatabaseConfig", "M370,241 L552,241", "arrow");
  b += gpath("batch-17-autoconfig-class", "CustomDatabaseConfigConfiguration", "DatabaseConfig", "M370,453 L552,286", "mapLine", "M370,453 L500,453 L500,286 L552,286");
  b += gpath("batch-17-autoconfig-class", "ExposedAutoConfiguration", "SpringTransactionManager", "M370,666 L552,449", "arrow", "M370,666 L454,666 L454,449 L552,449");
  b += gpath("batch-17-autoconfig-class", "DatabaseInitializer", "TestTable", "M852,640 L1018,379", "mapLine", "M852,640 L934,640 L934,379 L1018,379");
  b += gpath("batch-17-autoconfig-class", "TestEntity", "TestTable", "M1236,398 L1194,398", "mapLine");
  b += gpath("batch-17-autoconfig-class", "AsyncExposedService", "TestEntity", "M1324,586 L1324,454", "runtimeUse");
  b += note(168, 900, 1164, "Source check: generate-ddl=false suppresses DatabaseInitializer, generate-ddl=true creates TestTable, and async/coroutine tests query TestEntity rows through Exposed transactions.");
  return b;
}

function autoconfigureSequence() {
  let b = "";
  const participants = [
    [105, "SpringBootTest"],
    [315, "Application"],
    [545, "AutoConfig"],
    [775, "Tx Manager"],
    [1005, "Initializer"],
    [1215, "TestTable"],
    [1390, "Assertions"],
  ];
  for (const [x, label] of participants) {
    b += card(x - 76, 130, 152, 54, label, 0, "");
    b += `<line x1="${x}" y1="184" x2="${x}" y2="760" class="lifeline"/>\n`;
  }
  b += seqBand(82, 222, 254, "1", "load Application with H2 properties", 0);
  b += seqArrow(105, 315, 276, "arrow");
  b += seqBand(292, 304, 304, "2", "ExposedAutoConfiguration creates beans", 1);
  b += seqArrow(315, 545, 358, "arrow");
  b += seqBand(520, 386, 312, "3", "SpringTransactionManager is registered", 5);
  b += seqArrow(545, 775, 440, "arrow");
  b += seqBand(740, 468, 318, "4", "generate-ddl=true runs initializer", 4);
  b += seqArrow(545, 1005, 522, "runtimeUse");
  b += seqArrow(1005, 1215, 550, "dbUse");
  b += seqBand(982, 604, 284, "5", "sync/async/coroutine queries table", 2);
  b += seqArrow(775, 1215, 658, "dbUse");
  b += seqBand(1166, 718, 248, "6", "assert empty rows and missing name", 6);
  b += seqArrow(1215, 1390, 690, "returnLine");
  b += note(126, 830, 1248, "Sequence rule: bean creation and table verification are solid calls; result checks return dashed. DatabaseInitializer is conditional on spring.exposed.generate-ddl=true.");
  return b;
}

function transactionTemplateClass() {
  let b = "";
  b += panel(56, 130, 420, 740, "Configuration and service");
  b += uml(104, 172, 324, 170, "TransactionTemplateConfig", ["@Configuration(proxyBeanMethods=false)"], ["exposedTransactionTemplate(tm)", "withoutTransactionOperations()"], 0);
  b += uml(104, 404, 324, 220, "BookService", ["exposedTransactionTemplate", "withoutTransactionOperations"], ["executeSpringTransaction()", "execWithExposedTransaction()", "execWithoutSpringTransaction()"], 1);
  b += uml(104, 714, 324, 122, "TransactionTemplateTest", ["REPEAT_SIZE = 5"], ["verifies author count +1"], 4);

  b += panel(548, 130, 342, 740, "Spring transaction beans");
  b += uml(600, 182, 238, 132, "SpringTransactionManager", ["Exposed transaction manager"], ["used by TransactionTemplate"], 5);
  b += uml(600, 394, 238, 132, "TransactionTemplate", ["Qualifier exposedTransactionTemplate"], ["execute { createNewAuthor() }"], 6);
  b += uml(600, 616, 238, 132, "TransactionOperations", ["withoutTransaction()"], ["execute { transaction { ... } }"], 7);

  b += panel(962, 130, 482, 740, "BookSchema domain");
  b += uml(1000, 166, 150, 58, "LongIdTable", [], [], 7);
  b += uml(1240, 166, 120, 58, "Table", [], [], 7);
  b += uml(982, 294, 176, 150, "AuthorTable", ["name: varchar(50)", "description: text?"], ["LongIdTable(\"authors\")"], 2);
  b += uml(1184, 294, 176, 150, "BookTable", ["title: varchar(255)", "description: text?"], ["LongIdTable(\"books\")"], 2);
  b += uml(1208, 476, 190, 112, "BookAuthorTable", ["book_id -> books", "author_id -> authors"], ["many-to-many bridge"], 4);
  b += uml(1000, 570, 160, 58, "LongEntity", [], [], 7);
  b += uml(984, 700, 174, 160, "Author", ["name", "description"], ["books via bridge"], 5);
  b += uml(1216, 700, 174, 160, "Book", ["title", "description"], ["authors via bridge"], 5);

  b += inheritRouteFrom("batch-17-transaction-class", "LongIdTable", "AuthorTable", 1075, 224, 1070, 294, 258);
  b += inheritRouteFrom("batch-17-transaction-class", "LongIdTable", "BookTable", 1075, 224, 1272, 294, 258);
  b += inheritSideRouteFrom("batch-17-transaction-class", "Table", "BookAuthorTable", 1300, 224, 1303, 476, 1418, 448, 258);
  b += inheritRouteFrom("batch-17-transaction-class", "LongEntity", "Author", 1080, 628, 1071, 700, 664);
  b += inheritRouteFrom("batch-17-transaction-class", "LongEntity", "Book", 1080, 628, 1303, 700, 664);
  b += gpath("batch-17-transaction-class", "TransactionTemplateConfig", "TransactionTemplate", "M428,253 L600,448", "arrow", "M428,253 L514,253 L514,448 L600,448");
  b += gpath("batch-17-transaction-class", "BookService", "TransactionTemplate", "M428,491 L600,448", "arrow", "M428,491 L514,491 L514,448 L600,448");
  b += gpath("batch-17-transaction-class", "BookService", "TransactionOperations", "M428,538 L600,682", "runtimeUse", "M428,538 L530,538 L530,682 L600,682");
  b += gpath("batch-17-transaction-class", "TransactionTemplateTest", "BookService", "M266,714 L266,624", "arrow");
  b += gpath("batch-17-transaction-class", "BookService", "AuthorTable", "M428,548 L982,369", "mapLine", "M428,548 L912,548 L912,369 L982,369");
  b += gpath("batch-17-transaction-class", "BookService", "Book", "M428,590 L1216,780", "mapLine", "M428,590 L912,590 L912,682 L1196,682 L1196,780 L1216,780");
  b += gpath("batch-17-transaction-class", "BookAuthorTable", "AuthorTable", "M1208,532 L1158,369", "dbUse", "M1208,532 L1176,532 L1176,369 L1158,369");
  b += gpath("batch-17-transaction-class", "BookAuthorTable", "BookTable", "M1350,476 L1350,444", "dbUse");
  b += note(168, 900, 1164, "Source check: BookService creates Author rows through TransactionTemplate/TransactionOperations and Book rows through Exposed transaction; BookAuthorTable is the bridge table.");
  return b;
}

function transactionTemplateSequence() {
  let b = "";
  const participants = [
    [105, "Test"],
    [320, "BookService"],
    [540, "TxTemplate"],
    [760, "Spring TM"],
    [980, "Exposed tx"],
    [1210, "BookSchema"],
    [1390, "JdbcTemplate"],
  ];
  for (const [x, label] of participants) {
    b += card(x - 76, 130, 152, 54, label, 0, "");
    b += `<line x1="${x}" y1="184" x2="${x}" y2="846" class="lifeline"/>\n`;
  }
  b += seqBand(82, 222, 274, "1", "executeSpringTransaction()", 1);
  b += seqArrow(105, 320, 276, "arrow");
  b += seqArrow(320, 540, 304, "arrow");
  b += seqArrow(540, 760, 332, "arrow");
  b += seqArrow(760, 1210, 360, "dbUse");
  b += seqBand(296, 410, 300, "2", "execWithExposedTransaction()", 5);
  b += seqArrow(105, 320, 464, "arrow");
  b += seqArrow(320, 980, 492, "runtimeUse");
  b += seqArrow(980, 1210, 520, "dbUse");
  b += seqBand(296, 570, 340, "3", "withoutTransactionOperations + transaction{}", 7);
  b += seqArrow(105, 320, 624, "arrow");
  b += seqArrow(320, 980, 652, "runtimeUse");
  b += seqArrow(980, 1210, 680, "dbUse");
  b += seqBand(298, 724, 338, "4", "@Transactional inherits outer boundary", 4);
  b += seqArrow(105, 320, 792, "arrow");
  b += seqArrow(320, 760, 816, "arrow");
  b += seqArrow(760, 1210, 840, "dbUse");
  b += seqArrow(1390, 1210, 858, "returnLine");
  b += note(126, 876, 1248, "Source check: tests repeat five execution modes and verify author count increments through JdbcTemplate SELECT COUNT(*) FROM AUTHORS.");
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

function uml(x, y, w, h, name, fields, methods, c) {
  const [fill, stroke] = colors[c % colors.length];
  const headerH = Math.min(54, Math.max(44, h * 0.34));
  const fieldH = fields.length ? Math.min(82, fields.length * 24 + 22) : 0;
  let out = `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="4" fill="#fff" stroke="${stroke}" class="uml"/>
<rect x="${x}" y="${y}" width="${w}" height="${headerH}" rx="4" fill="${fill}" stroke="${stroke}" stroke-width="1.7"/>
<line x1="${x}" y1="${y + headerH}" x2="${x + w}" y2="${y + headerH}" stroke="${stroke}" stroke-width="1.2"/>
<text x="${x + w / 2}" y="${y + headerH / 2 + 6}" class="className" text-anchor="middle">${esc(name)}</text>\n`;
  if (fields.length) {
    fields.forEach((field, i) => (out += `<text x="${x + 18}" y="${y + headerH + 24 + i * 22}" class="detail">${esc(field)}</text>\n`));
    out += `<line x1="${x}" y1="${y + headerH + fieldH}" x2="${x + w}" y2="${y + headerH + fieldH}" stroke="${stroke}" stroke-width="1.2"/>\n`;
  }
  methods.forEach((method, i) => (out += `<text x="${x + 18}" y="${y + headerH + fieldH + 24 + i * 22}" class="detail">${esc(method)}</text>\n`));
  return out;
}

function inherit(x1, y1, x2, y2) {
  const triY = y1 + 16;
  return `<path d="M${x2},${y2} L${x2},${triY}" class="inherit"/>
<path d="M${x1},${y1} L${x1 - 10},${triY} L${x1 + 10},${triY} Z" fill="#fff" stroke="#334155" stroke-width="1.8"/>\n`;
}

function inheritRoute(parentX, parentY, childX, childY, routeY) {
  const triY = parentY + 16;
  return `<path d="M${childX},${childY} L${childX},${routeY} L${parentX},${routeY} L${parentX},${triY}" class="inherit"/>
<path d="M${parentX},${parentY} L${parentX - 10},${triY} L${parentX + 10},${triY} Z" fill="#fff" stroke="#334155" stroke-width="1.8"/>\n`;
}

function inheritSideRoute(parentX, parentY, childX, childY, laneX, lowY, highY) {
  const triY = parentY + 16;
  return `<path d="M${childX},${childY} L${childX},${lowY} L${laneX},${lowY} L${laneX},${highY} L${parentX},${highY} L${parentX},${triY}" class="inherit"/>
<path d="M${parentX},${parentY} L${parentX - 10},${triY} L${parentX + 10},${triY} Z" fill="#fff" stroke="#334155" stroke-width="1.8"/>\n`;
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

function parseGraphvizPlain(plain) {
  return plain
    .split(/\n/)
    .filter((line) => line.startsWith("edge "))
    .map((line) => {
      const parts = line.trim().split(/\s+/);
      const count = Number(parts[3]);
      const points = [];
      for (let i = 0; i < count; i++) {
        points.push({ x: Number(parts[4 + i * 2]), y: Number(parts[5 + i * 2]) });
      }
      return {
        from: parts[1],
        to: parts[2],
        points,
        firstLeg: legDirection(points[0], points[1]),
        lastLeg: legDirection(points[points.length - 2], points[points.length - 1]),
      };
    });
}

function legDirection(a, b) {
  if (!a || !b) return "unknown";
  if (Math.abs(a.x - b.x) >= Math.abs(a.y - b.y)) return b.x >= a.x ? "right" : "left";
  return b.y >= a.y ? "down" : "up";
}

function requireGraphvizRoute(sketch, from, to) {
  const routes = graphvizRouting.get(sketch) || [];
  const route = routes.find((item) => item.from === from && item.to === to);
  if (!route) throw new Error(`missing_graphviz_route ${sketch}: ${from}->${to}`);
  if (route.points.length < 2) throw new Error(`insufficient_graphviz_route ${sketch}: ${from}->${to}`);
  return route;
}

function gpath(sketch, from, to, defaultD, cls, explicitD = null) {
  requireGraphvizRoute(sketch, from, to);
  return path(defaultD, cls, explicitD);
}

function inheritFrom(sketch, from, to, x1, y1, x2, y2) {
  requireGraphvizRoute(sketch, from, to);
  return inherit(x1, y1, x2, y2);
}

function inheritRouteFrom(sketch, from, to, parentX, parentY, childX, childY, routeY) {
  requireGraphvizRoute(sketch, from, to);
  return inheritRoute(parentX, parentY, childX, childY, routeY);
}

function inheritSideRouteFrom(sketch, from, to, parentX, parentY, childX, childY, laneX, lowY, highY) {
  requireGraphvizRoute(sketch, from, to);
  return inheritSideRoute(parentX, parentY, childX, childY, laneX, lowY, highY);
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

function validateBatch17Semantics() {
  const svgs = diagrams.map((d) => fs.readFileSync(d.file, "utf8"));
  const failures = [];
  for (const required of ["01 AutoConfigure", "02 TransactionTemplate", "05 Coroutine Repository", "07 Suspended Cache"]) {
    if (!svgs[0].includes(required)) failures.push(`spring architecture missing ${required}`);
  }
  for (const required of ["Application", "DatabaseInitializer", "TestTable", "TestEntity", "AsyncExposedService"]) {
    if (!svgs[1].includes(required)) failures.push(`autoconfig class missing ${required}`);
  }
  for (const required of ["SpringTransactionManager", "generate-ddl=true", "TestTable", "sync/async/coroutine"]) {
    if (!svgs[2].includes(required)) failures.push(`autoconfig sequence missing ${required}`);
  }
  for (const required of ["TransactionTemplateConfig", "BookService", "AuthorTable", "BookAuthorTable", "LongEntity"]) {
    if (!svgs[3].includes(required)) failures.push(`transaction class missing ${required}`);
  }
  for (const required of ["executeSpringTransaction", "execWithExposedTransaction", "withoutTransactionOperations", "@Transactional"]) {
    if (!svgs[4].includes(required)) failures.push(`transaction sequence missing ${required}`);
  }
  if (failures.length) throw new Error(`batch17_semantic_validation_failed\n${failures.join("\n")}`);
  console.log("batch17_semantics=ok");
}

function validateConnectorGeometry() {
  const connectorClasses = "(?:arrow|mapLine|dbUse|runtimeUse|returnLine|inherit)";
  const failures = [];
  for (const diagram of diagrams) {
    const svg = fs.readFileSync(diagram.file, "utf8");
    const blockers = [
      ...svg.matchAll(/<rect x="([\d.]+)" y="([\d.]+)" width="([\d.]+)" height="([\d.]+)" rx="10"[^>]*class="card"/g),
      ...svg.matchAll(/<rect x="([\d.]+)" y="([\d.]+)" width="([\d.]+)" height="([\d.]+)" rx="4"[^>]*class="uml"/g),
    ].map((m) => ({ x: Number(m[1]), y: Number(m[2]), w: Number(m[3]), h: Number(m[4]) }));
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
        for (const rect of blockers) {
          if (segmentCrossesInterior(a, b, rect)) failures.push(`${diagram.file}: connector crosses component interior ${cls} ${a.x},${a.y}->${b.x},${b.y}`);
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
  if (failures.length) throw new Error(`batch17_connector_validation_failed\n${failures.join("\n")}`);
  console.log("batch17_connectors=ok");
}

function segmentCrossesInterior(a, b, rect) {
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
