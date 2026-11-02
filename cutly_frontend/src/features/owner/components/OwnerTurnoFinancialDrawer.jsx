import { useEffect, useRef, useState } from 'react'
import { Banknote, CalendarDays, Check, CircleAlert, CircleCheck, CreditCard, Gift, LoaderCircle, RotateCcw, X } from 'lucide-react'

const paymentMethods = { EFECTIVO: 'Efectivo', TRANSFERENCIA_QR: 'Transferencia / QR', DEBITO: 'Débito', CREDITO: 'Crédito', OTRO: 'Otro' }
const decisions = [
  { value: 'COBRO_DIRECTO', label: 'Cobro directo', detail: 'Registrar un pago ahora', Icon: Banknote },
  { value: 'CUENTA_CORRIENTE', label: 'Cuenta corriente', detail: 'Generar una deuda del cliente', Icon: CreditCard },
  { value: 'GRATUIDAD', label: 'Gratuidad', detail: 'Completar sin cargo', Icon: Gift },
]
const money = (value) => new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' }).format(Number(value || 0))

export default function OwnerTurnoFinancialDrawer({ turno, error, isPending, successDecision, onCancel, onSubmit }) {
  const [decision, setDecision] = useState('COBRO_DIRECTO')
  const [amount, setAmount] = useState(String(turno.precio ?? ''))
  const [method, setMethod] = useState('EFECTIVO')
  const [other, setOther] = useState('')
  const [reason, setReason] = useState('')
  const [validation, setValidation] = useState('')
  const closeRef = useRef(null)
  const client = [turno.clienteNombre, turno.clienteApellido].filter(Boolean).join(' ') || 'Cliente'
  useEffect(() => { closeRef.current?.focus() }, [])
  function submit(event) {
    event.preventDefault()
    if (isPending || successDecision) return
    let body
    if (decision === 'GRATUIDAD') {
      if (reason.trim().length < 3 || reason.trim().length > 250) return setValidation('El motivo debe tener entre 3 y 250 caracteres.')
      body = { decisionFinanciera: decision, motivoGratuidad: reason.trim() }
    } else {
      const parsed = Number(amount)
      if (!Number.isFinite(parsed) || parsed <= 0) return setValidation('Ingresá un importe mayor que cero.')
      if (decision === 'COBRO_DIRECTO' && method === 'OTRO' && !other.trim()) return setValidation('Describí el otro método de pago.')
      body = { decisionFinanciera: decision, monto: parsed }
      if (decision === 'COBRO_DIRECTO') {
        body.metodoPago = method
        if (method === 'OTRO') body.descripcionOtro = other.trim().slice(0, 100)
      }
    }
    setValidation('')
    onSubmit(body)
  }
  const completed = Boolean(successDecision)
  const completedDecision = decisions.find(({ value }) => value === successDecision)
  return <div className="owner-turno-drawer-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget && !isPending) onCancel() }}>
    <aside className="owner-turno-drawer" role="dialog" aria-modal="true" aria-labelledby="owner-financial-title">
      <header className="owner-turno-drawer-header"><div><span className="eyebrow"><span className="eyebrow-dot" /> CIERRE FINANCIERO</span><h2 id="owner-financial-title">Completar turno</h2></div><button ref={closeRef} type="button" className="icon-button" onClick={onCancel} disabled={isPending} aria-label="Cerrar formulario"><X size={19} /></button></header>
      <form className={`owner-turno-drawer-content owner-financial-form${completed ? ' is-complete' : ''}`} onSubmit={submit}>
        <div className="owner-financial-context"><div className="owner-financial-context-icon"><CalendarDays size={19} /></div><div className="owner-financial-context-main"><span className="owner-financial-overline">TURNO DE {client}</span><strong>{turno.servicioNombre || 'Turno'}</strong><span>{turno.fechaHoraInicio ? new Intl.DateTimeFormat('es-AR', { dateStyle: 'full', timeStyle: 'short' }).format(new Date(turno.fechaHoraInicio)) : 'Fecha y hora no disponibles'}</span></div><div className="owner-financial-original-price"><small>PRECIO ORIGINAL</small><strong>{money(turno.precio)}</strong></div><span className={`owner-financial-status${completed ? ' is-complete' : ''}`}>{completed ? <><Check size={12} /> COMPLETADO · REGULARIZADO</> : turno.status || 'PENDIENTE'}</span></div>
        {completed ? <section className="owner-financial-success" role="status" aria-live="polite"><span className="owner-financial-success-icon"><CircleCheck size={31} /></span><span className="owner-financial-overline">OPERACIÓN CONFIRMADA</span><h3>{successDecision === 'COBRO_DIRECTO' ? '¡Turno cobrado con éxito!' : '¡Turno completado!'}</h3><p>{successDecision === 'COBRO_DIRECTO' ? 'El turno fue completado y el cobro se registró correctamente.' : successDecision === 'CUENTA_CORRIENTE' ? 'Se generó correctamente la deuda del cliente.' : 'El turno fue registrado como gratuito.'}</p><div className="owner-financial-success-summary"><Check size={15} />Turno completado y regularizado</div></section> : <>
          <section className="owner-financial-decision-section"><div className="owner-financial-section-heading"><span className="owner-financial-step">01</span><div><h3>¿Cómo querés cerrar el turno?</h3><p>Elegí la opción financiera que corresponda.</p></div></div><div className="owner-financial-options">{decisions.map(({ value, label, detail, Icon }) => <label key={value} className={`owner-financial-choice${decision === value ? ' is-selected' : ''}`}><input type="radio" name="decision" value={value} checked={decision === value} onChange={() => setDecision(value)} disabled={isPending} /><span className="owner-financial-choice-icon"><Icon size={18} /></span><span className="owner-financial-choice-copy"><strong>{label}</strong><small>{detail}</small></span><span className="owner-financial-choice-indicator">{decision === value && <Check size={13} />}</span></label>)}</div></section>
          <section className="owner-financial-fields"><div className="owner-financial-section-heading"><span className="owner-financial-step">02</span><div><h3>Detalles financieros</h3><p>Completá la información para registrar el cierre.</p></div></div>{decision !== 'GRATUIDAD' ? <div className="owner-financial-field-grid"><label className="owner-form-field"><span>Importe a registrar <b>*</b></span><div className="owner-financial-money-input"><span>$</span><input type="number" min="0.01" step="0.01" value={amount} onChange={(event) => setAmount(event.target.value)} disabled={isPending} aria-invalid={Boolean(validation && validation.includes('importe'))} /></div><small>El precio original del turno no se modifica.</small></label>{decision === 'COBRO_DIRECTO' && <label className="owner-form-field"><span>Método de pago <b>*</b></span><select value={method} onChange={(event) => setMethod(event.target.value)} disabled={isPending}>{Object.entries(paymentMethods).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>}{decision === 'COBRO_DIRECTO' && method === 'OTRO' && <label className="owner-form-field owner-financial-wide"><span>Descripción del método <b>*</b></span><input maxLength={100} value={other} onChange={(event) => setOther(event.target.value)} disabled={isPending} /></label>}</div> : <label className="owner-form-field"><span>Motivo de gratuidad <b>*</b></span><textarea minLength={3} maxLength={250} value={reason} onChange={(event) => setReason(event.target.value)} disabled={isPending} placeholder="Contanos por qué se registra como gratuito…" /><small>{reason.length}/250 caracteres</small></label>}</section>
          {validation && <div className="owner-financial-message is-error" role="alert"><CircleAlert size={17} /><span>{validation}</span></div>}{error && <div className="owner-financial-message is-error" role="alert"><CircleAlert size={17} /><span>{error}{!error.includes('sesión') && <button type="button" className="owner-financial-retry" onClick={() => onSubmit(null)}><RotateCcw size={13} /> Reintentar misma operación</button>}</span></div>}
          <footer className="owner-financial-actions"><button className="owners-secondary-button" type="button" onClick={onCancel} disabled={isPending}>Cancelar</button><button className="owner-create-button owner-financial-submit" type="submit" disabled={isPending}>{isPending ? <><LoaderCircle size={16} className="spin" /> Procesando cierre…</> : <><Check size={16} /> Confirmar y completar</>}</button></footer>
        </>}
        {completed && <footer className="owner-financial-actions owner-financial-actions-success"><button className="owner-create-button owner-financial-submit" type="button" onClick={onCancel}><Check size={16} /> Cerrar y volver a la agenda</button></footer>}
      </form>
    </aside>
  </div>
}
