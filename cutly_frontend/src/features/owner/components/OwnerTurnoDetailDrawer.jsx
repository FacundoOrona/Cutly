import { useEffect, useRef } from 'react'
import { CalendarDays, CircleAlert, LoaderCircle, RefreshCw, X } from 'lucide-react'
import { useOwnerTurno } from '../hooks/useOwnerTurnos.js'

const statusLabels = {
  PENDING: 'Pendiente', CONFIRMED: 'Confirmado', IN_PROGRESS: 'En curso',
  COMPLETED: 'Completado', CANCELLED: 'Cancelado', NO_SHOW: 'Ausencia', REJECTED: 'Rechazado',
}

function dateTime(value) {
  if (!value) return '—'
  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) return '—'
  return new Intl.DateTimeFormat('es-AR', { dateStyle: 'full', timeStyle: 'short' }).format(parsed)
}

function money(value) {
  if (value == null || Number.isNaN(Number(value))) return null
  return new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' }).format(Number(value))
}

function getErrorMessage(error) {
  const status = error.response?.status
  const body = error.response?.data
  const message = typeof body === 'string' ? body : body?.mensaje || body?.message
  if (status === 404) return 'No se encontró este turno. Puede haber sido eliminado o ya no estar disponible.'
  if (status === 401) return 'Tu sesión venció. Iniciá sesión nuevamente.'
  if (status === 403) return 'Tu cuenta no tiene acceso a este turno.'
  if (!error.response) return 'No pudimos conectar con el servidor. Revisá tu conexión e intentá de nuevo.'
  if (status >= 500) return 'El servicio no está disponible en este momento. Intentá más tarde.'
  return message || 'No pudimos cargar los detalles del turno.'
}

export default function OwnerTurnoDetailDrawer({ negocioId, turnoId, onClose }) {
  const query = useOwnerTurno({ negocioId, turnoId })
  const closeButtonRef = useRef(null)
  const headingId = `owner-turno-detail-title-${negocioId}-${turnoId}`

  useEffect(() => {
    closeButtonRef.current?.focus()
    function handleKeyDown(event) {
      if (event.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [onClose])

  const turno = query.data
  const clientName = [turno?.clienteNombre, turno?.clienteApellido].filter(Boolean).join(' ')
  const employeeName = [turno?.empleadoNombre, turno?.empleadoApellido].filter(Boolean).join(' ')
  const start = dateTime(turno?.fechaHoraInicio)
  const endTime = turno?.fechaHoraFin ? new Intl.DateTimeFormat('es-AR', { hour: '2-digit', minute: '2-digit' }).format(new Date(turno.fechaHoraFin)) : null
  const price = money(turno?.precio)

  return <div className="owner-turno-drawer-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose() }}>
    <aside className="owner-turno-drawer" role="dialog" aria-modal="true" aria-labelledby={headingId}>
      <header className="owner-turno-drawer-header">
        <div><span className="eyebrow"><span className="eyebrow-dot" /> DETALLE DEL TURNO</span><h2 id={headingId}>Turno</h2></div>
        <button ref={closeButtonRef} type="button" className="icon-button" onClick={onClose} aria-label="Cerrar detalle del turno"><X size={19} /></button>
      </header>
      {query.isPending ? <div className="owner-turno-drawer-state" role="status"><LoaderCircle size={19} className="spin" /> Cargando detalle del turno…</div>
        : query.isError ? <div className="owner-turno-drawer-state owner-turno-drawer-error" role="alert"><CircleAlert size={19} /><p>{getErrorMessage(query.error)}</p>{query.error.response?.status !== 404 && query.error.response?.status !== 400 && query.error.response?.status !== 401 && query.error.response?.status !== 403 && <button type="button" className="owners-secondary-button" onClick={() => query.refetch()}><RefreshCw size={14} /> Reintentar</button>}</div>
          : !turno ? <div className="owner-turno-drawer-state" role="status">No hay datos disponibles para este turno.</div>
            : <div className="owner-turno-drawer-content">
              <div className="owner-turno-detail-date"><CalendarDays size={18} /><div><span>Fecha y horario</span><strong>{start}</strong>{endTime && <small>Finaliza a las {endTime}</small>}</div></div>
              {turno.status && <section className="owner-turno-detail-section"><h3>Estado</h3><span className={`owner-status owner-turno-status owner-turno-status-${String(turno.status).toLowerCase()}`}>{statusLabels[turno.status] || turno.status}</span></section>}
              {(clientName || turno.tipoCliente) && <section className="owner-turno-detail-section"><h3>Cliente</h3><p>{clientName || 'Nombre no disponible'}</p>{turno.tipoCliente && <small>{turno.tipoCliente === 'LOCAL' ? 'Cliente del negocio' : 'Cliente registrado'}</small>}</section>}
              {turno.servicioNombre && <section className="owner-turno-detail-section"><h3>Servicio</h3><p>{turno.servicioNombre}</p></section>}
              <section className="owner-turno-detail-section"><h3>Empleado asignado</h3><p>{employeeName || (turno.tipoResponsable === 'OWNER' ? ( [turno.responsableNombre, turno.responsableApellido].filter(Boolean).join(' ') || 'Propietario' ) : 'Sin empleado asignado')}</p></section>
              {price && <section className="owner-turno-detail-section"><h3>Precio</h3><p>{price}</p></section>}
              {turno.observaciones && <section className="owner-turno-detail-section"><h3>Observaciones</h3><p className="owner-turno-detail-observations">{turno.observaciones}</p></section>}
            </div>}
      <footer className="owner-turno-drawer-footer"><button type="button" className="owners-secondary-button" onClick={onClose}>Volver a la agenda</button></footer>
    </aside>
  </div>
}
