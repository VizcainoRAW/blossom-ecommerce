export type AccountType = 'EMPLOYEE' | 'CUSTOMER'

/**
 * Reads the account type from a JWT, or null if the token is missing, malformed or expired.
 * No signature check: this only drives the UI, the backend enforces real access.
 */
export function parseToken(token: string | null, now = Date.now()): { accountType: AccountType } | null {
  if (!token) return null
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')))
    if (typeof payload.exp !== 'number' || payload.exp * 1000 <= now) return null
    return { accountType: payload.account_type }
  } catch {
    return null
  }
}
