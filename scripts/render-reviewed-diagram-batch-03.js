#!/usr/bin/env node

const fs = require("fs");
const { spawnSync } = require("child_process");

const RSVG = process.env.RSVG_CONVERT || "rsvg-convert";

const style = `
.canvas{fill:#f6f9fc}.frame{fill:#fff;stroke:#d7e2ec;stroke-width:1.5}
.title{font-family:"Architects Daughter","Comic Sans MS",cursive;font-size:32px;fill:#1e293b}
.subtitle,.detail{font-family:"Comic Sans MS","Comic Sans","Comic Neue",Arial,sans-serif;fill:#536476}
.subtitle{font-size:13px}.detail{font-size:12px}.tiny{font-family:"Comic Sans MS","Comic Sans","Comic Neue",Arial,sans-serif;font-size:10px;fill:#64748b}
.label{font-family:"Architects Daughter","Comic Sans MS",cursive;font-size:17px;fill:#1e293b}
.panelTitle{font-family:"Architects Daughter","Comic Sans MS",cursive;font-size:12px;letter-spacing:.08em;fill:#94a3b8}
.card{stroke-width:1.7;filter:url(#shadow)}.panel{fill:#f8fafc;stroke:#d7e2ec;stroke-width:1.2}
.arrow{fill:none;stroke:#64748b;stroke-width:2;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrow)}
.softArrow{fill:none;stroke:#94a3b8;stroke-width:1.35;stroke-dasharray:5 5;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrow)}
.fkArrow{fill:none;stroke:#0f766e;stroke-width:2;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowTeal)}
.mapArrow{fill:none;stroke:#16a34a;stroke-width:1.7;stroke-dasharray:5 5;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowGreen)}
.sessionUse{fill:none;stroke:#7c3aed;stroke-width:1.7;stroke-dasharray:5 5;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowPurple)}
.repoUse{fill:none;stroke:#2563eb;stroke-width:1.7;stroke-dasharray:5 5;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowBlue)}
.opsUse{fill:none;stroke:#16a34a;stroke-width:1.7;stroke-dasharray:5 5;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowGreen)}
.clientUse{fill:none;stroke:#ea580c;stroke-width:1.7;stroke-dasharray:5 5;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowOrange)}
.converterUse{fill:none;stroke:#db2777;stroke-width:1.7;stroke-dasharray:5 5;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrowPink)}
.lifeline{fill:none;stroke:#94a3b8;stroke-width:1.5;stroke-dasharray:5 5;stroke-linecap:round;stroke-linejoin:round}
.inherit{fill:none;stroke:#475569;stroke-width:2.2;stroke-linecap:round;stroke-linejoin:round}
.inheritHead{fill:#fff;stroke:#475569;stroke-width:1.8;stroke-linejoin:round}
.assoc{fill:none;stroke:#0f766e;stroke-width:1.9;stroke-linecap:round;stroke-linejoin:round}
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
    file: "docs/images/readme-diagrams/02-alternatives-to-jpa-hibernate-reactive-example-class-03.svg",
    title: "Hibernate Reactive Class Model",
    subtitle: "JPA entities extend AbstractValueObject; repositories extend the Mutiny session base",
    width: 1500,
    height: 900,
    body: hibernateClass,
  },
  {
    file: "docs/images/readme-diagrams/02-alternatives-to-jpa-hibernate-reactive-example-erd-02.svg",
    title: "Hibernate Reactive ERD",
    subtitle: "Team and Member JPA entities map to a one-to-many relationship through member.team",
    width: 1220,
    height: 760,
    body: hibernateErd,
  },
  {
    file: "docs/images/readme-diagrams/02-alternatives-to-jpa-r2dbc-example-sequence-01.svg",
    title: "R2DBC Comment Request Flow",
    subtitle: "PostController returns a Flow by delegating Criteria queries to R2dbcEntityOperations",
    width: 1320,
    height: 780,
    body: r2dbcSequence,
  },
  {
    file: "docs/images/readme-diagrams/02-alternatives-to-jpa-r2dbc-example-class-03.svg",
    title: "R2DBC Class Model",
    subtitle: "Spring Data R2DBC repositories use coroutine flows, entity operations, and mapped table records",
    width: 1460,
    height: 880,
    body: r2dbcClass,
  },
  {
    file: "docs/images/readme-diagrams/02-alternatives-to-jpa-r2dbc-example-erd-02.svg",
    title: "R2DBC Post Comment ERD",
    subtitle: "comments.post_id is the only foreign key; customer is an independent repository example",
    width: 1180,
    height: 760,
    body: r2dbcErd,
  },
];

function hibernateClass() {
  let b = panel(46, 120, 1408, 160, "Supertypes and framework services");
  b += umlClass(82, 150, 250, 92, "AbstractValueObject", ["equals/hashCode contract", "buildStringHelper()"], 0, "base class");
  b += umlClass(384, 150, 300, 92, "AbstractMutinySessionRepository<T, ID>", ["findById(session, id)", "findAll(session)", "save/delete helpers"], 1, "repository base");
  b += umlClass(744, 150, 210, 92, "SessionFactory", ["withSessionSuspending", "Mutiny session source"], 5, "Hibernate Reactive");
  b += umlClass(1040, 150, 220, 92, "ApplicationRunner", ["run(args)"], 6, "Spring contract");

  b += panel(56, 330, 422, 270, "JPA entities");
  b += umlClass(88, 370, 180, 118, "Team", ["id: Long", "name: String", "members: MutableList<Member>"], 2, "entity");
  b += umlClass(286, 370, 160, 118, "Member", ["id: Long", "name: String", "team: Team?"], 3, "entity");
  b += inheritArrow(178, 370, 178, 242, 304);
  b += inheritArrow(366, 370, 252, 242, 304);
  b += `<path d="M268,430 L286,430" class="assoc"/>
