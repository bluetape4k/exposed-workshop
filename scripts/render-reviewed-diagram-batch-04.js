#!/usr/bin/env node

const fs = require("fs");
const { spawnSync } = require("child_process");

const RSVG = process.env.RSVG_CONVERT || "rsvg-convert";
const DOT = process.env.DOT || "dot";

const style = `
.canvas{fill:#f6f9fc}.frame{fill:#fff;stroke:#d7e2ec;stroke-width:1.5}
.title{font-family:"Architects Daughter","Comic Sans MS",cursive;font-size:31px;fill:#1e293b}
.subtitle,.detail{font-family:"Comic Mono","Comic Sans MS","Comic Sans","Comic Neue",Arial,sans-serif;fill:#536476}
.subtitle{font-size:13px}.detail{font-size:12px}.tiny{font-family:"Comic Mono","Comic Sans MS","Comic Sans","Comic Neue",Arial,sans-serif;font-size:10px;fill:#64748b}
.label{font-family:"Architects Daughter","Comic Sans MS",cursive;font-size:17px;fill:#1e293b}
.panelTitle{font-family:"Architects Daughter","Comic Sans MS",cursive;font-size:12px;letter-spacing:.08em;fill:#94a3b8}
.card{stroke-width:1.7;filter:url(#shadow)}.panel{fill:#f8fafc;stroke:#d7e2ec;stroke-width:1.2}
.arrow{fill:none;stroke:#64748b;stroke-width:2;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrow)}
.queryUse{fill:none;stroke:#2563eb;stroke-width:2;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowBlue)}
.mapUse{fill:none;stroke:#16a34a;stroke-width:1.8;stroke-dasharray:5 5;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowGreen)}
.runtimeUse{fill:none;stroke:#7c3aed;stroke-width:1.8;stroke-dasharray:5 5;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowPurple)}
.dbUse{fill:none;stroke:#ea580c;stroke-width:2;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowOrange)}
.fkArrow{fill:none;stroke:#0f766e;stroke-width:2;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowTeal)}
.assoc{fill:none;stroke:#0f766e;stroke-width:1.9;stroke-linecap:round;stroke-linejoin:round}
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
    file: "docs/images/readme-diagrams/02-alternatives-to-jpa-vertx-sqlclient-example-architecture-01.svg",
    title: "Vert.x SQL Client Architecture Flow",
    subtitle: "JDBC Pool and SqlTemplate examples execute direct SQL on coroutine-aware Vert.x pools",
    width: 1460,
    height: 860,
    body: vertxArchitecture,
  },
  {
    file: "docs/images/readme-diagrams/02-alternatives-to-jpa-vertx-sqlclient-example-erd-02.svg",
    title: "Vert.x SQL Client ERD",
    subtitle: "The examples create SQL tables directly; no ORM metadata or foreign keys are inferred",
    width: 1160,
    height: 720,
    body: vertxErd,
  },
  {
    file: "docs/images/readme-diagrams/03-exposed-basic-class-01.svg",
    title: "Exposed Basic User Cities Domain",
    subtitle: "The chapter compares SQL DSL table objects with DAO entity modeling over the same City/User shape",
    width: 1500,
    height: 900,
    body: exposedBasicDomain,
  },
  {
    file: "docs/images/readme-diagrams/03-exposed-basic-exposed-dao-example-erd-01.svg",
    title: "Exposed DAO User Cities ERD",
    subtitle: "users.city_id is a nullable FK to cities.id; sample data includes users without a city",
    width: 1120,
    height: 760,
    body: daoErd,
  },
  {
    file: "docs/images/readme-diagrams/03-exposed-basic-exposed-dao-example-class-02.svg",
    title: "Exposed DAO Domain Model",
    subtitle: "IntIdTable and IntEntity supertypes stay above concrete tables and entities",
    width: 1480,
    height: 880,
    body: daoClass,
  },
];

fs.mkdirSync(".omx/artifacts/diagram-sketches", { recursive: true });
writeGraphvizSketches();

function writeGraphvizSketches() {
  const sketches = {
    "batch-04-vertx-architecture": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=0.72, ranksep=1.05, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8", margin="0.12,0.08"];
      edge [color="#64748b", arrowsize=0.7];
      { rank=same; jdbc_examples; template_examples; test_context; }
      { rank=same; event_loop; abstract_test; jdbc_pool; pg_pool; tx; }
      { rank=same; pool_query; sql_template; row_mapper; tuple_mapper; }
      { rank=same; h2_db; postgres_db; }
      jdbc_examples [label="JDBCPoolExamples"];
      template_examples [label="SqlClientTemplate\\nPostgresExamples"];
      test_context [label="VertxTestContext"];
      event_loop [label="Vertx Event Loop"];
      abstract_test [label="AbstractSqlClientTest"];
      jdbc_pool [label="JDBCPool"];
      pg_pool [label="PgPool"];
      tx [label="withSuspendTransaction"];
      pool_query [label="Pool.query\\npreparedQuery"];
      sql_template [label="SqlTemplate"];
      row_mapper [label="RowMapper"];
      tuple_mapper [label="TupleMapper"];
      h2_db [shape=cylinder, label="H2 memory DB"];
      postgres_db [shape=cylinder, label="PostgreSQL\\ncustomers"];
      jdbc_examples -> event_loop -> abstract_test -> jdbc_pool;
      pool_query -> jdbc_pool -> h2_db;
      template_examples -> event_loop;
      abstract_test -> pg_pool;
      sql_template -> pg_pool -> postgres_db;
      test_context -> tx;
      tx -> jdbc_pool;
      tx -> pg_pool;
      sql_template -> row_mapper;
      sql_template -> tuple_mapper;
    }`,
    "batch-04-vertx-erd": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=1.2, ranksep=1.6, pad=0.2];
      node [shape=record, style="rounded,filled", fillcolor="#ffffff", color="#94a3b8"];
      h2 [label="{test|id INT PK\\lname VARCHAR(255)}"];
      customers [label="{customers|id BIGINT PK\\lfirst_name VARCHAR(255)\\llast_name VARCHAR(255)\\lemail VARCHAR(255) NULL\\lmobile VARCHAR(255) NULL\\lage INT NULL}"];
      h2 -> customers [style=invis];
    }`,
    "batch-04-exposed-basic-domain": `digraph G {
      graph [rankdir=TB, splines=ortho, nodesep=0.65, ranksep=0.72, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      subgraph cluster_sql {
        label="SQL DSL";
        table_base -> city_table;
        table_base -> user_table;
        user_table -> city_table [label="nullable FK"];
        city_table -> sql_example;
        user_table -> suspended_example;
        tx -> sql_example;
        tx -> suspended_example;
      }
      subgraph cluster_dao {
        label="DAO";
        int_id_table -> dao_city_table;
        int_id_table -> dao_user_table;
        int_entity -> city_entity;
        int_entity -> user_entity;
        dao_user_table -> dao_city_table [label="nullable FK"];
        user_entity -> city_entity [label="optional reference"];
      }
    }`,
    "batch-04-dao-erd": `digraph G {
      graph [rankdir=LR, splines=ortho, nodesep=1.4, ranksep=1.8, pad=0.2];
      node [shape=record, style="rounded,filled", fillcolor="#ffffff", color="#94a3b8"];
      cities [label="{cities|id SERIAL PK\\lname VARCHAR(50) NOT NULL}"];
      users [label="{users|id SERIAL PK\\lname VARCHAR(50) NOT NULL\\lage INT NOT NULL\\lcity_id INT NULL FK}"];
      users -> cities [label="users.city_id"];
    }`,
    "batch-04-dao-class": `digraph G {
      graph [rankdir=TB, splines=ortho, nodesep=0.75, ranksep=0.8, pad=0.2];
      node [shape=box, style="rounded,filled", fillcolor="#f8fafc", color="#94a3b8"];
      int_id_table -> city_table;
      int_id_table -> user_table;
      int_entity -> city;
      int_entity -> user;
      int_entity_class -> city;
      int_entity_class -> user;
      user_table -> city_table [label="nullable FK"];
      user -> city [label="optionalReferencedOn"];
      abstract_test -> dao_sync_tests;
      abstract_test -> dao_coroutine_tests;
      city -> sample_data;
      user -> sample_data;
    }`,
  };

  for (const [name, dot] of Object.entries(sketches)) {
    const dotPath = `.omx/artifacts/diagram-sketches/${name}.dot`;
    fs.writeFileSync(dotPath, dot);
    spawnSync(DOT, ["-Tsvg", "-o", `.omx/artifacts/diagram-sketches/${name}.svg`, dotPath], { encoding: "utf8" });
    spawnSync(DOT, ["-Tplain", "-o", `.omx/artifacts/diagram-sketches/${name}.plain`, dotPath], { encoding: "utf8" });
  }
}

function vertxArchitecture() {
  let b = panel(48, 126, 298, 596, "Test entry points");
  b += card(80, 174, 236, 78, "JDBCPoolExamples", 0, "H2 JDBC pool tests");
  b += card(80, 364, 236, 92, "SqlClientTemplate Postgres Examples", 4, "PostgreSQL SqlTemplate tests");
  b += card(80, 602, 236, 66, "VertxTestContext", 1, "async test boundary");

  b += panel(396, 126, 306, 596, "Vert.x runtime");
  b += card(436, 164, 256, 68, "Vertx Event Loop", 1, "coroutine dispatcher");
  b += card(436, 282, 256, 76, "AbstractSqlClientTest", 7, "pool factory helpers");
  b += card(436, 632, 256, 52, "withSuspendTransaction", 2, "SqlConnection boundary");

  b += panel(742, 126, 204, 596, "Direct SQL API");
  b += card(774, 404, 150, 72, "Pool.query / preparedQuery", 0, "String SQL + Tuple");
  b += card(774, 512, 150, 72, "SqlTemplate", 6, "#{param} binding");
  b += card(770, 632, 78, 50, "Row Mapper", 2, "Row -> domain");
  b += card(856, 632, 78, 50, "Tuple Mapper", 4, "domain -> params");

  b += panel(988, 126, 206, 596, "Pool owners");
  b += card(1032, 404, 118, 72, "JDBCPool", 3, "H2");
  b += card(1032, 512, 118, 72, "PgPool", 5, "Postgres");

  b += panel(1230, 126, 182, 596, "Database targets");
  b += cylinder(1254, 380, 132, 132, "H2 DB", ["test", "id PK", "name"], 3);
  b += cylinder(1254, 548, 132, 154, "PostgreSQL", ["customers", "id PK", "first_name", "last_name"], 5);

  b += `<path d="M316,213 L376,213 L376,198 L436,198" class="runtimeUse"/>
<path d="M316,410 L376,410 L376,214 L436,214" class="runtimeUse"/>
<path d="M316,635 L386,635 L386,658 L436,658" class="runtimeUse"/>
<path d="M564,232 L564,282" class="runtimeUse"/>
<path d="M692,320 L972,320 L972,440 L1032,440" class="dbUse"/>
<path d="M692,336 L958,336 L958,548 L1032,548" class="dbUse"/>
<path d="M692,658 L718,658 L718,492 L968,492 L968,440 L1032,440" class="runtimeUse"/>
<path d="M692,670 L708,670 L708,610 L968,610 L968,548 L1032,548" class="runtimeUse"/>
<path d="M924,440 L1032,440" class="queryUse"/>
<path d="M924,548 L1032,548" class="queryUse"/>
<path d="M1150,422 L1210,422 L1210,446 L1254,446" class="dbUse"/>
<path d="M1150,566 L1210,566 L1210,625 L1254,625" class="dbUse"/>
<path d="M809,584 L809,632" class="mapUse"/>
<path d="M895,584 L895,632" class="mapUse"/>\n`;
  b += note(102, 738, 1248, "Source check: H2 tests use Pool.query/preparedQuery against table test; PostgreSQL tests use SqlTemplate with CustomerRowMapper and tupleMapperOfRecord.");
  return b;
}

function vertxErd() {
  let b = panel(74, 128, 424, 300, "H2 setup table");
  b += erdTable(120, 170, 330, "test", ["id INT PK", "name VARCHAR(255)", "seed rows: Hello, World"], 3, 150);
  b += card(142, 350, 286, 46, "Created in JDBCPoolExamples.beforeAll", 0);

  b += panel(620, 128, 424, 372, "PostgreSQL SqlTemplate table");
  b += erdTable(666, 170, 330, "customers", ["id BIGINT PK", "first_name VARCHAR(255) NOT NULL", "last_name VARCHAR(255) NOT NULL", "email VARCHAR(255) NULL", "mobile VARCHAR(255) NULL", "age INT NULL"], 5, 202);
  b += card(690, 406, 282, 54, "Created in setup", 4, "SqlClientTemplatePostgresExamples");

  b += panel(180, 536, 796, 72, "Relationship rule");
  b += `<text x="578" y="580" class="detail" text-anchor="middle">No FK line is drawn: the current source creates two independent example schemas.</text>\n`;
  b += `<rect x="504" y="242" width="132" height="30" rx="8" fill="#fff7ed" stroke="#fed7aa"/>
<text x="570" y="262" class="tiny" text-anchor="middle">separate schemas</text>\n`;
  return b;
}

function exposedBasicDomain() {
  let b = panel(48, 124, 660, 674, "SQL DSL path");
  b += umlClass(92, 164, 220, 90, "Table", ["base for plain DSL tables", "explicit PrimaryKey"], 7, "Exposed base");
  b += umlClass(426, 164, 220, 90, "JdbcTransaction", ["withTables()", "withTablesSuspending()"], 1, "transaction");
  b += umlClass(92, 326, 224, 132, "CityTable", ["Table(\"cities\")", "id autoIncrement", "name varchar(50)", "PrimaryKey(id)"], 0, "SQL DSL table");
  b += umlClass(388, 326, 224, 148, "UserTable", ["Table(\"users\")", "id varchar(10)", "name varchar(50)", "cityId optReference"], 2, "SQL DSL table");
  b += umlClass(92, 584, 248, 130, "ExposedSQLExample", ["update/delete", "innerJoin/leftJoin", "groupBy count"], 6, "sync test");
  b += umlClass(388, 584, 248, 130, "ExposedSQLSuspendedExample", ["same DSL scenarios", "runSuspendIO", "suspended helper"], 4, "coroutine test");
  b += inheritArrow(204, 326, 204, 254, 286);
  b += inheritArrow(500, 326, 204, 254, 286);
  b += `<path d="M388,400 L316,400" class="fkArrow"/>
<rect x="328" y="382" width="48" height="24" rx="8" fill="#fff" stroke="#99f6e4"/>
<text x="352" y="399" class="tiny" text-anchor="middle">FK</text>
<path d="M204,458 L204,584" class="queryUse"/>
<path d="M500,474 L500,584" class="queryUse"/>
<path d="M426,210 L360,210 L360,649 L340,649" class="runtimeUse"/>
<path d="M536,254 L536,300 L674,300 L674,550 L512,550 L512,584" class="runtimeUse"/>\n`;

  b += panel(792, 124, 660, 674, "DAO entity path");
  b += umlClass(832, 164, 210, 90, "IntIdTable", ["id EntityID<Int>", "SERIAL PK"], 7, "Exposed base");
  b += umlClass(1120, 164, 210, 90, "IntEntity", ["id: EntityID<Int>", "entity cache"], 1, "Exposed base");
  b += umlClass(832, 326, 210, 132, "CityTable", ["IntIdTable(\"cities\")", "name varchar(50)"], 0, "DAO table");
  b += umlClass(1100, 326, 210, 148, "UserTable", ["IntIdTable(\"users\")", "name varchar(50)", "age integer", "cityId optReference"], 2, "DAO table");
  b += umlClass(832, 548, 210, 132, "City", ["var name", "users optionalReferrersOn"], 5, "DAO entity");
  b += umlClass(1100, 548, 210, 148, "User", ["var name", "var age", "city optionalReferencedOn"], 4, "DAO entity");
  b += inheritArrow(936, 326, 936, 254, 286);
  b += inheritArrow(1205, 326, 936, 254, 286);
  b += `<path d="M936,548 L936,516 L1342,516 L1342,286 L1225,286 L1225,270" class="inherit"/>
<path d="M1205,548 L1205,528 L1358,528 L1358,302 L1225,302 L1225,270" class="inherit"/>
<path d="M1225,254 L1213,270 L1237,270 Z" class="inheritHead"/>\n`;
  b += `<path d="M1100,400 L1042,400" class="fkArrow"/>
<path d="M1042,614 L1100,614" class="assoc"/>
<text x="1072" y="602" class="tiny" text-anchor="middle">City.users / User.city</text>
<path d="M1100,636 L1042,636" class="assoc"/>\n`;
  b += note(166, 826, 1168, "Root chapter diagram intentionally compares modeling paths; table-to-entity mapping lines are omitted because DAO names already pair them.");
  return b;
}

function daoErd() {
  let b = panel(76, 166, 420, 264, "Parent table");
  b += erdTable(124, 210, 322, "cities", ["id SERIAL PK", "name VARCHAR(50) NOT NULL"], 0, 132);

  b += panel(622, 166, 420, 296, "Child table");
  b += erdTable(670, 210, 322, "users", ["id SERIAL PK", "name VARCHAR(50) NOT NULL", "age INT NOT NULL", "city_id INT NULL FK"], 2, 166);

  b += `<path d="M670,296 L446,296" class="fkArrow"/>
<rect x="500" y="278" width="118" height="24" rx="8" fill="#fff" stroke="#99f6e4"/>
<text x="559" y="295" class="tiny" text-anchor="middle">users.city_id</text>
<text x="458" y="288" class="tiny">1</text>
<text x="652" y="288" class="tiny">0..N</text>\n`;

  b += panel(116, 520, 396, 118, "Sample shape");
  b += card(144, 552, 152, 52, "3 cities", 0, "Seoul Busan Daegu");
  b += card(326, 552, 152, 52, "4 users", 2, "alex has no city");
  b += note(136, 672, 850, "Source check: User.city is optionalReferencedOn UserTable.cityId; City.users is optionalReferrersOn the same nullable FK.");
  return b;
}

function daoClass() {
  let b = panel(68, 126, 600, 522, "Tables");
  b += umlClass(248, 164, 232, 92, "IntIdTable", ["id column", "EntityID<Int> primary key"], 7, "table base");
  b += umlClass(116, 394, 220, 126, "CityTable", ["IntIdTable(\"cities\")", "name varchar(50)"], 0, "object");
  b += umlClass(374, 394, 220, 150, "UserTable", ["IntIdTable(\"users\")", "name varchar(50)", "age integer", "cityId optReference"], 2, "object");
  b += inheritArrow(226, 394, 364, 256, 326);
  b += inheritArrow(484, 394, 364, 256, 326);
  b += `<path d="M374,468 L336,468" class="fkArrow"/>
<rect x="338" y="450" width="36" height="24" rx="8" fill="#fff" stroke="#99f6e4"/>
<text x="356" y="467" class="tiny" text-anchor="middle">FK</text>\n`;

  b += panel(758, 126, 600, 522, "Entities");
  b += umlClass(806, 164, 232, 92, "IntEntity", ["id: EntityID<Int>", "lifecycle in transaction"], 1, "entity base");
  b += umlClass(1064, 164, 260, 92, "IntEntityClass<T>", ["find/findById/all", "companion object delegate"], 6, "entity class");
  b += umlClass(806, 394, 220, 150, "City", ["companion: IntEntityClass", "var name", "users: SizedIterable<User>"], 5, "class");
  b += umlClass(1064, 394, 220, 166, "User", ["companion: IntEntityClass", "var name", "var age", "city: City?"], 4, "class");
  b += inheritArrow(916, 394, 922, 256, 326);
  b += inheritArrow(1174, 394, 922, 256, 326);
  b += `<path d="M916,394 L916,344 L1194,344 L1194,256" class="mapUse"/>
<path d="M1174,394 L1174,256" class="mapUse"/>
<path d="M1026,470 L1064,470" class="assoc"/>
<path d="M1064,492 L1026,492" class="assoc"/>
<text x="1045" y="458" class="tiny" text-anchor="middle">1:N</text>
<text x="1045" y="510" class="tiny" text-anchor="middle">N:0..1</text>\n`;

  b += panel(104, 694, 1220, 82, "Test harness and scenarios");
  b += card(136, 714, 224, 42, "AbstractExposedTest", 0);
  b += card(430, 714, 224, 42, "DAO sync tests", 0);
  b += card(704, 714, 270, 42, "DAO coroutine tests", 4);
  b += card(1024, 714, 246, 42, "Sample data helper", 2);
  b += `<path d="M360,735 L430,735" class="runtimeUse"/>
<path d="M248,714 L248,674 L839,674 L839,714" class="runtimeUse"/>
<path d="M916,544 L916,672 L1147,672 L1147,714" class="queryUse"/>\n`;
  b += note(142, 804, 1190, "UML check: hollow generalization triangles are explicit and the stems meet their bases from below at 90 degrees.");
  return b;
}

function shell(diagram) {
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${diagram.width} ${diagram.height}" width="${diagram.width}" height="${diagram.height}" role="img" aria-label="${esc(diagram.title)}">
<defs>
<filter id="shadow" x="-10%" y="-10%" width="120%" height="130%"><feDropShadow dx="2" dy="4" stdDeviation="5" flood-color="#1f2937" flood-opacity="0.12"/></filter>
<marker id="arrow" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto"><path d="M1,1 L8,4.5 L1,8 Z" fill="#64748b"/></marker>
<marker id="arrowBlue" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto"><path d="M1,1 L8,4.5 L1,8 Z" fill="#2563eb"/></marker>
<marker id="arrowGreen" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto"><path d="M1,1 L8,4.5 L1,8 Z" fill="#16a34a"/></marker>
<marker id="arrowOrange" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto"><path d="M1,1 L8,4.5 L1,8 Z" fill="#ea580c"/></marker>
<marker id="arrowPurple" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto"><path d="M1,1 L8,4.5 L1,8 Z" fill="#7c3aed"/></marker>
<marker id="arrowTeal" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto"><path d="M1,1 L8,4.5 L1,8 Z" fill="#0f766e"/></marker>
<style>${style}</style>
</defs>
<rect width="${diagram.width}" height="${diagram.height}" class="canvas"/>
<rect x="20" y="20" width="${diagram.width - 40}" height="${diagram.height - 40}" rx="16" class="frame"/>
<text x="48" y="58" class="title">${esc(diagram.title)}</text>
<text x="48" y="80" class="subtitle">${esc(diagram.subtitle)}</text>
${diagram.body()}</svg>
`;
}

