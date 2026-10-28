import { useState } from 'react'
import { CheckCircle2, LoaderCircle } from 'lucide-react'
import { getOwnerBusinessEmployeesErrorMessage } from '../api/ownerBusinessEmployeesApi.js'
import { useCreateOwnerBusinessEmployee, useUpdateOwnerBusinessEmployee } from '../hooks/useOwnerBusinessEmployees.js'

const fields = [
  ['nombre', 'Nombre', 'text', 20],
  ['apellido', 'Apellido', 'text', 20],
  ['email', 'Email', 'email', undefined],
  ['dni', 'DNI', 'text', 8],
]

function getValues(employee) {
  return employee
    ? { nombre: employee.nombre || '', apellido: employee.apellido || '', email: employee.email || '', dni: employee.dni || '', contrasena: '' }
    : { nombre: '', apellido: '', email: '', dni: '', contrasena: '' }
}

function validate(values, editing) {
  const errors = {}
  for (const [name, label] of [['nombre', 'El nombre'], ['apellido', 'El apellido']]) {
    const value = values[name].trim()
    if (!value) errors[name] = `${label} es obligatorio.`
    else if (value.length < 2 || value.length > 20) errors[name] = `${label} debe contener entre 2 y 20 caracteres.`
  }
  const email = values.email.trim()
  if (!email) errors.email = 'El email es obligatorio.'
  else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) errors.email = 'Ingresá un email válido.'
  if (!values.dni.trim()) errors.dni = 'El DNI es obligatorio.'
  else if (!/^\d{7,8}$/.test(values.dni.trim())) errors.dni = 'El DNI debe contener 7 u 8 dígitos.'
  if (!editing && !values.contrasena) errors.contrasena = 'La contraseña es obligatoria.'
  else if (!editing && values.contrasena.length < 8) errors.contrasena = 'La contraseña debe contener al menos 8 caracteres.'
  return errors
}

export default function OwnerBusinessEmployeeForm({ id, employee, onCancel, onSaved }) {
  const editing = Boolean(employee)
  const [values, setValues] = useState(() => getValues(employee))
  const [fieldErrors, setFieldErrors] = useState({})
  const [submitError, setSubmitError] = useState('')
  const createMutation = useCreateOwnerBusinessEmployee()
  const updateMutation = useUpdateOwnerBusinessEmployee()
  const mutation = editing ? updateMutation : createMutation

  function changeField(event) {
    const { name, value } = event.target
    setValues((current) => ({ ...current, [name]: value }))
    setFieldErrors((current) => ({ ...current, [name]: '' }))
    setSubmitError('')
  }

  async function handleSubmit(event) {
    event.preventDefault()
    const errors = validate(values, editing)
    setFieldErrors(errors)
    setSubmitError('')
    if (Object.keys(errors).length || mutation.isPending) return

    try {
      if (editing) {
        const changes = {}
        for (const [name] of fields) {
          const value = values[name].trim()
          if (value !== String(employee[name] ?? '')) changes[name] = value
        }
        if (Object.keys(changes).length) await updateMutation.mutateAsync({ id, empleadoId: employee.empleadoId, changes })
        setValues(getValues(null))
        onSaved('Los datos del empleado se actualizaron correctamente.')
      } else {
        const { contrasena, ...identity } = values
        await createMutation.mutateAsync({
          id,
          employee: { ...identity, nombre: identity.nombre.trim(), apellido: identity.apellido.trim(), email: identity.email.trim(), dni: identity.dni.trim(), contrasena },
        })
        setValues(getValues(null))
        onSaved('El empleado se creó correctamente.')
      }
    } catch (error) {
      setSubmitError(getOwnerBusinessEmployeesErrorMessage(error, editing ? 'actualizar' : 'crear'))
    }
  }

  const formFields = editing ? fields : [...fields, ['contrasena', 'Contraseña', 'password', undefined]]

  return <form className="owner-form owner-employee-form" onSubmit={handleSubmit} noValidate>
    <div className="owner-form-grid">
      {formFields.map(([name, label, type, maxLength]) => {
        const errorId = `employee-${id}-${employee?.empleadoId || 'new'}-${name}-error`
        return <label className="owner-form-field" htmlFor={errorId.replace('-error', '')} key={name}>
          <span>{label}{editing || name !== 'contrasena' ? ' *' : ' *'}</span>
          <input id={errorId.replace('-error', '')} name={name} type={type} value={values[name]} onChange={changeField} maxLength={maxLength} required autoComplete={name === 'contrasena' ? 'new-password' : undefined} inputMode={name === 'dni' ? 'numeric' : undefined} aria-invalid={Boolean(fieldErrors[name])} aria-describedby={fieldErrors[name] ? errorId : undefined} />
          {fieldErrors[name] && <small id={errorId} className="owner-field-error">{fieldErrors[name]}</small>}
        </label>
      })}
    </div>
    {submitError && <p className="owner-form-submit-error" role="alert">{submitError}</p>}
    <div className="owner-form-actions">
      <button type="button" className="owners-secondary-button" onClick={onCancel} disabled={mutation.isPending}>Cancelar</button>
      <button type="submit" className="owner-create-button" disabled={mutation.isPending}>{mutation.isPending ? <><LoaderCircle size={15} className="spin" /> Guardando…</> : <><CheckCircle2 size={15} /> {editing ? 'Guardar cambios' : 'Crear empleado'}</>}</button>
    </div>
  </form>
}
