import type { LoginResponse, User } from '../api/authApi'

const ACCESS_TOKEN_KEY = 'accessToken'
const REFRESH_TOKEN_KEY = 'refreshToken'
const USER_KEY = 'user'

export function saveAuthSession(data: LoginResponse) {
    sessionStorage.setItem(ACCESS_TOKEN_KEY, data.accessToken)
    sessionStorage.setItem(REFRESH_TOKEN_KEY, data.refreshToken)
    sessionStorage.setItem(USER_KEY, JSON.stringify(data.user))
}

export function getAccessToken() {
    return sessionStorage.getItem(ACCESS_TOKEN_KEY)
}

export function getRefreshToken() {
    return sessionStorage.getItem(REFRESH_TOKEN_KEY)
}

export function getCurrentUser(): User | null {
    const storedUser = sessionStorage.getItem(USER_KEY)

    if (!storedUser) {
        return null
    }

    try {
        return JSON.parse(storedUser) as User
    } catch {
        clearAuthSession()
        return null
    }
}

export function isAuthenticated() {
    return getAccessToken() !== null
}

export function clearAuthSession() {
    sessionStorage.removeItem(ACCESS_TOKEN_KEY)
    sessionStorage.removeItem(REFRESH_TOKEN_KEY)
    sessionStorage.removeItem(USER_KEY)
}