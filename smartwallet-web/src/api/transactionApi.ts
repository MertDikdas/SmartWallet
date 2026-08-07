import { apiRequest } from './apiClient'

export type TransactionType = 'INCOME' | 'EXPENSE'

export type Transaction = {
    id: number
    accountId: number
    categoryId: number
    categoryName: string
    type: TransactionType
    amount: number
    description: string
    transactionDate: string
    createdAt: string
}

export type PageResponse<T> = {
    content: T[]
    page: number
    size: number
    totalElements: number
    totalPages: number
    first: boolean
    last: boolean
    hasNext: boolean
    hasPrevious: boolean
}

export type TransactionFilters = {
    accountId?: number
    categoryId?: number
    type?: TransactionType
    startDate?: string
    endDate?: string
    page?: number
    size?: number
}

export type CreateTransactionRequest = {
    accountId: number
    categoryId: number
    type: TransactionType
    amount: number
    description?: string
    transactionDate: string
}

export function getTransactions(filters: TransactionFilters = {}) {
    const searchParams = new URLSearchParams()

    Object.entries(filters).forEach(([key, value]) => {
        if (value !== undefined && value !== '') {
            searchParams.set(key, String(value))
        }
    })

    const query = searchParams.toString()

    return apiRequest<PageResponse<Transaction>>(
        `/api/transactions${query ? `?${query}` : ''}`,
    )
}

export function createTransaction(request: CreateTransactionRequest) {
    return apiRequest<Transaction>('/api/transactions', {
        method: 'POST',
        body: JSON.stringify(request),
    })
}

export function deleteTransaction(transactionId: number) {
    return apiRequest<void>(`/api/transactions/${transactionId}`, {
        method: 'DELETE',
    })
}
