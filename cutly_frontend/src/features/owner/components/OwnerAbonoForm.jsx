import { useRef, useState } from 'react'
import { Banknote, Check, CircleAlert, LoaderCircle, RotateCcw } from 'lucide-react'

const paymentMethods = {
  EFECTIVO: 'Efectivo',
  TRANSFERENCIA_QR: 'Transferencia / QR',
  DEBITO: 'Débito',
  CREDITO: 'Crédito',
  OTRO: 'Otro',
}

function money(value) {
  return new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' }).format(Number(value || 0))
}

export default function OwnerAbonoForm({ saldo, isPending, error, onSubmit }) {
  const [amount, setAmount] = useState('')
  const [method, setMethod] = useState('EFECTIVO')
  const [other, setOther] = useState('')
  const [validation, setValidation] = useState('')
  const operationRef = useRef(null)

  function edit(setter, value) {
    setter(value)
    operationRef.current = null
    setValidation('')
  }

  function submit(event) {
    event.preventDefault()
    if (isPending) return
    const parsedAmount = Number(amount)
    if (!amount.trim() || !Number.isFinite(parsedAmount) || parsedAmount <= 0) {
      setValidation('Ingresá un importe mayor que cero.')
      return
    }
    if (parsedAmount > Number(saldo)) {
      setValidation('El importe no puede superar el saldo pendiente.')
      return
    }
    if (method === 'OTRO' && (!other.trim() || other.trim().length > 100)) {
      setValidation('Ingresá una descripción de hasta 100 caracteres para el método OTRO.')
      return
    }

    const body = {
      monto: parsedAmount,
      metodoPago: method,
      descripcionOtro: method === 'OTRO' ? other.trim() : null,
    }
    const serializedBody = JSON.stringify(body)
    if (!operationRef.current || operationRef.current.serializedBody !== serializedBody) {
      operationRef.current = { idempotencyKey: crypto.randomUUID(), body, serializedBody }
    }
    setValidation('')
    onSubmit(operationRef.current)
  }

  return <form className="owner-abono-form" onSubmit={submit}>
    <div className="owner-abono-balance"><span>Saldo pendiente</span><strong>{money(saldo)}</strong><small>Importe máximo para este abono: {money(saldo)}</small></div>
    <div className="owner-abono-fields">
      <label className="owner-form-field" htmlFor="owner-abono-amount"><span>Importe del abono <b>*</b></span><div className="owner-financial-money-input"><span>$</span><input id="owner-abono-amount" type="number" min="0.01" max={saldo} step="0.01" required value={amount} onChange={(event) => edit(setAmount, event.target.value)} disabled={isPending} aria-invalid={Boolean(validation && validation.includes('importe'))} /></div></label>
      <label className="owner-form-field" htmlFor="owner-abono-method"><span>Método de pago <b>*</b></span><select id="owner-abono-method" value={method} onChange={(event) => edit(setMethod, event.target.value)} disabled={isPending}>{Object.entries(paymentMethods).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
      {method === 'OTRO' && <label className="owner-form-field owner-abono-wide" htmlFor="owner-abono-other"><span>Descripción del método <b>*</b></span><input id="owner-abono-other" type="text" maxLength={100} required value={other} onChange={(event) => edit(setOther, event.target.value)} disabled={isPending} /></label>}
    </div>
    {validation && <div className="owner-financial-message is-error" role="alert"><CircleAlert size={16} /><span>{validation}</span></div>}
    {error && <div className="owner-financial-message is-error" role="alert"><CircleAlert size={16} /><span>{error}<button type="button" className="owner-financial-retry" onClick={() => operationRef.current && onSubmit(operationRef.current)} disabled={isPending}><RotateCcw size={13} /> Reintentar misma operación</button></span></div>}
    <button className="owner-create-button owner-abono-submit" type="submit" disabled={isPending || Number(saldo) <= 0}>{isPending ? <><LoaderCircle size={15} className="spin" /> Registrando abono…</> : <><Banknote size={15} /> Registrar abono <Check size={14} /></>}</button>
  </form>
}
