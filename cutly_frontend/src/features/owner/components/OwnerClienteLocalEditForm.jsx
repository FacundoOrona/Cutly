import { useState } from 'react'
import { Check, LoaderCircle, X } from 'lucide-react'
import { getCreateOwnerClienteLocalErrorMessage } from '../api/ownerTurnosApi.js'
import { useUpdateOwnerClienteLocal } from '../hooks/useOwnerTurnos.js'

const fields = [['nombre', 'Nombre', 20], ['apellido', 'Apellido', 20], ['dni', 'DNI', 20], ['telefono', 'Teléfono', 30]]

function validate(values) {
  const errors = {}
  for (const name of ['nombre', 'apellido']) {
    const value = values[name].trim()
    if (!value) errors[name] = `El ${name} es obligatorio.`
    else if (value.length < 2 || value.length > 20) errors[name] = `El ${name} debe contener entre 2 y 20 caracteres.`
  }
  if (!values.dni) errors.dni = 'El DNI es obligatorio.'
  else if (values.dni.length > 20) errors.dni = 'El DNI no puede superar los 20 caracteres.'
  else if (!/^\d{7,8}$/.test(values.dni)) errors.dni = 'El DNI debe contener 7 u 8 dígitos.'
  if (!values.telefono) errors.telefono = 'El teléfono es obligatorio.'
  else if (values.telefono.length > 30) errors.telefono = 'El teléfono no puede superar los 30 caracteres.'
  else if (!/^\d+$/.test(values.telefono)) errors.telefono = 'El teléfono debe contener únicamente números.'
  return errors
}

export default function OwnerClienteLocalEditForm({ negocioId, cliente, onSaved, onCancel }) {
  const mutation = useUpdateOwnerClienteLocal()
  const [values, setValues] = useState({ nombre: cliente.nombre || '', apellido: cliente.apellido || '', dni: cliente.dniPresentado || '', telefono: cliente.telefono || '' })
  const [errors, setErrors] = useState({})
  const [submitError, setSubmitError] = useState('')

  async function submit(event) {
    event.preventDefault()
    const nextErrors = validate(values)
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length || mutation.isPending) return
    try {
      const saved = await mutation.mutateAsync({ negocioId, clienteLocalId: cliente.clienteLocalId, cliente: Object.fromEntries(Object.entries(values).map(([key, value]) => [key, value.trim()])) })
      onSaved(saved)
    } catch (error) {
      setSubmitError(getCreateOwnerClienteLocalErrorMessage(error))
    }
  }

  return <form className="owner-form owner-client-form owner-client-edit-form" onSubmit={submit} noValidate>
    <div className="owner-form-grid">{fields.map(([name, label, maxLength]) => <label className="owner-form-field" htmlFor={`owner-edit-${cliente.clienteLocalId}-${name}`} key={name}><span>{label} *</span><input id={`owner-edit-${cliente.clienteLocalId}-${name}`} value={values[name]} maxLength={maxLength} inputMode={['dni', 'telefono'].includes(name) ? 'numeric' : undefined} onChange={(event) => { const value = ['dni', 'telefono'].includes(name) ? event.target.value.replace(/\D/g, '') : event.target.value; setValues((current) => ({ ...current, [name]: value })); setErrors((current) => ({ ...current, [name]: '' })); setSubmitError('') }} aria-invalid={Boolean(errors[name])} />{errors[name] && <small className="owner-field-error">{errors[name]}</small>}</label>)}</div>
    {submitError && <p className="owner-form-submit-error" role="alert">{submitError}</p>}
    <div className="owner-form-actions"><button type="submit" className="owner-create-button" disabled={mutation.isPending}>{mutation.isPending ? <><LoaderCircle size={15} className="spin" /> Guardando…</> : <><Check size={15} /> Guardar cambios</>}</button><button type="button" className="owners-secondary-button" onClick={onCancel} disabled={mutation.isPending}><X size={15} /> Cancelar</button></div>
  </form>
}
