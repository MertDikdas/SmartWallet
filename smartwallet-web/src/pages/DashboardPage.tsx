import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router'
import type { User } from '../api/authApi'
import { logout } from '../api/authApi'
import {
    createAccount,
    getAccounts,
    type Account,
    type AccountType,
    type Currency,
} from '../api/accountApi'
import {
    createCategory,
    getCategories,
    type Category,
} from '../api/categoryApi'
import {
    createTransaction,
    getTransactions,
    type Transaction,
    type TransactionType,
} from '../api/transactionApi'
import {
    createBudget,
    getBudgets,
    type Budget,
} from '../api/budgetApi'
import {
    getMonthlyAnalytics,
    getMonthlyCategoryAnalytics,
    getMonthlyTrend,
    type MonthlyAnalytics,
    type MonthlyCategoryAnalytics,
    type MonthlyTrend,
} from '../api/analyticsApi'
import {
    createTransfer,
    getTransfers,
    type Transfer,
} from '../api/transferApi'
import {
    createRecurringTransaction,
    getRecurringTransactions,
    pauseRecurringTransaction,
    resumeRecurringTransaction,
    type RecurrenceFrequency,
    type RecurringTransaction,
} from '../api/recurringApi'
import {
    getNotifications,
    getUnreadNotificationCount,
    markAllNotificationsRead,
    markNotificationRead,
    type Notification,
} from '../api/notificationApi'
import { getCurrentUser as getCurrentUserApi } from '../api/userApi'
import {
    clearAuthSession,
    getRefreshToken,
} from '../auth/authStorage'
import Icon, { type IconName } from '../components/Icon'
import Modal from '../components/Modal'
import {
    formatCompactMoney,
    formatDate,
    formatDateTime,
    formatMoney,
    monthName,
    percentage,
    toIsoInstant,
    toLocalDateTimeInput,
} from '../utils/format'
import './DashboardPage.css'

type DashboardView =
    | 'overview'
    | 'accounts'
    | 'transactions'
    | 'budgets'
    | 'transfers'
    | 'recurring'

type ModalName =
    | 'account'
    | 'transaction'
    | 'category'
    | 'budget'
    | 'transfer'
    | 'recurring'
    | null

const navigation: Array<{
    id: DashboardView
    label: string
    icon: IconName
}> = [
    { id: 'overview', label: 'Overview', icon: 'dashboard' },
    { id: 'accounts', label: 'Accounts', icon: 'wallet' },
    { id: 'transactions', label: 'Transactions', icon: 'transaction' },
    { id: 'budgets', label: 'Budgets', icon: 'budget' },
    { id: 'transfers', label: 'Transfers', icon: 'transfer' },
    { id: 'recurring', label: 'Recurring', icon: 'repeat' },
]

