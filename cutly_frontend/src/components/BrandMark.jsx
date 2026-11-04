export default function BrandMark({ light = false, to = '/dashboard' }) {
  return (
    <a className={`brand ${light ? 'brand-light' : ''}`} href={to} aria-label="Cutly, inicio">
      <span className="brand-icon" aria-hidden="true">
        <svg className="brand-symbol" viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M23.8 7.9a11 11 0 1 0 0 16.2" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" />
          <path d="M11 16h10m0 0-3.2-3.2M21 16l-3.2 3.2" stroke="currentColor" strokeWidth="2.1" strokeLinecap="round" strokeLinejoin="round" />
          <circle cx="24.5" cy="7.5" r="2.5" fill="currentColor" />
          <circle cx="24.5" cy="24.5" r="2.5" fill="currentColor" />
        </svg>
      </span>
      <span>cutly<span className="brand-dot">.</span></span>
    </a>
  )
}
