import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import AppProviders from './app/AppProviders.jsx'
import AppRoutes from './routes/AppRoutes.jsx'
import './styles/global.css'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <AppProviders>
      <AppRoutes />
    </AppProviders>
  </StrictMode>,
)
