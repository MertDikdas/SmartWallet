import { apiRequest } from './apiClient'

export type MonthlyAnalytics = {
    year: number
    month: number
    totalIncome: number
    totalExpense: number
    netAmount: number
    transactionCount: number
}

export type CategoryExpense = {
    categoryId: number
    categoryName: string
    totalExpense: number
    percentage: number
    transactionCount: number
}

export type MonthlyCategoryAnalytics = {
    year: number
    month: number
    totalExpense: number
    categories: CategoryExpense[]
}

export type MonthlyTrendItem = {
    year: number
    month: number
    totalIncome: number
    totalExpense: number
    netAmount: number
    transactionCount: number
}

export type MonthlyTrend = {
    months: MonthlyTrendItem[]
}

export function getMonthlyAnalytics(year: number, month: number) {
    return apiRequest<MonthlyAnalytics>(
        `/api/analytics/monthly?year=${year}&month=${month}`,
    )
}

export function getMonthlyCategoryAnalytics(year: number, month: number) {
    return apiRequest<MonthlyCategoryAnalytics>(
        `/api/analytics/monthly/categories?year=${year}&month=${month}`,
    )
}

export function getMonthlyTrend(months = 6) {
    return apiRequest<MonthlyTrend>(
        `/api/analytics/monthly-trend?months=${months}`,
    )
}
