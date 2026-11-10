import { AlertCircle, ArrowLeft, Clock3, LoaderCircle, Mail, MapPin, Phone, Users } from 'lucide-react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { getClientBusinessEmployeesErrorMessage, getClientBusinessErrorMessage, getClientBusinessServicesErrorMessage } from '../api/clientApi.js'
import { useClientBusiness, useClientBusinessEmployees, useClientBusinessServices } from '../hooks/useClientBusinessDetail.js'
import './ClientBusinessDetailPage.css'

const currencyFormatter = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' })
const validId = (id) => typeof id === 'string' && /^[1-9]\d*$/.test(id) && Number.isSafeInteger(Number(id))
const present = (value) => typeof value === 'string' && value.trim() ? value.trim() : ''

function SectionState({ error, onRetry, children }) {
  if (error) return <div className="client-detail-inline-error" role="alert"><AlertCircle size={17} /><span>{error}</span><button type="button" onClick={onRetry}>Reintentar</button></div>
  return children
}

function SectionLoading({ label }) {
  return <div className="client-detail-section-loading"><LoaderCircle size={17} aria-hidden="true" /> Cargando {label}…</div>
}

export default function ClientBusinessDetailPage() {
  const { negocioId } = useParams()
  const location = useLocation()
  const catalogState = location.state?.catalogState
  const backState = catalogState ? { catalogState } : null
  const businessQuery = useClientBusiness(negocioId)
  const business = businessQuery.data
  const sectionsEnabled = validId(negocioId) && Boolean(business) && !businessQuery.isError
  const servicesQuery = useClientBusinessServices(negocioId, sectionsEnabled)
  const employeesQuery = useClientBusinessEmployees(negocioId, sectionsEnabled)

  if (!validId(negocioId)) {
    return <DetailMessage title="Negocio no disponible" message="El identificador del negocio no es válido." backState={backState} />
  }
  if (businessQuery.isPending) {
    return <div className="client-detail-loading"><LoaderCircle size={22} aria-hidden="true" /><span>Cargando negocio…</span></div>
  }
  if (businessQuery.isError) {
    return <DetailMessage title="No pudimos cargar este negocio" message={getClientBusinessErrorMessage(businessQuery.error)} backState={backState} onRetry={() => businessQuery.refetch()} />
  }

  const address = [present(business.calle), present(business.numero)].filter(Boolean).join(' ')
  const locality = [present(business.ciudad), present(business.provincia)].filter(Boolean).join(', ')
  const fullAddress = [address, locality, present(business.codigoPostal)].filter(Boolean).join(' · ')
  const phone = present(business.telefono)
  const email = present(business.email)

  return (
    <section className="client-business-detail">
      <Link className="client-detail-back" to="/client/explorar" state={backState?.catalogState}><ArrowLeft size={16} aria-hidden="true" /> Volver a explorar</Link>
      <header className="client-detail-hero">
        <div className="client-detail-mark" aria-hidden="true">{present(business.nombre).charAt(0).toUpperCase() || 'C'}</div>
        <div className="client-detail-hero-copy">
          <span className="client-detail-eyebrow">NEGOCIO CUTLY</span>
          <h1>{present(business.nombre) || 'Negocio'}</h1>
          {present(business.descripcion) && <p>{present(business.descripcion)}</p>}
          {fullAddress && <div className="client-detail-location"><MapPin size={16} aria-hidden="true" /><span>{fullAddress}</span></div>}
        </div>
      </header>

      {(phone || email) && (
        <section className="client-detail-contact" aria-labelledby="client-contact-title">
          <h2 id="client-contact-title">Contacto</h2>
          <div className="client-detail-contact-items">
            {phone && <a href={`tel:${phone}`}><Phone size={16} aria-hidden="true" /><span>{phone}</span></a>}
            {email && <a href={`mailto:${email}`}><Mail size={16} aria-hidden="true" /><span>{email}</span></a>}
          </div>
        </section>
      )}

      <section className="client-detail-section" aria-labelledby="client-services-title">
        <div className="client-detail-section-heading"><span className="client-detail-section-icon"><Clock3 size={18} aria-hidden="true" /></span><div><h2 id="client-services-title">Servicios</h2><p>Opciones disponibles en este negocio</p></div></div>
        {servicesQuery.isPending ? <SectionLoading label="los servicios" /> : (
          <SectionState error={servicesQuery.isError ? getClientBusinessServicesErrorMessage(servicesQuery.error) : null} onRetry={() => servicesQuery.refetch()}>
            {servicesQuery.data?.length ? <div className="client-detail-services">{servicesQuery.data.map((service) => (
              <article className="client-detail-service" key={service.servicioId}>
                <div className="client-detail-service-copy"><h3>{present(service.nombre) || 'Servicio'}</h3>{present(service.descripcion) && <p>{present(service.descripcion)}</p>}</div>
                <div className="client-detail-service-meta"><strong>{service.precio !== null && service.precio !== undefined && Number.isFinite(Number(service.precio)) ? currencyFormatter.format(Number(service.precio)) : 'Precio no disponible'}</strong>{service.duracionMinutos !== null && service.duracionMinutos !== undefined && <span>{service.duracionMinutos} min</span>}</div>
              </article>
            ))}</div> : <p className="client-detail-empty">Este negocio todavía no tiene servicios activos para mostrar.</p>}
          </SectionState>
        )}
      </section>

      <section className="client-detail-section" aria-labelledby="client-team-title">
        <div className="client-detail-section-heading"><span className="client-detail-section-icon"><Users size={18} aria-hidden="true" /></span><div><h2 id="client-team-title">Nuestro equipo</h2><p>Personas activas en este negocio</p></div></div>
        {employeesQuery.isPending ? <SectionLoading label="el equipo" /> : (
          <SectionState error={employeesQuery.isError ? getClientBusinessEmployeesErrorMessage(employeesQuery.error) : null} onRetry={() => employeesQuery.refetch()}>
            {employeesQuery.data?.length ? <ul className="client-detail-team">{employeesQuery.data.map((employee) => (
              <li key={employee.empleadoId}><span aria-hidden="true">{present(employee.nombre).charAt(0).toUpperCase() || 'C'}</span><strong>{[present(employee.nombre), present(employee.apellido)].filter(Boolean).join(' ') || 'Integrante del equipo'}</strong></li>
            ))}</ul> : <p className="client-detail-empty">No hay integrantes activos para mostrar en este momento.</p>}
          </SectionState>
        )}
      </section>
    </section>
  )
}

function DetailMessage({ title, message, backState, onRetry }) {
  return (
    <section className="client-detail-message" role="status">
      <span><AlertCircle size={21} aria-hidden="true" /></span>
      <h1>{title}</h1>
      <p>{message}</p>
      {onRetry && <button type="button" onClick={onRetry}>Reintentar</button>}
      <Link className="client-detail-back" to="/client/explorar" state={backState?.catalogState}><ArrowLeft size={16} aria-hidden="true" /> Volver a explorar</Link>
    </section>
  )
}
