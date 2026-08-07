import { apiRequest } from './apiClient'

export type RegisterRequest = {
    firstName: string
    lastName: string
    email: string
    password: string
}

export type LoginRequest = {
    email: string
    password: string
}

export type User = {
    id: number
    firstName: string
    lastName: string
    email: string
    role: string
    enabled: boolean
    createdAt: string
}

export type LoginResponse = {
    accessToken: string
    refreshToken: string
    tokenType: string
    expiresIn: number
    user: User
}

export function register(request: RegisterRequest) {
    return apiRequest<User>('/api/auth/register', {
        method: 'POST',
        body: JSON.stringify(request),
    })
}

export function login(request: LoginRequest) {
    return apiRequest<LoginResponse>('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify(request),
    })
}

export function logout(refreshToken: string) {
    return apiRequest<void>('/api/auth/logout', {
        method: 'POST',
        body: JSON.stringify({ refreshToken }),
    })
}
