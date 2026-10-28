import { useState } from 'react'
import { Building2, LoaderCircle, X } from 'lucide-react'
import { getUpdateOwnerBusinessErrorMessage } from '../api/ownerBusinessesApi.js'
import { useUpdateOwnerBusiness } from '../hooks/useOwnerBusinesses.js'

function validate(values) {
  const errors = {}
  if (!values.nombre.trim()) errors.nombre = 'El nombre no puede estar vacío.'
  else if (values.nombre.trim().length > 100) errors.nombre = 'El nombre no puede superar los 100 caracteres.'
  if (values.descripcion.length > 500) errors.descripcion = 'La descripción no puede superar los 500 caracteres.'
  if (values.telefono.length > 30) errors.telefono = 'El teléfono no puede superar los 30 caracteres.'
  const email = values.email.trim()
  if (email.length > 150) errors.email = 'El email no puede superar los 150 caracteres.'
  else if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) errors.email = 'Ingresá un email válido.'
  return errors
}

export default function EditOwnerBusinessForm({ business, onCancel, onUpdated }) {
  const [values, setValues] = useState({
    nombre: business.nombre || '',
    descripcion: business.descripcion || '',
    telefono: business.telefono || '',
    email: business.email || '',
  })
  const [fieldErrors, setFieldErrors] = useState({})
  const [submitError, setSubmitError] = useState('')
  const mutation = useUpdateOwnerBusiness()

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
      const updatedBusiness = await mutation.mutateAsync({
        id: business.negocioId,
        changes: {
          nombre: values.nombre.trim(),
          descripcion: values.descripcion.trim() || null,
          telefono: values.telefono.trim() || null,
          email: values.email.trim() || null,
        },
      })
      onUpdated(updatedBusiness)
    } catch (error) {
      setSubmitError(getUpdateOwnerBusinessErrorMessage(error))
    }
  }

  function field(name, label, options = {}) {
    const errorId = `edit-business-${name}-error`
    return (
      <label className="owner-form-field" htmlFor={`edit-business-${name}`}>
        <span>{label}{options.required ? ' *' : ' (opcional)'}</span>
        <input id={`edit-business-${name}`} name={name} type={options.type || 'text'} value={values[name]} onChange={changeField} maxLength={options.maxLength} required={options.required} aria-invalid={Boolean(fieldErrors[name])} aria-describedby={fieldErrors[name] ? errorId : undefined} />
        {fieldErrors[name] && <small id={errorId} className="owner-field-error">{fieldErrors[name]}</small>}
      </label>
    )
  }

  return (
    <section className="create-owner-panel" aria-labelledby="edit-business-title">
      <div className="create-owner-heading"><div><span className="create-owner-icon"><Building2 size={17} /></span><div><h2 id="edit-business-title">Editar negocio</h2><p>Actualizá los datos generales del negocio.</p></div></div><button type="button" className="icon-button" onClick={onCancel} disabled={mutation.isPending} aria-label="Cancelar edición"><X size={18} /></button></div>
      <form className="owner-form" onSubmit={handleSubmit} noValidate>
        <div className="owner-form-grid">
          {field('nombre', 'Nombre', { required: true, maxLength: 100 })}
          {field('descripcion', 'Descripción', { maxLength: 500 })}
          {field('telefono', 'Teléfono', { maxLength: 30 })}
          {field('email', 'Email', { type: 'email', maxLength: 150 })}
        </div>
        {submitError && <p className="owner-form-submit-error" role="alert">{submitError}</p>}
        <div className="owner-form-actions"><button type="button" className="owners-secondary-button" onClick={onCancel} disabled={mutation.isPending}>Cancelar</button><button type="submit" className="owner-create-button" disabled={mutation.isPending}>{mutation.isPending ? <><LoaderCircle size={15} className="spin" /> Guardando…</> : <><Building2 size={15} /> Guardar cambios</>}</button></div>
      </form>
    </section>
  )
}
