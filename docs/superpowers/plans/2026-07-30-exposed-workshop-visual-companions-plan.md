# exposed-workshop Visual Companions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Publish bilingual, theme-aware Visual Companions for the Exposed + Redis cache strategies and DDD Modulith boundary examples, then register immutable snapshots in `bluetape4k.github.io`.

**Architecture:** `exposed-workshop` owns the approved manifest, standalone HTML documents, and source validation. `bluetape4k.github.io` owns immutable snapshots, the bilingual catalog, navigation, and production routes. The site integration starts only after site PR #302 has merged so the third repository extends the published multi-repository contract instead of modifying the pending foundation.

**Tech Stack:** Static HTML/CSS/JavaScript, Node.js built-in test runner, Kotlin/Exposed, Redisson, Spring Modulith, Gradle, Astro/Starlight, Playwright.

---

## File Map

### `exposed-workshop`

- Create `docs/visual-companions/manifest.json`
  - Registers the two approved bilingual documents and their design source.
- Create `scripts/validate-visual-companions.mjs`
  - Validates manifest shape, locale parity, source baseline, standalone HTML, themes, navigation, execution evidence, and document-specific technical markers.
- Create `tests/visual-companions/validator.test.mjs`
  - Proves that valid documents pass and missing theme, baseline, navigation, execution, cache, or DDD contracts fail.
- Create `docs/superpowers/specs/2026-07-30-exposed-redis-cache-strategies-visual-companion.html`
  - Korean cache strategy document.
- Create `docs/superpowers/specs/2026-07-30-exposed-redis-cache-strategies-visual-companion.en.html`
  - English cache strategy document.
- Create `docs/superpowers/specs/2026-07-30-ddd-modulith-boundaries-visual-companion.html`
  - Korean DDD document.
- Create `docs/superpowers/specs/2026-07-30-ddd-modulith-boundaries-visual-companion.en.html`
  - English DDD document.

### `bluetape4k.github.io`

- Modify `tests/visual-companions/repositories.test.mjs`
  - Adds the exposed-workshop registry expectation.
- Modify `tests/visual-companions/navigation.test.mjs`
  - Requires the third repository and its two catalog documents.
- Modify `src/data/visual-companions/repositories.json`
  - Pins the exact exposed-workshop source commit.
- Modify `src/data/visual-companions/catalog.json`
  - Adds bilingual repository, cache document, and DDD document descriptions.
- Create `src/data/visual-companions/exposed-workshop.snapshot.json`
  - Generated immutable snapshot metadata.
- Create four snapshot HTML files under:
  - `public/visual-companions/exposed-workshop/`
  - `public/ko/visual-companions/exposed-workshop/`

## Task 1: Add the source manifest contract

**Files:**
- Create: `tests/visual-companions/validator.test.mjs`
- Create: `docs/visual-companions/manifest.json`
- Create: `scripts/validate-visual-companions.mjs`

- [ ] **Step 1: Write a failing validator process test**

Create a Node test helper that runs the validator from the repository root:

```javascript
import assert from 'node:assert/strict';
import { execFile } from 'node:child_process';
import { promisify } from 'node:util';
import test from 'node:test';

const execute = promisify(execFile);
const root = new URL('../../', import.meta.url);

async function validate() {
  return execute('node', ['scripts/validate-visual-companions.mjs'], {
    cwd: root,
  });
}

test('approved bilingual visual companions satisfy the repository contract', async () => {
  const { stdout } = await validate();
  assert.match(stdout, /2 documents \/ 4 locale files/);
});
```

- [ ] **Step 2: Run the test and confirm the missing validator fails**

Run:

```bash
node --test tests/visual-companions/validator.test.mjs
```

Expected: FAIL because `scripts/validate-visual-companions.mjs` does not exist.

- [ ] **Step 3: Add the two-document manifest**

Use the design commit `a2edd9af77188f814ccae10917a9e6ad574402f9` as the `data-baseline` for both documents. Register:

