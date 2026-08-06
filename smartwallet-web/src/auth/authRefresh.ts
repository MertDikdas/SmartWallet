import type { LoginResponse } from '../api/authApi'
import {
    clearAuthSession,
    getRefreshToken,
    saveAuthSession,
} from './authStorage'

export async function refreshAuthSession(): Promise<string> {
    const refreshToken = getRefreshToken()

    if (!refreshToken) {
        clearAuthSession()
        throw new Error('Your session has expired')
    }

    const response = await fetch('/api/auth/refresh', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ refreshToken }),
    })

    if (!response.ok) {
        clearAuthSession()
        throw new Error('Your session has expired')
    }

    const data = (await response.json()) as LoginResponse

    saveAuthSession(data)

    return data.accessToken
}