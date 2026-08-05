import heroImg from '../assets/hero.png'
import '../App.css'
import './HomePage.css'
import { Link } from 'react-router'


function HomePage() {
    return (        <>
        <header className="navbar">
            <a href="/" className="logo">
                SmartWallet
            </a>

            <nav className="nav-actions">
                <Link to="/login" className="login-button">
                    Login
                </Link>

                <Link to="/register" className="register-button">
                    Register
                </Link>
            </nav>
        </header>
        <section id="center">
            <div className="hero">
                <div className="hero-content">
                    <h1>SmartWallet</h1>
                    <p>Manage your money smarter.</p>
                </div>

                <div className="hero-image">
                    <img src={heroImg} alt="SmartWallet dashboard" />
                </div>
            </div>
        </section>
    </>
    )
}

export default HomePage;