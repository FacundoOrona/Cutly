import { useState } from 'react'
import { Bell, Check, CheckCheck, ChevronLeft, ChevronRight, RefreshCw } from 'lucide-react'
import { getEmployeeNotificationErrorMessage } from '../api/employeeApi.js'
import {
  useEmployeeNotifications,
  useEmployeeUnreadNotificationCount,
  useMarkAllEmployeeNotificationsAsRead,
  useMarkEmployeeNotificationAsRead,
} from '../hooks/useEmployeeNotifications.js'
import './EmployeeNotificationsPage.css'

function formatNotificationDate(value) {
  if (typeof value !== 'string') return 'Fecha no disponible'
  const match = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2})(?:\.(\d+))?)?$/.exec(value)
  if (!match) return 'Fecha no disponible'

  const [, year, month, day, hour, minute, second = '0', fraction = '0'] = match
  const date = new Date(Number(year), Number(month) - 1, Number(day), Number(hour), Number(minute), Number(second), Number(`0.${fraction}`) * 1000)
  if (Number(hour) > 23 || Number(minute) > 59 || Number(second) > 59
    || date.getFullYear() !== Number(year) || date.getMonth() !== Number(month) - 1 || date.getDate() !== Number(day)) {
    return 'Fecha no disponible'
  }
  return new Intl.DateTimeFormat('es-AR', { dateStyle: 'medium', timeStyle: 'short' }).format(date)
}

function NotificationsSkeleton() {
  return <div className="employee-notifications-skeleton" role="status" aria-label="Cargando notificaciones">
    <span /><span /><span />
  </div>
}

export default function EmployeeNotificationsPage() {
  const [page, setPage] = useState(0)
  const notificationsQuery = useEmployeeNotifications(page)
  const unreadCountQuery = useEmployeeUnreadNotificationCount()
  const markReadMutation = useMarkEmployeeNotificationAsRead(page)
  const markAllMutation = useMarkAllEmployeeNotificationsAsRead()
  const result = notificationsQuery.data
  const notifications = result?.content ?? []
  const unreadCount = unreadCountQuery.data?.cantidad
  const totalPages = result?.totalPages ?? 0
  const isUpdating = markReadMutation.isPending || markAllMutation.isPending
  const mutationError = markReadMutation.error || markAllMutation.error

  return <section className="employee-notifications-page" aria-labelledby="employee-notifications-title">
    <header className="employee-notifications-heading">
      <div>
        <span className="employee-section-kicker">MI ESPACIO</span>
        <h1 id="employee-notifications-title">Notificaciones</h1>
        <p>Las novedades importantes de tu espacio, en un solo lugar.</p>
      </div>
      <div className="employee-notifications-header-actions">
        {unreadCountQuery.isSuccess && <span className="employee-notifications-count" aria-live="polite">
          {unreadCount} sin leer
        </span>}
        {unreadCountQuery.isSuccess && unreadCount > 0 && <button type="button" className="employee-action-button is-primary" disabled={isUpdating}
          onClick={() => markAllMutation.mutate()}>
          {markAllMutation.isPending ? <RefreshCw size={14} className="employee-action-spinner" /> : <CheckCheck size={15} />}
          {markAllMutation.isPending ? 'Marcando…' : 'Marcar todas como leídas'}
        </button>}
        <span className="employee-notifications-heading-icon"><Bell size={21} /></span>
      </div>
    </header>

    {notificationsQuery.isPending && <NotificationsSkeleton />}

    {notificationsQuery.isError && <div className="employee-notifications-error" role="alert">
      <strong>No pudimos cargar tus notificaciones</strong>
      <p>{getEmployeeNotificationErrorMessage(notificationsQuery.error)}</p>
      <button type="button" className="employee-action-button" onClick={() => notificationsQuery.refetch()} disabled={notificationsQuery.isFetching}>
        <RefreshCw size={14} className={notificationsQuery.isFetching ? 'employee-action-spinner' : ''} />
        {notificationsQuery.isFetching ? 'Reintentando…' : 'Reintentar'}
      </button>
    </div>}

    {mutationError && <p className="employee-notifications-mutation-error" role="alert">{getEmployeeNotificationErrorMessage(mutationError)}</p>}

    {unreadCountQuery.isError && <p className="employee-notifications-mutation-error" role="alert">No pudimos actualizar el contador de notificaciones no leídas.</p>}

    {notificationsQuery.isSuccess && notifications.length === 0 && <div className="employee-empty-state employee-notifications-empty">
      <Bell size={21} /><strong>No tenés notificaciones</strong><p>Cuando haya novedades, las vas a encontrar acá.</p>
    </div>}

    {notificationsQuery.isSuccess && notifications.length > 0 && <>
      {unreadCount === 0 && <p className="employee-notifications-all-read"><Check size={15} /> Todas tus notificaciones están leídas.</p>}
      <ul className="employee-notifications-list" aria-label="Lista de notificaciones">
        {notifications.map((notification) => {
          const isUnread = notification.leida === false
          const isMarkingThis = markReadMutation.isPending
            && String(markReadMutation.variables) === String(notification.notificacionId)
          return <li key={notification.notificacionId} className={`employee-notification-card${isUnread ? ' is-unread' : ''}`}>
            <span className="employee-notification-indicator" aria-label={isUnread ? 'No leída' : 'Leída'} />
            <div className="employee-notification-copy">
              <div className="employee-notification-title-row">
                <h2>{notification.titulo}</h2>
                {isUnread && <span className="employee-notification-unread-label">Nueva</span>}
              </div>
              <p>{notification.mensaje}</p>
              <time>{formatNotificationDate(notification.fechaCreacion)}</time>
            </div>
            {isUnread && <button type="button" className="employee-action-button employee-notification-action" disabled={isUpdating}
              aria-label={`Marcar como leída: ${notification.titulo}`} onClick={() => markReadMutation.mutate(notification.notificacionId)}>
              {isMarkingThis ? <RefreshCw size={14} className="employee-action-spinner" /> : <Check size={15} />}
              {isMarkingThis ? 'Marcando…' : 'Marcar leída'}
            </button>}
          </li>
        })}
      </ul>
      {totalPages > 1 && <nav className="employee-notifications-pagination" aria-label="Páginas de notificaciones">
        <button type="button" className="employee-action-button" onClick={() => setPage((current) => Math.max(0, current - 1))} disabled={page === 0 || notificationsQuery.isFetching}>
          <ChevronLeft size={15} /> Anterior
        </button>
        <span>Página {(result.number ?? page) + 1} de {totalPages}</span>
        <button type="button" className="employee-action-button" onClick={() => setPage((current) => current + 1)} disabled={(result.number ?? page) + 1 >= totalPages || notificationsQuery.isFetching}>
          Siguiente <ChevronRight size={15} />
        </button>
      </nav>}
    </>}
  </section>
}
