import { apiRequest } from './apiClient'
import type { TransactionType } from './transactionApi'

export type Category = {
    id: number
    name: string
    type: TransactionType
    createdAt: string
}

export function getCategories() {
    return apiRequest<Category[]>('/api/categories', {
        method: 'GET',
    })
}