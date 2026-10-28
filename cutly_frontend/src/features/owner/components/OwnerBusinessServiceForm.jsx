import { useState } from 'react'
import { CheckCircle2, LoaderCircle } from 'lucide-react'
import { getOwnerBusinessServicesErrorMessage } from '../api/ownerBusinessServicesApi.js'
import { useCreateOwnerBusinessService, useUpdateOwnerBusinessService } from '../hooks/useOwnerBusinessServices.js'

const emptyValues = { nombre: '', descripcion: '', precio: '', duracionMinutos: '' }

function getValues(service) {
  return service ? {
    nombre: service.nombre || '',
    descripcion: service.descripcion || '',
    precio: String(service.precio ?? ''),
    duracionMinutos: String(service.duracionMinutos ?? ''),
  } : emptyValues
}

function validate(values) {
  const errors = {}
  const name = values.nombre.trim()
  if (!name) errors.nombre = 'El nombre es obligatorio.'
  else if (name.length > 100) errors.nombre = 'El nombre no puede superar los 100 caracteres.'
  if (values.descripcion.length > 500) errors.descripcion = 'La descripción no puede superar los 500 caracteres.'

  const price = values.precio.trim()
  if (!price) errors.precio = 'El precio es obligatorio.'
  else if (!/^\d{1,8}(?:\.\d{1,2})?$/.test(price) || Number(price) <= 0) errors.precio = 'Ingresá un precio mayor que cero, con hasta 8 enteros y 2 decimales.'

  const duration = values.duracionMinutos.trim()
  if (!duration) errors.duracionMinutos = 'La duración es obligatoria.'
  else if (!/^\d+$/.test(duration) || !Number.isSafeInteger(Number(duration)) || Number(duration) <= 0) errors.duracionMinutos = 'La duración debe ser un entero mayor que cero.'
  return errors
}

export default function OwnerBusinessServiceForm({ id, service, onCancel, onSaved }) {
  const editing = Boolean(service)
  const [values, setValues] = useState(() => getValues(service))
  const [fieldErrors, setFieldErrors] = useState({})
  const [submitError, setSubmitError] = useState('')
  const createMutation = useCreateOwnerBusinessService()
  const updateMutation = useUpdateOwnerBusinessService()
  const mutation = editing ? updateMutation : createMutation

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
      if (editing) {
        const changes = {}
        if (values.nombre.trim() !== (service.nombre || '')) changes.nombre = values.nombre.trim()
        // The backend persists an empty string as-is, so clearing a description sends "" (never null).
        if (values.descripcion !== (service.descripcion || '')) changes.descripcion = values.descripcion
        if (values.precio.trim() !== String(service.precio)) changes.precio = values.precio.trim()
        if (values.duracionMinutos.trim() !== String(service.duracionMinutos)) changes.duracionMinutos = Number(values.duracionMinutos)
        if (Object.keys(changes).length) await mutation.mutateAsync({ id, servicioId: service.servicioId, changes })
        onSaved('El servicio se actualizó correctamente.')
      } else {
        await mutation.mutateAsync({
          id,
          service: {
            nombre: values.nombre.trim(),
            descripcion: values.descripcion.trim() || null,
            precio: values.precio.trim(),
            duracionMinutos: Number(values.duracionMinutos),
          },
        })
        onSaved('El servicio se creó correctamente.')
      }
    } catch (error) {
      setSubmitError(getOwnerBusinessServicesErrorMessage(error, editing ? 'actualizar' : 'crear'))
    }
  }

  const fields = [
    ['nombre', 'Nombre', 'text', 100, true],
    ['descripcion', 'Descripción', 'text', 500, false],
    ['precio', 'Precio', 'text', undefined, true],
    ['duracionMinutos', 'Duración (minutos)', 'text', undefined, true],
  ]

  return <form className="owner-form owner-service-form" onSubmit={handleSubmit} noValidate>
    <div className="owner-form-grid">
      {fields.map(([name, label, type, maxLength, required]) => {
        const errorId = `service-${id}-${service?.servicioId || 'new'}-${name}-error`
        return <label className="owner-form-field" htmlFor={errorId.replace('-error', '')} key={name}>
          <span>{label}{required ? ' *' : ' (opcional)'}</span>
          <input
            id={errorId.replace('-error', '')}
            name={name}
            type={type}
            value={values[name]}
            onChange={changeField}
            maxLength={maxLength}
            required={required}
            min={undefined}
            step={undefined}
            inputMode={name === 'precio' ? 'decimal' : name === 'duracionMinutos' ? 'numeric' : undefined}
            aria-invalid={Boolean(fieldErrors[name])}
            aria-describedby={fieldErrors[name] ? errorId : undefined}
          />
          {fieldErrors[name] && <small id={errorId} className="owner-field-error">{fieldErrors[name]}</small>}
        </label>
      })}
    </div>
    {submitError && <p className="owner-form-submit-error" role="alert">{submitError}</p>}
    <div className="owner-form-actions">
      <button type="button" className="owners-secondary-button" onClick={onCancel} disabled={mutation.isPending}>Cancelar</button>
      <button type="submit" className="owner-create-button" disabled={mutation.isPending}>{mutation.isPending ? <><LoaderCircle size={15} className="spin" /> Guardando…</> : <><CheckCircle2 size={15} /> {editing ? 'Guardar cambios' : 'Crear servicio'}</>}</button>
    </div>
  </form>
}
