import { useState } from 'react'
import { CheckCircle2, LoaderCircle } from 'lucide-react'
import { getCreateOwnerClienteLocalErrorMessage } from '../api/ownerTurnosApi.js'
import { useCreateOwnerClienteLocal } from '../hooks/useOwnerTurnos.js'

const initialValues = { nombre: '', apellido: '', dni: '', telefono: '' }
const fields = [
  ['nombre', 'Nombre', 20],
  ['apellido', 'Apellido', 20],
  ['dni', 'DNI', 20],
  ['telefono', 'Teléfono', 30],
]

function validate(values) {
  const errors = {}
  for (const [name, label] of fields.slice(0, 2)) {
    const value = values[name].trim()
    if (!value) errors[name] = `El ${label.toLowerCase()} es obligatorio.`
    else if (value.length < 2 || value.length > 20) errors[name] = `El ${label.toLowerCase()} debe contener entre 2 y 20 caracteres.`
  }
  const dni = values.dni.trim()
  if (!dni) errors.dni = 'El DNI es obligatorio.'
  else if (dni.length > 20) errors.dni = 'El DNI no puede superar los 20 caracteres.'
  else if (!/^\s*(?:\d[\d.\-\s]*\d|\d)\s*$/.test(dni) || dni.replace(/[.\-\s]/g, '').length < 7 || dni.replace(/[.\-\s]/g, '').length > 8) errors.dni = 'El DNI debe contener 7 u 8 dígitos ASCII. Se permiten puntos, guiones y espacios.'
  const telefono = values.telefono.trim()
  if (!telefono) errors.telefono = 'El teléfono es obligatorio.'
  else if (telefono.length > 30) errors.telefono = 'El teléfono no puede superar los 30 caracteres.'
  return errors
}

export default function OwnerClienteLocalForm({ negocioId, onCreated }) {
  const mutation = useCreateOwnerClienteLocal()
  const [values, setValues] = useState(initialValues)
  const [fieldErrors, setFieldErrors] = useState({})
  const [submitError, setSubmitError] = useState('')

  function changeField(event) {
    const { name, value } = event.target
    setValues((current) => ({ ...current, [name]: value }))
    setFieldErrors((current) => ({ ...current, [name]: '' }))
    setSubmitError('')
  }

  async function handleSubmit(event) {
    event.preventDefault()
    const errors = validate(values)
    setFieldErrors(errors)
    setSubmitError('')
    if (Object.keys(errors).length || mutation.isPending || !negocioId) return

    try {
      const cliente = await mutation.mutateAsync({
        negocioId,
        cliente: Object.fromEntries(Object.entries(values).map(([key, value]) => [key, value.trim()])),
      })
      setValues(initialValues)
      setFieldErrors({})
      onCreated(cliente)
    } catch (error) {
      setSubmitError(getCreateOwnerClienteLocalErrorMessage(error))
    }
  }

  return <form className="owner-form owner-client-form" onSubmit={handleSubmit} noValidate>
    <div className="owner-form-grid">
      {fields.map(([name, label, maxLength]) => {
        const inputId = `owner-client-${name}`
        const errorId = `${inputId}-error`
        return <label className="owner-form-field" htmlFor={inputId} key={name}>
          <span>{label} *</span>
          <input id={inputId} name={name} value={values[name]} onChange={changeField} maxLength={maxLength} required autoComplete={name === 'telefono' ? 'tel' : name} aria-invalid={Boolean(fieldErrors[name])} aria-describedby={fieldErrors[name] ? errorId : undefined} />
          {fieldErrors[name] && <small id={errorId} className="owner-field-error">{fieldErrors[name]}</small>}
        </label>
      })}
    </div>
    {submitError && <p className="owner-form-submit-error" role="alert">{submitError}</p>}
    <div className="owner-form-actions">
      <button type="submit" className="owner-create-button" disabled={mutation.isPending || !negocioId}>{mutation.isPending ? <><LoaderCircle size={15} className="spin" /> Registrando…</> : <><CheckCircle2 size={15} /> Registrar cliente</>}</button>
    </div>
  </form>
}
