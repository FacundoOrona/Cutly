import { useState } from 'react'
import { CheckCircle2, Clock3, LoaderCircle, Pencil, Plus, RefreshCw, Trash2, X } from 'lucide-react'
import { getOwnerBusinessHoursErrorMessage } from '../api/ownerBusinessHoursApi.js'
import { useCreateOwnerBusinessHour, useDeleteOwnerBusinessHour, useOwnerBusinessHours, useUpdateOwnerBusinessHour } from '../hooks/useOwnerBusinessHours.js'

const days = [
  ['MONDAY', 'Lunes'], ['TUESDAY', 'Martes'], ['WEDNESDAY', 'Miércoles'],
  ['THURSDAY', 'Jueves'], ['FRIDAY', 'Viernes'], ['SATURDAY', 'Sábado'], ['SUNDAY', 'Domingo'],
]
const dayNames = Object.fromEntries(days)
const emptyForm = { diaSemana: '', horaApertura: '', horaCierre: '', cerrado: false }

function toTimeInput(value) {
  return typeof value === 'string' ? value.slice(0, 5) : ''
}

function HourForm({ id, hours, hour, onCancel, onSaved }) {
  const editing = Boolean(hour)
  const [values, setValues] = useState(() => hour ? { diaSemana: hour.diaSemana, horaApertura: toTimeInput(hour.horaApertura), horaCierre: toTimeInput(hour.horaCierre), cerrado: hour.cerrado } : emptyForm)
  const [fieldError, setFieldError] = useState('')
  const [submitError, setSubmitError] = useState('')
  const createMutation = useCreateOwnerBusinessHour()
  const updateMutation = useUpdateOwnerBusinessHour()
  const mutation = editing ? updateMutation : createMutation
  const closedDays = new Set(hours.filter((item) => item.cerrado && item.horarioId !== hour?.horarioId).map((item) => item.diaSemana))

  function setField(name, value) {
    setValues((current) => ({ ...current, [name]: value }))
    setFieldError('')
    setSubmitError('')
  }

  async function handleSubmit(event) {
    event.preventDefault()
    if (!values.diaSemana) { setFieldError('Seleccioná un día de la semana.'); return }
    if (!values.cerrado && (!values.horaApertura || !values.horaCierre || values.horaApertura >= values.horaCierre)) {
      setFieldError('La hora de apertura debe ser anterior a la hora de cierre.')
      return
    }
    if (values.cerrado && hours.some((item) => item.diaSemana === values.diaSemana && item.horarioId !== hour?.horarioId)) {
      setFieldError('Eliminá primero los otros tramos para marcar el día como cerrado.')
      return
    }
    const payload = { diaSemana: values.diaSemana, horaApertura: values.cerrado ? null : `${values.horaApertura}:00`, horaCierre: values.cerrado ? null : `${values.horaCierre}:00`, cerrado: values.cerrado }
    try {
      if (editing) await mutation.mutateAsync({ id, horarioId: hour.horarioId, changes: payload })
      else await mutation.mutateAsync({ id, hour: payload })
      onSaved()
    } catch (error) { setSubmitError(getOwnerBusinessHoursErrorMessage(error, editing ? 'actualizar' : 'registrar')) }
  }

  return <form className="owner-form owner-hours-form" onSubmit={handleSubmit} noValidate>
    <div className="owner-form-grid">
      <label className="owner-form-field" htmlFor={`hours-day-${id}`}><span>Día *</span><select id={`hours-day-${id}`} value={values.diaSemana} onChange={(event) => setField('diaSemana', event.target.value)} required>
        <option value="">Seleccioná un día</option>{days.map(([value, label]) => <option key={value} value={value} disabled={closedDays.has(value)}>{label}{closedDays.has(value) ? ' · cerrado' : ''}</option>)}
      </select></label>
      <label className="owner-hours-closed"><input type="checkbox" checked={values.cerrado} onChange={(event) => setField('cerrado', event.target.checked)} /> Día cerrado</label>
      {!values.cerrado && <>
        <label className="owner-form-field" htmlFor={`hours-open-${id}`}><span>Hora de apertura *</span><input id={`hours-open-${id}`} type="time" value={values.horaApertura} onChange={(event) => setField('horaApertura', event.target.value)} required /></label>
        <label className="owner-form-field" htmlFor={`hours-close-${id}`}><span>Hora de cierre *</span><input id={`hours-close-${id}`} type="time" value={values.horaCierre} onChange={(event) => setField('horaCierre', event.target.value)} required /></label>
      </>}
    </div>
    {fieldError && <p className="owner-form-submit-error" role="alert">{fieldError}</p>}
    {submitError && <p className="owner-form-submit-error" role="alert">{submitError}</p>}
    <div className="owner-form-actions"><button type="button" className="owners-secondary-button" onClick={onCancel} disabled={mutation.isPending}>Cancelar</button><button type="submit" className="owner-create-button" disabled={mutation.isPending}>{mutation.isPending ? <><LoaderCircle size={15} className="spin" /> Guardando…</> : <><CheckCircle2 size={15} /> {editing ? 'Guardar cambios' : 'Registrar horario'}</>}</button></div>
  </form>
}