<text x="277" y="420" class="tiny" text-anchor="middle">1:N</text>
<text x="124" y="514" class="tiny">@OneToMany</text>
<text x="326" y="514" class="tiny">@ManyToOne</text>\n`;

  b += panel(516, 330, 372, 270, "Repositories");
  b += umlClass(548, 370, 150, 118, "TeamSessionRepository", ["findAllByName()", "findAllByMemberName()", "fetch members"], 4, "repository");
  b += umlClass(716, 370, 150, 118, "MemberSessionRepository", ["findAllByName()", "findById()", "deleteById()"], 5, "repository");
  b += inheritArrow(624, 370, 504, 242, 304);
  b += inheritArrow(790, 370, 558, 242, 304);

  b += panel(928, 330, 478, 270, "Entry and infrastructure");
  b += umlClass(956, 370, 180, 104, "TeamController", ["GET /teams", "fetch members", "map records"], 0, "rest controller");
  b += umlClass(1160, 370, 180, 104, "MemberController", ["GET /members", "stateless/session", "map records"], 1, "rest controller");
  b += umlClass(956, 506, 180, 86, "HibernateReactiveConfig", ["SessionFactory", "PostgreSQL config"], 7, "configuration");
  b += umlClass(1160, 506, 180, 86, "DatabaseInitializer", ["seed Team", "seed Member"], 2, "runner");
  b += `<path d="M849,242 L849,300 L1046,300 L1046,370" class="sessionUse"/>
