import { useState } from 'react'
import { CheckCircle2, Home, LoaderCircle, MapPin, Pencil, RefreshCw, X } from 'lucide-react'
import { getOwnerBusinessAddressErrorMessage, isOwnerBusinessAddressMissing } from '../api/ownerBusinessAddressApi.js'
import { useCreateOwnerBusinessAddress, useOwnerBusinessAddress, useUpdateOwnerBusinessAddress } from '../hooks/useOwnerBusinessAddress.js'

const initialValues = { calle: '', numero: '', ciudad: '', provincia: '', codigoPostal: '' }

function validate(values, isCreating) {
  const errors = {}
  if (isCreating && !values.calle.trim()) errors.calle = 'La calle es obligatoria.'
  else if (values.calle && !values.calle.trim()) errors.calle = 'La calle no puede estar vacía.'
  else if (values.calle.length > 100) errors.calle = 'La calle no puede superar los 100 caracteres.'
  if (isCreating && !values.numero.trim()) errors.numero = 'El número es obligatorio.'
  else if (values.numero && !values.numero.trim()) errors.numero = 'El número no puede estar vacío.'
  else if (values.numero.length > 10) errors.numero = 'El número no puede superar los 10 caracteres.'
  if (values.ciudad.length > 100) errors.ciudad = 'La ciudad no puede superar los 100 caracteres.'
  if (values.provincia.length > 100) errors.provincia = 'La provincia no puede superar los 100 caracteres.'
  if (values.codigoPostal.length > 10) errors.codigoPostal = 'El código postal no puede superar los 10 caracteres.'
  return errors
}

function formValues(address) {
  return Object.fromEntries(Object.keys(initialValues).map((key) => [key, address?.[key] || '']))
}

function AddressForm({ id, address, onCancel, onSaved }) {
  const isCreating = !address
  const [values, setValues] = useState(() => formValues(address))
  const [fieldErrors, setFieldErrors] = useState({})
  const [submitError, setSubmitError] = useState('')
  const createMutation = useCreateOwnerBusinessAddress()
  const updateMutation = useUpdateOwnerBusinessAddress()
  const mutation = isCreating ? createMutation : updateMutation

  function changeField(event) {
    const { name, value } = event.target
    setValues((current) => ({ ...current, [name]: value }))
    setFieldErrors((current) => ({ ...current, [name]: '' }))
    setSubmitError('')
  }

  async function handleSubmit(event) {
    event.preventDefault()
    const errors = validate(values, isCreating)
    setFieldErrors(errors)
    setSubmitError('')
    if (Object.keys(errors).length || mutation.isPending) return
    const payload = Object.fromEntries(Object.entries(values).map(([key, value]) => [key, value.trim() || null]))
    try {
      const saved = isCreating
        ? await mutation.mutateAsync({ id, address: payload })
        : await mutation.mutateAsync({ id, changes: payload })
      onSaved(saved)
    } catch (error) {
      setSubmitError(getOwnerBusinessAddressErrorMessage(error))
    }
  }

  const fields = [
    ['calle', 'Calle', 100, isCreating],
    ['numero', 'Número', 10, isCreating],
    ['ciudad', 'Ciudad', 100, false],
    ['provincia', 'Provincia', 100, false],
    ['codigoPostal', 'Código postal', 10, false],
  ]

  return (
    <form className="owner-form" onSubmit={handleSubmit} noValidate>
      <div className="owner-form-grid">
        {fields.map(([name, label, maxLength, required]) => {
          const errorId = `address-${id}-${name}-error`
          return <label className="owner-form-field" htmlFor={`address-${id}-${name}`} key={name}>
            <span>{label}{required ? ' *' : ' (opcional)'}</span>
            <input id={`address-${id}-${name}`} name={name} value={values[name]} onChange={changeField} maxLength={maxLength} required={required} aria-invalid={Boolean(fieldErrors[name])} aria-describedby={fieldErrors[name] ? errorId : undefined} />
            {fieldErrors[name] && <small id={errorId} className="owner-field-error">{fieldErrors[name]}</small>}
          </label>
        })}
      </div>
      {submitError && <p className="owner-form-submit-error" role="alert">{submitError}</p>}
      <div className="owner-form-actions">
        <button type="button" className="owners-secondary-button" onClick={onCancel} disabled={mutation.isPending}>Cancelar</button>
        <button type="submit" className="owner-create-button" disabled={mutation.isPending}>{mutation.isPending ? <><LoaderCircle size={15} className="spin" /> Guardando…</> : <><Home size={15} /> {isCreating ? 'Registrar dirección' : 'Guardar cambios'}</>}</button>
      </div>
    </form>
  )
}

export default function OwnerBusinessAddressSection({ negocioId }) {
  const query = useOwnerBusinessAddress(negocioId)
  const [isEditing, setIsEditing] = useState(false)
  const [showSavedNotice, setShowSavedNotice] = useState(false)
  const addressMissing = query.isError && isOwnerBusinessAddressMissing(query.error)
  const isCreating = addressMissing
  const isUpdating = query.isSuccess && isEditing

  function handleSaved() {
    setIsEditing(false)
    setShowSavedNotice(true)
  }

  return (
    <section className="create-owner-panel owner-address-panel" aria-labelledby="owner-address-title">
      <div className="create-owner-heading">
        <div><span className="create-owner-icon"><MapPin size={17} /></span><div><h2 id="owner-address-title">Dirección del negocio</h2><p>Ubicación registrada para este negocio.</p></div></div>
        {query.isSuccess && !isEditing && <button type="button" className="owners-secondary-button" onClick={() => { setShowSavedNotice(false); setIsEditing(true) }}><Pencil size={14} /> Editar dirección</button>}
      </div>
      {showSavedNotice && <div className="owner-created-notice" role="status"><CheckCircle2 size={17} /><span>La dirección se guardó correctamente.</span><button type="button" className="icon-button" onClick={() => setShowSavedNotice(false)} aria-label="Cerrar confirmación"><X size={16} /></button></div>}
      {query.isPending ? <div className="owner-business-state" role="status"><LoaderCircle size={19} className="spin" /> Cargando dirección…</div>
        : query.isError && !addressMissing ? <div className="owner-business-state user-detail-error" role="alert"><p>{getOwnerBusinessAddressErrorMessage(query.error)}</p><button type="button" className="owners-secondary-button" onClick={() => query.refetch()} disabled={query.isFetching}><RefreshCw size={15} /> Reintentar</button></div>
          : isCreating || isUpdating ? <AddressForm id={negocioId} address={isUpdating ? query.data : null} onCancel={() => setIsEditing(false)} onSaved={handleSaved} />
            : query.isSuccess ? <dl className="user-detail-grid owner-address-grid">{[['Calle', query.data.calle], ['Número', query.data.numero], ['Ciudad', query.data.ciudad], ['Provincia', query.data.provincia], ['Código postal', query.data.codigoPostal]].map(([label, value]) => <div className="user-detail-field" key={label}><dt>{label}</dt><dd>{value || '—'}</dd></div>)}</dl>
              : null}
    </section>
  )
}
