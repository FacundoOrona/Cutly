import { Outlet } from 'react-router-dom'

export default function AppLayout() {
  return (
    <div className="app-shell">
      <Outlet />
    </div>
  )
}
