import { useRef, useState } from 'react'
import { CheckCircle2, CircleAlert, LoaderCircle, X } from 'lucide-react'
import { getOwnerLiquidacionErrorMessage } from '../api/ownerRemuneracionesApi.js'
import { useCreateOwnerLiquidacion } from '../hooks/useOwnerRemuneraciones.js'

const methods = [['EFECTIVO', 'Efectivo'], ['TRANSFERENCIA_QR', 'Transferencia / QR'], ['DEBITO', 'Débito'], ['CREDITO', 'Crédito'], ['OTRO', 'Otro']]

function localToday() {
  const date = new Date()
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

function money(value) {
  if (value == null || Number.isNaN(Number(value))) return '—'
  return new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' }).format(Number(value))
}

function toCents(value) {
  if (typeof value !== 'string' || !/^\d+(?:\.\d{1,2})?$/.test(value.trim())) return null
  const [whole, fraction = ''] = value.trim().split('.')
  return Number(whole) * 100 + Number(fraction.padEnd(2, '0'))
}

function backendAmountToCents(value) {
  const match = String(value ?? '').match(/^(\d+)(?:\.(\d+))?$/)
  if (!match) return null
  const fraction = match[2] || ''
  if (fraction.length > 2 && /[^0]/.test(fraction.slice(2))) return null
  return Number(match[1]) * 100 + Number(fraction.slice(0, 2).padEnd(2, '0'))
}

function centsToMoney(cents) {
  return (cents / 100).toFixed(2)
}

function createIdempotencyKey() {
  if (globalThis.crypto?.randomUUID) return globalThis.crypto.randomUUID()
  const bytes = new Uint8Array(16)
  globalThis.crypto.getRandomValues(bytes)
  return Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0')).join('')
}

export default function OwnerLiquidacionForm({ negocioId, empleadoNombre, remuneraciones, onCancel, onSuccess }) {
  const [fechaEfectiva, setFechaEfectiva] = useState(localToday)
  const [metodoPago, setMetodoPago] = useState('')
  const [descripcionMetodo, setDescripcionMetodo] = useState('')
  const [observaciones, setObservaciones] = useState('')
  const [importes, setImportes] = useState(() => Object.fromEntries(remuneraciones.map((item) => [String(item.remuneracionId), centsToMoney(backendAmountToCents(item.saldoPendiente) ?? 0)])))
  const [error, setError] = useState('')
  const mutation = useCreateOwnerLiquidacion()
  const operationRef = useRef(null)

  const applications = remuneraciones.map((item) => {
    const amountCents = toCents(importes[String(item.remuneracionId)] ?? '')
    const balanceCents = backendAmountToCents(item.saldoPendiente)
    const valid = amountCents !== null && amountCents > 0 && balanceCents !== null && amountCents <= balanceCents
    return { item, amountCents, balanceCents, valid, remainingCents: valid ? balanceCents - amountCents : null }
  })
  const totalCents = applications.reduce((total, entry) => total + (entry.valid ? entry.amountCents : 0), 0)
  const invalidAmount = applications.some((entry) => !entry.valid)
  const validationMessages = [
    !fechaEfectiva && 'Ingresá la fecha efectiva.',
    !metodoPago && 'Seleccioná un método de pago.',
    metodoPago === 'OTRO' && !descripcionMetodo.trim() && 'Describí el medio de pago OTRO.',
    descripcionMetodo.length > 100 && 'La descripción admite hasta 100 caracteres.',
    observaciones.length > 500 && 'Las observaciones admiten hasta 500 caracteres.',
    invalidAmount && 'Cada importe debe ser positivo, tener hasta dos decimales y no superar el saldo disponible.',
    totalCents <= 0 && 'La liquidación debe incluir al menos una aplicación válida.',
  ].filter(Boolean)
  const formValid = Boolean(fechaEfectiva && metodoPago && (metodoPago !== 'OTRO' || descripcionMetodo.trim())
    && descripcionMetodo.length <= 100 && observaciones.length <= 500 && applications.length > 0 && !invalidAmount)

  function changeImporte(id, value) {
    setImportes((current) => ({ ...current, [id]: value }))
    setError('')
  }

  async function handleSubmit(event) {
    event.preventDefault()
    if (!formValid || mutation.isPending) {
      setError('Revisá los campos y los importes aplicados antes de continuar.')
      return
    }
    const body = {
      fechaEfectiva,
      metodoPago,
      descripcionMetodo: metodoPago === 'OTRO' ? descripcionMetodo.trim() : null,
      observaciones: observaciones || null,
      aplicaciones: Object.fromEntries(applications.map(({ item, amountCents }) => [String(item.remuneracionId), amountCents / 100])),
    }
    const fingerprint = JSON.stringify({ ...body, aplicaciones: Object.entries(body.aplicaciones).sort(([a], [b]) => Number(a) - Number(b)) })
    if (!window.confirm(`¿Confirmás registrar la liquidación de ${money(totalCents / 100)} para ${empleadoNombre}?`)) return
    if (!operationRef.current || operationRef.current.fingerprint !== fingerprint) {
      operationRef.current = { fingerprint, key: createIdempotencyKey() }
    }
    setError('')
    try {
      await mutation.mutateAsync({ negocioId, body, idempotencyKey: operationRef.current.key })
      operationRef.current = null
      onSuccess()
    } catch (requestError) {
      setError(getOwnerLiquidacionErrorMessage(requestError))
    }
  }

  return <div className="owner-remuneracion-overlay" role="presentation">
    <section className="owner-remuneracion-drawer owner-liquidacion-drawer" role="dialog" aria-modal="true" aria-labelledby="owner-liquidacion-title">
      <header className="owner-remuneracion-drawer-header"><div><span className="owner-remuneracion-overline">NUEVA OPERACIÓN</span><h2 id="owner-liquidacion-title">Registrar liquidación</h2><p>{empleadoNombre}</p></div><button type="button" className="icon-button" onClick={onCancel} aria-label="Cerrar formulario" disabled={mutation.isPending}><X size={19} /></button></header>
      <form className="owner-liquidacion-form" onSubmit={handleSubmit} noValidate>
        <div className="owner-liquidacion-allocations"><h3>Remuneraciones seleccionadas</h3>{applications.map(({ item, balanceCents, valid, remainingCents }) => <article className="owner-liquidacion-allocation" key={item.remuneracionId}>
          <div className="owner-liquidacion-allocation-heading"><strong>Turno #{item.turnoId ?? '—'} · {item.servicioNombre || 'Servicio'}</strong><span>Saldo disponible: {money(item.saldoPendiente)}</span></div>
          <div className="owner-liquidacion-allocation-fields"><label className="owner-form-field"><span>Importe a aplicar</span><input type="number" min="0.01" max={balanceCents == null ? undefined : centsToMoney(balanceCents)} step="0.01" inputMode="decimal" value={importes[String(item.remuneracionId)] ?? ''} onChange={(event) => changeImporte(String(item.remuneracionId), event.target.value)} aria-invalid={!valid} /></label><div className="owner-liquidacion-remaining"><small>Saldo después de la liquidación</small><strong>{valid ? money(remainingCents / 100) : 'Revisá el importe'}</strong></div></div>
        </article>)}</div>
        <div className="owner-liquidacion-total"><span>Total de la liquidación</span><strong>{money(totalCents / 100)}</strong></div>
        <div className="owner-liquidacion-form-grid">
          <label className="owner-form-field"><span>Fecha efectiva *</span><input type="date" required value={fechaEfectiva} onChange={(event) => { setFechaEfectiva(event.target.value); setError('') }} /></label>
          <label className="owner-form-field"><span>Método de pago *</span><select required value={metodoPago} onChange={(event) => { setMetodoPago(event.target.value); setError('') }}><option value="">Seleccioná un método</option>{methods.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
          {metodoPago === 'OTRO' && <label className="owner-form-field owner-liquidacion-wide"><span>Descripción del medio * <small>{descripcionMetodo.length}/100</small></span><input type="text" maxLength={100} value={descripcionMetodo} onChange={(event) => { setDescripcionMetodo(event.target.value); setError('') }} required /></label>}
          <label className="owner-form-field owner-liquidacion-wide"><span>Observaciones <small>{observaciones.length}/500</small></span><textarea maxLength={500} rows="3" value={observaciones} onChange={(event) => { setObservaciones(event.target.value); setError('') }} /></label>
        </div>
        <p className="owner-liquidacion-note">La liquidación registra un pago aplicado a estas remuneraciones. Confirmá los importes antes de continuar.</p>
        {!formValid && <ul className="owner-liquidacion-validation" aria-live="polite">{validationMessages.map((message) => <li key={message}>{message}</li>)}</ul>}
        {error && <p className="owner-liquidacion-error" role="alert"><CircleAlert size={16} />{error}</p>}
        <footer className="owner-liquidacion-actions"><button type="button" className="owners-secondary-button" onClick={onCancel} disabled={mutation.isPending}>Cancelar</button><button type="submit" className="owner-create-button" disabled={!formValid || mutation.isPending}>{mutation.isPending ? <><LoaderCircle size={15} className="spin" /> Registrando…</> : <><CheckCircle2 size={15} /> Confirmar liquidación</>}</button></footer>
      </form>
    </section>
  </div>
}
