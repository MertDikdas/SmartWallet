import { apiRequest } from './apiClient'
import type { TransactionType } from './transactionApi'

export type Category = {
    id: number
    name: string
    type: TransactionType
    createdAt: string
}

export type CreateCategoryRequest = {
    name: string
    type: TransactionType
}

export function getCategories() {
    return apiRequest<Category[]>('/api/categories')
}

export function createCategory(request: CreateCategoryRequest) {
    return apiRequest<Category>('/api/categories', {
        method: 'POST',
        body: JSON.stringify(request),
    })
}