function DashboardPage() {
    const navigate = useNavigate()
    const today = useMemo(() => new Date(), [])

    const [view, setView] = useState<DashboardView>('overview')
    const [modal, setModal] = useState<ModalName>(null)
    const [sidebarOpen, setSidebarOpen] = useState(false)
    const [notificationOpen, setNotificationOpen] = useState(false)
    const [search, setSearch] = useState('')

    const [user, setUser] = useState<User | null>(null)
    const [accounts, setAccounts] = useState<Account[]>([])
    const [transactions, setTransactions] = useState<Transaction[]>([])
    const [categories, setCategories] = useState<Category[]>([])
    const [budgets, setBudgets] = useState<Budget[]>([])
    const [analytics, setAnalytics] = useState<MonthlyAnalytics | null>(null)
    const [categoryAnalytics, setCategoryAnalytics] =
        useState<MonthlyCategoryAnalytics | null>(null)
    const [trend, setTrend] = useState<MonthlyTrend | null>(null)
    const [transfers, setTransfers] = useState<Transfer[]>([])
    const [recurring, setRecurring] = useState<RecurringTransaction[]>([])
    const [notifications, setNotifications] = useState<Notification[]>([])
    const [unreadCount, setUnreadCount] = useState(0)

    const [isLoading, setIsLoading] = useState(true)
    const [error, setError] = useState('')

    const loadDashboard = useCallback(async () => {
        setIsLoading(true)
        setError('')

        try {
            const [currentUser, accountList, transactionPage, categoryList] =
                await Promise.all([
                    getCurrentUserApi(),
                    getAccounts(),
                    getTransactions({ page: 0, size: 50 }),
                    getCategories(),
                ])

            setUser(currentUser)
            setAccounts(accountList)
            setTransactions(transactionPage.content)
            setCategories(categoryList)

            const optionalRequests = await Promise.allSettled([
                getBudgets(),
                getMonthlyAnalytics(today.getFullYear(), today.getMonth() + 1),
                getMonthlyCategoryAnalytics(
                    today.getFullYear(),
                    today.getMonth() + 1,
                ),
                getMonthlyTrend(6),
                getTransfers(0, 30),
                getRecurringTransactions(),
                getNotifications(false, 0, 20),
                getUnreadNotificationCount(),
            ])

            const [
                budgetResult,
                analyticsResult,
                categoryAnalyticsResult,
                trendResult,
                transferResult,
                recurringResult,
                notificationResult,
                unreadResult,
            ] = optionalRequests

            if (budgetResult.status === 'fulfilled') {
                setBudgets(budgetResult.value)
            }
            if (analyticsResult.status === 'fulfilled') {
                setAnalytics(analyticsResult.value)
            }
            if (categoryAnalyticsResult.status === 'fulfilled') {
                setCategoryAnalytics(categoryAnalyticsResult.value)
            }
            if (trendResult.status === 'fulfilled') {
                setTrend(trendResult.value)
            }
            if (transferResult.status === 'fulfilled') {
                setTransfers(transferResult.value.content)
            }
            if (recurringResult.status === 'fulfilled') {
                setRecurring(recurringResult.value)
            }
            if (notificationResult.status === 'fulfilled') {
                setNotifications(notificationResult.value.content)
            }
            if (unreadResult.status === 'fulfilled') {
                setUnreadCount(unreadResult.value.unreadCount)
            }
        } catch (loadError) {
            setError(
                loadError instanceof Error
                    ? loadError.message
                    : 'Dashboard could not be loaded.',
            )
        } finally {
            setIsLoading(false)
        }
    }, [today])

    useEffect(() => {
        const timeoutId = window.setTimeout(() => {
            void loadDashboard()
        }, 0)

        return () => window.clearTimeout(timeoutId)
    }, [loadDashboard])

    const primaryCurrency = accounts[0]?.currency ?? 'TRY'

    const balancesByCurrency = useMemo(() => {
        return accounts.reduce<Record<string, number>>((groups, account) => {
            groups[account.currency] =
                (groups[account.currency] ?? 0) + Number(account.balance)
            return groups
        }, {})
    }, [accounts])

    const filteredTransactions = useMemo(() => {
        const query = search.trim().toLocaleLowerCase('en-US')

        if (!query) {
            return transactions
        }

        return transactions.filter((transaction) =>
            [transaction.description, transaction.categoryName, transaction.type]
                .join(' ')
                .toLocaleLowerCase('en-US')
                .includes(query),
        )
    }, [search, transactions])

    const categoryName = (categoryId: number) =>
        categories.find((category) => category.id === categoryId)?.name ??
        'Category'

    const accountFor = (accountId: number) =>
        accounts.find((account) => account.id === accountId)

    async function handleLogout() {
        const refreshToken = getRefreshToken()

        try {
            if (refreshToken) {
                await logout(refreshToken)
            }
        } catch {
            // Local logout still completes if the auth service is unavailable.
        } finally {
            clearAuthSession()
            navigate('/login', { replace: true })
        }
    }

    function changeView(nextView: DashboardView) {
        setView(nextView)
        setSidebarOpen(false)
    }

    async function handleNotificationClick(notification: Notification) {
        if (!notification.read) {
            try {
                const updated = await markNotificationRead(notification.id)
                setNotifications((current) =>
                    current.map((item) =>
                        item.id === updated.id ? updated : item,
                    ),
                )
                setUnreadCount((current) => Math.max(0, current - 1))
            } catch {
                return
            }
        }
    }

    async function handleMarkAllRead() {
        try {
            await markAllNotificationsRead()
            setNotifications((current) =>
                current.map((item) => ({
                    ...item,
                    read: true,
                    readAt: item.readAt ?? new Date().toISOString(),
                })),
            )
            setUnreadCount(0)
        } catch {
            // The panel remains usable even if the service is unavailable.
        }
    }

    if (isLoading) {
        return <DashboardSkeleton />
    }

    if (error) {
        return (
            <main className="dashboard-error-page">
                <div className="dashboard-error-card">
                    <span className="error-icon">!</span>
                    <h1>We could not open your wallet</h1>
                    <p>{error}</p>
                    <div className="error-actions">
                        <button className="button primary" onClick={loadDashboard}>
                            Try again
                        </button>
                        <button className="button secondary" onClick={handleLogout}>
                            Sign out
                        </button>
                    </div>
                </div>
            </main>
        )
    }

    return (
        <div className="dashboard-shell">
            <aside className={`sidebar ${sidebarOpen ? 'sidebar-open' : ''}`}>
                <div className="sidebar-brand">
                    <span className="brand-mark">
                        <Icon name="wallet" size={22} />
                    </span>
                    <span>SmartWallet</span>
                </div>

                <nav className="sidebar-nav" aria-label="Dashboard navigation">
                    <p className="sidebar-label">Workspace</p>
                    {navigation.map((item) => (
                        <button
                            className={`sidebar-link ${
                                view === item.id ? 'active' : ''
                            }`}
                            type="button"
                            key={item.id}
                            onClick={() => changeView(item.id)}
                        >
                            <Icon name={item.icon} size={19} />
                            <span>{item.label}</span>
                        </button>
                    ))}
                </nav>

                <div className="sidebar-card">
                    <span className="sidebar-card-icon">
                        <Icon name="sparkle" size={18} />
                    </span>
                    <strong>Wallet health</strong>
                    <p>
                        {analytics && analytics.netAmount >= 0
                            ? 'You are cash-flow positive this month.'
                            : 'Review your spending to improve cash flow.'}
                    </p>
                </div>

                <div className="sidebar-user">
                    <span className="avatar">
                        {user?.firstName?.charAt(0)}{user?.lastName?.charAt(0)}
                    </span>
                    <div>
                        <strong>
                            {user?.firstName} {user?.lastName}
                        </strong>
                        <span>{user?.email}</span>
                    </div>
                    <button
                        className="icon-button sidebar-logout"
                        type="button"
                        aria-label="Sign out"
                        onClick={handleLogout}
                    >
                        <Icon name="logout" size={18} />
                    </button>
                </div>
            </aside>

            {sidebarOpen && (
                <button
                    className="sidebar-overlay"
                    type="button"
                    aria-label="Close navigation"
                    onClick={() => setSidebarOpen(false)}
                />
            )}

            <main className="dashboard-main">
                <header className="topbar">
                    <div className="topbar-left">
                        <button
                            className="icon-button mobile-menu"
                            type="button"
                            aria-label="Open navigation"
                            onClick={() => setSidebarOpen(true)}
                        >
                            <Icon name="menu" />
                        </button>
                        <div className="search-box">
                            <Icon name="search" size={18} />
                            <input
                                type="search"
                                placeholder="Search transactions..."
                                value={search}
                                onChange={(event) => setSearch(event.target.value)}
                            />
                        </div>
                    </div>

                    <div className="topbar-actions">
                        <span className="today-label">
                            {new Intl.DateTimeFormat('en-US', {
                                weekday: 'short',
                                day: '2-digit',
                                month: 'short',
                            }).format(today)}
                        </span>
                        <div className="notification-wrap">
                            <button
                                className="icon-button notification-button"
                                type="button"
                                aria-label="Notifications"
                                onClick={() =>
                                    setNotificationOpen((current) => !current)
                                }
                            >
                                <Icon name="bell" size={19} />
                                {unreadCount > 0 && (
                                    <span className="notification-badge">
                                        {unreadCount > 9 ? '9+' : unreadCount}
                                    </span>
                                )}
                            </button>
                            {notificationOpen && (
                                <NotificationPanel
                                    notifications={notifications}
                                    onClose={() => setNotificationOpen(false)}
                                    onMarkAll={handleMarkAllRead}
                                    onSelect={handleNotificationClick}
                                />
                            )}
                        </div>
                    </div>
                </header>

                <div className="dashboard-content">
                    {view === 'overview' && (
                        <OverviewView
                            user={user}
                            accounts={accounts}
                            balancesByCurrency={balancesByCurrency}
                            primaryCurrency={primaryCurrency}
                            analytics={analytics}
                            categoryAnalytics={categoryAnalytics}
                            trend={trend}
                            budgets={budgets}
                            transactions={filteredTransactions.slice(0, 6)}
                            categoryName={categoryName}
                            accountFor={accountFor}
                            onAddTransaction={() => setModal('transaction')}
                            onAddAccount={() => setModal('account')}
                            onViewTransactions={() => changeView('transactions')}
                            onViewBudgets={() => changeView('budgets')}
                        />
                    )}

                    {view === 'accounts' && (
                        <AccountsView
                            accounts={accounts}
                            onAdd={() => setModal('account')}
                        />
                    )}

                    {view === 'transactions' && (
                        <TransactionsView
                            transactions={filteredTransactions}
                            accounts={accounts}
                            onAdd={() => setModal('transaction')}
                            onAddCategory={() => setModal('category')}
                        />
                    )}

                    {view === 'budgets' && (
                        <BudgetsView
                            budgets={budgets}
                            categoryName={categoryName}
                            primaryCurrency={primaryCurrency}
                            onAdd={() => setModal('budget')}
                        />
                    )}

                    {view === 'transfers' && (
                        <TransfersView
                            transfers={transfers}
                            onAdd={() => setModal('transfer')}
                        />
                    )}

                    {view === 'recurring' && (
                        <RecurringView
                            recurring={recurring}
                            primaryCurrency={primaryCurrency}
                            onAdd={() => setModal('recurring')}
                            onStatusChange={async (item) => {
                                const updated =
                                    item.status === 'ACTIVE'
                                        ? await pauseRecurringTransaction(item.id)
                                        : await resumeRecurringTransaction(item.id)

                                setRecurring((current) =>
                                    current.map((value) =>
                                        value.id === updated.id ? updated : value,
                                    ),
                                )
                            }}
                        />
                    )}
                </div>
            </main>

            {modal === 'account' && (
                <Modal
                    title="Create account"
                    description="Add a wallet, bank account, cash balance, or card."
                    onClose={() => setModal(null)}
                >
                    <AccountForm
                        onSubmit={async (request) => {
                            const created = await createAccount(request)
                            setAccounts((current) => [...current, created])
                            setModal(null)
                        }}
                    />
                </Modal>
            )}

            {modal === 'transaction' && (
                <Modal
                    title="Add transaction"
                    description="Record income or an expense and keep balances current."
                    onClose={() => setModal(null)}
                >
                    <TransactionForm
                        accounts={accounts}
                        categories={categories}
                        onCreateCategory={() => setModal('category')}
                        onSubmit={async (request) => {
                            await createTransaction(request)
                            const [accountList, transactionPage] = await Promise.all([
                                getAccounts(),
                                getTransactions({ page: 0, size: 50 }),
                            ])
                            setAccounts(accountList)
                            setTransactions(transactionPage.content)
                            setModal(null)
                            void refreshOverviewData(today, setBudgets, setAnalytics, setCategoryAnalytics, setTrend)
                        }}
                    />
                </Modal>
            )}

            {modal === 'category' && (
                <Modal
                    title="Create category"
                    description="Categories keep transactions and budgets organized."
                    onClose={() => setModal(null)}
                >
                    <CategoryForm
                        onSubmit={async (request) => {
                            const created = await createCategory(request)
                            setCategories((current) => [...current, created])
                            setModal('transaction')
                        }}
                    />
                </Modal>
            )}

            {modal === 'budget' && (
                <Modal
                    title="Create budget"
                    description="Set a monthly spending limit for an expense category."
                    onClose={() => setModal(null)}
                >
                    <BudgetForm
                        categories={categories}
                        today={today}
                        onSubmit={async (request) => {
                            const created = await createBudget(request)
                            setBudgets((current) => [...current, created])
                            setModal(null)
                        }}
                    />
                </Modal>
            )}

            {modal === 'transfer' && (
                <Modal
                    title="Transfer money"
                    description="Move funds safely between accounts in the same currency."
                    onClose={() => setModal(null)}
                >
                    <TransferForm
                        accounts={accounts}
                        onSubmit={async (request) => {
                            const created = await createTransfer(request)
                            const accountList = await getAccounts()
                            setTransfers((current) => [created, ...current])
                            setAccounts(accountList)
                            setModal(null)
                        }}
                    />
                </Modal>
            )}

            {modal === 'recurring' && (
                <Modal
                    title="Create recurring transaction"
                    description="Automate weekly or monthly income and expenses."
                    onClose={() => setModal(null)}
                >
                    <RecurringForm
                        accounts={accounts}
                        categories={categories}
                        onSubmit={async (request) => {
                            const created = await createRecurringTransaction(request)
                            setRecurring((current) => [created, ...current])
                            setModal(null)
                        }}
                    />
                </Modal>
            )}
        </div>
    )
}

