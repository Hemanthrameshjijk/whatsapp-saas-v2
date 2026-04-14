import { Link, useLocation } from 'react-router-dom';
import { LayoutDashboard, ShoppingCart, Users, Settings, Package, MessageSquare, LogOut, ClipboardCheck, Tag, Megaphone, Phone } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export default function Sidebar() {
  const { pathname } = useLocation();
  const { logout } = useAuth();

  const links = [
    { name: 'Analytics', path: '/', icon: LayoutDashboard },
    { name: 'Conversations', path: '/conversations', icon: MessageSquare },
    { name: 'Orders', path: '/orders', icon: ShoppingCart },
    { name: 'Tickets', path: '/tickets', icon: ClipboardCheck },
    { name: 'Promotions', path: '/promotions', icon: Tag },
    { name: 'Products', path: '/products', icon: Package },
    { name: 'Customers', path: '/customers', icon: Users },
    { name: 'Marketing', path: '/marketing', icon: Megaphone },
    { name: 'Settings', path: '/settings', icon: Settings },
    { name: 'Simulator', path: '/simulator', icon: Phone },
  ];

  return (
    <nav className="sidebar glass-panel" style={{ height: '100vh', position: 'fixed', borderRadius: 0, borderTop: 0, borderBottom: 0, borderLeft: 0 }}>
      <div className="brand">WhatsApp AI</div>
      <div className="nav-menu">
        {links.map((link) => {
          const Icon = link.icon;
          const isActive = pathname === link.path;
          return (
            <Link key={link.name} to={link.path} className={`nav-item ${isActive ? 'active' : ''}`}>
              <Icon size={20} />
              <span>{link.name}</span>
            </Link>
          );
        })}
      </div>
      
      <div style={{ marginTop: 'auto', padding: '1rem', borderTop: '1px solid var(--color-glass-border)' }}>
        <button className="glass-button danger" style={{ width: '100%' }} onClick={logout}>
          <LogOut size={18} /> Logout
        </button>
      </div>
    </nav>
  );
}
