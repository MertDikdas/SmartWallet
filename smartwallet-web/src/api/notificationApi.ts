import { apiRequest } from './apiClient'
import type { PageResponse } from './transactionApi'

export type NotificationType =
    | 'BUDGET_EXCEEDED'
    | 'RECURRING_TRANSACTION_FAILED'

export type Notification = {
    id: number
    type: NotificationType
    title: string
    message: string
    resourceType: 'BUDGET' | 'RECURRING_TRANSACTION'
    resourceId: number
    read: boolean
    createdAt: string
    readAt: string | null
}

export function getNotifications(unreadOnly = false, page = 0, size = 20) {
    return apiRequest<PageResponse<Notification>>(
        `/api/notifications?unreadOnly=${unreadOnly}&page=${page}&size=${size}`,
    )
}

export function getUnreadNotificationCount() {
    return apiRequest<{ unreadCount: number }>('/api/notifications/unread-count')
}

export function markNotificationRead(notificationId: number) {
    return apiRequest<Notification>(
        `/api/notifications/${notificationId}/read`,
        { method: 'PATCH' },
    )
}

export function markAllNotificationsRead() {
    return apiRequest<{ updatedCount: number }>('/api/notifications/read-all', {
        method: 'PATCH',
    })
}