async function refreshOverviewData(
    date: Date,
    setBudgets: (value: Budget[]) => void,
    setAnalytics: (value: MonthlyAnalytics) => void,
    setCategoryAnalytics: (value: MonthlyCategoryAnalytics) => void,
    setTrend: (value: MonthlyTrend) => void,
) {
    const results = await Promise.allSettled([
        getBudgets(),
        getMonthlyAnalytics(date.getFullYear(), date.getMonth() + 1),
        getMonthlyCategoryAnalytics(date.getFullYear(), date.getMonth() + 1),
        getMonthlyTrend(6),
    ])

    if (results[0].status === 'fulfilled') setBudgets(results[0].value)
    if (results[1].status === 'fulfilled') setAnalytics(results[1].value)
    if (results[2].status === 'fulfilled') setCategoryAnalytics(results[2].value)
    if (results[3].status === 'fulfilled') setTrend(results[3].value)
}

interface OverviewProps {
    user: User | null
    accounts: Account[]
    balancesByCurrency: Record<string, number>
    primaryCurrency: string
    analytics: MonthlyAnalytics | null
    categoryAnalytics: MonthlyCategoryAnalytics | null
    trend: MonthlyTrend | null
    budgets: Budget[]
    transactions: Transaction[]
    categoryName: (id: number) => string
    accountFor: (id: number) => Account | undefined
    onAddTransaction: () => void
    onAddAccount: () => void
    onViewTransactions: () => void
    onViewBudgets: () => void
}

