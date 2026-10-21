import { LoaderCircle, Power, PowerOff, ShieldAlert } from 'lucide-react'

export default function BusinessStatusActions({ business, pending, onChangeStatus }) {
  const actions = {
    ACTIVE: [
      ['suspend', 'Suspender', ShieldAlert],
      ['deactivate', 'Desactivar', PowerOff],
    ],
    SUSPENDED: [
      ['reactivate', 'Reactivar', Power],
      ['deactivate', 'Desactivar', PowerOff],
    ],
    INACTIVE: [['reactivate', 'Reactivar', Power]],
  }[business.status] || []

  return <div className="businesses-row-actions">
    {actions.map(([action, label, Icon]) => (
      <button key={action} type="button" className="owners-secondary-button owner-status-action" onClick={() => onChangeStatus(business, action)} disabled={pending} aria-label={`${label} el negocio ${business.nombre}`}>
        {pending ? <LoaderCircle size={14} className="spin" /> : <Icon size={14} />} {label}
      </button>
    ))}
  </div>
}
