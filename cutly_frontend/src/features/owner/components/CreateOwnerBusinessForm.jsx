import { useState } from 'react'
import { Building2, LoaderCircle, X } from 'lucide-react'
import { getCreateOwnerBusinessErrorMessage } from '../api/ownerBusinessesApi.js'
import { useCreateOwnerBusiness } from '../hooks/useOwnerBusinesses.js'

const initialValues = { nombre: '', descripcion: '', telefono: '', email: '' }

function validate(values) {
  const errors = {}
  if (!values.nombre.trim()) errors.nombre = 'El nombre es obligatorio.'
  else if (values.nombre.trim().length > 100) errors.nombre = 'El nombre no puede superar los 100 caracteres.'
  if (values.descripcion.length > 500) errors.descripcion = 'La descripción no puede superar los 500 caracteres.'
  if (values.telefono.length > 30) errors.telefono = 'El teléfono no puede superar los 30 caracteres.'
  const email = values.email.trim()
  if (email.length > 150) errors.email = 'El email no puede superar los 150 caracteres.'
  else if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) errors.email = 'Ingresá un email válido.'
  return errors
}

export default function CreateOwnerBusinessForm({ onCancel, onCreated }) {
  const [values, setValues] = useState(initialValues)
  const [fieldErrors, setFieldErrors] = useState({})
  const [submitError, setSubmitError] = useState('')
  const mutation = useCreateOwnerBusiness()

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
    if (Object.keys(errors).length || mutation.isPending) return
    try {
      const createdBusiness = await mutation.mutateAsync({
        nombre: values.nombre.trim(),
        descripcion: values.descripcion.trim() || null,
        telefono: values.telefono.trim() || null,
        email: values.email.trim() || null,
      })
      setValues(initialValues)
      onCreated(createdBusiness)
    } catch (error) {
      setSubmitError(getCreateOwnerBusinessErrorMessage(error))
    }
  }

  function handleCancel() {
    if (mutation.isPending) return
    setValues(initialValues)
    setFieldErrors({})
    setSubmitError('')
    onCancel()
  }

  function field(name, label, options = {}) {
    const errorId = `business-${name}-error`
    return (
      <label className="owner-form-field" htmlFor={`business-${name}`}>
        <span>{label}{options.required ? ' *' : ' (opcional)'}</span>
        <input id={`business-${name}`} name={name} type={options.type || 'text'} value={values[name]} onChange={changeField} maxLength={options.maxLength} required={options.required} aria-invalid={Boolean(fieldErrors[name])} aria-describedby={fieldErrors[name] ? errorId : undefined} />
        {fieldErrors[name] && <small id={errorId} className="owner-field-error">{fieldErrors[name]}</small>}
      </label>
    )
  }

  return (
    <section className="create-owner-panel" aria-labelledby="create-business-title">
      <div className="create-owner-heading"><div><span className="create-owner-icon"><Building2 size={17} /></span><div><h2 id="create-business-title">Nuevo negocio</h2><p>Completá los datos básicos. El nombre es obligatorio.</p></div></div><button type="button" className="icon-button" onClick={handleCancel} disabled={mutation.isPending} aria-label="Cancelar creación"><X size={18} /></button></div>
      <form className="owner-form" onSubmit={handleSubmit} noValidate>
        <div className="owner-form-grid">
          {field('nombre', 'Nombre', { required: true, maxLength: 100 })}
          {field('descripcion', 'Descripción', { maxLength: 500 })}
          {field('telefono', 'Teléfono', { maxLength: 30 })}
          {field('email', 'Email', { type: 'email', maxLength: 150 })}
        </div>
        {submitError && <p className="owner-form-submit-error" role="alert">{submitError}</p>}
        <div className="owner-form-actions"><button type="button" className="owners-secondary-button" onClick={handleCancel} disabled={mutation.isPending}>Cancelar</button><button type="submit" className="owner-create-button" disabled={mutation.isPending}>{mutation.isPending ? <><LoaderCircle size={15} className="spin" /> Creando…</> : <><Building2 size={15} /> Crear negocio</>}</button></div>
      </form>
    </section>
  )
}
