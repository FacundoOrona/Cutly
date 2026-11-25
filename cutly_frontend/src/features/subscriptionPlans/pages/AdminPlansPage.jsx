import { useState } from 'react'
import { CheckCircle2, LoaderCircle, PackageCheck, Pencil, Plus, RefreshCw, X } from 'lucide-react'
import { getAdminPlanErrorMessage } from '../api/adminPlansApi.js'
import {
  useAdminPlan,
  useAdminPlans,
  useChangeAdminPlanStatus,
  useCreateAdminPlan,
  useUpdateAdminPlan,
} from '../hooks/useAdminPlans.js'
import './adminPlans.css'

const money = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' })

function isTrialPlan(plan) {
  const normalizedName = plan.nombre?.normalize('NFC').trim().replace(/\s+/gu, ' ').toLocaleLowerCase('es-AR')
  return plan.tipo === 'TRIAL' || normalizedName === 'prueba gratuita'
}

function PlanForm({ plan, pending, onSave, onClose, onConflict }) {
  const [nombre, setNombre] = useState(plan?.nombre ?? '')
  const [descripcion, setDescripcion] = useState(plan?.descripcion ?? '')
  const [precioMensual, setPrecioMensual] = useState(plan?.precioMensual == null ? '' : String(plan.precioMensual))
  const [maxNegocios, setMaxNegocios] = useState(plan?.maxNegocios == null ? '1' : String(plan.maxNegocios))
  const [errorMessage, setErrorMessage] = useState('')
  const [saving, setSaving] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setErrorMessage('')
    if (!nombre.trim()) {
      setErrorMessage('El nombre del plan es obligatorio.')
      return
    }
    if (!precioMensual || Number(precioMensual) <= 0) {
      setErrorMessage('El precio mensual debe ser mayor que cero.')
      return
    }
    if (!/^\d+$/.test(maxNegocios) || Number(maxNegocios) < 1) {
      setErrorMessage('La cantidad máxima de negocios debe ser un entero mayor que cero.')
      return
    }
    if (Number(precioMensual) > 99999999.99 || !/^\d{1,8}(?:\.\d{1,2})?$/.test(precioMensual)) {
      setErrorMessage('El precio admite hasta 8 enteros y 2 decimales.')
      return
    }

    setSaving(true)
    try {
      await onSave({ nombre, descripcion: descripcion || null, precioMensual, maxNegocios: Number(maxNegocios) })
    } catch (error) {
      setErrorMessage(getAdminPlanErrorMessage(error))
      if (error.response?.status === 409) onConflict()
    } finally {
      setSaving(false)
    }
  }

  const isPending = pending || saving
  return <form className="admin-plan-form" onSubmit={handleSubmit}>
    {errorMessage && <p className="admin-plan-form-error" role="alert">{errorMessage}</p>}
    <label className="owner-form-field"><span>Nombre <b>*</b></span><input required value={nombre} onChange={(event) => setNombre(event.target.value)} disabled={isPending} autoFocus /><small>Entre 1 y 50 caracteres tras la normalización del servidor. Los nombres duplicados se rechazan sin distinguir mayúsculas y espacios.</small></label>
    <label className="owner-form-field"><span>Descripción</span><textarea maxLength={500} value={descripcion} onChange={(event) => setDescripcion(event.target.value)} disabled={isPending} /><small>{descripcion.length}/500</small></label>
    <label className="owner-form-field"><span>Precio mensual (ARS) <b>*</b></span><input required type="number" min="0.01" max="99999999.99" step="0.01" value={precioMensual} onChange={(event) => setPrecioMensual(event.target.value)} disabled={isPending} /><small>Mayor que cero; hasta 8 enteros y 2 decimales.</small></label>
    <label className="owner-form-field"><span>Negocios permitidos <b>*</b></span><input required type="number" min="1" step="1" value={maxNegocios} onChange={(event) => setMaxNegocios(event.target.value)} disabled={isPending} /></label>
    {plan && <p className="admin-plan-version-note">Versión usada para guardar: {plan.version}</p>}
    <div className="admin-plan-dialog-actions"><button type="button" className="owners-secondary-button" onClick={onClose} disabled={isPending}>Cancelar</button><button type="submit" className="owners-secondary-button" disabled={isPending}>{isPending && <LoaderCircle size={14} className="spin" />}{plan ? 'Guardar cambios' : 'Crear plan'}</button></div>
  </form>
}

