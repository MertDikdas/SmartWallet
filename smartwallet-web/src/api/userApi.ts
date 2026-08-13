import { apiRequest } from './apiClient'
import type { User } from './authApi'

export function getCurrentUser() {
    return apiRequest<User>('/api/users/me', {
        method: 'GET',
    })
}