function panel(x, y, w, h, title) {
  return `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="12" class="panel"/><text x="${x + w / 2}" y="${y - 8}" class="panelTitle" text-anchor="middle">${esc(title.toUpperCase())}</text>\n`;
}

function card(x, y, w, h, title, c, detail = "") {
  const [fill, stroke] = colors[c % colors.length];
  const titleLines = wrap(title, Math.floor(w / 10), 2);
  const shownDetail = titleLines.length > 1 && h < 68 ? "" : detail;
  const titleY = y + (shownDetail ? 25 : h / 2 - (titleLines.length - 1) * 10 + 6);
  let out = `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="10" fill="${fill}" stroke="${stroke}" class="card"/>\n`;
  titleLines.forEach((line, i) => (out += `<text x="${x + w / 2}" y="${titleY + i * 18}" class="label" text-anchor="middle">${esc(line)}</text>\n`));
  if (shownDetail) out += `<text x="${x + w / 2}" y="${y + h - 14}" class="detail" text-anchor="middle">${esc(shownDetail)}</text>\n`;
  return out;
}

function cylinder(x, y, w, h, title, rows, c) {
  const [fill, stroke] = colors[c % colors.length];
  const capH = 28;
  let out = `<path d="M${x},${y + capH / 2} C${x},${y - 4} ${x + w},${y - 4} ${x + w},${y + capH / 2} L${x + w},${y + h - capH / 2} C${x + w},${y + h + 4} ${x},${y + h + 4} ${x},${y + h - capH / 2} Z" fill="#fff" stroke="${stroke}" stroke-width="1.7" class="card"/>
<ellipse cx="${x + w / 2}" cy="${y + capH / 2}" rx="${w / 2}" ry="${capH / 2}" fill="${fill}" stroke="${stroke}" stroke-width="1.7"/>
<path d="M${x},${y + h - capH / 2} C${x},${y + h + 4} ${x + w},${y + h + 4} ${x + w},${y + h - capH / 2}" fill="none" stroke="${stroke}" stroke-width="1.7"/>
<text x="${x + w / 2}" y="${y + 23}" class="label" text-anchor="middle">${esc(title)}</text>\n`;
  rows.forEach((row, i) => (out += `<text x="${x + 18}" y="${y + 56 + i * 17}" class="tiny">${esc(row)}</text>\n`));
  return out;
}

