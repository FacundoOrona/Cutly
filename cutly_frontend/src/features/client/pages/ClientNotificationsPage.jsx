import { useState } from 'react'
import { Bell, Check, CheckCheck, ChevronLeft, ChevronRight, LoaderCircle, RefreshCw } from 'lucide-react'
import { getClientNotificationErrorMessage } from '../api/clientApi.js'
import {
  useClientNotifications,
  useClientUnreadNotificationCount,
  useMarkAllClientNotificationsAsRead,
  useMarkClientNotificationAsRead,
} from '../hooks/useClientNotifications.js'
import './ClientNotificationsPage.css'

function notificationDate(value) {
  const date = typeof value === 'string' ? new Date(value) : null
  if (!date || Number.isNaN(date.getTime())) return 'Fecha no disponible'
  return new Intl.DateTimeFormat('es-AR', { dateStyle: 'medium', timeStyle: 'short' }).format(date)
}

export default function ClientNotificationsPage() {
  const [page, setPage] = useState(0)
  const notificationsQuery = useClientNotifications(page)
  const unreadCountQuery = useClientUnreadNotificationCount()
  const markReadMutation = useMarkClientNotificationAsRead(page)
  const markAllMutation = useMarkAllClientNotificationsAsRead()
  const result = notificationsQuery.data
  const notifications = result?.content ?? []
  const totalPages = result?.totalPages ?? 0
  const unreadCount = unreadCountQuery.data?.cantidad
  const isUpdating = markReadMutation.isPending || markAllMutation.isPending
  const mutationError = markReadMutation.error || markAllMutation.error

  return <section className="client-notifications-page" aria-labelledby="client-notifications-title">
    <header className="client-notifications-heading">
      <div><span className="client-profile-eyebrow">MI ESPACIO</span><h1 id="client-notifications-title">Notificaciones</h1><p>Las novedades de tus turnos y negocios.</p></div>
      <div className="client-notifications-heading-actions">
        {unreadCountQuery.isSuccess && <span className="client-notifications-count" aria-live="polite">{unreadCount} sin leer</span>}
        {unreadCountQuery.isSuccess && unreadCount > 0 && <button type="button" className="client-notifications-button is-primary" onClick={() => markAllMutation.mutate()} disabled={isUpdating}>
          {markAllMutation.isPending ? <RefreshCw size={14} className="client-notifications-spinner" /> : <CheckCheck size={15} />}
          Marcar todas como leídas
        </button>}
      </div>
    </header>

    {unreadCountQuery.isError && <p className="client-notifications-error" role="alert">No pudimos actualizar el contador de notificaciones no leídas.</p>}
    {notificationsQuery.isPending ? <div className="client-notifications-state" role="status"><LoaderCircle size={17} className="client-notifications-spinner" /> Cargando notificaciones…</div>
      : notificationsQuery.isError ? <div className="client-notifications-state is-error" role="alert"><strong>No pudimos cargar tus notificaciones.</strong><p>{getClientNotificationErrorMessage(notificationsQuery.error)}</p><button type="button" className="client-notifications-button" onClick={() => notificationsQuery.refetch()} disabled={notificationsQuery.isFetching}>Reintentar</button></div>
        : notifications.length === 0 ? <div className="client-notifications-state client-notifications-empty"><Bell size={21} /><strong>No tenés notificaciones</strong><p>Cuando haya novedades, las vas a encontrar acá.</p></div>
          : <>
            {mutationError && <p className="client-notifications-error" role="alert">{getClientNotificationErrorMessage(mutationError)}</p>}
            <ul className="client-notifications-list" aria-label="Lista de notificaciones">
              {notifications.map((notification) => {
                const unread = notification.leida === false
                const marking = markReadMutation.isPending && String(markReadMutation.variables) === String(notification.notificacionId)
                return <li className={`client-notification-card${unread ? ' is-unread' : ''}`} key={notification.notificacionId}>
                  <span className="client-notification-indicator" aria-label={unread ? 'No leída' : 'Leída'} />
                  <div className="client-notification-copy"><div className="client-notification-title"><h2>{notification.titulo}</h2>{unread && <span>Nueva</span>}</div>
                    <p>{notification.mensaje}</p><time>{notificationDate(notification.fechaCreacion)}</time>
                  </div>
                  {unread && <button type="button" className="client-notifications-button client-notification-read" onClick={() => markReadMutation.mutate(notification.notificacionId)} disabled={isUpdating} aria-label={`Marcar como leída: ${notification.titulo}`}>
                    {marking ? <RefreshCw size={14} className="client-notifications-spinner" /> : <Check size={15} />}{marking ? 'Marcando…' : 'Marcar leída'}
                  </button>}
                </li>
              })}
            </ul>
            {totalPages > 1 && <nav className="client-notifications-pagination" aria-label="Páginas de notificaciones">
              <button type="button" className="client-notifications-button" onClick={() => setPage((current) => Math.max(0, current - 1))} disabled={page === 0 || notificationsQuery.isFetching}><ChevronLeft size={15} /> Anterior</button>
              <span>Página {(result.number ?? page) + 1} de {totalPages}</span>
              <button type="button" className="client-notifications-button" onClick={() => setPage((current) => current + 1)} disabled={(result.number ?? page) + 1 >= totalPages || notificationsQuery.isFetching}>Siguiente <ChevronRight size={15} /></button>
            </nav>}
          </>}
  </section>
}