<path d="M849,242 L849,292 L1250,292 L1250,370" class="sessionUse"/>
<path d="M956,422 L866,422" class="repoUse"/>
<path d="M1250,370 L1250,312 L790,312 L790,370" class="repoUse"/>
<path d="M956,546 L918,546 L918,306 L834,306 L834,242" class="sessionUse"/>
<path d="M1340,550 L1426,550 L1426,258 L1162,258" class="inherit"/>
<path d="M1150,242 L1138,258 L1162,258 Z" class="inheritHead"/>\n`;

  b += panel(88, 672, 1260, 146, "DTO records and query conditions");
  b += umlClass(124, 704, 180, 104, "TeamRecord", ["id", "name"], 4, "record");
  b += umlClass(328, 704, 190, 104, "MemberRecord", ["id", "name", "age"], 3, "record");
  b += umlClass(548, 704, 240, 104, "TeamAndMemberRecord", ["teamId", "teamName", "members"], 5, "record");
  b += umlClass(818, 704, 230, 104, "MemberAndTeamRecord", ["id", "name", "team"], 6, "record");
  b += umlClass(1078, 704, 220, 104, "MemberSearchCondition", ["memberName", "teamName", "age range"], 1, "record");
  b += `<path d="M178,488 L178,704" class="mapArrow"/>
<path d="M366,488 L366,704" class="mapArrow"/>
<path d="M218,488 L218,648 L668,648 L668,704" class="mapArrow"/>
<path d="M366,488 L366,632 L934,632 L934,704" class="mapArrow"/>\n`;
  b += note(112, 838, 1180, "Source check: Team.members is mappedBy member.team; repositories receive Mutiny Session objects from controller SessionFactory boundaries.");
  return b;
}

function hibernateErd() {
  let b = panel(72, 122, 480, 250, "Parent table");
  b += erdTable(138, 160, 348, "Team", ["id BIGINT PK", "name VARCHAR", "generated identity"], 2, 150);

  b += panel(668, 292, 480, 250, "Child table");
  b += erdTable(734, 330, 348, "Member", ["id BIGINT PK", "name VARCHAR", "age INTEGER NULL", "team_id BIGINT FK NOT NULL"], 3, 168);
  b += `<path d="M734,406 L616,406 L616,254 L486,254" class="fkArrow"/>
<rect x="556" y="388" width="118" height="24" rx="8" fill="#fff" stroke="#99f6e4"/>
<text x="615" y="405" class="tiny" text-anchor="middle">member.team</text>
<text x="498" y="246" class="tiny">1</text>
<text x="718" y="398" class="tiny">N</text>\n`;

  b += panel(92, 512, 990, 116, "JPA mapping notes");
  b += card(128, 544, 230, 54, "@OneToMany", 2, "Team.members");
  b += card(410, 544, 230, 54, "@ManyToOne", 3, "Member.team eager");
  b += card(692, 544, 270, 54, "No explicit @Table", 7, "entity names drive defaults");
  b += note(126, 672, 930, "ERD keeps only persisted tables and the FK relationship; DTO records and controllers are intentionally omitted.");
  return b;
}

function r2dbcSequence() {
  const xs = [112, 308, 524, 748, 966, 1160];
  const names = ["Client", "PostController", "Comment Repository", "R2dbcEntity Operations", "Criteria Query", "comments Table"];
  let b = "";
  xs.forEach((x, i) => {
    b += card(x - 72, 122, 144, 52, names[i], i, "");
    b += `<path d="M${x},184 L${x},704" class="lifeline"/>\n`;
  });
  [
    [0, 1, "GET /posts/{postId}/comments", 224, false],
    [1, 2, "findAllByPostId(postId)", 286, false],
    [2, 4, "Criteria.where(postId).isEqual", 348, false],
    [2, 3, "selectSuspending<Comment>(query)", 410, false],
    [3, 5, "SELECT comments WHERE post_id", 472, false],
    [5, 3, "matching rows", 534, true],
    [3, 2, "Flow<Comment>", 596, true],
    [2, 1, "stream comments", 658, true],
    [1, 0, "HTTP response body", 704, true],
  ].forEach(([from, to, label, y, reverse], i) => {
    b += band(xs[from], xs[to], y, `${i + 1}. ${label}`, reverse);
  });
  b += note(78, 714, 1164, "PostController.saveComment writes the same table through insertSuspending; the read path shown here is the Criteria based Flow query.");
  return b;
}

function r2dbcClass() {
  let b = panel(46, 120, 1368, 158, "Spring Data R2DBC abstractions");
  b += umlClass(78, 150, 270, 92, "AbstractR2dbcConfiguration", ["connectionFactory()", "repository infrastructure"], 0, "Spring base");
  b += umlClass(410, 150, 220, 92, "R2dbcEntityOperations", ["select/insert/count/delete"], 2, "operations");
  b += umlClass(686, 150, 180, 92, "DatabaseClient", ["SQL client"], 5, "R2DBC");
  b += umlClass(914, 150, 180, 92, "MappingR2dbcConverter", ["row mapping"], 6, "converter");
  b += umlClass(1136, 150, 240, 92, "CoroutineCrudRepository<Customer, Long>", ["findById/save/delete", "Flow query methods"], 1, "repository base");

  b += panel(58, 326, 370, 300, "Configuration and records");
  b += umlClass(94, 356, 292, 104, "R2dbcConfig", ["@EnableR2dbcRepositories", "h2/postgres profiles", "initializer()"], 3, "configuration");
  b += umlClass(94, 498, 138, 96, "Post", ["id", "title", "content"], 0, "table record");
  b += umlClass(248, 498, 138, 96, "Comment", ["id", "content", "postId"], 6, "table record");
  b += inheritArrow(240, 356, 214, 242, 300);

  b += panel(478, 326, 392, 300, "Repositories");
  b += umlClass(516, 356, 300, 104, "PostRepository", ["findByIdOrNull()", "save()", "deleteById()"], 2, "repository");
  b += umlClass(516, 500, 300, 104, "CommentRepository", ["findAllByPostId()", "countByPostId()", "save()"], 4, "repository");
  b += `<path d="M520,242 L520,300 L610,300 L610,356" class="opsUse"/>