function OverviewView({
    user,
    accounts,
    balancesByCurrency,
    primaryCurrency,
    analytics,
    categoryAnalytics,
    trend,
    budgets,
    transactions,
    categoryName,
    accountFor,
    onAddTransaction,
    onAddAccount,
    onViewTransactions,
    onViewBudgets,
}: OverviewProps) {
    const savingsRate = analytics?.totalIncome
        ? Math.max((analytics.netAmount / analytics.totalIncome) * 100, 0)
        : 0
    const trendMax = Math.max(
        ...(trend?.months.flatMap((month) => [
            Number(month.totalIncome),
            Number(month.totalExpense),
        ]) ?? [1]),
        1,
    )

    return (
        <>
            <section className="page-heading overview-heading">
                <div>
                    <p className="eyebrow">Financial overview</p>
                    <h1>Good to see you, {user?.firstName}.</h1>
                    <p>Here is what is happening with your money this month.</p>
                </div>
                <div className="heading-actions">
                    <button className="button secondary" onClick={onAddAccount}>
                        <Icon name="wallet" size={18} />
                        New account
                    </button>
                    <button className="button primary" onClick={onAddTransaction}>
                        <Icon name="plus" size={18} />
                        Add transaction
                    </button>
                </div>
            </section>

            <section className="summary-grid">
                <article className="summary-card balance-summary">
                    <div className="summary-icon purple">
                        <Icon name="wallet" />
                    </div>
                    <div className="summary-copy">
                        <span>Total balance</span>
                        {Object.keys(balancesByCurrency).length === 0 ? (
                            <strong>{formatMoney(0, primaryCurrency)}</strong>
                        ) : (
                            <div className="currency-balances">
                                {Object.entries(balancesByCurrency).map(
                                    ([currency, balance]) => (
                                        <strong key={currency}>
                                            {formatMoney(balance, currency)}
                                        </strong>
                                    ),
                                )}
                            </div>
                        )}
                        <small>{accounts.length} active accounts</small>
                    </div>
                </article>

                <SummaryCard
                    title="Income"
                    value={formatCompactMoney(
                        analytics?.totalIncome ?? 0,
                        primaryCurrency,
                    )}
                    detail="This month"
                    icon="arrow-down"
                    tone="green"
                />
                <SummaryCard
                    title="Expenses"
                    value={formatCompactMoney(
                        analytics?.totalExpense ?? 0,
                        primaryCurrency,
                    )}
                    detail={`${analytics?.transactionCount ?? transactions.length} transactions`}
                    icon="arrow-up"
                    tone="red"
                />
                <SummaryCard
                    title="Savings rate"
                    value={`${savingsRate.toFixed(1)}%`}
                    detail={savingsRate >= 20 ? 'Healthy momentum' : 'Room to improve'}
                    icon="chart"
                    tone="blue"
                />
            </section>

            <section className="overview-grid">
                <article className="panel cashflow-panel">
                    <PanelHeader
                        title="Cash flow"
                        subtitle="Income and expenses over the last 6 months"
                    />
                    {trend?.months.length ? (
                        <div className="cashflow-chart">
                            <div className="chart-legend">
                                <span><i className="legend-income" />Income</span>
                                <span><i className="legend-expense" />Expense</span>
                            </div>
                            <div className="chart-bars">
                                {trend.months.map((item) => (
                                    <div className="chart-month" key={`${item.year}-${item.month}`}>
                                        <div className="bar-pair">
                                            <span
                                                className="bar income-bar"
                                                style={{
                                                    height: `${Math.max(
                                                        (Number(item.totalIncome) / trendMax) * 100,
                                                        3,
                                                    )}%`,
                                                }}
                                                title={`Income: ${formatMoney(item.totalIncome, primaryCurrency)}`}
                                            />
                                            <span
                                                className="bar expense-bar"
                                                style={{
                                                    height: `${Math.max(
                                                        (Number(item.totalExpense) / trendMax) * 100,
                                                        3,
                                                    )}%`,
                                                }}
                                                title={`Expense: ${formatMoney(item.totalExpense, primaryCurrency)}`}
                                            />
                                        </div>
                                        <span>{monthName(item.month)}</span>
                                    </div>
                                ))}
                            </div>
                        </div>
                    ) : (
                        <EmptyState
                            icon="chart"
                            title="No analytics yet"
                            description="Add transactions to build your monthly cash-flow chart."
                        />
                    )}
                </article>

                <article className="panel spending-panel">
                    <PanelHeader
                        title="Spending by category"
                        subtitle="Current month"
                    />
                    {categoryAnalytics?.categories.length ? (
                        <div className="category-spending-list">
                            {categoryAnalytics.categories.slice(0, 5).map((item, index) => (
                                <div className="category-spending-item" key={item.categoryId}>
                                    <div className={`category-dot category-${index % 5}`} />
                                    <div className="category-spending-main">
                                        <div>
                                            <strong>{item.categoryName}</strong>
                                            <span>{Number(item.percentage).toFixed(0)}%</span>
                                        </div>
                                        <div className="progress-track slim">
                                            <span style={{ width: `${Math.min(Number(item.percentage), 100)}%` }} />
                                        </div>
                                    </div>
                                    <strong>{formatCompactMoney(item.totalExpense, primaryCurrency)}</strong>
                                </div>
                            ))}
                        </div>
                    ) : (
                        <EmptyState
                            icon="budget"
                            title="No category data"
                            description="Expense categories will appear here automatically."
                        />
                    )}
                </article>
            </section>

            <section className="overview-lower-grid">
                <article className="panel transactions-panel">
                    <PanelHeader
                        title="Recent transactions"
                        subtitle="Your latest wallet activity"
                        actionLabel="View all"
                        onAction={onViewTransactions}
                    />
                    <TransactionList
                        transactions={transactions}
                        accountFor={accountFor}
                        compact
                    />
                </article>

                <article className="panel budget-panel">
                    <PanelHeader
                        title="Budget progress"
                        subtitle="Monthly limits"
                        actionLabel="Manage"
                        onAction={onViewBudgets}
                    />
                    {budgets.length ? (
                        <div className="budget-mini-list">
                            {budgets.slice(0, 4).map((budget) => {
                                const progress = percentage(
                                    Number(budget.spentAmount),
                                    Number(budget.limitAmount),
                                )
                                return (
                                    <div className="budget-mini-item" key={budget.id}>
                                        <div>
                                            <strong>{categoryName(budget.categoryId)}</strong>
                                            <span>
                                                {formatCompactMoney(budget.spentAmount, primaryCurrency)} /{' '}
                                                {formatCompactMoney(budget.limitAmount, primaryCurrency)}
                                            </span>
                                        </div>
                                        <div className={`progress-track ${budget.status === 'EXCEEDED' ? 'danger' : ''}`}>
                                            <span style={{ width: `${progress}%` }} />
                                        </div>
                                    </div>
                                )
                            })}
                        </div>
                    ) : (
                        <EmptyState
                            icon="budget"
                            title="No budgets yet"
                            description="Create a monthly limit to keep spending on track."
                        />
                    )}
                </article>
            </section>
        </>
    )
}

function SummaryCard({
    title,
    value,
    detail,
    icon,
    tone,
}: {
    title: string
    value: string
    detail: string
    icon: IconName
    tone: string
}) {
    return (
        <article className="summary-card">
            <div className={`summary-icon ${tone}`}>
                <Icon name={icon} />
            </div>
            <div className="summary-copy">
                <span>{title}</span>
                <strong>{value}</strong>
                <small>{detail}</small>
            </div>
        </article>
    )
}

function PanelHeader({
    title,
    subtitle,
    actionLabel,
    onAction,
}: {
    title: string
    subtitle: string
    actionLabel?: string
    onAction?: () => void
}) {
    return (
        <header className="panel-header">
            <div>
                <h2>{title}</h2>
                <p>{subtitle}</p>
            </div>
            {actionLabel && onAction && (
                <button className="text-button" type="button" onClick={onAction}>
                    {actionLabel}
                    <Icon name="arrow-right" size={16} />
                </button>
            )}
        </header>
    )
}