```json
{
  "schemaVersion": 1,
  "repository": "bluetape4k/exposed-workshop",
  "documents": [
    {
      "id": "exposed-redis-cache-strategies",
      "source": "docs/superpowers/specs/2026-07-30-exposed-workshop-visual-companions-design.md",
      "status": "approved",
      "public": true,
      "presentation": {
        "mode": "simulation",
        "defaultView": "simulation",
        "views": ["simulation"]
      },
      "locales": {
        "en": {
          "title": "Exposed and Redis Cache Strategies",
          "html": "docs/superpowers/specs/2026-07-30-exposed-redis-cache-strategies-visual-companion.en.html"
        },
        "ko": {
          "title": "Exposed와 Redis 캐시 전략",
          "html": "docs/superpowers/specs/2026-07-30-exposed-redis-cache-strategies-visual-companion.html"
        }
      }
    },
    {
      "id": "ddd-modulith-boundaries",
      "source": "docs/superpowers/specs/2026-07-30-exposed-workshop-visual-companions-design.md",
      "status": "approved",
      "public": true,
      "presentation": {
        "mode": "simulation",
        "defaultView": "simulation",
        "views": ["simulation"]
      },
      "locales": {
        "en": {
          "title": "DDD Modulith Boundaries",
          "html": "docs/superpowers/specs/2026-07-30-ddd-modulith-boundaries-visual-companion.en.html"
        },
        "ko": {
          "title": "DDD Modulith 경계",
          "html": "docs/superpowers/specs/2026-07-30-ddd-modulith-boundaries-visual-companion.html"
        }
      }
    }
  ]
}
```

- [ ] **Step 4: Implement structural validation**

The validator must reject:

```javascript
const forbidden = [
  /<script\b[^>]*\bsrc\s*=/i,
  /<link\b[^>]*\brel\s*=\s*["']?stylesheet\b/i,
  /<(?:img|iframe|audio|video|source)\b[^>]*\bsrc\s*=\s*["'](?!data:|#)[^"']+["']/i,
  /<form\b/i,
  /\bfetch\s*\(/,
  /\bXMLHttpRequest\b/,
  /\bWebSocket\s*\(/,
  /\bnavigator\.sendBeacon\s*\(/,
];
```

For every locale document, require:

```javascript
[
  /^\s*<!doctype html>/i,
  /<meta\b[^>]*name=["']color-scheme["'][^>]*content=["']light dark["']/i,
  /:root\[data-theme=["']light["']\]/i,
  /:root\[data-theme=["']dark["']\]/i,
  /localStorage\.getItem\(storageKey\)/i,
  /localStorage\.setItem\(themeStorageKey,/i,
  /<button\b[^>]*class=["'][^"']*theme-toggle/i,
  /<section\b[^>]*id=["']simulation["']/i,
  /data-baseline=["']a2edd9af77188f814ccae10917a9e6ad574402f9["']/i,
  /2026-07-30-exposed-workshop-visual-companions-design\.md/i,
];
```

Require each document to link the sibling document, its opposite locale, the source design, the relevant README, source class, test class, and Gradle execution command.

- [ ] **Step 5: Run the validator test and confirm HTML absence is now the failure**

Run:

```bash
node --test tests/visual-companions/validator.test.mjs
```

Expected: FAIL with four missing HTML paths.

- [ ] **Step 6: Commit the contract**

```bash
git add docs/visual-companions/manifest.json scripts/validate-visual-companions.mjs tests/visual-companions/validator.test.mjs
git commit -m "Define the exposed visual companion publication contract" \
  -m "Constraint: Source HTML must remain bilingual, standalone, theme-aware, and pinned to the approved design commit." \
  -m "Confidence: high" \
  -m "Scope-risk: narrow" \
  -m "Tested: node --test tests/visual-companions/validator.test.mjs fails on the four missing locale files." \
  -m "Not-tested: Rendered documents are implemented in the next tasks."
```

## Task 2: Build the Korean cache strategy document

