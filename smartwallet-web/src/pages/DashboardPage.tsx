import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router'
import { getCurrentUser } from '../api/userApi'
import type { User } from '../api/authApi'
import { clearAuthSession } from '../auth/authStorage'
import './DashboardPage.css'
import { createAccount, getAccounts } from '../api/accountApi'
import type {
    Account,
    AccountType,
    Currency,
} from '../api/accountApi'

function DashboardPage() {
    const navigate = useNavigate()

    const [user, setUser] = useState<User | null>(null)
    const [accounts, setAccounts] = useState<Account[]>([])
    const [error, setError] = useState('')
    const [isLoading, setIsLoading] = useState(true)
    const [name, setName] = useState('')
    const [type, setType] = useState<AccountType>('CHECKING')
    const [currency, setCurrency] = useState<Currency>('TRY')
    const [initialBalance, setInitialBalance] = useState('0')
    const [isCreating, setIsCreating] = useState(false)
    const [createError, setCreateError] = useState('')

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

    async function handleCreateAccount(
        event: React.FormEvent<HTMLFormElement>,
    ) {
        event.preventDefault()
        setCreateError('')
        setIsCreating(true)

        try {
            const newAccount = await createAccount({
                name: name.trim(),
                type,
                currency,
                initialBalance: Number(initialBalance),
            })

            setAccounts((currentAccounts) => [
                ...currentAccounts,
                newAccount,
            ])

            setName('')
            setType('CHECKING')
            setCurrency('TRY')
            setInitialBalance('0')
        } catch (err) {
            setCreateError(
                err instanceof Error
                    ? err.message
                    : 'Account could not be created',
            )
        } finally {
            setIsCreating(false)
        }
    }
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
            <section className="create-account-section">
                <h2>Create Account</h2>

                <form
                    className="create-account-form"
                    onSubmit={handleCreateAccount}
                >
                    <label>
                        Account name
                        <input
                            type="text"
                            value={name}
                            maxLength={100}
                            required
                            onChange={(event) => setName(event.target.value)}
                        />
                    </label>

                    <label>
                        Account type
                        <select
                            value={type}
                            onChange={(event) =>
                                setType(event.target.value as AccountType)
                            }
                        >
                            <option value="CHECKING">Checking</option>
                            <option value="SAVINGS">Savings</option>
                            <option value="CASH">Cash</option>
                            <option value="CREDIT_CARD">Credit card</option>
                        </select>
                    </label>

                    <label>
                        Currency
                        <select
                            value={currency}
                            onChange={(event) =>
                                setCurrency(event.target.value as Currency)
                            }
                        >
                            <option value="TRY">TRY</option>
                            <option value="USD">USD</option>
                            <option value="EUR">EUR</option>
                        </select>
                    </label>

                    <label>
                        Initial balance
                        <input
                            type="number"
                            value={initialBalance}
                            min="0"
                            step="0.01"
                            required
                            onChange={(event) =>
                                setInitialBalance(event.target.value)
                            }
                        />
                    </label>

                    {createError && (
                        <p className="create-account-error">{createError}</p>
                    )}

                    <button type="submit" disabled={isCreating}>
                        {isCreating ? 'Creating...' : 'Create Account'}
                    </button>
                </form>
            </section>

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