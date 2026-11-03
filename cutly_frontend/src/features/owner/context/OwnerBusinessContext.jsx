import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import { useOwnerBusinesses } from '../hooks/useOwnerBusinesses.js'
import useAuth from '../../../auth/useAuth.js'

const OwnerBusinessContext = createContext(null)

export function OwnerBusinessProvider({ children }) {
  const { user } = useAuth()
  const businessesQuery = useOwnerBusinesses()
  const businesses = Array.isArray(businessesQuery.data) ? businessesQuery.data : []
  const storageKey = `cutly:owner:${user?.userId}:activeBusiness`
  const [activeBusinessId, setActiveBusinessId] = useState(() => {
    try { return localStorage.getItem(storageKey) || '' } catch { return '' }
  })
  const activeBusiness = businesses.find((business) => String(business.negocioId) === activeBusinessId)
    || businesses[0]
    || null
  const resolvedBusinessId = activeBusiness ? String(activeBusiness.negocioId) : ''

  useEffect(() => {
    if (!businessesQuery.isSuccess) return
    if (resolvedBusinessId !== activeBusinessId) setActiveBusinessId(resolvedBusinessId)
  }, [activeBusinessId, businessesQuery.isSuccess, resolvedBusinessId])

  useEffect(() => {
    if (!resolvedBusinessId || !businessesQuery.isSuccess) return
    try { localStorage.setItem(storageKey, resolvedBusinessId) } catch { /* Storage may be unavailable. */ }
  }, [businessesQuery.isSuccess, resolvedBusinessId, storageKey])

  const value = useMemo(() => ({
    businesses,
    businessesQuery,
    activeBusinessId: resolvedBusinessId,
    setActiveBusinessId,
    activeBusiness,
  }), [businesses, businessesQuery, activeBusiness, resolvedBusinessId])

  return <OwnerBusinessContext.Provider value={value}>{children}</OwnerBusinessContext.Provider>
}

export function useActiveOwnerBusiness() {
  const context = useContext(OwnerBusinessContext)
  if (!context) throw new Error('useActiveOwnerBusiness debe usarse dentro de OwnerBusinessProvider')
  return context
}
