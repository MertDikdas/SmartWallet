import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router'
import { getCurrentUser } from '../api/userApi'
import type { User } from '../api/authApi'
import { clearAuthSession } from '../auth/authStorage'
import './DashboardPage.css'
import { createAccount, getAccounts } from '../api/accountApi'
import {
    createTransaction,
    getTransactions,
} from '../api/transactionApi'
import type {
    Transaction,
    TransactionType,
} from '../api/transactionApi'

import { getCategories } from '../api/categoryApi'
import type { Category } from '../api/categoryApi'

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
    const [transactions, setTransactions] = useState<Transaction[]>([])
    const [categories, setCategories] = useState<Category[]>([])
    const [transactionAccountId, setTransactionAccountId] =
        useState('')
    const [transactionType, setTransactionType] =
        useState<TransactionType>('EXPENSE')
    const [categoryId, setCategoryId] = useState('')
    const [amount, setAmount] = useState('')
    const [description, setDescription] = useState('')
    const [transactionDate, setTransactionDate] = useState(
        new Date().toISOString().slice(0, 16),
    )
    const [isCreatingTransaction, setIsCreatingTransaction] =
        useState(false)
    const [transactionError, setTransactionError] = useState('')

    const filteredCategories = categories.filter(
        (category) => category.type === transactionType,
    )

    useEffect(() => {
        async function loadDashboard() {
            try {
                const [
                    currentUser,
                    accountList,
                    transactionPage,
                    categoryList,
                ] = await Promise.all([
                    getCurrentUser(),
                    getAccounts(),
                    getTransactions({
                        page: 0,
                        size: 10,
                    }),
                    getCategories(),
                ])

                setUser(currentUser)
                setAccounts(accountList)
                setTransactions(transactionPage.content)
                setCategories(categoryList)

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

    async function handleCreateTransaction(
        event: React.FormEvent<HTMLFormElement>,
    ) {
        event.preventDefault()
        setTransactionError('')
        setIsCreatingTransaction(true)

        try {
            await createTransaction({
                accountId: Number(transactionAccountId),
                categoryId: Number(categoryId),
                type: transactionType,
                amount: Number(amount),
                description: description.trim(),
                transactionDate,
            })

            const [accountList, transactionPage] = await Promise.all([
                getAccounts(),
                getTransactions({
                    page: 0,
                    size: 10,
                }),
            ])

            setAccounts(accountList)
            setTransactions(transactionPage.content)

            setCategoryId('')
            setAmount('')
            setDescription('')
            setTransactionDate(new Date().toISOString().slice(0, 16))
        } catch (err) {
            setTransactionError(
                err instanceof Error
                    ? err.message
                    : 'Transaction could not be created',
            )
        } finally {
            setIsCreatingTransaction(false)
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
            <section className="create-transaction-section">
                <h2>Create Transaction</h2>

                {accounts.length === 0 ? (
                    <p>You must create an account first.</p>
                ) : (
                    <form
                        className="create-transaction-form"
                        onSubmit={handleCreateTransaction}
                    >
                        <label>
                            Account
                            <select
                                value={transactionAccountId}
                                required
                                onChange={(event) =>
                                    setTransactionAccountId(event.target.value)
                                }
                            >
                                <option value="">Select account</option>

                                {accounts.map((account) => (
                                    <option key={account.id} value={account.id}>
                                        {account.name} ({account.currency})
                                    </option>
                                ))}
                            </select>
                        </label>

                        <label>
                            Transaction type
                            <select
                                value={transactionType}
                                onChange={(event) => {
                                    setTransactionType(
                                        event.target.value as TransactionType,
                                    )
                                    setCategoryId('')
                                }}
                            >
                                <option value="EXPENSE">Expense</option>
                                <option value="INCOME">Income</option>
                            </select>
                        </label>

                        <label>
                            Category
                            <select
                                value={categoryId}
                                required
                                onChange={(event) => setCategoryId(event.target.value)}
                            >
                                <option value="">Select category</option>

                                {filteredCategories.map((category) => (
                                    <option key={category.id} value={category.id}>
                                        {category.name}
                                    </option>
                                ))}
                            </select>
                        </label>

                        <label>
                            Amount
                            <input
                                type="number"
                                value={amount}
                                min="0.01"
                                step="0.01"
                                required
                                onChange={(event) => setAmount(event.target.value)}
                            />
                        </label>

                        <label>
                            Description
                            <input
                                type="text"
                                value={description}
                                maxLength={255}
                                onChange={(event) => setDescription(event.target.value)}
                            />
                        </label>

                        <label>
                            Transaction date
                            <input
                                type="datetime-local"
                                value={transactionDate}
                                required
                                onChange={(event) =>
                                    setTransactionDate(event.target.value)
                                }
                            />
                        </label>

                        {transactionError && (
                            <p className="transaction-form-error">
                                {transactionError}
                            </p>
                        )}

                        <button type="submit" disabled={isCreatingTransaction}>
                            {isCreatingTransaction
                                ? 'Creating...'
                                : 'Create Transaction'}
                        </button>
                    </form>
                )}
            </section>
            <section className="transactions-section">
                <h2>Recent Transactions</h2>

                {transactions.length === 0 ? (
                    <div className="empty-state">
                        <p>You do not have any transactions yet.</p>
                    </div>
                ) : (
                    <div className="transaction-list">
                        {transactions.map((transaction) => (
                            <article
                                className="transaction-item"
                                key={transaction.id}
                            >
                                <div className="transaction-info">
                                    <h3>
                                        {transaction.description ||
                                            transaction.categoryName}
                                    </h3>

                                    <p>
                                        {transaction.categoryName} ·{' '}
                                        {new Intl.DateTimeFormat('tr-TR', {
                                            dateStyle: 'medium',
                                        }).format(new Date(transaction.transactionDate))}
                                    </p>
                                </div>

                                <strong
                                    className={`transaction-amount ${
                                        transaction.type === 'INCOME'
                                            ? 'transaction-income'
                                            : 'transaction-expense'
                                    }`}
                                >
                                    {transaction.type === 'INCOME' ? '+' : '-'}
                                    {new Intl.NumberFormat('tr-TR', {
                                        style: 'currency',
                                        currency:
                                            accounts.find(
                                                (account) =>
                                                    account.id === transaction.accountId,
                                            )?.currency ?? 'TRY',
                                    }).format(transaction.amount)}
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