<path d="M544,242 L544,294 L848,294 L848,552 L816,552" class="opsUse"/>
<path d="M776,242 L776,302 L748,302 L748,356" class="clientUse"/>
<path d="M1004,242 L1004,310 L816,310 L816,408" class="converterUse"/>\n`;

  b += panel(920, 326, 438, 300, "Entry and repository interface");
  b += umlClass(958, 356, 284, 104, "PostController", ["GET/POST /posts", "comments endpoints", "CoroutineScope(IO)"], 5, "rest controller");
  b += umlClass(958, 500, 180, 96, "Customer", ["id", "firstname", "lastname"], 7, "table record");
  b += umlClass(1162, 500, 160, 96, "CustomerRepository", ["findByFirstname()", "findByLastname()"], 1, "repository interface");
  b += inheritArrow(1242, 500, 1248, 242, 310);
  b += `<path d="M958,408 L816,408" class="repoUse"/>
<path d="M958,438 L848,438 L848,552 L816,552" class="repoUse"/>\n`;

  b += `<path d="M386,548 L476,548 L476,548 L516,548" class="fkArrow"/>
<rect x="412" y="532" width="90" height="24" rx="8" fill="#fff" stroke="#99f6e4"/>
<text x="457" y="549" class="tiny" text-anchor="middle">postId FK</text>
<path d="M232,546 L248,546" class="assoc"/>\n`;
  b += note(150, 798, 1120, "Source check: PostRepository and CommentRepository are classes over R2dbcEntityOperations; only CustomerRepository extends CoroutineCrudRepository.");
  return b;
}

function r2dbcErd() {
  let b = panel(76, 122, 480, 250, "Parent table");
  b += erdTable(140, 160, 350, "posts", ["id SERIAL PK", "title VARCHAR(255)", "content VARCHAR(255)"], 0, 146);
  b += panel(624, 312, 480, 250, "Child table");
  b += erdTable(690, 350, 350, "comments", ["id SERIAL PK", "content VARCHAR(255)", "post_id BIGINT FK"], 6, 146);
  b += `<path d="M690,424 L596,424 L596,246 L490,246" class="fkArrow"/>