**Files:**
- Modify: `tests/visual-companions/validator.test.mjs`
- Create: `docs/superpowers/specs/2026-07-30-exposed-redis-cache-strategies-visual-companion.html`

- [ ] **Step 1: Add failing cache-content assertions**

Read the Korean HTML and require these identifiers and controls:

```javascript
assert.match(html, /JdbcCacheRepository/);
assert.match(html, /AbstractJdbcRedissonRepository/);
assert.match(html, /RLocalCachedMap/);
assert.match(html, /EntityMapLoader/);
assert.match(html, /EntityMapWriter/);
assert.match(html, /READ_WRITE_THROUGH_WITH_NEAR_CACHE/);
assert.match(html, /READ_ONLY_WITH_NEAR_CACHE/);
assert.match(html, /WRITE_BEHIND_WITH_NEAR_CACHE/);
assert.match(html, /요청 수락/);
assert.match(html, /DB 반영 완료/);
assert.match(html, /deleteFromDBOnInvalidate/);
assert.match(html, /data-strategy=["']read-through["']/);
assert.match(html, /data-strategy=["']write-through["']/);
assert.match(html, /data-strategy=["']read-only["']/);
assert.match(html, /data-strategy=["']write-behind["']/);
```

- [ ] **Step 2: Run the test and confirm the cache document is missing**

Run:

```bash
node --test tests/visual-companions/validator.test.mjs
```

Expected: FAIL on the Korean cache HTML path.

- [ ] **Step 3: Implement the standalone document shell**

Build a complete HTML document with:

- early `starlight-theme` resolution before the first `<style>`;
- light and dark tokens;
- sticky navigation with repository, sibling document, locale, and theme controls;
- a first viewport containing the concrete title and the L1/Redis/DB processing path;
- responsive desktop and mobile layouts;
- `prefers-reduced-motion` handling;
- focus-visible styles and `aria-pressed` on strategy controls.

- [ ] **Step 4: Implement the cache simulation**

Use one selected strategy at a time. JavaScript must update:

```javascript
const strategyState = {
  'read-through': {
    stages: ['Near Cache miss', 'Redis miss', 'EntityMapLoader', 'Exposed DB', 'Near Cache + Redis'],
    completion: '조회 완료',
  },
  'write-through': {
    stages: ['RLocalCachedMap put', 'EntityMapWriter', 'Exposed transaction', 'DB + Redis'],
    completion: 'DB 반영 완료',
  },
  'read-only': {
    stages: ['캐시 무효화', 'DB 유지', '다음 조회에서 EntityMapLoader'],
    completion: '재조회 완료',
  },
  'write-behind': {
    stages: ['요청 수락', 'Redis 적재', '비동기 batch', 'Exposed transaction', 'DB 반영 완료'],
    completion: 'DB 반영 완료',
  },
};
```

The visible result must distinguish L1, Redis, and DB values and must not mark Write-Behind complete at queue acceptance.

- [ ] **Step 5: Add source-backed technical sections**

Include:

- strategy structure and selection reason;
- Near Cache need and effect;
- Exposed + Redis implementation sequence;
- strategy-specific effects;
- invalidation and `deleteFromDBOnInvalidate`;
- Redis failure/reconnection, local staleness, and Write-Behind loss caveats;
- Spring MVC versus coroutine runtime comparison;
- exact commands:

```bash
./gradlew :01-cache-strategies:test
./gradlew :01-cache-strategies:bootRun
./gradlew :02-cache-strategies-coroutines:test
```

- [ ] **Step 6: Apply the Korean technical-writing checklist**

Confirm the document contains none of:

```text
권위
권위 경계
의존 서비스
의존 라이브러리
진실의 원천
DB를 치고
다양한 장점을 제공합니다
```

Keep `Near Cache`, `RLocalCachedMap`, class names, configuration keys, and strategy names exact.

- [ ] **Step 7: Run the test**

Run:

```bash
node --test tests/visual-companions/validator.test.mjs
```