function AccountsView({ accounts, onAdd }: { accounts: Account[]; onAdd: () => void }) {
    return (
        <>
            <PageHeading
                eyebrow="Money sources"
                title="Accounts"
                description="Keep every balance in one clear view."
                action="Create account"
                icon="plus"
                onAction={onAdd}
            />
            {accounts.length ? (
                <section className="accounts-grid">
                    {accounts.map((account, index) => (
                        <article className={`wallet-card wallet-tone-${index % 4}`} key={account.id}>
                            <header>
                                <span className="wallet-card-icon"><Icon name="wallet" size={18} /></span>
                                <span className="account-type-pill">{account.type.replace('_', ' ')}</span>
                            </header>
                            <div>
                                <p>{account.name}</p>
                                <strong>{formatMoney(account.balance, account.currency)}</strong>
                            </div>
                            <footer>
                                <span>{account.currency}</span>
                                <span>•••• {String(account.id).padStart(4, '0')}</span>
                            </footer>
                        </article>
                    ))}
                </section>
            ) : (
                <EmptyStateCard
                    icon="wallet"
                    title="Create your first account"
                    description="Add a bank account, card, or cash wallet to start tracking your finances."
                    action="Create account"
                    onAction={onAdd}
                />
            )}
        </>
    )
}

function TransactionsView({
    transactions,
    accounts,
    onAdd,
    onAddCategory,
}: {
    transactions: Transaction[]
    accounts: Account[]
    onAdd: () => void
    onAddCategory: () => void
}) {
    return (
        <>
            <PageHeading
                eyebrow="Activity"
                title="Transactions"
                description="Review income and expenses across every account."
                action="Add transaction"
                icon="plus"
                onAction={onAdd}
                secondaryAction="New category"
                onSecondaryAction={onAddCategory}
            />
            <section className="panel table-panel">
                <TransactionList
                    transactions={transactions}
                    accountFor={(id) => accounts.find((account) => account.id === id)}
                />
            </section>
        </>
    )
}

function TransactionList({
    transactions,
    accountFor,
    compact = false,
}: {
    transactions: Transaction[]
    accountFor: (id: number) => Account | undefined
    compact?: boolean
}) {
    if (!transactions.length) {
        return (
            <EmptyState
                icon="transaction"
                title="No transactions found"
                description="Your recorded income and expenses will appear here."
            />
        )
    }

    return (
        <div className={`transaction-list-modern ${compact ? 'compact' : ''}`}>
            {transactions.map((transaction) => {
                const account = accountFor(transaction.accountId)
                const isIncome = transaction.type === 'INCOME'
                return (
                    <article className="transaction-row" key={transaction.id}>
                        <span className={`transaction-icon ${isIncome ? 'income' : 'expense'}`}>
                            <Icon name={isIncome ? 'arrow-down' : 'arrow-up'} size={17} />
                        </span>
                        <div className="transaction-description">
                            <strong>{transaction.description || transaction.categoryName}</strong>
                            <span>{transaction.categoryName} · {account?.name ?? 'Account'}</span>
                        </div>
                        {!compact && (
                            <span className="transaction-date">{formatDate(transaction.transactionDate)}</span>
                        )}
                        <strong className={isIncome ? 'money-positive' : 'money-negative'}>
                            {isIncome ? '+' : '-'}{formatMoney(transaction.amount, account?.currency ?? 'TRY')}
                        </strong>
                    </article>
                )
            })}
        </div>
    )
}

function BudgetsView({
    budgets,
    categoryName,
    primaryCurrency,
    onAdd,
}: {
    budgets: Budget[]
    categoryName: (id: number) => string
    primaryCurrency: string
    onAdd: () => void
}) {
    return (
        <>
            <PageHeading
                eyebrow="Spending plan"
                title="Budgets"
                description="Set monthly limits and catch overspending early."
                action="Create budget"
                icon="plus"
                onAction={onAdd}
            />
            {budgets.length ? (
                <section className="budget-grid">
                    {budgets.map((budget) => {
                        const progress = percentage(Number(budget.spentAmount), Number(budget.limitAmount))
                        return (
                            <article className="budget-card" key={budget.id}>
                                <header>
                                    <span className="budget-card-icon"><Icon name="budget" size={18} /></span>
                                    <span className={`status-pill ${budget.status.toLowerCase()}`}>{budget.status}</span>
                                </header>
                                <h2>{categoryName(budget.categoryId)}</h2>
                                <p>{monthName(budget.month)} {budget.year}</p>
                                <div className="budget-values">
                                    <strong>{formatMoney(budget.spentAmount, primaryCurrency)}</strong>
                                    <span>of {formatMoney(budget.limitAmount, primaryCurrency)}</span>
                                </div>
                                <div className={`progress-track large ${budget.status === 'EXCEEDED' ? 'danger' : ''}`}>
                                    <span style={{ width: `${progress}%` }} />
                                </div>
                                <footer>
                                    <span>{progress.toFixed(0)}% used</span>
                                    <strong className={Number(budget.remainingAmount) < 0 ? 'money-negative' : ''}>
                                        {formatMoney(budget.remainingAmount, primaryCurrency)} left
                                    </strong>
                                </footer>
                            </article>
                        )
                    })}
                </section>
            ) : (
                <EmptyStateCard
                    icon="budget"
                    title="Build your first budget"
                    description="Choose an expense category and set a monthly limit."
                    action="Create budget"
                    onAction={onAdd}
                />
            )}
        </>
    )
}

function TransfersView({ transfers, onAdd }: { transfers: Transfer[]; onAdd: () => void }) {
    return (
        <>
            <PageHeading
                eyebrow="Internal movement"
                title="Transfers"
                description="Move money between accounts without changing your net worth."
                action="New transfer"
                icon="transfer"
                onAction={onAdd}
            />
            <section className="panel table-panel">
                {transfers.length ? (
                    <div className="transfer-list">
                        {transfers.map((transfer) => (
                            <article className="transfer-row" key={transfer.id}>
                                <span className="transfer-icon"><Icon name="transfer" size={18} /></span>
                                <div className="transfer-route">
                                    <strong>{transfer.fromAccountName}</strong>
                                    <span><Icon name="arrow-right" size={14} />{transfer.toAccountName}</span>
                                </div>
                                <div className="transfer-meta">
                                    <span>{formatDateTime(transfer.transferredAt)}</span>
                                    <small>{transfer.description || 'Account transfer'}</small>
                                </div>
                                <strong>{formatMoney(transfer.amount, transfer.currency)}</strong>
                            </article>
                        ))}
                    </div>
                ) : (
                    <EmptyState
                        icon="transfer"
                        title="No transfers yet"
                        description="Transfers between your accounts will appear here."
                    />
                )}
            </section>
        </>
    )
}