export default function OwnerBusinessHoursSection({ negocioId }) {
  const query = useOwnerBusinessHours(negocioId)
  const deleteMutation = useDeleteOwnerBusinessHour()
  const [editingHour, setEditingHour] = useState(null)
  const [isCreating, setIsCreating] = useState(false)
  const [notice, setNotice] = useState('')
  const [error, setError] = useState('')
  const [deletingId, setDeletingId] = useState(null)

  async function handleDelete(hour) {
    if (!window.confirm(`¿Eliminar el horario del ${dayNames[hour.diaSemana]}?`)) return
    setError('')
    setDeletingId(hour.horarioId)
    try {
      await deleteMutation.mutateAsync({ id: negocioId, horarioId: hour.horarioId })
      setNotice('El horario se eliminó correctamente.')
    } catch (deleteError) { setError(getOwnerBusinessHoursErrorMessage(deleteError, 'eliminar')) }
    finally { setDeletingId(null) }
  }

  function handleSaved(message) {
    setIsCreating(false)
    setEditingHour(null)
    setError('')
    setNotice(message)
  }

  return <section className="create-owner-panel owner-hours-panel" aria-labelledby="owner-hours-title">
    <div className="create-owner-heading"><div><span className="create-owner-icon"><Clock3 size={17} /></span><div><h2 id="owner-hours-title">Horarios de atención</h2><p>Podés registrar más de un tramo por día.</p></div></div>{query.isSuccess && !isCreating && !editingHour && <button type="button" className="owner-create-button" onClick={() => { setNotice(''); setIsCreating(true) }}><Plus size={15} /> Agregar horario</button>}</div>
    {notice && <div className="owner-created-notice" role="status"><CheckCircle2 size={17} /><span>{notice}</span><button type="button" className="icon-button" onClick={() => setNotice('')} aria-label="Cerrar confirmación"><X size={16} /></button></div>}
    {error && <p className="owner-form-submit-error" role="alert">{error}</p>}
    {query.isPending ? <div className="owner-business-state" role="status"><LoaderCircle size={19} className="spin" /> Cargando horarios…</div>
      : query.isError ? <div className="owner-business-state user-detail-error" role="alert"><p>{getOwnerBusinessHoursErrorMessage(query.error)}</p><button type="button" className="owners-secondary-button" onClick={() => query.refetch()} disabled={query.isFetching}><RefreshCw size={15} /> Reintentar</button></div>
        : <>
          {isCreating && <HourForm id={negocioId} hours={query.data} onCancel={() => setIsCreating(false)} onSaved={() => handleSaved('El horario se registró correctamente.')} />}
          {!query.data.length && !isCreating ? <div className="owner-business-state"><p>Todavía no hay horarios registrados.</p><button type="button" className="owner-create-button" onClick={() => setIsCreating(true)}><Plus size={15} /> Registrar primer horario</button></div>
            : <ul className="owner-hours-list">{query.data.slice().sort((a, b) => days.findIndex(([key]) => key === a.diaSemana) - days.findIndex(([key]) => key === b.diaSemana)).map((hour) => <li className="owner-hours-row" key={hour.horarioId}>
              {editingHour?.horarioId === hour.horarioId ? <HourForm id={`${negocioId}-${hour.horarioId}`} hours={query.data} hour={hour} onCancel={() => setEditingHour(null)} onSaved={() => handleSaved('El horario se actualizó correctamente.')} /> : <><div className="owner-hours-day"><strong>{dayNames[hour.diaSemana] || hour.diaSemana}</strong><span className={hour.cerrado ? 'owner-hours-badge is-closed' : 'owner-hours-badge'}>{hour.cerrado ? 'Cerrado' : `${toTimeInput(hour.horaApertura)} – ${toTimeInput(hour.horaCierre)}`}</span></div><div className="owner-hours-actions"><button type="button" className="owners-secondary-button" onClick={() => { setNotice(''); setEditingHour(hour) }}><Pencil size={14} /> Editar</button><button type="button" className="owners-secondary-button owner-hours-delete" onClick={() => void handleDelete(hour)} disabled={deletingId === hour.horarioId}><Trash2 size={14} /> {deletingId === hour.horarioId ? 'Eliminando…' : 'Eliminar'}</button></div></>}
            </li>)}</ul>}
        </>}
  </section>
}
