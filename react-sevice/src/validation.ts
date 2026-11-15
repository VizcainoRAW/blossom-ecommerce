export const emptyRegisterForm = {
  firstName: '', lastName: '', identifier: '', password: '', confirmPassword: '',
  documentType: '', documentNumber: '',
}
export type RegisterForm = typeof emptyRegisterForm
export type RegisterErrors = Partial<Record<keyof RegisterForm, string>>

export type VariantForm = { color: string; size: string; sku: string; price: string; stock: string }
export const emptyVariant: VariantForm = { color: '', size: '', sku: '', price: '', stock: '' }
export type ProductForm = {
  categoryId: string; name: string; description: string
  imageUrls: string // one URL per line
  variants: VariantForm[]
}
/** Keys are field names, or `variants.<index>.<field>` for variant rows. */
export type ProductErrors = Record<string, string>

// Mirrors the backend ProductRequest constraints.
export function validateProduct(f: ProductForm): ProductErrors {
  const e: ProductErrors = {}
  if (!f.categoryId) e.categoryId = 'Choose a category'
  if (!f.name.trim()) e.name = 'Name is required'
  else if (f.name.trim().length > 255) e.name = 'Name must be at most 255 characters'
  const badUrl = f.imageUrls.split('\n').map(u => u.trim()).find(u => u && !URL.canParse(u))
  if (badUrl) e.imageUrls = `Not a valid URL: ${badUrl}`
  if (f.variants.length === 0) e.variants = 'Add at least one variant'

  const skus = f.variants.map(v => v.sku.trim().toLowerCase())
  f.variants.forEach((v, i) => {
    const k = (field: string) => `variants.${i}.${field}`
    if (!v.color.trim()) e[k('color')] = 'Required'
    else if (v.color.trim().length > 50) e[k('color')] = 'Max 50 characters'
    if (!v.size.trim()) e[k('size')] = 'Required'
    else if (v.size.trim().length > 20) e[k('size')] = 'Max 20 characters'
    if (!v.sku.trim()) e[k('sku')] = 'Required'
    else if (v.sku.trim().length > 100) e[k('sku')] = 'Max 100 characters'
    else if (skus.indexOf(skus[i]) !== i) e[k('sku')] = 'Duplicate SKU'
    if (!/^\d{1,10}(\.\d{1,2})?$/.test(v.price.trim())) e[k('price')] = 'Up to 2 decimals, not negative'
    if (!/^\d+$/.test(v.stock.trim())) e[k('stock')] = 'Whole number, not negative'
  })
  return e
}

export function validateRegister(f: RegisterForm): RegisterErrors {
  const e: RegisterErrors = {}
  if (!f.firstName.trim()) e.firstName = 'First name is required'
  if (!f.lastName.trim()) e.lastName = 'Last name is required'
  if (!f.identifier.trim()) e.identifier = 'Email is required'
  else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(f.identifier.trim())) e.identifier = 'Enter a valid email'
  if (f.password.length < 8 || f.password.length > 72) e.password = 'Password must be 8 to 72 characters'
  if (f.confirmPassword !== f.password) e.confirmPassword = 'Passwords do not match'
  // Backend rule: document type and number are both given or both left empty.
  if (f.documentType && !f.documentNumber.trim()) e.documentNumber = 'Enter the document number'
  if (!f.documentType && f.documentNumber.trim()) e.documentType = 'Choose the document type'
  return e
}