Expected: the Korean cache assertions pass; the remaining three locale files still fail.

- [ ] **Step 8: Commit the Korean cache document**

```bash
git add tests/visual-companions/validator.test.mjs docs/superpowers/specs/2026-07-30-exposed-redis-cache-strategies-visual-companion.html
git commit -m "Explain cache convergence with the implemented Exposed and Redis path" \
  -m "Constraint: Write-Behind completion must mean database persistence, not request acceptance." \
  -m "Confidence: high" \
  -m "Scope-risk: moderate" \
  -m "Tested: node --test tests/visual-companions/validator.test.mjs." \
  -m "Not-tested: English parity and browser rendering remain pending."
```

## Task 3: Add the English cache document

**Files:**
- Create: `docs/superpowers/specs/2026-07-30-exposed-redis-cache-strategies-visual-companion.en.html`
- Modify: `tests/visual-companions/validator.test.mjs`

- [ ] **Step 1: Add locale-parity assertions**

Parse both cache documents and assert equal:

- section IDs;
- strategy button values;
- `data-source`;
- `data-baseline`;
- source link count;
- execution command count;
- previous/next document IDs.

- [ ] **Step 2: Run the test and confirm English cache parity fails**

Run:

```bash
node --test tests/visual-companions/validator.test.mjs
```

Expected: FAIL because the English document does not exist.

- [ ] **Step 3: Localize the full document**

Translate explanatory prose naturally. Preserve:

- identifiers and commands;
- technical claim strength;
- the distinction between request acceptance and DB persistence;
- every caveat and source link;
- the same interaction and layout behavior.

- [ ] **Step 4: Run the validator tests**

Run:

```bash
node --test tests/visual-companions/validator.test.mjs
```

Expected: both cache locale files pass; DDD locale files remain missing.

- [ ] **Step 5: Commit cache locale parity**

```bash
git add tests/visual-companions/validator.test.mjs docs/superpowers/specs/2026-07-30-exposed-redis-cache-strategies-visual-companion.en.html
git commit -m "Keep the cache strategy companion source-equivalent in English" \
  -m "Constraint: Locale documents share interactions, evidence, caveats, and source baseline." \
  -m "Confidence: high" \
  -m "Scope-risk: narrow" \
  -m "Tested: node --test tests/visual-companions/validator.test.mjs." \
  -m "Not-tested: Browser rendering remains pending."
```

## Task 4: Build the Korean DDD document

**Files:**
- Create: `docs/superpowers/specs/2026-07-30-ddd-modulith-boundaries-visual-companion.html`
- Modify: `tests/visual-companions/validator.test.mjs`

- [ ] **Step 1: Add failing DDD-content assertions**

Require:

```javascript
assert.match(html, /AcceptOrderCommand/);
assert.match(html, /OrderApplicationService/);
assert.match(html, /OrderAcceptedEvent/);
assert.match(html, /ShippingReservationHandler/);
assert.match(html, /orders :: events/);
assert.match(html, /ApplicationModules\.verify\(\)/);
assert.match(html, /Violations/);
assert.match(html, /orders\.internal/);
assert.match(html, /data-scenario=["']normal["']/);
assert.match(html, /data-scenario=["']violation["']/);
assert.match(html, /DDD가 필요한 상황/);
assert.match(html, /DDD 적용 효과/);
```

- [ ] **Step 2: Run the test and confirm the Korean DDD document is missing**

Run:

```bash
node --test tests/visual-companions/validator.test.mjs
```

Expected: FAIL on the Korean DDD HTML path.

- [ ] **Step 3: Implement the normal event handoff**

Show:

```text
AcceptOrderCommand
  -> orders transaction
  -> ExposedOrderRepository
  -> ddd_modulith_orders
  -> OrderAcceptedEvent
  -> ShippingReservationHandler
  -> shipping transaction
  -> ddd_modulith_shipping_reservations
```

Update the active module, transaction, row state, and verification result for each step.