export default function AdminPlansPage() {
  const plansQuery = useAdminPlans()
  const createMutation = useCreateAdminPlan()
  const updateMutation = useUpdateAdminPlan()
  const statusMutation = useChangeAdminPlanStatus()
  const [dialog, setDialog] = useState(null)
  const [notice, setNotice] = useState(null)
  const [statusError, setStatusError] = useState('')
  const editingPlanQuery = useAdminPlan(dialog?.type === 'edit' ? dialog.id : null, dialog?.type === 'edit')
  const plans = plansQuery.data ?? []
  const activePlans = plans.filter(plan => plan.activo && plan.tipo === 'COMERCIAL').length
  const inactivePlans = plans.filter(plan => !plan.activo && plan.tipo === 'COMERCIAL').length
  const isMutating = createMutation.isPending || updateMutation.isPending || statusMutation.isPending

  function closeDialog() {
    if (!isMutating) setDialog(null)
  }

  async function savePlan(values) {
    setNotice(null)
    if (dialog.type === 'create') {
      const created = await createMutation.mutateAsync(values)
      setNotice({ type: 'success', text: `El plan «${created.nombre}» se creó inactivo. Activá el plan para ofrecerlo al asignar suscripciones.` })
    } else {
      await updateMutation.mutateAsync({ id: editingPlanQuery.data.planId, ...values, versionEsperada: editingPlanQuery.data.version })
      setNotice({ type: 'success', text: 'Los datos del plan se actualizaron correctamente.' })
    }
    setDialog(null)
  }

  async function togglePlan(plan) {
    const active = !plan.activo
    const operation = active ? 'activar' : 'desactivar'
    if (!window.confirm(`¿Querés ${operation} el plan «${plan.nombre}»?`)) return
    setStatusError('')
    setNotice(null)
    try {
      await statusMutation.mutateAsync({ id: plan.planId, active, versionEsperada: plan.version })
      setNotice({ type: 'success', text: `El plan «${plan.nombre}» se ${active ? 'activó' : 'desactivó'} correctamente.` })
    } catch (error) {
      setStatusError(getAdminPlanErrorMessage(error))
      if (error.response?.status === 409) plansQuery.refetch()
    }
  }

  return <>
    <span className="eyebrow"><span className="eyebrow-dot" /> ADMINISTRACIÓN</span>
    <div className="owners-heading admin-plans-heading"><div><h1>Planes comerciales</h1><p className="owners-description">Administrá los planes y la capacidad de negocios de cada OWNER.</p></div><button type="button" className="owner-create-button" onClick={() => { setNotice(null); setDialog({ type: 'create' }) }} disabled={isMutating}><Plus size={15} /> Crear plan</button></div>
    {notice && <div className={`owner-created-notice${notice.type === 'error' ? ' owner-status-error' : ''}`} role={notice.type === 'error' ? 'alert' : 'status'}>{notice.type === 'success' && <CheckCircle2 size={17} />}{notice.text}<button type="button" className="icon-button" onClick={() => setNotice(null)} aria-label="Cerrar mensaje"><X size={16} /></button></div>}
    {statusError && <div className="owner-created-notice owner-status-error" role="alert">{statusError}<button type="button" className="owners-secondary-button" onClick={() => plansQuery.refetch()}><RefreshCw size={14} /> Actualizar lista</button><button type="button" className="icon-button" onClick={() => setStatusError('')} aria-label="Cerrar mensaje"><X size={16} /></button></div>}
    <section className="admin-plans-list" aria-label="Lista de planes" aria-busy={plansQuery.isFetching}>
      <div className="admin-plans-overview"><div><span>Planes comerciales activos</span><strong>{plansQuery.isPending ? '—' : activePlans}</strong></div><div><span>Inactivos</span><strong>{plansQuery.isPending ? '—' : inactivePlans}</strong></div><p>Los planes activos pueden asignarse a nuevos OWNER.</p></div>
      {plansQuery.isPending ? <div className="owners-message" role="status"><LoaderCircle size={19} className="spin" /> Cargando planes…</div>
        : plansQuery.isError && !plansQuery.data ? <div className="owners-message owners-error" role="alert"><p>{getAdminPlanErrorMessage(plansQuery.error)}</p><button type="button" className="owners-secondary-button" onClick={() => plansQuery.refetch()}><RefreshCw size={15} /> Reintentar</button></div>
          : <>
            {plansQuery.isError && <p className="owners-inline-error" role="alert">No se pudo actualizar la lista. {getAdminPlanErrorMessage(plansQuery.error)} <button type="button" className="owners-secondary-button" onClick={() => plansQuery.refetch()}>Reintentar</button></p>}
            {plans.length === 0 ? <div className="owners-message admin-plans-empty"><PackageCheck size={22} /><p>Todavía no hay planes registrados.</p><button type="button" className="owners-secondary-button" onClick={() => setDialog({ type: 'create' })}><Plus size={14} /> Crear primer plan</button></div>
              : <div className="admin-plans-table-wrap"><table className="admin-plans-table"><thead><tr><th>Plan</th><th>Tipo</th><th>Precio mensual</th><th>Negocios</th><th>Estado y asignación</th><th>Acciones</th></tr></thead><tbody>{plans.map((plan) => {
              const trial = isTrialPlan(plan)
              const commercial = plan.tipo === 'COMERCIAL' && !trial
              return <tr key={plan.planId}>
                <td data-label="Plan"><strong>{plan.nombre}</strong><span>{plan.descripcion || 'Sin descripción'}</span></td>
                <td data-label="Tipo">{plan.tipo === 'COMERCIAL' ? 'Comercial' : trial ? 'Prueba' : plan.tipo || 'Sin clasificar'}</td>
                <td data-label="Precio mensual">{money.format(Number(plan.precioMensual))}</td>
                <td data-label="Negocios permitidos">{plan.maxNegocios ?? '—'}</td>
                <td data-label="Estado y asignación"><span className={`admin-plan-state${plan.activo ? ' is-active' : ''}`}>{plan.activo ? 'Activo' : 'Inactivo'}</span><small>{commercial && plan.activo ? 'Disponible para asignar' : trial ? 'Plan especial del sistema' : 'No disponible para asignar'}</small></td>
                <td data-label="Acciones"><div className="admin-plan-actions">{commercial ? <><button type="button" className="owners-secondary-button" onClick={() => { setNotice(null); setDialog({ type: 'edit', id: plan.planId }) }} disabled={isMutating}><Pencil size={14} /> Editar</button><button type="button" className="owners-secondary-button" onClick={() => togglePlan(plan)} disabled={isMutating}>{plan.activo ? 'Desactivar' : 'Activar'}</button></> : <span className="admin-plan-protected-note">No modificable</span>}</div></td>
              </tr>
              })}</tbody></table></div>}
          </>}
    </section>

    {dialog && <div className="admin-plan-dialog-backdrop"><section className="admin-plan-dialog" role="dialog" aria-modal="true" aria-labelledby="admin-plan-dialog-title">
      <div className="admin-plan-dialog-heading"><div><h2 id="admin-plan-dialog-title">{dialog.type === 'create' ? 'Crear plan comercial' : 'Editar plan comercial'}</h2><p>{dialog.type === 'create' ? 'Los planes nuevos se crean inactivos.' : 'La versión actual del plan se usará para validar los cambios.'}</p></div><button type="button" className="icon-button" onClick={closeDialog} disabled={isMutating} aria-label="Cerrar diálogo"><X size={17} /></button></div>
      {dialog.type === 'create' ? <PlanForm key="create-plan" pending={createMutation.isPending} onSave={savePlan} onClose={closeDialog} onConflict={() => plansQuery.refetch()} />
        : editingPlanQuery.isPending ? <div className="owners-message" role="status"><LoaderCircle size={18} className="spin" /> Cargando versión actual del plan…</div>
          : editingPlanQuery.isError ? <div className="owners-message owners-error" role="alert"><p>{getAdminPlanErrorMessage(editingPlanQuery.error)}</p><button type="button" className="owners-secondary-button" onClick={() => editingPlanQuery.refetch()}><RefreshCw size={14} /> Reintentar</button></div>
            : isTrialPlan(editingPlanQuery.data) || editingPlanQuery.data.tipo !== 'COMERCIAL' ? <div className="admin-plan-protected"><p>Este plan es especial y no se puede editar como plan comercial.</p><button type="button" className="owners-secondary-button" onClick={closeDialog}>Cerrar</button></div>
              : <PlanForm key={`edit-plan-${editingPlanQuery.data.planId}`} plan={editingPlanQuery.data} pending={updateMutation.isPending} onSave={savePlan} onClose={closeDialog} onConflict={() => editingPlanQuery.refetch()} />}
    </section></div>}
  </>
}
