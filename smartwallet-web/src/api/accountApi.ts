import { apiRequest } from './apiClient'

export type Account = {
    id: number
    name: string
    type: string
    balance: number
    currency: string
    createdAt: string
    updatedAt: string
}

export function getAccounts() {
    return apiRequest<Account[]>('/api/accounts', {
        method: 'GET',
    })
}