function erdTable(x, y, w, title, rows, c, h = 170) {
  const [fill, stroke] = colors[c % colors.length];
  let out = `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="12" fill="#fff" stroke="${stroke}" class="card"/>
<path d="M${x},${y + 42} L${x},${y + 12} Q${x},${y} ${x + 12},${y} L${x + w - 12},${y} Q${x + w},${y} ${x + w},${y + 12} L${x + w},${y + 42} Z" fill="${fill}"/>
<text x="${x + w / 2}" y="${y + 28}" class="label" text-anchor="middle">${esc(title)}</text>\n`;
  rows.forEach((r, i) => (out += `<text x="${x + 18}" y="${y + 70 + i * 17}" class="tiny">${esc(r)}</text>\n`));
  return out;
}

function umlClass(x, y, w, h, title, rows, c, stereotype = "") {
  const [fill, stroke] = colors[c % colors.length];
  const titleLines = wrapIdentifier(title, Math.floor(w / 10), 2);
  const headerH = titleLines.length > 1 ? 62 : 48;
  let out = `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="4" fill="#fff" stroke="${stroke}" class="card"/>
<path d="M${x},${y + headerH} L${x},${y + 4} Q${x},${y} ${x + 4},${y} L${x + w - 4},${y} Q${x + w},${y} ${x + w},${y + 4} L${x + w},${y + headerH} Z" fill="${fill}"/>
<text x="${x + w / 2}" y="${y + 18}" class="tiny" text-anchor="middle">&lt;&lt;${esc(stereotype)}&gt;&gt;</text>\n`;
  titleLines.forEach((line, i) => (out += `<text x="${x + w / 2}" y="${y + 38 + i * 17}" class="label" text-anchor="middle">${esc(line)}</text>\n`));
  out += `<path d="M${x},${y + headerH} H${x + w}" stroke="${stroke}" stroke-width="1.1"/>\n`;
  const maxRows = Math.max(0, Math.floor((h - headerH - 12) / 15));
  rows.slice(0, maxRows).forEach((r, i) => (out += `<text x="${x + 14}" y="${y + headerH + 18 + i * 15}" class="tiny">${esc(r)}</text>\n`));
  return out;
}