- [ ] **Step 4: Implement the rejected module reference**

The violation scenario must show:

```text
shipping -> orders.internal.LeakyOrderRepository
```

Mark it as a forbidden compile-time reference, then show `ApplicationModules.verify()` returning `Violations`. Do not imply that the verifier validates business rules or runtime message delivery.

- [ ] **Step 5: Add source-backed DDD explanation**

Include:

- why this module is the representative DDD example;
- when table-centered code needs explicit state-change responsibilities;
- bounded context ownership;
- event contract effects;
- additional mapping and event-contract cost;
- same-process `@EventListener` limitation;
- no outbox, broker durability, duplicate handling, or idempotency guarantee;
- exact command:

```bash
./gradlew :08-ddd-modulith-boundaries:test
```

- [ ] **Step 6: Apply the Korean technical-writing checklist**

Use `책임 경계`, `상태 변경 책임`, `참조 모듈`, and `컴파일 시점 참조` where they match. Do not use `권위`, mechanical `의존` wording, promotional claims, or essay-style conclusions.

- [ ] **Step 7: Run the validator tests**

Run:

```bash
node --test tests/visual-companions/validator.test.mjs
```

Expected: Korean cache, English cache, and Korean DDD pass; English DDD remains missing.

- [ ] **Step 8: Commit the Korean DDD document**

```bash
git add tests/visual-companions/validator.test.mjs docs/superpowers/specs/2026-07-30-ddd-modulith-boundaries-visual-companion.html
git commit -m "Show DDD state ownership as an executable Modulith boundary" \
  -m "Constraint: The visual must separate verified code references from unimplemented delivery durability." \
  -m "Confidence: high" \
  -m "Scope-risk: moderate" \
  -m "Tested: node --test tests/visual-companions/validator.test.mjs." \
  -m "Not-tested: English parity and browser rendering remain pending."
```

## Task 5: Add the English DDD document

**Files:**
- Create: `docs/superpowers/specs/2026-07-30-ddd-modulith-boundaries-visual-companion.en.html`
- Modify: `tests/visual-companions/validator.test.mjs`

- [ ] **Step 1: Add DDD locale-parity assertions**

Assert equal section IDs, scenario values, source metadata, source link counts, execution commands, and sibling navigation.

- [ ] **Step 2: Run the test and confirm the English DDD file is missing**

Run:

```bash
node --test tests/visual-companions/validator.test.mjs
```

Expected: FAIL because the English DDD document does not exist.

- [ ] **Step 3: Localize the DDD document**

Keep the same:

- module and transaction states;
- normal and violation scenarios;
- reasons for selecting the example;
- DDD need, effects, costs, and limitations;
- source links and commands.

- [ ] **Step 4: Run all source validators**

Run:

```bash
node --test tests/visual-companions/validator.test.mjs
node scripts/validate-visual-companions.mjs
git diff --check
```

Expected:

```text
Visual companion validation passed: 2 documents / 4 locale files
```

- [ ] **Step 5: Commit DDD locale parity**

```bash
git add tests/visual-companions/validator.test.mjs docs/superpowers/specs/2026-07-30-ddd-modulith-boundaries-visual-companion.en.html
git commit -m "Keep the DDD boundary companion source-equivalent in English" \
  -m "Constraint: Locale documents must preserve the same verified and unverified boundaries." \
  -m "Confidence: high" \
  -m "Scope-risk: narrow" \
  -m "Tested: Node validator tests, repository validator, and git diff --check." \
  -m "Not-tested: Kotlin examples and browser rendering remain pending."
```

## Task 6: Verify the examples and rendered documents

**Files:**
- Modify only if verification exposes a defect in the four HTML documents, manifest, validator, or tests.
- Store temporary Playwright output under `output/playwright/`; do not commit it.

- [ ] **Step 1: Run the cache example**

```bash
repo-test-summary -- ./gradlew :01-cache-strategies:test
```

