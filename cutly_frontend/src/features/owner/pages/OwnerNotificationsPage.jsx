import { useState } from 'react'
import { Bell, Check, CheckCheck, ChevronLeft, ChevronRight, RefreshCw } from 'lucide-react'
import { getOwnerNotificationErrorMessage } from '../api/ownerNotificationsApi.js'
import {
  useMarkAllOwnerNotificationsAsRead,
  useMarkOwnerNotificationAsRead,
  useOwnerNotifications,
  useOwnerUnreadNotificationCount,
} from '../hooks/useOwnerNotifications.js'
import './OwnerNotificationsPage.css'

function formatNotificationDate(value) {
  if (typeof value !== 'string') return 'Fecha no disponible'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'Fecha no disponible'
  return new Intl.DateTimeFormat('es-AR', { dateStyle: 'medium', timeStyle: 'short' }).format(date)
}

export default function OwnerNotificationsPage() {
  const [page, setPage] = useState(0)
  const notificationsQuery = useOwnerNotifications(page)
  const unreadCountQuery = useOwnerUnreadNotificationCount()
  const markReadMutation = useMarkOwnerNotificationAsRead(page)
  const markAllMutation = useMarkAllOwnerNotificationsAsRead()
  const result = notificationsQuery.data
  const notifications = result?.content ?? []
  const unreadCount = unreadCountQuery.data?.cantidad
  const totalPages = result?.totalPages ?? 0
  const isUpdating = markReadMutation.isPending || markAllMutation.isPending
  const mutationError = markReadMutation.error || markAllMutation.error

  return <section className="owner-notifications" aria-labelledby="owner-notifications-title">
    <header className="owner-notifications-heading">
      <div><span className="owner-notifications-eyebrow">MI ESPACIO</span><h1 id="owner-notifications-title">Notificaciones</h1><p>Las novedades importantes de tus negocios, en un solo lugar.</p></div>
      <div className="owner-notifications-actions">
        {unreadCountQuery.isSuccess && <span className="owner-notifications-count" aria-live="polite">{unreadCount} sin leer</span>}
        {unreadCountQuery.isSuccess && unreadCount > 0 && <button type="button" className="owner-notifications-button is-primary" disabled={isUpdating} onClick={() => markAllMutation.mutate()}>
          {markAllMutation.isPending ? <RefreshCw size={14} className="owner-notifications-spinner" /> : <CheckCheck size={15} />}
          {markAllMutation.isPending ? 'Marcando…' : 'Marcar todas como leídas'}
        </button>}
      </div>
    </header>

    {unreadCountQuery.isError && <p className="owner-notifications-error" role="alert">No pudimos actualizar el contador de notificaciones no leídas.</p>}
    {mutationError && <p className="owner-notifications-error" role="alert">{getOwnerNotificationErrorMessage(mutationError)}</p>}
    {notificationsQuery.isPending ? <div className="owner-notifications-state" role="status">Cargando notificaciones…</div>
      : notificationsQuery.isError ? <div className="owner-notifications-state is-error" role="alert"><strong>No pudimos cargar tus notificaciones.</strong><p>{getOwnerNotificationErrorMessage(notificationsQuery.error)}</p><button className="owner-notifications-button" type="button" disabled={notificationsQuery.isFetching} onClick={() => notificationsQuery.refetch()}><RefreshCw size={14} /> Reintentar</button></div>
        : notifications.length === 0 ? <div className="owner-notifications-state is-empty"><Bell size={21} /><strong>No tenés notificaciones</strong><p>Cuando haya novedades, las vas a encontrar acá.</p></div>
          : <>
            {unreadCount === 0 && <p className="owner-notifications-all-read"><Check size={15} /> Todas tus notificaciones están leídas.</p>}
            <ul className="owner-notifications-list" aria-label="Lista de notificaciones">
              {notifications.map((notification) => {
                const unread = notification.leida === false
                const marking = markReadMutation.isPending && String(markReadMutation.variables) === String(notification.notificacionId)
                return <li key={notification.notificacionId} className={`owner-notification-card${unread ? ' is-unread' : ''}`}>
                  <span className="owner-notification-indicator" aria-label={unread ? 'No leída' : 'Leída'} />
                  <div className="owner-notification-copy"><div className="owner-notification-title"><h2>{notification.titulo}</h2>{unread && <span>Nueva</span>}</div>
                    <p>{notification.mensaje}</p><time>{formatNotificationDate(notification.fechaCreacion)}</time>
                  </div>
                  {unread && <button type="button" className="owner-notifications-button owner-notification-read" disabled={isUpdating} aria-label={`Marcar como leída: ${notification.titulo}`} onClick={() => markReadMutation.mutate(notification.notificacionId)}>
                    {marking ? <RefreshCw size={14} className="owner-notifications-spinner" /> : <Check size={15} />}{marking ? 'Marcando…' : 'Marcar leída'}
                  </button>}
                </li>
              })}
            </ul>
            {totalPages > 1 && <nav className="owner-notifications-pagination" aria-label="Páginas de notificaciones">
              <button type="button" className="owner-notifications-button" onClick={() => setPage((current) => Math.max(0, current - 1))} disabled={page === 0 || notificationsQuery.isFetching}><ChevronLeft size={15} /> Anterior</button>
              <span>Página {(result.number ?? page) + 1} de {totalPages}</span>
              <button type="button" className="owner-notifications-button" onClick={() => setPage((current) => current + 1)} disabled={(result.number ?? page) + 1 >= totalPages || notificationsQuery.isFetching}>Siguiente <ChevronRight size={15} /></button>
            </nav>}
          </>}
  </section>
}