function RecurringView({
    recurring,
    primaryCurrency,
    onAdd,
    onStatusChange,
}: {
    recurring: RecurringTransaction[]
    primaryCurrency: string
    onAdd: () => void
    onStatusChange: (item: RecurringTransaction) => Promise<void>
}) {
    const [busyId, setBusyId] = useState<number | null>(null)

    return (
        <>
            <PageHeading
                eyebrow="Automation"
                title="Recurring transactions"
                description="Automate regular bills, subscriptions, and income."
                action="Create recurring"
                icon="repeat"
                onAction={onAdd}
            />
            {recurring.length ? (
                <section className="recurring-grid">
                    {recurring.map((item) => (
                        <article className="recurring-card" key={item.id}>
                            <header>
                                <span className={`transaction-icon ${item.type === 'INCOME' ? 'income' : 'expense'}`}>
                                    <Icon name="repeat" size={18} />
                                </span>
                                <span className={`status-pill ${item.status.toLowerCase()}`}>{item.status}</span>
                            </header>
                            <h2>{item.description || item.categoryName}</h2>
                            <p>{item.accountName} · {item.frequency.toLowerCase()}</p>
                            <strong className={item.type === 'INCOME' ? 'money-positive' : 'money-negative'}>
                                {item.type === 'INCOME' ? '+' : '-'}{formatMoney(item.amount, primaryCurrency)}
                            </strong>
                            <div className="recurring-details">
                                <span>Next run</span>
                                <strong>{item.nextExecutionDate ? formatDate(item.nextExecutionDate) : 'Not scheduled'}</strong>
                            </div>
                            {item.status !== 'CANCELLED' && (
                                <button
                                    className="button secondary full-width"
                                    type="button"
                                    disabled={busyId === item.id}
                                    onClick={async () => {
                                        setBusyId(item.id)
                                        try {
                                            await onStatusChange(item)
                                        } finally {
                                            setBusyId(null)
                                        }
                                    }}
                                >
                                    {busyId === item.id
                                        ? 'Updating...'
                                        : item.status === 'ACTIVE'
                                          ? 'Pause schedule'
                                          : 'Resume schedule'}
                                </button>
                            )}
                        </article>
                    ))}
                </section>
            ) : (
                <EmptyStateCard
                    icon="repeat"
                    title="Automate a regular payment"
                    description="Create a weekly or monthly transaction and SmartWallet will handle the schedule."
                    action="Create recurring"
                    onAction={onAdd}
                />
            )}
        </>
    )
}

function PageHeading({
    eyebrow,
    title,
    description,
    action,
    icon,
    onAction,
    secondaryAction,
    onSecondaryAction,
}: {
    eyebrow: string
    title: string
    description: string
    action: string
    icon: IconName
    onAction: () => void
    secondaryAction?: string
    onSecondaryAction?: () => void
}) {
    return (
        <section className="page-heading">
            <div>
                <p className="eyebrow">{eyebrow}</p>
                <h1>{title}</h1>
                <p>{description}</p>
            </div>
            <div className="heading-actions">
                {secondaryAction && onSecondaryAction && (
                    <button className="button secondary" onClick={onSecondaryAction}>
                        {secondaryAction}
                    </button>
                )}
                <button className="button primary" onClick={onAction}>
                    <Icon name={icon} size={18} />
                    {action}
                </button>
            </div>
        </section>
    )
}

function EmptyState({
    icon,
    title,
    description,
}: {
    icon: IconName
    title: string
    description: string
}) {
    return (
        <div className="empty-state-modern">
            <span><Icon name={icon} /></span>
            <strong>{title}</strong>
            <p>{description}</p>
        </div>
    )
}

function EmptyStateCard({
    icon,
    title,
    description,
    action,
    onAction,
}: {
    icon: IconName
    title: string
    description: string
    action: string
    onAction: () => void
}) {
    return (
        <section className="empty-state-card">
            <span><Icon name={icon} size={26} /></span>
            <h2>{title}</h2>
            <p>{description}</p>
            <button className="button primary" onClick={onAction}>
                <Icon name="plus" size={18} />{action}
            </button>
        </section>
    )
}

function NotificationPanel({
    notifications,
    onClose,
    onMarkAll,
    onSelect,
}: {
    notifications: Notification[]
    onClose: () => void
    onMarkAll: () => void
    onSelect: (notification: Notification) => void
}) {
    return (
        <section className="notification-panel">
            <header>
                <div>
                    <h2>Notifications</h2>
                    <p>Important wallet updates</p>
                </div>
                <button className="icon-button" type="button" onClick={onClose} aria-label="Close notifications">
                    <Icon name="close" size={18} />
                </button>
            </header>
            {notifications.length ? (
                <div className="notification-list">
                    {notifications.map((notification) => (
                        <button
                            className={`notification-item ${notification.read ? '' : 'unread'}`}
                            type="button"
                            key={notification.id}
                            onClick={() => onSelect(notification)}
                        >
                            <span className="notification-type-icon">
                                <Icon name={notification.type === 'BUDGET_EXCEEDED' ? 'budget' : 'repeat'} size={17} />
                            </span>
                            <span>
                                <strong>{notification.title}</strong>
                                <p>{notification.message}</p>
                                <small>{formatDateTime(notification.createdAt)}</small>
                            </span>
                            {!notification.read && <i />}
                        </button>
                    ))}
                </div>
            ) : (
                <EmptyState icon="bell" title="All quiet" description="No notifications to show." />
            )}
            {notifications.some((item) => !item.read) && (
                <button className="mark-all-button" type="button" onClick={onMarkAll}>
                    <Icon name="mark-read" size={17} /> Mark all as read
                </button>
            )}
        </section>
    )
}

function AccountForm({
    onSubmit,
}: {
    onSubmit: (request: {
        name: string
        type: AccountType
        currency: Currency
        initialBalance: number
    }) => Promise<void>
}) {
    const [name, setName] = useState('')
    const [type, setType] = useState<AccountType>('CHECKING')
    const [currency, setCurrency] = useState<Currency>('TRY')
    const [initialBalance, setInitialBalance] = useState('0')
    return (
        <SmartForm onSubmit={() => onSubmit({ name: name.trim(), type, currency, initialBalance: Number(initialBalance) })}>
            <FormField label="Account name">
                <input value={name} maxLength={100} required placeholder="Daily account" onChange={(event) => setName(event.target.value)} />
            </FormField>
            <div className="form-grid two-columns">
                <FormField label="Account type">
                    <select value={type} onChange={(event) => setType(event.target.value as AccountType)}>
                        <option value="CHECKING">Checking</option>
                        <option value="SAVINGS">Savings</option>
                        <option value="CASH">Cash</option>
                        <option value="CREDIT_CARD">Credit card</option>
                    </select>
                </FormField>
                <FormField label="Currency">
                    <select value={currency} onChange={(event) => setCurrency(event.target.value as Currency)}>
                        <option value="TRY">TRY</option>
                        <option value="USD">USD</option>
                        <option value="EUR">EUR</option>
                    </select>
                </FormField>
            </div>
            <FormField label="Initial balance">
                <input type="number" min="0" step="0.01" value={initialBalance} required onChange={(event) => setInitialBalance(event.target.value)} />
            </FormField>
            <SubmitButton label="Create account" />
        </SmartForm>
    )
}