function inheritArrow(childX, childY, apexX, apexY, laneY) {
  const baseY = apexY + 16;
  return `<path d="M${childX},${childY} L${childX},${laneY} L${apexX},${laneY} L${apexX},${baseY}" class="inherit"/>
<path d="M${apexX},${apexY} L${apexX - 12},${baseY} L${apexX + 12},${baseY} Z" class="inheritHead"/>\n`;
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
    } else line = next;
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
    } else line = next;
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

validateBatch04Semantics();
validateConnectorGeometry();

function validateBatch04Semantics() {
  const architecture = fs.readFileSync(diagrams[0].file, "utf8");
  const domain = fs.readFileSync(diagrams[2].file, "utf8");
  const daoClassSvg = fs.readFileSync(diagrams[4].file, "utf8");
  const requiredArchitectureEdges = [
    'M924,440 L1032,440',
    'M924,548 L1032,548',
    'M692,658 L718,658 L718,492 L968,492 L968,440 L1032,440',
    'M692,670 L708,670 L708,610 L968,610 L968,548 L1032,548',
    'M1150,422 L1210,422 L1210,446 L1254,446',
    'M1150,566 L1210,566 L1210,625 L1254,625',
  ];
  const forbiddenArchitectureEdges = [
    'M1064,440 L1198,440',
    'M1064,548 L1198,548',
    'M1064,657 L1198,657',
    'M924,440 L1254',
    'M924,548 L1254',
    'M1091,476 L1091,492 L968,492 L968,610 L692,610 L692,658',
    'M1091,584 L1091,610 L708,610 L708,670 L692,670',
  ];
  const requiredDomainEdges = [
    'M536,254 L536,300 L674,300 L674,550 L512,550 L512,584',
  ];
  const requiredDaoEdges = [
    'M248,714 L248,674 L839,674 L839,714',
  ];

  const failures = [];
  for (const edge of requiredArchitectureEdges) if (!architecture.includes(edge)) failures.push(`missing architecture edge: ${edge}`);
  for (const edge of forbiddenArchitectureEdges) if (architecture.includes(edge)) failures.push(`forbidden API-to-DB edge: ${edge}`);
  for (const edge of requiredDomainEdges) if (!domain.includes(edge)) failures.push(`missing 90-degree JdbcTransaction edge: ${edge}`);
  for (const edge of requiredDaoEdges) if (!daoClassSvg.includes(edge)) failures.push(`missing separated test harness edge: ${edge}`);
  if (failures.length) throw new Error(`batch04_semantic_validation_failed\n${failures.join("\n")}`);
  console.log("batch04_semantics=ok");
}

function validateConnectorGeometry() {
  const connectorClasses = "(?:arrow|queryUse|mapUse|runtimeUse|dbUse|fkArrow|assoc|inherit)";
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
        for (const cardRect of cards) {
          const endpoint =
            (i === 0 && pointInside(cardRect, points[i])) ||
            (i === points.length - 2 && pointInside(cardRect, points[i + 1]));
          const crossing = segmentCrossesRect(cardRect, points[i], points[i + 1]);
          if (crossing && !endpoint) failures.push(`${diagram.file}: ${crossing} ${points[i].x},${points[i].y}->${points[i + 1].x},${points[i + 1].y}`);
        }
      }
    }
  }

  if (failures.length) throw new Error(`batch04_connector_validation_failed\n${failures.join("\n")}`);
  console.log("batch04_connectors=ok");
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
