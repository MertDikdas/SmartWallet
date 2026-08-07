import { useEffect, type ReactNode } from 'react'
import Icon from './Icon'

interface ModalProps {
    title: string
    description?: string
    children: ReactNode
    onClose: () => void
}

function Modal({ title, description, children, onClose }: ModalProps) {
    useEffect(() => {
        function closeOnEscape(event: KeyboardEvent) {
            if (event.key === 'Escape') {
                onClose()
            }
        }

        document.body.classList.add('modal-open')
        window.addEventListener('keydown', closeOnEscape)

        return () => {
            document.body.classList.remove('modal-open')
            window.removeEventListener('keydown', closeOnEscape)
        }
    }, [onClose])

    return (
        <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
            <section
                className="modal-card"
                role="dialog"
                aria-modal="true"
                aria-labelledby="modal-title"
                onMouseDown={(event) => event.stopPropagation()}
            >
                <header className="modal-header">
                    <div>
                        <h2 id="modal-title">{title}</h2>
                        {description && <p>{description}</p>}
                    </div>
                    <button
                        className="icon-button"
                        type="button"
                        aria-label="Close dialog"
                        onClick={onClose}
                    >
                        <Icon name="close" />
                    </button>
                </header>
                <div className="modal-body">{children}</div>
            </section>
        </div>
    )
}

export default Modal