function TransactionForm({
    accounts,
    categories,
    onCreateCategory,
    onSubmit,
}: {
    accounts: Account[]
    categories: Category[]
    onCreateCategory: () => void
    onSubmit: (request: {
        accountId: number
        categoryId: number
        type: TransactionType
        amount: number
        description?: string
        transactionDate: string
    }) => Promise<void>
}) {
    const [accountId, setAccountId] = useState(String(accounts[0]?.id ?? ''))
    const [type, setType] = useState<TransactionType>('EXPENSE')
    const [categoryId, setCategoryId] = useState('')
    const [amount, setAmount] = useState('')
    const [description, setDescription] = useState('')
    const [transactionDate, setTransactionDate] = useState(toLocalDateTimeInput())
    const filteredCategories = categories.filter((category) => category.type === type)

    return (
        <SmartForm onSubmit={() => onSubmit({
            accountId: Number(accountId),
            categoryId: Number(categoryId),
            type,
            amount: Number(amount),
            description: description.trim() || undefined,
            transactionDate: toIsoInstant(transactionDate),
        })}>
            {accounts.length === 0 && <FormNotice>No account exists yet. Create an account before recording a transaction.</FormNotice>}
            <div className="form-grid two-columns">
                <FormField label="Account">
                    <select value={accountId} required onChange={(event) => setAccountId(event.target.value)}>
                        <option value="">Select account</option>
                        {accounts.map((account) => <option key={account.id} value={account.id}>{account.name} · {account.currency}</option>)}
                    </select>
                </FormField>
                <FormField label="Type">
                    <select value={type} onChange={(event) => { setType(event.target.value as TransactionType); setCategoryId('') }}>
                        <option value="EXPENSE">Expense</option>
                        <option value="INCOME">Income</option>
                    </select>
                </FormField>
            </div>
            <FormField label="Category" hint={filteredCategories.length === 0 ? 'No matching category exists.' : undefined}>
                <div className="field-with-action">
                    <select value={categoryId} required onChange={(event) => setCategoryId(event.target.value)}>
                        <option value="">Select category</option>
                        {filteredCategories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}
                    </select>
                    <button className="mini-action" type="button" onClick={onCreateCategory}><Icon name="plus" size={16} />New</button>
                </div>
            </FormField>
            <div className="form-grid two-columns">
                <FormField label="Amount">
                    <input type="number" min="0.01" step="0.01" value={amount} required placeholder="0.00" onChange={(event) => setAmount(event.target.value)} />
                </FormField>
                <FormField label="Date and time">
                    <input type="datetime-local" value={transactionDate} required onChange={(event) => setTransactionDate(event.target.value)} />
                </FormField>
            </div>
            <FormField label="Description" optional>
                <input value={description} maxLength={255} placeholder="Coffee, salary, grocery shopping..." onChange={(event) => setDescription(event.target.value)} />
            </FormField>
            <SubmitButton label="Save transaction" disabled={!accounts.length} />
        </SmartForm>
    )
}

function CategoryForm({ onSubmit }: { onSubmit: (request: { name: string; type: TransactionType }) => Promise<void> }) {
    const [name, setName] = useState('')
    const [type, setType] = useState<TransactionType>('EXPENSE')
    return (
        <SmartForm onSubmit={() => onSubmit({ name: name.trim(), type })}>
            <FormField label="Category name">
                <input value={name} required maxLength={100} placeholder="Groceries" onChange={(event) => setName(event.target.value)} />
            </FormField>
            <FormField label="Category type">
                <select value={type} onChange={(event) => setType(event.target.value as TransactionType)}>
                    <option value="EXPENSE">Expense</option>
                    <option value="INCOME">Income</option>
                </select>
            </FormField>
            <SubmitButton label="Create category" />
        </SmartForm>
    )
}

function BudgetForm({
    categories,
    today,
    onSubmit,
}: {
    categories: Category[]
    today: Date
    onSubmit: (request: { categoryId: number; limitAmount: number; year: number; month: number }) => Promise<void>
}) {
    const expenseCategories = categories.filter((category) => category.type === 'EXPENSE')
    const [categoryId, setCategoryId] = useState('')
    const [limitAmount, setLimitAmount] = useState('')
    const [month, setMonth] = useState(today.getMonth() + 1)
    const [year, setYear] = useState(today.getFullYear())
    return (
        <SmartForm onSubmit={() => onSubmit({ categoryId: Number(categoryId), limitAmount: Number(limitAmount), year, month })}>
            <FormField label="Expense category">
                <select value={categoryId} required onChange={(event) => setCategoryId(event.target.value)}>
                    <option value="">Select category</option>
                    {expenseCategories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}
                </select>
            </FormField>
            <FormField label="Monthly limit">
                <input type="number" min="0.01" step="0.01" value={limitAmount} required placeholder="5000" onChange={(event) => setLimitAmount(event.target.value)} />
            </FormField>
            <div className="form-grid two-columns">
                <FormField label="Month">
                    <select value={month} onChange={(event) => setMonth(Number(event.target.value))}>
                        {Array.from({ length: 12 }, (_, index) => index + 1).map((value) => <option key={value} value={value}>{monthName(value)}</option>)}
                    </select>
                </FormField>
                <FormField label="Year">
                    <input type="number" min="2000" value={year} required onChange={(event) => setYear(Number(event.target.value))} />
                </FormField>
            </div>
            <SubmitButton label="Create budget" disabled={!expenseCategories.length} />
        </SmartForm>
    )
}

