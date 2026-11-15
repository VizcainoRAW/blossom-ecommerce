// Run: node --test scripts/validation.test.ts
import assert from 'node:assert/strict'
import { test } from 'node:test'
import { emptyRegisterForm, validateRegister } from '../src/validation.ts'

const valid = {
  ...emptyRegisterForm,
  firstName: 'Ana', lastName: 'Ruiz', identifier: 'ana@mail.com', password: 'secret123', confirmPassword: 'secret123',
}

test('valid form has no errors', () => {
  assert.deepEqual(validateRegister(valid), {})
  assert.deepEqual(validateRegister({ ...valid, documentType: 'CC', documentNumber: '123' }), {})
})

test('empty form flags required fields', () => {
  const e = validateRegister(emptyRegisterForm)
  assert.ok(e.firstName && e.lastName && e.identifier && e.password)
})

test('email, password length and confirmation', () => {
  assert.ok(validateRegister({ ...valid, identifier: 'ana@mail' }).identifier)
  assert.ok(validateRegister({ ...valid, password: 'short', confirmPassword: 'short' }).password)
  assert.ok(validateRegister({ ...valid, password: 'x'.repeat(73), confirmPassword: 'x'.repeat(73) }).password)
  assert.ok(validateRegister({ ...valid, confirmPassword: 'other123' }).confirmPassword)
})

test('document type and number go together', () => {
  assert.ok(validateRegister({ ...valid, documentType: 'CC' }).documentNumber)
  assert.ok(validateRegister({ ...valid, documentNumber: '123' }).documentType)
})