<rect x="534" y="406" width="126" height="24" rx="8" fill="#fff" stroke="#99f6e4"/>
<text x="597" y="423" class="tiny" text-anchor="middle">fk_post_id</text>
<text x="502" y="238" class="tiny">1</text>
<text x="674" y="416" class="tiny">N</text>\n`;

  b += panel(126, 460, 350, 160, "Independent table");
  b += erdTable(164, 500, 276, "customer", ["id SERIAL PK", "firstname VARCHAR(255)", "lastname VARCHAR(255)"], 1, 106);

  b += panel(584, 604, 430, 70, "Schema source");
  b += card(614, 622, 370, 36, "schema-postgres.sql declares comments.post_id -> posts.id", 5);
  b += note(132, 694, 910, "The H2 schema mirrors posts/comments tables for tests; the PostgreSQL schema explicitly declares the foreign key.");
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
<marker id="arrowPink" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto"><path d="M1,1 L8,4.5 L1,8 Z" fill="#db2777"/></marker>
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
  const titleLines = wrap(title, Math.floor(w / 10), detail ? 2 : 2);
  const shownDetail = titleLines.length > 1 && h < 70 ? "" : detail;
  const titleY = y + (shownDetail ? 24 : h / 2 - (titleLines.length - 1) * 10 + 6);
  let out = `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="10" fill="${fill}" stroke="${stroke}" class="card"/>\n`;
  titleLines.forEach((line, i) => (out += `<text x="${x + w / 2}" y="${titleY + i * 18}" class="label" text-anchor="middle">${esc(line)}</text>\n`));
  if (shownDetail) out += `<text x="${x + w / 2}" y="${y + h - 14}" class="detail" text-anchor="middle">${esc(shownDetail)}</text>\n`;
  return out;
}

function erdTable(x, y, w, title, rows, c, h = 170) {
  const [fill, stroke] = colors[c % colors.length];
  let out = `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="12" fill="#fff" stroke="${stroke}" class="card"/>
<path d="M${x},${y + 40} L${x},${y + 12} Q${x},${y} ${x + 12},${y} L${x + w - 12},${y} Q${x + w},${y} ${x + w},${y + 12} L${x + w},${y + 40} Z" fill="${fill}"/>
<text x="${x + w / 2}" y="${y + 27}" class="label" text-anchor="middle">${esc(title)}</text>\n`;
  rows.forEach((r, i) => (out += `<text x="${x + 18}" y="${y + 66 + i * 17}" class="tiny">${esc(r)}</text>\n`));
  return out;
}

function umlClass(x, y, w, h, title, rows, c, stereotype = "") {
  const [fill, stroke] = colors[c % colors.length];
  const titleLines = wrapIdentifier(title, Math.floor(w / 10), 2);
  const headerH = titleLines.length > 1 ? 60 : 46;
  let out = `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="4" fill="#fff" stroke="${stroke}" class="card"/>
<path d="M${x},${y + headerH} L${x},${y + 4} Q${x},${y} ${x + 4},${y} L${x + w - 4},${y} Q${x + w},${y} ${x + w},${y + 4} L${x + w},${y + headerH} Z" fill="${fill}"/>
<text x="${x + w / 2}" y="${y + 18}" class="tiny" text-anchor="middle">&lt;&lt;${esc(stereotype)}&gt;&gt;</text>\n`;
  titleLines.forEach((line, i) => (out += `<text x="${x + w / 2}" y="${y + 36 + i * 17}" class="label" text-anchor="middle">${esc(line)}</text>\n`));
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

function band(x1, x2, y, label, reverse) {
  const left = Math.min(x1, x2);
  const right = Math.max(x1, x2);
  const path = reverse ? `M${right - 16},${y} L${left + 16},${y}` : `M${left + 16},${y} L${right - 16},${y}`;
  const cls = reverse ? "softArrow" : "arrow";
  return `<rect x="${left + 30}" y="${y - 28}" width="${right - left - 60}" height="24" rx="8" fill="#fff" stroke="#d7e2ec"/>
<text x="${(left + right) / 2}" y="${y - 11}" class="tiny" text-anchor="middle">${esc(label)}</text>
<path d="${path}" class="${cls}"/>\n`;
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
