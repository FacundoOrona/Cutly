import { MapPin } from 'lucide-react'
import { Link } from 'react-router-dom'

export default function ClientBusinessCard({ business, catalogState }) {
  const cityAndProvince = [business.ciudad, business.provincia].filter(Boolean).join(', ')
  const address = [business.calle, business.numero].filter(Boolean).join(' ')

  return (
    <Link className="client-business-card" to={`/client/negocios/${business.negocioId}`} state={{ catalogState }} aria-label={`Ver detalle de ${business.nombre || 'negocio'}`}>
      <div className="client-business-card-mark" aria-hidden="true">
        {business.nombre?.trim()?.charAt(0)?.toUpperCase() || 'C'}
      </div>
      <div className="client-business-card-content">
        <h2>{business.nombre || 'Negocio'}</h2>
        {business.descripcion?.trim() && <p className="client-business-description">{business.descripcion}</p>}
        {(cityAndProvince || address) && (
          <div className="client-business-location">
            <MapPin size={15} aria-hidden="true" />
            <span>{[address, cityAndProvince].filter(Boolean).join(' · ')}</span>
          </div>
        )}
      </div>
    </Link>
  )
}