Expected: all module tests pass, including cache hit/miss, write-through DB readback, invalidation, and 10,000-event Write-Behind completion.

- [ ] **Step 2: Run the DDD example**

```bash
repo-test-summary -- ./gradlew :08-ddd-modulith-boundaries:test
```

Expected: positive module verification, negative violation fixture, and event handoff tests pass.

- [ ] **Step 3: Start a local static server**

```bash
python3 -m http.server 4328
```

Run from the exposed-workshop root and keep the process in a separately polled terminal session.

- [ ] **Step 4: Verify desktop rendering**

Use Playwright at `1440x1000` for all four documents:

- initial light and dark render;
- strategy/scenario selection;
- sibling navigation;
- locale navigation;
- source links;
- no horizontal overflow;
- console errors 0.

- [ ] **Step 5: Verify mobile rendering**

Use Playwright at `390x844` and confirm controls, titles, diagrams, tables, and code blocks do not overlap or overflow.

- [ ] **Step 6: Verify theme persistence**

For each document:

1. select dark;
2. reload;
3. confirm dark remains;
4. select auto;
5. emulate light and dark color schemes;
6. confirm the document follows the system choice.

- [ ] **Step 7: Run final source checks**

```bash
node --test tests/visual-companions/validator.test.mjs
node scripts/validate-visual-companions.mjs
git diff --check
```

- [ ] **Step 8: Commit verification fixes, if any**

Use a Lore commit that identifies the concrete visual or interaction defect and the Playwright evidence. If no files changed, do not create an empty commit.

## Task 7: Publish the exact exposed-workshop source head

**Files:**
- Modify: PR metadata only after local verification passes.

- [ ] **Step 1: Verify source scope**

```bash
repo-status
repo-diff
git log --oneline origin/develop..HEAD
```

Expected: only the approved design, plan, manifest, validator, tests, and four HTML files.

- [ ] **Step 2: Create or update the GitHub issue**

Use English public metadata. Record both document IDs, bilingual scope, source-backed content requirements, theme contract, site follow-up, and validation.

- [ ] **Step 3: Push the exact head**

```bash
git push -u origin docs/exposed-visual-companions
```

Read back local, remote branch, and PR head SHAs and require equality.

- [ ] **Step 4: Create the exposed-workshop PR**

Base: `develop`.

The final PR section must be:

```markdown
## DoD Status
```

Report tests, validator counts, browser viewports, locale parity, exact head, current reviews, unresolved threads, and the fact that site publication is a separate follow-up.

- [ ] **Step 5: Wait for exact-head CI and report merge-ready**

Do not merge. Re-read checks, reviews, comments, unresolved threads, mergeability, and exact head immediately before the merge-ready report.

## Task 8: Establish the site follow-up base

**Files:**
- No file changes until the gate passes.

- [ ] **Step 1: Verify site PR #302 has merged**

```bash
gh pr view 302 --repo bluetape4k/bluetape4k.github.io --json state,mergedAt,mergeCommit,headRefOid
```

Expected: `state=MERGED` and the exact previously reviewed head is part of `develop`.

- [ ] **Step 2: Verify the exposed-workshop source PR has merged**

```bash
gh pr view docs/exposed-visual-companions \
  --repo bluetape4k/exposed-workshop \
  --json state,mergedAt,mergeCommit,headRefOid
```

Expected: `state=MERGED` and the merge commit contains the validated manifest and four HTML files.

- [ ] **Step 3: Sync site `develop`**

```bash
git switch develop
git pull --ff-only
```

- [ ] **Step 4: Create the follow-up branch**

```bash
git switch -c docs/exposed-visual-companion-catalog
```

Do not reuse or amend `docs/visual-companion-catalog-navigation`.

## Task 9: Add failing site registry and catalog tests

**Files:**
- Modify: `tests/visual-companions/repositories.test.mjs`
- Modify: `tests/visual-companions/navigation.test.mjs`

- [ ] **Step 1: Add the exposed-workshop registry expectation**

Require:

