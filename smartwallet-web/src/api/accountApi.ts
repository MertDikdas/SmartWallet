import { apiRequest } from './apiClient'

export type AccountType =
    | 'CHECKING'
    | 'SAVINGS'
    | 'CASH'
    | 'CREDIT_CARD'

export type Currency = 'TRY' | 'USD' | 'EUR'

export type Account = {
    id: number
    name: string
    type: AccountType
    balance: number
    currency: Currency
    createdAt: string
    updatedAt: string
}

export type CreateAccountRequest = {
    name: string
    type: AccountType
    currency: Currency
    initialBalance: number
}

export type UpdateAccountRequest = {
    name: string
    type: AccountType
    currency: Currency
}

export function createAccount(request: CreateAccountRequest) {
    return apiRequest<Account>('/api/accounts', {
        method: 'POST',
        body: JSON.stringify(request),
    })
}

export function getAccounts() {
    return apiRequest<Account[]>('/api/accounts')
}

export function deleteAccount(accountId: number) {
    return apiRequest<void>(`/api/accounts/${accountId}`, {
        method: 'DELETE',
    })
}

export function updateAccount(
    accountId: number,
    request: UpdateAccountRequest
) {
    console.log("accountId:", accountId);
    console.log("typeof:", typeof accountId);
    return apiRequest<Account>(`/api/accounts/${accountId}`, {
        method: 'PATCH',
        body: JSON.stringify(request),
    })
}
