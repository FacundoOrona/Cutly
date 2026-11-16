import { useEffect, useRef, useState } from 'react'
import { AlertCircle, ArrowDown, CalendarDays, Check, Clock3, LoaderCircle, Sparkles, UserRound, X } from 'lucide-react'
import { useClientAvailability, useClientEmployeeAvailability } from '../hooks/useClientAvailability.js'
import './ClientBookingSection.css'

const currencyFormatter = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' })
const present = (value) => typeof value === 'string' && value.trim() ? value.trim() : ''

function localDateString(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function displayDate(value, options = { weekday: 'long', day: 'numeric', month: 'long' }) {
  const [year, month, day] = value.split('-').map(Number)
  return new Intl.DateTimeFormat('es-AR', options).format(new Date(year, month - 1, day))
}

function errorMessage(error) {
  return error.response?.data?.mensaje || 'No pudimos consultar los horarios en este momento. Intentá nuevamente.'
}

export default function ClientBookingSection({ negocioId, servicesQuery, employeesQuery }) {
  const today = localDateString(new Date())
  const [servicioId, setServicioId] = useState('')
  const [empleadoId, setEmpleadoId] = useState('any')
  const [fecha, setFecha] = useState(today)
  const [slotSelection, setSlotSelection] = useState(null)
  const [continued, setContinued] = useState(false)
  const [isExpanded, setIsExpanded] = useState(false)
  const sectionRef = useRef(null)
  const pendingScrollRef = useRef(false)

  useEffect(() => {
    if (!isExpanded || !window.matchMedia('(prefers-reduced-motion: reduce)').matches) return undefined

    let secondFrame
    const firstFrame = requestAnimationFrame(() => {
      secondFrame = requestAnimationFrame(() => {
        if (!pendingScrollRef.current) return
        pendingScrollRef.current = false
        sectionRef.current?.scrollIntoView({ behavior: 'auto', block: 'start' })
      })
    })

    return () => {
      cancelAnimationFrame(firstFrame)
      if (secondFrame !== undefined) cancelAnimationFrame(secondFrame)
    }
  }, [isExpanded])

  const expandBooking = () => {
    pendingScrollRef.current = true
    setIsExpanded(true)
  }

  const collapseBooking = () => {
    pendingScrollRef.current = false
    setIsExpanded(false)
  }

  const handleFormTransitionEnd = (event) => {
    if (event.target !== event.currentTarget || event.propertyName !== 'grid-template-rows'
      || !isExpanded || !pendingScrollRef.current) return
    pendingScrollRef.current = false
    sectionRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }

  const services = servicesQuery.data || []
  const employees = employeesQuery.data || []
  const selectedService = services.find((service) => String(service.servicioId) === servicioId)
  const selectionKey = `${negocioId}:${servicioId}:${empleadoId}:${fecha}`
  const selectedSlot = slotSelection?.selectionKey === selectionKey ? slotSelection.slot : null

  const generalAvailability = useClientAvailability({
    negocioId,
    servicioId,
    fecha,
  }, {
    enabled: Boolean(selectedService && fecha && empleadoId === 'any'),
  })
  const employeeAvailability = useClientEmployeeAvailability({
    negocioId,
    empleadoId,
    servicioId,
    fecha,
  }, {
    enabled: Boolean(selectedService && fecha && empleadoId !== 'any'),
  })
  const availability = empleadoId === 'any' ? generalAvailability : employeeAvailability

  const clearSlot = () => {
    setSlotSelection(null)
    setContinued(false)
  }

  const assignedEmployeeId = selectedSlot?.empleadoId
  const assignedEmployee = employees.find((employee) => String(employee.empleadoId) === String(assignedEmployeeId))
  const assignedEmployeeName = assignedEmployee
    ? [present(assignedEmployee.nombre), present(assignedEmployee.apellido)].filter(Boolean).join(' ')
    : ''

  return (
    <section ref={sectionRef} className={`client-booking${isExpanded ? ' is-expanded' : ''}`} aria-labelledby="client-booking-title">
      <div className="client-booking-heading">
        <span className="client-booking-heading-icon"><CalendarDays size={19} aria-hidden="true" /></span>
        <div><span className="client-booking-eyebrow">RESERVÁ TU VISITA</span><h2 id="client-booking-title">{isExpanded ? 'Elegí tu turno' : '¿Querés reservar un turno?'}</h2>{isExpanded && <p>Seleccioná el servicio, profesional y horario que te queden mejor.</p>}</div>
      </div>

      {!isExpanded ? (
        <button className="client-booking-invite" type="button" aria-expanded={false} aria-controls="client-booking-form" onClick={expandBooking}>
          <span>Reservar un turno</span><ArrowDown size={17} aria-hidden="true" />
        </button>
      ) : (
        <button className="client-booking-collapse" type="button" aria-expanded={true} aria-controls="client-booking-form" onClick={collapseBooking}>
          <X size={15} aria-hidden="true" /><span>Ocultar reserva</span>
        </button>
      )}

      <div className={`client-booking-form-panel${isExpanded ? ' is-open' : ''}`} id="client-booking-form" aria-hidden={!isExpanded} inert={!isExpanded} onTransitionEnd={handleFormTransitionEnd}>
        <div className="client-booking-form-inner">
      {servicesQuery.isPending ? <div className="client-booking-state"><LoaderCircle size={18} aria-hidden="true" /> Cargando servicios…</div> : servicesQuery.isError ? (
        <div className="client-booking-error" role="alert"><AlertCircle size={17} /><span>No pudimos cargar los servicios para reservar.</span><button type="button" onClick={() => servicesQuery.refetch()}>Reintentar</button></div>
      ) : services.length === 0 ? <p className="client-booking-empty">Este negocio todavía no tiene servicios activos para reservar.</p> : (
        <>
          <div className="client-booking-grid">
            <div className="client-booking-fields">
              <fieldset className="client-booking-fieldset">
                <legend><span>1</span> Servicio</legend>
                <div className="client-booking-service-options">
                  {services.map((service) => {
                    const selected = servicioId === String(service.servicioId)
                    return (
                      <button
                        className={`client-booking-service-option${selected ? ' is-selected' : ''}`}
                        type="button"
                        key={service.servicioId}
                        aria-pressed={selected}
                        onClick={() => { setServicioId(String(service.servicioId)); clearSlot() }}
                      >
                        <span className="client-booking-option-check">{selected && <Check size={14} aria-hidden="true" />}</span>
                        <span className="client-booking-service-copy"><strong>{present(service.nombre) || 'Servicio'}</strong>{present(service.descripcion) && <small>{service.descripcion}</small>}</span>
                        <span className="client-booking-service-meta"><strong>{service.precio !== null && service.precio !== undefined && Number.isFinite(Number(service.precio)) ? currencyFormatter.format(Number(service.precio)) : 'Precio no disponible'}</strong>{service.duracionMinutos !== null && service.duracionMinutos !== undefined && <small>{service.duracionMinutos} min</small>}</span>
                      </button>
                    )
                  })}
                </div>
              </fieldset>

              <fieldset className="client-booking-fieldset">
                <legend><span>2</span> Profesional</legend>
                <label className="client-booking-select-wrap"><UserRound size={16} aria-hidden="true" /><select value={empleadoId} onChange={(event) => { setEmpleadoId(event.target.value); clearSlot() }} aria-label="Elegir profesional">
                  <option value="any">Cualquier profesional</option>
                  {employees.map((employee) => <option key={employee.empleadoId} value={String(employee.empleadoId)}>{[present(employee.nombre), present(employee.apellido)].filter(Boolean).join(' ') || 'Profesional'}</option>)}
                </select></label>
                {employeesQuery.isError && <small className="client-booking-field-note">No pudimos cargar el equipo. Podés consultar con cualquier profesional.</small>}
              </fieldset>

              <fieldset className="client-booking-fieldset">
                <legend><span>3</span> Fecha</legend>
                <label className="client-booking-date-wrap"><CalendarDays size={16} aria-hidden="true" /><input type="date" value={fecha} min={today} onChange={(event) => { setFecha(event.target.value); clearSlot() }} aria-label="Elegir fecha" /></label>
                {fecha && <small className="client-booking-date-readable">{displayDate(fecha)}</small>}
              </fieldset>
            </div>

            <div className="client-booking-slot-panel" aria-live="polite">
              <div className="client-booking-slots-heading"><Clock3 size={17} aria-hidden="true" /><div><h3>Horarios disponibles</h3><p>{fecha ? displayDate(fecha) : 'Elegí una fecha'}</p></div></div>
              {!selectedService || !fecha ? <p className="client-booking-empty">Elegí un servicio y una fecha para consultar los horarios.</p> : availability.isPending ? (
                <div className="client-booking-state"><LoaderCircle size={18} aria-hidden="true" /> Buscando horarios…</div>
              ) : availability.isError ? (
                <div className="client-booking-error" role="alert"><AlertCircle size={17} /><span>{errorMessage(availability.error)}</span><button type="button" onClick={() => availability.refetch()}>Reintentar</button></div>
              ) : availability.data?.length ? (
                <div className="client-booking-slots">
                  {availability.data.map((slot, index) => {
                    const selected = selectedSlot === slot
                    return <button className={`client-booking-slot${selected ? ' is-selected' : ''}`} type="button" key={`${slot.fechaHoraInicio}-${slot.empleadoId}-${index}`} aria-pressed={selected} onClick={() => { setSlotSelection({ selectionKey, slot }); setContinued(false) }}>
                      {typeof slot.fechaHoraInicio === 'string' ? slot.fechaHoraInicio.slice(11, 16) : '—'}
                    </button>
                  })}
                </div>
              ) : <p className="client-booking-empty">No hay horarios disponibles para esta selección. Probá con otra fecha o profesional.</p>}
            </div>
          </div>

          {selectedSlot && selectedService && (
            <div className="client-booking-summary" aria-live="polite">
              <div className="client-booking-summary-copy"><span className="client-booking-summary-icon"><Sparkles size={17} aria-hidden="true" /></span><div><strong>Tu selección</strong><p>{present(selectedService.nombre) || 'Servicio'} · {displayDate(fecha)} · {selectedSlot.fechaHoraInicio.slice(11, 16)}{assignedEmployeeName ? ` · ${assignedEmployeeName}` : ''}</p><small>{selectedService.precio !== null && selectedService.precio !== undefined && Number.isFinite(Number(selectedService.precio)) ? currencyFormatter.format(Number(selectedService.precio)) : 'Precio no disponible'}{selectedService.duracionMinutos !== null && selectedService.duracionMinutos !== undefined ? ` · ${selectedService.duracionMinutos} min` : ''}</small></div></div>
              <button className="client-booking-continue" type="button" onClick={() => setContinued(true)}>Continuar</button>
              {continued && <p className="client-booking-next-step" role="status">Tu selección está lista. En el siguiente paso podrás revisar y confirmar el turno.</p>}
            </div>
          )}
        </>
      )}
        </div>
      </div>
    </section>
  )
}
