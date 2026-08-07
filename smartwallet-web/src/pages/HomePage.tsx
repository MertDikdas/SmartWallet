import { Link } from 'react-router'
import heroImg from '../assets/hero.png'
import Icon from '../components/Icon'
import './HomePage.css'

function HomePage() {
    return (
        <main className="landing-page">
            <header className="landing-nav">
                <Link to="/" className="landing-brand" aria-label="SmartWallet home">
                    <span><Icon name="wallet" size={21} /></span>
                    SmartWallet
                </Link>

                <nav className="landing-links" aria-label="Primary navigation">
                    <a href="#features">Features</a>
                    <a href="#security">Security</a>
                </nav>

                <div className="landing-actions">
                    <Link to="/login" className="landing-login">Sign in</Link>
                    <Link to="/register" className="landing-register">
                        Get started <Icon name="arrow-right" size={16} />
                    </Link>
                </div>
            </header>

            <section className="landing-hero">
                <div className="hero-copy">
                    <span className="hero-badge">
                        <Icon name="sparkle" size={15} />
                        One clear view of your money
                    </span>
                    <h1>
                        Make every lira
                        <span>work smarter.</span>
                    </h1>
                    <p>
                        Track accounts, plan budgets, automate recurring payments,
                        and understand your spending without spreadsheet chaos.
                    </p>
                    <div className="hero-actions">
                        <Link to="/register" className="hero-primary">
                            Create your wallet <Icon name="arrow-right" size={17} />
                        </Link>
                        <Link to="/login" className="hero-secondary">Open dashboard</Link>
                    </div>
                    <div className="hero-trust">
                        <span><Icon name="shield" size={16} />JWT-secured sessions</span>
                        <span><Icon name="check" size={16} />Real-time balances</span>
                    </div>
                </div>

                <div className="hero-visual" aria-label="SmartWallet dashboard preview">
                    <div className="hero-glow" />
                    <div className="preview-card">
                        <header>
                            <div>
                                <span>SmartWallet</span>
                                <strong>Financial overview</strong>
                            </div>
                            <i />
                        </header>
                        <div className="preview-balance">
                            <span>Total balance</span>
                            <strong>₺48.760,00</strong>
                            <small>+12.4% this month</small>
                        </div>
                        <div className="preview-chart">
                            {[35, 55, 44, 72, 62, 88].map((height, index) => (
                                <span key={index} style={{ height: `${height}%` }} />
                            ))}
                        </div>
                        <div className="preview-row">
                            <span><Icon name="transaction" size={17} /></span>
                            <div><strong>Latest activity</strong><small>6 transactions today</small></div>
                            <b>₺1.240</b>
                        </div>
                    </div>
                    <img src={heroImg} alt="Wallet illustration" />
                </div>
            </section>

            <section className="feature-strip" id="features">
                <article>
                    <span><Icon name="wallet" /></span>
                    <div><strong>All accounts</strong><p>Bank, cash, savings, and cards together.</p></div>
                </article>
                <article>
                    <span><Icon name="budget" /></span>
                    <div><strong>Smart budgets</strong><p>Monthly limits with automatic spend tracking.</p></div>
                </article>
                <article>
                    <span><Icon name="chart" /></span>
                    <div><strong>Useful analytics</strong><p>See cash flow and category trends clearly.</p></div>
                </article>
                <article id="security">
                    <span><Icon name="shield" /></span>
                    <div><strong>Secure by design</strong><p>Short-lived access tokens and refresh rotation.</p></div>
                </article>
            </section>
        </main>
    )
}

export default HomePage
