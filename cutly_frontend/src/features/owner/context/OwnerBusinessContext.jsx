import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { useOwnerBusinesses } from '../hooks/useOwnerBusinesses.js'
import { useOwnerSubscription } from '../hooks/useOwnerSubscription.js'
import useAuth from '../../../auth/useAuth.js'

const OwnerBusinessContext = createContext(null)

export function OwnerBusinessProvider({ children }) {
  const { user } = useAuth()
  const businessesQuery = useOwnerBusinesses()
  const subscriptionQuery = useOwnerSubscription()
  const businesses = Array.isArray(businessesQuery.data) ? businessesQuery.data : []
  const subscription = subscriptionQuery.data
  const [trialActivationPending, setTrialActivationPending] = useState(false)
  const [trialActivationError, setTrialActivationError] = useState('')
  const accessReady = businessesQuery.isSuccess && subscriptionQuery.isSuccess
  const isOnboarding = accessReady
    && subscription?.suscripcionId === null
    && subscription?.trialUtilizado === false
    && subscription?.cantidadNegocios === 0
    && businesses.length === 0
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

  const confirmTrialActivation = useCallback(async () => {
    setTrialActivationPending(true)
    setTrialActivationError('')
    try {
      const [businessesResult, subscriptionResult] = await Promise.all([
        businessesQuery.refetch({ throwOnError: true }),
        subscriptionQuery.refetch({ throwOnError: true }),
      ])
      const updatedBusinesses = businessesResult.data
      const updatedSubscription = subscriptionResult.data
      const trialConfirmed = Array.isArray(updatedBusinesses)
        && updatedBusinesses.length > 0
        && updatedSubscription?.suscripcionId != null
        && updatedSubscription?.status === 'TRIAL'
        && updatedSubscription?.trialUtilizado === true
        && updatedSubscription?.cantidadNegocios > 0
      if (!trialConfirmed) {
        setTrialActivationError('El negocio se creó, pero todavía no pudimos confirmar la activación de la prueba gratuita. Volvé a intentar la verificación.')
        return false
      }
      setTrialActivationPending(false)
      return true
    } catch {
      setTrialActivationError('El negocio se creó, pero no pudimos confirmar la activación de la prueba gratuita. Revisá la conexión e intentá verificar de nuevo.')
      return false
    }
  }, [businessesQuery.refetch, subscriptionQuery.refetch])

  const retryAccessLoad = useCallback(async () => {
    await Promise.allSettled([businessesQuery.refetch(), subscriptionQuery.refetch()])
  }, [businessesQuery.refetch, subscriptionQuery.refetch])

  const value = useMemo(() => ({
    businesses,
    businessesQuery,
    subscription,
    subscriptionQuery,
    accessReady,
    accessError: businessesQuery.isError || subscriptionQuery.isError,
    isOnboarding,
    trialActivationPending,
    trialActivationError,
    confirmTrialActivation,
    retryAccessLoad,
    activeBusinessId: resolvedBusinessId,
    setActiveBusinessId,
    activeBusiness,
  }), [businesses, businessesQuery, subscription, subscriptionQuery, accessReady, isOnboarding, trialActivationPending, trialActivationError, confirmTrialActivation, retryAccessLoad, activeBusiness, resolvedBusinessId])

  return <OwnerBusinessContext.Provider value={value}>{children}</OwnerBusinessContext.Provider>
}

export function useActiveOwnerBusiness() {
  const context = useContext(OwnerBusinessContext)
  if (!context) throw new Error('useActiveOwnerBusiness debe usarse dentro de OwnerBusinessProvider')
  return context
}
