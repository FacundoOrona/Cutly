import { useState } from 'react'
import { LoaderCircle, UserPlus, X } from 'lucide-react'
import { getCreateOwnerErrorMessage } from '../api/ownersApi.js'
import { useCreateOwner } from '../hooks/useOwners.js'

const initialValues = { nombre: '', apellido: '', email: '', dni: '', contrasena: '' }

function validate(values) {
  const errors = {}
  const nombre = values.nombre.trim()
  const apellido = values.apellido.trim()
  const email = values.email.trim()
  const dni = values.dni.trim()
  if (nombre.length < 2 || nombre.length > 20) errors.nombre = 'El nombre debe contener entre 2 y 20 caracteres.'
  if (apellido.length < 2 || apellido.length > 20) errors.apellido = 'El apellido debe contener entre 2 y 20 caracteres.'
  if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) errors.email = 'Ingresá un email válido.'
  if (!/^\d{7,8}$/.test(dni)) errors.dni = 'El DNI debe contener entre 7 y 8 dígitos.'
  if (values.contrasena.length < 8) errors.contrasena = 'La contraseña debe contener al menos 8 caracteres.'
  return errors
}

export default function CreateOwnerForm({ onCancel, onCreated }) {
  const [values, setValues] = useState(initialValues)
  const [fieldErrors, setFieldErrors] = useState({})
  const [submitError, setSubmitError] = useState('')
  const mutation = useCreateOwner()

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
    if (Object.keys(errors).length) return

    try {
      const createdOwner = await mutation.mutateAsync({
        nombre: values.nombre.trim(),
        apellido: values.apellido.trim(),
        email: values.email.trim(),
        dni: values.dni.trim(),
        contrasena: values.contrasena,
      })
      setValues(initialValues)
      onCreated(createdOwner)
    } catch (error) {
      setValues((current) => ({ ...current, contrasena: '' }))
      setSubmitError(getCreateOwnerErrorMessage(error))
    }
  }

  function handleCancel() {
    setValues(initialValues)
    setFieldErrors({})
    setSubmitError('')
    onCancel()
  }

  function field(name, label, options = {}) {
    const errorId = `${name}-error`
    return (
      <label className="owner-form-field" htmlFor={`owner-${name}`}>
        <span>{label}</span>
        <input id={`owner-${name}`} name={name} type={options.type || 'text'} value={values[name]} onChange={changeField} autoComplete={options.autoComplete} inputMode={options.inputMode} maxLength={options.maxLength} aria-invalid={Boolean(fieldErrors[name])} aria-describedby={fieldErrors[name] ? errorId : undefined} required />
        {fieldErrors[name] && <small id={errorId} className="owner-field-error">{fieldErrors[name]}</small>}
      </label>
    )
  }

  return (
    <section className="create-owner-panel" aria-labelledby="create-owner-title">
      <div className="create-owner-heading"><div><span className="create-owner-icon"><UserPlus size={17} /></span><div><h2 id="create-owner-title">Crear owner</h2><p>Completá los datos de la nueva cuenta.</p></div></div><button type="button" className="icon-button" onClick={handleCancel} aria-label="Cancelar creación"><X size={18} /></button></div>
      <form className="owner-form" onSubmit={handleSubmit} noValidate>
        <div className="owner-form-grid">
          {field('nombre', 'Nombre', { autoComplete: 'given-name', maxLength: 20 })}
          {field('apellido', 'Apellido', { autoComplete: 'family-name', maxLength: 20 })}
          {field('email', 'Email', { type: 'email', autoComplete: 'email' })}
          {field('dni', 'DNI', { inputMode: 'numeric', maxLength: 8 })}
          {field('contrasena', 'Contraseña', { type: 'password', autoComplete: 'new-password' })}
        </div>
        {submitError && <p className="owner-form-submit-error" role="alert">{submitError}</p>}
        <div className="owner-form-actions"><button type="button" className="owners-secondary-button" onClick={handleCancel} disabled={mutation.isPending}>Cancelar</button><button type="submit" className="owner-create-button" disabled={mutation.isPending}>{mutation.isPending ? <><LoaderCircle size={15} className="spin" /> Creando…</> : <><UserPlus size={15} /> Crear owner</>}</button></div>
      </form>
    </section>
  )
}
