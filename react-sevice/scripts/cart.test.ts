// Run: node --test scripts/cart.test.ts
import assert from 'node:assert/strict'
import { test } from 'node:test'
import { addItem, cartCount, cartTotal, loadCart, removeItem, setQuantity } from '../src/cart.ts'

const tee = { variantId: 1, productId: 10, name: 'Tee', color: 'Red', size: 'M', sku: 'T-R-M', price: 10, stock: 3 }
const cap = { variantId: 2, productId: 11, name: 'Cap', color: 'Blue', size: 'U', sku: 'C-B-U', price: 5.5, stock: 10 }

test('adding merges the same variant and never exceeds stock', () => {
  let items = addItem([], tee, 2)
  items = addItem(items, tee, 5)
  assert.equal(items.length, 1)
  assert.equal(items[0].quantity, 3)
  assert.deepEqual(addItem([], { ...tee, stock: 0 }, 1), [])
})

test('setQuantity clamps to stock and removes at zero', () => {
  const items = addItem(addItem([], tee, 1), cap, 1)
  assert.equal(setQuantity(items, 1, 99)[0].quantity, 3)
  assert.deepEqual(setQuantity(items, 1, 0).map(i => i.variantId), [2])
  assert.deepEqual(removeItem(items, 2).map(i => i.variantId), [1])
})

test('totals and counts', () => {
  const items = addItem(addItem([], tee, 2), cap, 2)
  assert.equal(cartTotal(items), 31)
  assert.equal(cartCount(items), 4)
})

test('loadCart survives garbage', () => {
  assert.deepEqual(loadCart(null), [])
  assert.deepEqual(loadCart('{oops'), [])
  assert.deepEqual(loadCart('{"a":1}'), [])
  const saved = JSON.stringify([{ ...tee, quantity: 1 }, { variantId: 'x' }, { ...cap, quantity: 0 }])
  assert.deepEqual(loadCart(saved).map(i => i.variantId), [1])
})
