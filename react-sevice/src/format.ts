// shortcut: currency is hard-coded to USD, move to config if the store sells in another currency.
const currency = new Intl.NumberFormat(undefined, { style: 'currency', currency: 'USD' })
export const money = (n: number) => currency.format(n)

const dateTime = new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' })
export const formatDate = (iso: string) => dateTime.format(new Date(iso))