function TransferForm({
    accounts,
    onSubmit,
}: {
    accounts: Account[]
    onSubmit: (request: { fromAccountId: number; toAccountId: number; amount: number; description?: string; transferredAt?: string }) => Promise<void>
}) {
    const [fromAccountId, setFromAccountId] = useState(String(accounts[0]?.id ?? ''))
    const fromAccount = accounts.find((account) => account.id === Number(fromAccountId))
    const compatibleAccounts = accounts.filter((account) => account.id !== Number(fromAccountId) && account.currency === fromAccount?.currency)
    const [toAccountId, setToAccountId] = useState('')
    const [amount, setAmount] = useState('')
    const [description, setDescription] = useState('')
    const [transferredAt, setTransferredAt] = useState(toLocalDateTimeInput())

    return (
        <SmartForm onSubmit={() => onSubmit({
            fromAccountId: Number(fromAccountId),
            toAccountId: Number(toAccountId),
            amount: Number(amount),
            description: description.trim() || undefined,
            transferredAt: toIsoInstant(transferredAt),
        })}>
            <FormField label="From account">
                <select value={fromAccountId} required onChange={(event) => { setFromAccountId(event.target.value); setToAccountId('') }}>
                    <option value="">Select account</option>
                    {accounts.map((account) => <option key={account.id} value={account.id}>{account.name} · {formatMoney(account.balance, account.currency)}</option>)}
                </select>
            </FormField>
            <FormField label="To account" hint={fromAccount && !compatibleAccounts.length ? `No other ${fromAccount.currency} account is available.` : undefined}>
                <select value={toAccountId} required onChange={(event) => setToAccountId(event.target.value)}>
                    <option value="">Select account</option>
                    {compatibleAccounts.map((account) => <option key={account.id} value={account.id}>{account.name} · {account.currency}</option>)}
                </select>
            </FormField>
            <div className="form-grid two-columns">
                <FormField label="Amount">
                    <input type="number" min="0.01" step="0.01" value={amount} required placeholder="0.00" onChange={(event) => setAmount(event.target.value)} />
                </FormField>
                <FormField label="Transfer date">
                    <input type="datetime-local" value={transferredAt} required onChange={(event) => setTransferredAt(event.target.value)} />
                </FormField>
            </div>
            <FormField label="Description" optional>
                <input value={description} maxLength={255} placeholder="Move to savings" onChange={(event) => setDescription(event.target.value)} />
            </FormField>
            <SubmitButton label="Transfer money" disabled={!compatibleAccounts.length} />
        </SmartForm>
    )
}

function RecurringForm({
    accounts,
    categories,
    onSubmit,
}: {
    accounts: Account[]
    categories: Category[]
    onSubmit: (request: {
        accountId: number
        categoryId: number
        type: TransactionType
        amount: number
        description?: string
        frequency: RecurrenceFrequency
        startDate: string
        endDate?: string
    }) => Promise<void>
}) {
    const [accountId, setAccountId] = useState(String(accounts[0]?.id ?? ''))
    const [type, setType] = useState<TransactionType>('EXPENSE')
    const [categoryId, setCategoryId] = useState('')
    const [amount, setAmount] = useState('')
    const [description, setDescription] = useState('')
    const [frequency, setFrequency] = useState<RecurrenceFrequency>('MONTHLY')
    const [startDate, setStartDate] = useState(new Date().toISOString().slice(0, 10))
    const [endDate, setEndDate] = useState('')
    const filteredCategories = categories.filter((category) => category.type === type)

    return (
        <SmartForm onSubmit={() => onSubmit({
            accountId: Number(accountId),
            categoryId: Number(categoryId),
            type,
            amount: Number(amount),
            description: description.trim() || undefined,
            frequency,
            startDate,
            endDate: endDate || undefined,
        })}>
            <div className="form-grid two-columns">
                <FormField label="Account">
                    <select value={accountId} required onChange={(event) => setAccountId(event.target.value)}>
                        <option value="">Select account</option>
                        {accounts.map((account) => <option key={account.id} value={account.id}>{account.name}</option>)}
                    </select>
                </FormField>
                <FormField label="Type">
                    <select value={type} onChange={(event) => { setType(event.target.value as TransactionType); setCategoryId('') }}>
                        <option value="EXPENSE">Expense</option>
                        <option value="INCOME">Income</option>
                    </select>
                </FormField>
            </div>
            <FormField label="Category">
                <select value={categoryId} required onChange={(event) => setCategoryId(event.target.value)}>
                    <option value="">Select category</option>
                    {filteredCategories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}
                </select>
            </FormField>
            <div className="form-grid two-columns">
                <FormField label="Amount">
                    <input type="number" min="0.01" step="0.01" value={amount} required onChange={(event) => setAmount(event.target.value)} />
                </FormField>
                <FormField label="Frequency">
                    <select value={frequency} onChange={(event) => setFrequency(event.target.value as RecurrenceFrequency)}>
                        <option value="WEEKLY">Weekly</option>
                        <option value="MONTHLY">Monthly</option>
                    </select>
                </FormField>
            </div>
            <div className="form-grid two-columns">
                <FormField label="Start date">
                    <input type="date" min={new Date().toISOString().slice(0, 10)} value={startDate} required onChange={(event) => setStartDate(event.target.value)} />
                </FormField>
                <FormField label="End date" optional>
                    <input type="date" min={startDate} value={endDate} onChange={(event) => setEndDate(event.target.value)} />
                </FormField>
            </div>
            <FormField label="Description" optional>
                <input value={description} maxLength={255} placeholder="Rent, salary, subscription..." onChange={(event) => setDescription(event.target.value)} />
            </FormField>
            <SubmitButton label="Create schedule" disabled={!accounts.length || !filteredCategories.length} />
        </SmartForm>
    )
}

function SmartForm({
    children,
    onSubmit,
}: {
    children: React.ReactNode
    onSubmit: () => Promise<void>
}) {
    const [isSubmitting, setIsSubmitting] = useState(false)
    const [error, setError] = useState('')

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        setError('')
        setIsSubmitting(true)
        try {
            await onSubmit()
        } catch (submitError) {
            setError(submitError instanceof Error ? submitError.message : 'The request could not be completed.')
        } finally {
            setIsSubmitting(false)
        }
    }

    return (
        <form className="smart-form" onSubmit={handleSubmit}>
            <fieldset disabled={isSubmitting}>{children}</fieldset>
            {error && <p className="form-error">{error}</p>}
            {isSubmitting && <div className="form-loading"><span />Saving...</div>}
        </form>
    )
}

function FormField({
    label,
    hint,
    optional = false,
    children,
}: {
    label: string
    hint?: string
    optional?: boolean
    children: React.ReactNode
}) {
    return (
        <label className="form-field">
            <span>{label}{optional && <small>Optional</small>}</span>
            {children}
            {hint && <em>{hint}</em>}
        </label>
    )
}

function SubmitButton({ label, disabled = false }: { label: string; disabled?: boolean }) {
    return <button className="button primary form-submit" type="submit" disabled={disabled}>{label}<Icon name="arrow-right" size={17} /></button>
}

function FormNotice({ children }: { children: React.ReactNode }) {
    return <p className="form-notice">{children}</p>
}

function DashboardSkeleton() {
    return (
        <div className="dashboard-skeleton">
            <aside />
            <main>
                <header />
                <div className="skeleton-content">
                    <span className="skeleton-line large" />
                    <span className="skeleton-line" />
                    <div className="skeleton-grid">
                        {Array.from({ length: 4 }, (_, index) => <article key={index} />)}
                    </div>
                    <div className="skeleton-panels"><article /><article /></div>
                </div>
            </main>
        </div>
    )
}

export default DashboardPage