```javascript
const exposed = visualRegistry.repositories.find(
  ({ repository }) => repository === 'bluetape4k/exposed-workshop',
);
assert.ok(exposed);
assert.match(exposed.sourceRef, /^[0-9a-f]{40}$/);
```

- [ ] **Step 2: Add catalog expectations**

Require:

```javascript
const exposedCatalog = visualCatalog.repositories.find(
  ({ repository }) => repository === 'bluetape4k/exposed-workshop',
);
assert.deepEqual(
  exposedCatalog.documents.map(({ id }) => id),
  ['exposed-redis-cache-strategies', 'ddd-modulith-boundaries'],
);
assert.ok(exposedCatalog.documents.every(({ featured }) => featured));
```

- [ ] **Step 3: Run the tests and confirm missing registry/catalog entries**

```bash
node --test tests/visual-companions/repositories.test.mjs tests/visual-companions/navigation.test.mjs
```

Expected: FAIL because exposed-workshop is not registered.

- [ ] **Step 4: Commit the failing site tests**

```bash
git add tests/visual-companions/repositories.test.mjs tests/visual-companions/navigation.test.mjs
git commit -m "Require exposed-workshop in the visual companion catalog" \
  -m "Constraint: Site publication must use an immutable source head with two bilingual documents." \
  -m "Confidence: high" \
  -m "Scope-risk: narrow" \
  -m "Tested: Targeted Node tests fail on the missing repository." \
  -m "Not-tested: Snapshot generation follows after source PR merge."
```

## Task 10: Sync the exposed-workshop snapshot

**Files:**
- Modify: `src/data/visual-companions/repositories.json`
- Modify: `src/data/visual-companions/catalog.json`
- Create: `src/data/visual-companions/exposed-workshop.snapshot.json`
- Create: four published HTML snapshot files

- [ ] **Step 1: Pin the merged exposed-workshop commit**

After the exposed-workshop PR merges, read its merge result and require that the commit is part of `origin/develop`:

```bash
EXPOSED_SOURCE_REF="$(
  gh pr view docs/exposed-visual-companions \
    --repo bluetape4k/exposed-workshop \
    --json mergeCommit \
    --jq '.mergeCommit.oid'
)"
test "${#EXPOSED_SOURCE_REF}" -eq 40
git -C /Users/debop/work/bluetape4k/exposed-workshop fetch origin develop
git -C /Users/debop/work/bluetape4k/exposed-workshop \
  merge-base --is-ancestor "$EXPOSED_SOURCE_REF" origin/develop
```

Update `src/data/visual-companions/repositories.json` with `jq` so the stored value is the computed SHA:

```bash
jq --arg sourceRef "$EXPOSED_SOURCE_REF" '
  .repositories += [{
    repository: "bluetape4k/exposed-workshop",
    sourceRef: $sourceRef,
    manifestPath: "docs/visual-companions/manifest.json"
  }]
' src/data/visual-companions/repositories.json > \
  src/data/visual-companions/repositories.json.tmp
mv src/data/visual-companions/repositories.json.tmp \
  src/data/visual-companions/repositories.json
```

- [ ] **Step 2: Add bilingual catalog text**

Repository label:

```json
{
  "en": "Exposed workshop",
  "ko": "Exposed 워크숍"
}
```

Cache summary must state that the reader compares cache read, update, invalidation, and deferred DB persistence across Near Cache, Redis, and Exposed.

DDD summary must state that orders and shipping own separate persistence and communicate only through the published event contract.

- [ ] **Step 3: Generate the immutable snapshot**

```bash
npm run sync:visual-companions -- \
  --repository bluetape4k/exposed-workshop \
  --source-root /Users/debop/work/bluetape4k/exposed-workshop \
  --source-ref "$EXPOSED_SOURCE_REF"
```

Expected:

```text
Synced 2 documents / 4 locale assets at the exact EXPOSED_SOURCE_REF value
```

- [ ] **Step 4: Run the targeted site tests**

