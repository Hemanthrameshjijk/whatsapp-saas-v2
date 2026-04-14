import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { useEffect, useState } from 'react';
import { ShoppingCart, Megaphone, UserPlus, Info, X, Zap } from 'lucide-react';
import Sidebar from './components/Sidebar';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Products from './pages/Products';
import Orders from './pages/Orders';
import Customers from './pages/Customers';
import Conversations from './pages/Conversations';
import SupportTickets from './pages/SupportTickets';
import Promotions from './pages/Promotions';
import Marketing from './pages/Marketing';
import Settings from './pages/Settings';
import Simulator from './pages/Simulator';
import './index.css';

const ProtectedRoute = ({ children }) => {
  const { isAuthenticated } = useAuth();
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  return (
    <div className="app-container">
      <Sidebar />
      <div className="main-content">
        {children}
      </div>
    </div>
  );
};

function AppRoutes() {
  const [toasts, setToasts] = useState([]);

  useEffect(() => {
    let eventSource;
    const token = localStorage.getItem('token');
    if (token) {
      eventSource = new EventSource(`/api/dashboard/sse/events?token=${token}`);
      
      const handleEvent = (event) => {
        try {
          const data = JSON.parse(event.data);
          const toastId = Date.now();
          
          // Only show toasts for marketing and orders
          if (data.type === 'marketing' || data.type === 'new_order') {
            const newToast = {
              id: toastId,
              title: data.title || (data.type === 'new_order' ? 'New Order!' : 'Growth Alert'),
              description: data.description || (data.type === 'new_order' ? `Order from ${data.customer}` : 'Something happened'),
              icon: data.icon || (data.type === 'new_order' ? 'ShoppingCart' : 'Megaphone'),
              type: data.type
            };
            
            setToasts(prev => [newToast, ...prev]);
            
            // Auto remove
            setTimeout(() => {
              setToasts(prev => prev.filter(t => t.id !== toastId));
            }, 5000);
          }
        } catch (e) { console.error('SSE Error', e); }
      };

      eventSource.addEventListener('message', handleEvent);
      eventSource.addEventListener('marketing', handleEvent);
      eventSource.addEventListener('order', handleEvent);
    }
    return () => eventSource && eventSource.close();
  }, []);

  const getIcon = (iconName) => {
    switch(iconName) {
      case 'ShoppingCart': return <ShoppingCart size={20} />;
      case 'UserPlus': return <UserPlus size={20} />;
      case 'Link': return <Zap size={20} />;
      default: return <Megaphone size={20} />;
    }
  };

  return (
    <>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/" element={<ProtectedRoute><Dashboard /></ProtectedRoute>} />
        <Route path="/products" element={<ProtectedRoute><Products /></ProtectedRoute>} />
        <Route path="/orders" element={<ProtectedRoute><Orders /></ProtectedRoute>} />
        <Route path="/customers" element={<ProtectedRoute><Customers /></ProtectedRoute>} />
        <Route path="/conversations" element={<ProtectedRoute><Conversations /></ProtectedRoute>} />
        <Route path="/tickets" element={<ProtectedRoute><SupportTickets /></ProtectedRoute>} />
        <Route path="/promotions" element={<ProtectedRoute><Promotions /></ProtectedRoute>} />
        <Route path="/marketing" element={<ProtectedRoute><Marketing /></ProtectedRoute>} />
        <Route path="/settings" element={<ProtectedRoute><Settings /></ProtectedRoute>} />
        <Route path="/simulator" element={<ProtectedRoute><Simulator /></ProtectedRoute>} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>

      {/* Global Toast Container */}
      <div className="toast-container" style={{ position: 'fixed', bottom: '2rem', right: '2rem', zIndex: 1000, display: 'flex', flexDirection: 'column', gap: '1rem' }}>
        {toasts.map(t => (
          <div key={t.id} className="glass-card toast-entry" style={{ 
            minWidth: '300px', padding: '1rem', background: 'rgba(255,255,255,0.1)', 
            backdropFilter: 'blur(20px)', border: '1px solid var(--color-glass-border)',
            display: 'flex', gap: '1rem', alignItems: 'center', boxShadow: '0 10px 40px rgba(0,0,0,0.3)',
            animation: 'toastIn 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275)'
          }}>
            <div style={{ padding: '0.6rem', background: t.type === 'new_order' ? 'var(--color-secondary-glow)' : 'var(--color-primary-glow)', borderRadius: '10px' }}>
              {getIcon(t.icon)}
            </div>
            <div style={{ flex: 1 }}>
              <div style={{ fontWeight: 600, fontSize: '14px' }}>{t.title}</div>
              <div style={{ fontSize: '12px', opacity: 0.7 }}>{t.description}</div>
            </div>
            <button 
              onClick={() => setToasts(prev => prev.filter(toast => toast.id !== t.id))}
              style={{ background: 'transparent', border: 'none', color: 'var(--color-text-dim)', cursor: 'pointer' }}
            >
              <X size={16} />
            </button>
          </div>
        ))}
      </div>

      <style>{`
        @keyframes toastIn {
          from { transform: translateX(100%) scale(0.8); opacity: 0; }
          to { transform: translateX(0) scale(1); opacity: 1; }
        }
      `}</style>
    </>
  );
}

function App() {
  return (
    <AuthProvider>
      <div className="ambient-bg">
        <div className="ambient-blob primary"></div>
        <div className="ambient-blob secondary"></div>
      </div>
      <Router>
        <AppRoutes />
      </Router>
    </AuthProvider>
  );
}

export default App;
