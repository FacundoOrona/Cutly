import { useState } from 'react'
import { ArrowRight, Building2, CalendarDays, CheckCircle2, LoaderCircle, RefreshCw, Sparkles } from 'lucide-react'
import { useActiveOwnerBusiness } from '../context/OwnerBusinessContext.jsx'
import CreateOwnerBusinessForm from '../components/CreateOwnerBusinessForm.jsx'
import './OwnerOnboardingPage.css'

export default function OwnerOnboardingPage() {
  const [isCreateOpen, setIsCreateOpen] = useState(false)
  const { trialActivationPending, trialActivationError, confirmTrialActivation } = useActiveOwnerBusiness()

  return (
    <section className="owner-onboarding" aria-labelledby="owner-onboarding-title">
      <div className="owner-onboarding-card">
        <span className="owner-onboarding-icon"><Building2 size={25} /></span>
        <span className="owner-onboarding-eyebrow"><Sparkles size={13} /> TU ESPACIO EN CUTLY</span>
        {trialActivationPending ? (
          <div className="owner-onboarding-confirmation" role={trialActivationError ? 'alert' : 'status'}>
            {trialActivationError ? <span className="owner-onboarding-icon is-error"><RefreshCw size={20} /></span> : <LoaderCircle size={24} className="spin" />}
            <h1 id="owner-onboarding-title">{trialActivationError ? 'Verifiquemos la activación' : 'Estamos preparando tu espacio'}</h1>
            <p>{trialActivationError || 'Tu negocio ya se creó. Estamos consultando al servidor para confirmar tu prueba gratuita antes de habilitar el panel.'}</p>
            {trialActivationError && <button type="button" className="owner-onboarding-primary" onClick={confirmTrialActivation}><RefreshCw size={16} /> Verificar nuevamente</button>}
          </div>
        ) : (
          <>
            <h1 id="owner-onboarding-title">¡Bienvenido a tu espacio!</h1>
            <p className="owner-onboarding-lead">Creá tu primer negocio para empezar a organizar y hacer crecer tu actividad desde Cutly.</p>
            <div className="owner-onboarding-trial">
              <span className="owner-onboarding-trial-icon"><CalendarDays size={19} /></span>
              <div><strong>14 días gratis</strong><p>Tu prueba se activa automáticamente al crear el negocio. El período comienza en ese momento, no al registrarte ni al iniciar sesión.</p></div>
              <CheckCircle2 size={18} className="owner-onboarding-check" />
            </div>
            {!isCreateOpen ? (
              <button type="button" className="owner-onboarding-primary" onClick={() => setIsCreateOpen(true)}>Crear mi primer negocio <ArrowRight size={17} /></button>
            ) : <CreateOwnerBusinessForm onCancel={() => setIsCreateOpen(false)} onCreated={confirmTrialActivation} />}
            {!isCreateOpen && <p className="owner-onboarding-footnote">Podés completar los datos básicos ahora y configurar el resto después.</p>}
          </>
        )}
      </div>
    </section>
  )
}