```bash
node --test tests/visual-companions/repositories.test.mjs \
  tests/visual-companions/navigation.test.mjs \
  tests/visual-companions/snapshot.test.mjs
npm run check:visual-companions
```

Expected: three repositories, six documents, and twelve locale assets validate.

- [ ] **Step 5: Commit the site snapshot**

Use a Lore commit with the exact exposed-workshop source SHA in the body and `Tested:` trailer.

## Task 11: Verify the site and prepare the publication PR

**Files:**
- Modify only if build or route verification exposes a scoped defect.

- [ ] **Step 1: Run the complete site test suite**

```bash
npm test
```

- [ ] **Step 2: Build all routes**

```bash
npm run build
```

Expected: exit code 0 with all existing and four new Visual Companion routes generated.

- [ ] **Step 3: Start the Astro preview**

```bash
npm run dev -- --host 127.0.0.1 --port 4329
```

- [ ] **Step 4: Verify catalog routes**

Check:

```text
/visual-companions/
/ko/visual-companions/
/ecosystem/examples/
/ko/ecosystem/examples/
```

Confirm the Exposed workshop repository and both documents appear in English and Korean.

- [ ] **Step 5: Verify four published document routes**

Check:

```text
/visual-companions/exposed-workshop/exposed-redis-cache-strategies/
/ko/visual-companions/exposed-workshop/exposed-redis-cache-strategies/
/visual-companions/exposed-workshop/ddd-modulith-boundaries/
/ko/visual-companions/exposed-workshop/ddd-modulith-boundaries/
```

Repeat desktop/mobile theme and interaction checks. Require browser console errors 0.

- [ ] **Step 6: Verify final site scope**

```bash
repo-status
repo-diff
git diff --check
```

- [ ] **Step 7: Push and create the site PR**

Base: `develop`.

Use English GitHub metadata and end the body with:

```markdown
## DoD Status
```

- [ ] **Step 8: Report exact-head merge readiness**

Re-read exact head, checks, reviews, comments, unresolved threads, and mergeability. Stop before merge and request fresh approval for the site follow-up PR.

## Task 12: Merge, deploy, sync, and clean up after fresh approval

**Files:**
- GitHub and local branch state only.

- [ ] **Step 1: Re-read the site follow-up PR immediately before merge**

Require the exact approved head, passing required checks, no unresolved threads, and mergeable state.

- [ ] **Step 2: Confirm the exposed-workshop source commit remains on `develop`**

Re-run the ancestry check for `EXPOSED_SOURCE_REF`. Stop if the source commit is no longer reachable from `origin/develop`.

- [ ] **Step 3: Merge the site follow-up PR**

Verify the `develop` deployment workflow starts from the merge commit.

- [ ] **Step 4: Verify GitHub Pages deployment**

Require the `Deploy Website` workflow to finish successfully on `develop`.

- [ ] **Step 5: Check production routes**

Use cache-busted requests and browser verification for the catalog and four new document routes.

- [ ] **Step 6: Sync local repositories**

For `exposed-workshop`:

```bash
git switch develop
git pull --ff-only
EXPOSED_FEATURE_HEAD="$(git rev-parse refs/remotes/origin/docs/exposed-visual-companions)"
git merge-base --is-ancestor "$EXPOSED_FEATURE_HEAD" origin/develop
```

For `bluetape4k.github.io`:

```bash
git switch develop
git pull --ff-only
SITE_FEATURE_HEAD="$(git rev-parse refs/remotes/origin/docs/exposed-visual-companion-catalog)"
git merge-base --is-ancestor "$SITE_FEATURE_HEAD" origin/develop
```

- [ ] **Step 7: Remove only verified feature branches**

Delete local and remote feature branches only after ancestry or patch-equivalence proof. Preserve dirty, unrelated, or unmerged work.

- [ ] **Step 8: Report final evidence**

Report merge SHAs, deployment run, live routes, local/upstream parity, removed branches, and any validation gap.
