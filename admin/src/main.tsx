import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AuthProvider } from './auth/AuthContext'
import { AvisosProvider } from './componentes/Avisos'
import { ApiError } from './api/client'
import { App } from './App'
import './styles.css'

const cliente = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      refetchOnWindowFocus: true,
      retry: (falhas, erro) => !(erro instanceof ApiError && erro.status >= 400 && erro.status < 500) && falhas < 2,
    },
  },
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={cliente}>
      <BrowserRouter>
        <AuthProvider>
          <AvisosProvider>
            <App />
          </AvisosProvider>
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  </StrictMode>,
)
