import assert from 'node:assert/strict';
import { execFile } from 'node:child_process';
import { readFile } from 'node:fs/promises';
import { promisify } from 'node:util';
import test from 'node:test';

const execute = promisify(execFile);
const root = new URL('../../', import.meta.url);

async function validate() {
  return execute('node', ['scripts/validate-visual-companions.mjs'], { cwd: root });
}

async function html(relativePath) {
  return readFile(new URL(relativePath, root), 'utf8');
}

function values(content, pattern) {
  return [...content.matchAll(pattern)].map((match) => match[1]).sort();
}

test('approved bilingual visual companions satisfy the repository contract', async () => {
  const { stdout } = await validate();
  assert.match(stdout, /2 documents \/ 4 locale files/);
});

test('cache companion explains the implemented Exposed and Redis path', async () => {
  const content = await html(
    'docs/superpowers/specs/2026-07-30-exposed-redis-cache-strategies-visual-companion.html',
  );
  for (const marker of [
    'Caffeine',
    'JdbcCacheRepository',
    'AbstractJdbcRedissonRepository',
    'RLocalCachedMap',
    'LettuceSuspendNearCache',
    'LettuceSuspendedCache',
    'EntityMapLoader',
    'EntityMapWriter',
    'READ_WRITE_THROUGH_WITH_NEAR_CACHE',
    'READ_ONLY_WITH_NEAR_CACHE',
    'WRITE_BEHIND_WITH_NEAR_CACHE',
    '요청 수락',
    'DB 반영 완료',
    'deleteFromDBOnInvalidate',
    'data-layer="api"',
    'data-layer="cache"',
    'data-layer="db"',
    'layer-legend',
    'stage-layer',
    'data-visual-id="cache-strategy-architecture"',
    'data-visual-id="cache-strategy-benchmark"',
  ]) {
    assert.match(content, new RegExp(marker));
  }
  for (const correctedText of [
    '<h2>캐시 전략</h2>',
    'Redis와 로컬 캐시에 적재한다',
    'Exposed와 Redis로 다양한 캐시 전략을 구현하는 방법',
  ]) {
    assert.match(content, new RegExp(correctedText));
  }
  for (const rejectedText of [
    '하나의 캐시 API가 네 가지 데이터 처리 규칙',
    'Redis와 Near Cache를 채운다',
    'Exposed와 Redis로 Near Cache를 구현하는 경로',
    'README의 Architecture Diagram',
  ]) {
    assert.doesNotMatch(content, new RegExp(rejectedText));
  }
  for (const strategy of ['read-through', 'write-through', 'read-only', 'write-behind']) {
    assert.match(content, new RegExp(`data-strategy=["']${strategy}["']`));
  }
});

test('cache locale documents expose the same structure and interactions', async () => {
  const ko = await html(
    'docs/superpowers/specs/2026-07-30-exposed-redis-cache-strategies-visual-companion.html',
  );
  const en = await html(
    'docs/superpowers/specs/2026-07-30-exposed-redis-cache-strategies-visual-companion.en.html',
  );
  assert.deepEqual(values(en, /<section\b[^>]*id=["']([^"']+)/gi), values(ko, /<section\b[^>]*id=["']([^"']+)/gi));
  assert.deepEqual(values(en, /data-strategy=["']([^"']+)/gi), values(ko, /data-strategy=["']([^"']+)/gi));
  assert.equal((en.match(/\.\/gradlew /g) ?? []).length, (ko.match(/\.\/gradlew /g) ?? []).length);
  assert.match(en, /data-baseline=["']a2edd9af77188f814ccae10917a9e6ad574402f9["']/);
  for (const content of [ko, en]) {
    assert.ok(content.indexOf('id="strategy-architecture"') < content.indexOf('id="implementation"'));
    assert.ok(content.indexOf('id="effects"') < content.indexOf('data-visual-id="cache-strategy-benchmark"'));
  }
  assert.doesNotMatch(en, /README architecture diagram|from the README/i);
});

test('DDD companion separates the accepted event path from a rejected module reference', async () => {
  const content = await html(
    'docs/superpowers/specs/2026-07-30-ddd-modulith-boundaries-visual-companion.html',
  );
  for (const marker of [
    'AcceptOrderCommand',
    'OrderApplicationService',
    'OrderSummary',
    'ExposedOrderRepository',
    'WorkshopOrders',
    'OrderAcceptedEvent',
    'ShippingReservationHandler',
    'ExposedShippingReservationRepository',
    'ShippingReservations',
    'orders :: events',
    'ApplicationModules\\.verify\\(\\)',
    'Violations',
    'orders\\.internal',
    'DDD가 필요한 상황',
    'DDD 적용 효과',
    '아키텍처 다이어그램',
    '클래스 다이어그램',
    'Exposed로 주문 Aggregate의 일관성 경계를 구현한 방식',
    '별도의 <code>Order</code> Aggregate Root 클래스는 없다',
    'data-visual-id="ddd-modulith-boundaries"',
  ]) {
    assert.match(content, new RegExp(marker));
  }
  assert.doesNotMatch(content, /README의 Modulith 경계 다이어그램/);
  for (const scenario of ['normal', 'violation']) {
    assert.match(content, new RegExp(`data-scenario=["']${scenario}["']`));
  }
});

test('DDD locale documents expose the same structure and scenarios', async () => {
  const ko = await html(
    'docs/superpowers/specs/2026-07-30-ddd-modulith-boundaries-visual-companion.html',
  );
  const en = await html(
    'docs/superpowers/specs/2026-07-30-ddd-modulith-boundaries-visual-companion.en.html',
  );
  assert.deepEqual(values(en, /<section\b[^>]*id=["']([^"']+)/gi), values(ko, /<section\b[^>]*id=["']([^"']+)/gi));
  assert.deepEqual(values(en, /data-scenario=["']([^"']+)/gi), values(ko, /data-scenario=["']([^"']+)/gi));
  assert.equal((en.match(/\.\/gradlew /g) ?? []).length, (ko.match(/\.\/gradlew /g) ?? []).length);
  assert.match(en, /data-baseline=["']a2edd9af77188f814ccae10917a9e6ad574402f9["']/);
  assert.doesNotMatch(en, /from the README/i);
});
