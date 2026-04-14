import { useEffect, useState } from 'react';
import api from '../api';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts';
import { IndianRupee, MessageSquareWarning, Package, ShoppingBag } from 'lucide-react';

export default function Dashboard() {
  const [data, setData] = useState(null);

  const fetchAnalytics = async () => {
    try {
      const res = await api.get('/dashboard/analytics');
      setData(res.data);
    } catch (e) { console.error(e); }
  };

  useEffect(() => {
    fetchAnalytics();
  }, []);

  // Real-time updates for new orders
  useEffect(() => {
    let eventSource;
    const token = localStorage.getItem('token');
    if (token) {
      eventSource = new EventSource(`/api/dashboard/sse/events?token=${token}`);
      eventSource.addEventListener('order', (event) => {
        try {
          console.log('New order received via SSE in Dashboard:', event.data);
          fetchAnalytics(); // Refresh analytics for live numbers
        } catch (e) {
          console.error(e);
        }
      });
      eventSource.onerror = (e) => console.warn('SSE connection error in Dashboard', e);
    }
    return () => eventSource && eventSource.close();
  }, []);

  if (!data) return <div style={{ padding: '2rem' }}>Loading Analytics...</div>;

  const chartData = [
    { name: '1 Day', revenue: data.revenue1d || 0, orders: data.orders1d },
    { name: '7 Days', revenue: data.revenue7d || 0, orders: data.orders7d },
    { name: '30 Days', revenue: data.revenue30d || 0, orders: data.orders30d },
  ];

  return (
    <div>
      <h1 style={{ fontSize: '2rem', marginBottom: '2rem' }}>Analytics Overview</h1>
      
      <div className="grid-cards">
        <div className="glass-card" style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <div style={{ background: 'rgba(34, 197, 94, 0.2)', padding: '1rem', borderRadius: '50%', color: '#4ade80' }}>
            <IndianRupee size={24} />
          </div>
          <div>
            <p style={{ margin: 0 }}>Revenue (30d)</p>
            <h2 style={{ margin: 0, fontSize: '1.8rem' }}>₹{data.revenue30d || 0}</h2>
          </div>
        </div>

        <div className="glass-card" style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <div style={{ background: 'rgba(56, 189, 248, 0.2)', padding: '1rem', borderRadius: '50%', color: '#38bdf8' }}>
            <ShoppingBag size={24} />
          </div>
          <div>
            <p style={{ margin: 0 }}>Orders (30d)</p>
            <h2 style={{ margin: 0, fontSize: '1.8rem' }}>{data.orders30d}</h2>
          </div>
        </div>

        <div className="glass-card" style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <div style={{ background: 'rgba(239, 68, 68, 0.2)', padding: '1rem', borderRadius: '50%', color: '#ef4444' }}>
            <MessageSquareWarning size={24} />
          </div>
          <div>
            <p style={{ margin: 0 }}>Blocked Messages</p>
            <h2 style={{ margin: 0, fontSize: '1.8rem' }}>{data.guardrailTriggers}</h2>
          </div>
        </div>
      </div>

      <div className="glass-card" style={{ marginTop: '2rem', height: '400px', minWidth: 0, minHeight: 0 }}>
        <h3 style={{ marginBottom: '1.5rem' }}>Revenue Chart</h3>
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={chartData}>
            <XAxis dataKey="name" stroke="var(--color-text-dim)" />
            <YAxis stroke="var(--color-text-dim)" />
            <Tooltip 
              contentStyle={{ background: 'var(--color-bg)', border: '1px solid var(--color-glass-border)', borderRadius: '8px' }}
              itemStyle={{ color: 'var(--color-text)' }}
            />
            <Bar dataKey="revenue" fill="var(--color-primary)" radius={[4, 4, 0, 0]} />
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
