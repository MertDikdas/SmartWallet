import { useEffect, useState } from 'react'
import { getCurrentUser } from '../api/userApi'
import type { User } from '../api/authApi'
import { useNavigate } from 'react-router'
import { clearAuthSession } from '../auth/authStorage'

function DashboardPage() {
    const [user, setUser] = useState<User | null>(null)
    const [error, setError] = useState('')
    const [isLoading, setIsLoading] = useState(true)
    const navigate = useNavigate()

    function handleLogout() {
        clearAuthSession()
        navigate('/', { replace: true })
    }

    useEffect(() => {
        async function loadCurrentUser() {
            try {
                const currentUser = await getCurrentUser()
                setUser(currentUser)
            } catch (err) {
                setError(
                    err instanceof Error
                        ? err.message
                        : 'User information could not be loaded',
                )
            } finally {
                setIsLoading(false)
            }
        }

        loadCurrentUser()
    }, [])

    if (isLoading) {
        return <p>Loading...</p>
    }

    if (error) {
        return <p>{error}</p>
    }

    return (
        <main>
            <h1>Dashboard</h1>

            <p>
                Welcome, {user?.firstName} {user?.lastName}.
            </p>

            <p>{user?.email}</p>

            <button type="button" onClick={handleLogout}>
                Logout
            </button>
        </main>
    )
}

export default DashboardPage