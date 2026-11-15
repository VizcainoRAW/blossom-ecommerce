// Run: node --test scripts/product.test.ts
import assert from 'node:assert/strict'
import { test } from 'node:test'
import { parseToken } from '../src/jwt.ts'
import { validateProduct } from '../src/validation.ts'

const variant = { color: 'Red', size: 'M', sku: 'TS-RED-M', price: '19.99', stock: '5' }
const valid = { categoryId: '1', name: 'Tee', description: '', imageUrls: 'https://x.com/a.png\n\n', variants: [variant] }

test('valid product has no errors', () => {
  assert.deepEqual(validateProduct(valid), {})
})

test('product-level rules', () => {
  const e = validateProduct({ ...valid, categoryId: '', name: ' ', imageUrls: 'not a url', variants: [] })
  assert.ok(e.categoryId && e.name && e.imageUrls && e.variants)
})

test('variant rules', () => {
  const bad = (v: object) => validateProduct({ ...valid, variants: [{ ...variant, ...v }] })
  assert.ok(bad({ price: '-1' })['variants.0.price'])
  assert.ok(bad({ price: '1.999' })['variants.0.price'])
  assert.ok(bad({ price: '' })['variants.0.price'])
  assert.ok(bad({ stock: '1.5' })['variants.0.stock'])
  assert.ok(bad({ size: 'x'.repeat(21) })['variants.0.size'])
  const dup = validateProduct({ ...valid, variants: [variant, { ...variant, sku: 'ts-red-m ' }] })
  assert.ok(dup['variants.1.sku'] && !dup['variants.0.sku'])
})

const jwt = (payload: object) => `h.${btoa(JSON.stringify(payload)).replace(/=+$/, '')}.s`

test('parseToken reads account type and rejects expired or garbage tokens', () => {
  const now = 1_000_000_000_000
  assert.deepEqual(parseToken(jwt({ account_type: 'EMPLOYEE', exp: now / 1000 + 60 }), now), { accountType: 'EMPLOYEE' })
  assert.equal(parseToken(jwt({ account_type: 'CUSTOMER', exp: now / 1000 - 1 }), now), null)
  assert.equal(parseToken('garbage', now), null)
  assert.equal(parseToken(null, now), null)
})
