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
    type: string
    balance: number
    currency: string
    createdAt: string
    updatedAt: string
}


export type CreateAccountRequest = {
    name: string
    type: AccountType
    currency: Currency
    initialBalance: number
}

export function createAccount(request: CreateAccountRequest) {
    return apiRequest<Account>('/api/accounts', {
        method: 'POST',
        body: JSON.stringify(request),
    })
}

export function getAccounts() {
    return apiRequest<Account[]>('/api/accounts', {
        method: 'GET',
    })
}