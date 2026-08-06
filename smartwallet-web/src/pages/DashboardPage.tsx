import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router'
import { getCurrentUser } from '../api/userApi'
import { getAccounts } from '../api/accountApi'
import type { User } from '../api/authApi'
import type { Account } from '../api/accountApi'
import { clearAuthSession } from '../auth/authStorage'
import './DashboardPage.css'

function DashboardPage() {
    const navigate = useNavigate()

    const [user, setUser] = useState<User | null>(null)
    const [accounts, setAccounts] = useState<Account[]>([])
    const [error, setError] = useState('')
    const [isLoading, setIsLoading] = useState(true)

    useEffect(() => {
        async function loadDashboard() {
            try {
                const [currentUser, accountList] = await Promise.all([
                    getCurrentUser(),
                    getAccounts(),
                ])

                setUser(currentUser)
                setAccounts(accountList)
            } catch (err) {
                setError(
                    err instanceof Error
                        ? err.message
                        : 'Dashboard could not be loaded',
                )
            } finally {
                setIsLoading(false)
            }
        }

        loadDashboard()
    }, [])

    function handleLogout() {
        clearAuthSession()
        navigate('/login', { replace: true })
    }

    if (isLoading) {
        return <p>Loading...</p>
    }

    if (error) {
        return <p>{error}</p>
    }

    return (
        <main className="dashboard">
            <header className="dashboard-header">
                <div>
                    <h1>Dashboard</h1>
                    <p>
                        Welcome, {user?.firstName} {user?.lastName}.
                    </p>
                </div>

                <button
                    className="logout-button"
                    type="button"
                    onClick={handleLogout}
                >
                    Logout
                </button>
            </header>

            <section className="accounts-section">
                <h2>Your Accounts</h2>

                {accounts.length === 0 ? (
                    <div className="empty-state">
                        <p>You do not have an account yet.</p>
                    </div>
                ) : (
                    <div className="account-grid">
                        {accounts.map((account) => (
                            <article className="account-card" key={account.id}>
                                <div className="account-card-header">
                                    <h3>{account.name}</h3>
                                    <span>{account.type}</span>
                                </div>

                                <p className="balance-label">Current balance</p>

                                <strong className="account-balance">
                                    {new Intl.NumberFormat('tr-TR', {
                                        style: 'currency',
                                        currency: account.currency,
                                    }).format(account.balance)}
                                </strong>
                            </article>
                        ))}
                    </div>
                )}
            </section>
        </main>
    )
}

export default DashboardPage