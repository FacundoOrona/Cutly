import { Scissors } from 'lucide-react'

export default function BrandMark({ light = false }) {
  return (
    <a className={`brand ${light ? 'brand-light' : ''}`} href="/dashboard" aria-label="Cutly, inicio">
      <span className="brand-icon"><Scissors size={19} strokeWidth={2.4} /></span>
      <span>cutly<span className="brand-dot">.</span></span>
    </a>
  )
}
